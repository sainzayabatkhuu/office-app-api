package com.sol.office.report.service;

import com.sol.office.common.event.NotificationEvent;
import com.sol.office.common.exception.OfficeException;
import com.sol.office.report.config.ReportProperties;
import com.sol.office.report.domain.ExecutionMode;
import com.sol.office.report.domain.ExecutionStatus;
import com.sol.office.report.domain.ReportExecution;
import com.sol.office.report.domain.ReportFormat;
import com.sol.office.report.domain.ReportParameter;
import com.sol.office.report.engine.JasperReportEngine;
import com.sol.office.report.engine.ParameterConverter;
import com.sol.office.report.engine.RenderedReport;
import com.sol.office.report.engine.ReportGenerationException;
import com.sol.office.report.messaging.ReportMessagePublisher;
import com.sol.office.report.repository.ReportExecutionRepository;
import com.sol.office.report.storage.FileStorage;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Runs one execution: PENDING -> PROCESSING -> SUCCESS/FAILED. Database state changes happen in short
 * transactions; the (possibly minutes long) Jasper fill runs outside any transaction.
 */
@Slf4j
@Service
public class ReportExecutor {

    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneOffset.ofHours(8));
    private static final DateTimeFormatter DIR_DATE = DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneOffset.UTC);

    private final ReportExecutionRepository executionRepository;
    private final JasperReportEngine engine;
    private final FileStorage storage;
    private final ReportMessagePublisher publisher;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final MeterRegistry meterRegistry;
    private final Semaphore slots;
    private final Duration syncWait;

    public ReportExecutor(ReportExecutionRepository executionRepository, JasperReportEngine engine, FileStorage storage,
                          ReportMessagePublisher publisher, TransactionTemplate tx, Clock clock,
                          MeterRegistry meterRegistry, ReportProperties properties) {
        this.executionRepository = executionRepository;
        this.engine = engine;
        this.storage = storage;
        this.publisher = publisher;
        this.tx = tx;
        this.clock = clock;
        this.meterRegistry = meterRegistry;
        this.slots = new Semaphore(properties.execution().maxConcurrent(), true);
        this.syncWait = properties.execution().syncTimeout();
    }

    /** What the fill needs, captured in a short transaction so no entity is used outside it. */
    private record Job(String reportCode, String reportTitle, String templateKey, ReportFormat format,
                       ExecutionMode mode, String requestedBy, Map<String, Object> parameters) {
    }

    /**
     * Executes a queued (ASYNC) or immediate (SYNC) execution. Safe to call twice for the same id
     * (message redelivery): only a PENDING execution is started.
     */
    public void execute(String executionId) {
        boolean sync = tx.execute(s -> executionRepository.findById(executionId)
                .map(e -> e.getMode() == ExecutionMode.SYNC).orElse(false));
        acquireSlot(executionId, sync);
        try {
            Job job = start(executionId);
            if (job == null) {
                return;
            }
            run(executionId, job);
        } finally {
            slots.release();
        }
    }

    private void acquireSlot(String executionId, boolean sync) {
        try {
            if (!sync) {
                slots.acquire();
                return;
            }
            if (!slots.tryAcquire(syncWait.toMillis(), TimeUnit.MILLISECONDS)) {
                fail(executionId, "Report server is busy");
                throw new OfficeException(HttpStatus.SERVICE_UNAVAILABLE, "REPORT_BUSY",
                        "All report slots are busy. Try again later or run the report in the background.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for a report slot", e);
        }
    }

    private Job start(String executionId) {
        return tx.execute(status -> {
            ReportExecution execution = executionRepository.findById(executionId).orElse(null);
            if (execution == null || execution.getStatus() != ExecutionStatus.PENDING) {
                log.info("Skipping execution {}: {}", executionId, execution == null ? "not found" : execution.getStatus());
                return null;
            }
            execution.setStatus(ExecutionStatus.PROCESSING);
            execution.setStartedAt(clock.instant());
            execution.setTemplateVersion(execution.getReport().getTemplateVersion());

            Map<String, String> raw = execution.getParameters() == null ? Map.of() : execution.getParameters();
            Map<String, Object> typed = new HashMap<>();
            for (ReportParameter p : execution.getReport().getParameters()) {
                Object value = ParameterConverter.convert(p.getName(), raw.get(p.getName()), p.getJavaType());
                if (value != null) {
                    typed.put(p.getName(), value);
                }
            }
            return new Job(execution.getReportCode(), execution.getReportTitle(),
                    execution.getReport().getTemplateStorageKey(), execution.getFormat(), execution.getMode(),
                    execution.getRequestedBy(), typed);
        });
    }

    private void run(String executionId, Job job) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "success";
        try {
            RenderedReport rendered = engine.render(job.templateKey(), job.parameters(), job.format());
            Instant now = clock.instant();
            String key = "outputs/" + DIR_DATE.format(now) + "/" + executionId + "." + job.format().extension();
            storage.store(key, new ByteArrayInputStream(rendered.content()));
            String fileName = job.reportCode() + "_" + FILE_TS.format(now) + "." + job.format().extension();

            tx.executeWithoutResult(s -> executionRepository.findById(executionId).ifPresent(e -> {
                e.setStatus(ExecutionStatus.SUCCESS);
                e.setOutputStorageKey(key);
                e.setOutputFileName(fileName);
                e.setOutputSize((long) rendered.content().length);
                e.setPageCount(rendered.pageCount());
                finish(e);
            }));
            if (job.mode() == ExecutionMode.ASYNC) {
                publisher.notifyUser(NotificationEvent.of("report-service", job.requestedBy(), "SUCCESS", "REPORT",
                        "Тайлан бэлэн боллоо", job.reportTitle() + " тайлан амжилттай үүслээ.",
                        "/report-history?execution=" + executionId,
                        Map.of("executionId", executionId, "fileName", fileName)));
            }
        } catch (ReportGenerationException e) {
            outcome = "failed";
            log.warn("Execution {} of {} failed: {}", executionId, job.reportCode(), e.getMessage(), e);
            failAndNotify(executionId, job, e.getMessage());
        } catch (RuntimeException e) {
            outcome = "error";
            log.error("Execution {} of {} crashed", executionId, job.reportCode(), e);
            failAndNotify(executionId, job, "Unexpected error while generating the report");
        } finally {
            sample.stop(Timer.builder("office.report.execution")
                    .description("Report generation time")
                    .tag("report", job.reportCode())
                    .tag("format", job.format().name())
                    .tag("outcome", outcome)
                    .register(meterRegistry));
        }
    }

    private void failAndNotify(String executionId, Job job, String message) {
        fail(executionId, message);
        if (job.mode() == ExecutionMode.ASYNC) {
            publisher.notifyUser(NotificationEvent.of("report-service", job.requestedBy(), "ERROR", "REPORT",
                    "Тайлан үүсгэж чадсангүй", job.reportTitle() + ": " + message,
                    "/report-history?execution=" + executionId, Map.of("executionId", executionId)));
        }
    }

    void fail(String executionId, String message) {
        tx.executeWithoutResult(s -> executionRepository.findById(executionId).ifPresent(e -> {
            e.setStatus(ExecutionStatus.FAILED);
            e.setErrorMessage(message != null && message.length() > 1000 ? message.substring(0, 1000) : message);
            finish(e);
        }));
    }

    private void finish(ReportExecution e) {
        Instant now = clock.instant();
        e.setFinishedAt(now);
        if (e.getStartedAt() != null) {
            e.setDurationMs(Duration.between(e.getStartedAt(), now).toMillis());
        }
    }
}
