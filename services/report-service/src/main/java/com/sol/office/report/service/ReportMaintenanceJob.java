package com.sol.office.report.service;

import com.sol.office.report.config.ReportProperties;
import com.sol.office.report.domain.ExecutionStatus;
import com.sol.office.report.domain.ReportExecution;
import com.sol.office.report.repository.ReportExecutionRepository;
import com.sol.office.report.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Housekeeping. Both jobs are idempotent, so running them on every instance is harmless
 * (add ShedLock if they ever become expensive).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportMaintenanceJob {

    private final ReportExecutionRepository executionRepository;
    private final FileStorage storage;
    private final ReportProperties properties;
    private final TransactionTemplate tx;
    private final Clock clock;

    /** Deletes generated files past the retention period; the history row stays (status EXPIRED). */
    @Scheduled(cron = "${office.report.execution.retention-cron:0 15 3 * * *}")
    public void expireOldOutputs() {
        Instant cutoff = clock.instant().minus(Duration.ofDays(properties.execution().outputRetentionDays()));
        int total = 0;
        List<ReportExecution> batch;
        do {
            batch = executionRepository.findTop200ByStatusAndFinishedAtBefore(ExecutionStatus.SUCCESS, cutoff);
            for (ReportExecution e : batch) {
                storage.delete(e.getOutputStorageKey());
                tx.executeWithoutResult(s -> executionRepository.findById(e.getId()).ifPresent(x -> {
                    x.setStatus(ExecutionStatus.EXPIRED);
                    x.setOutputStorageKey(null);
                }));
            }
            total += batch.size();
        } while (!batch.isEmpty());
        if (total > 0) {
            log.info("Expired {} report outputs older than {}", total, cutoff);
        }
    }

    /** Executions abandoned by a crashed instance would otherwise stay PROCESSING forever. */
    @Scheduled(fixedDelayString = "PT10M", initialDelayString = "PT1M")
    public void failStaleExecutions() {
        Instant now = clock.instant();
        Integer count = tx.execute(s -> executionRepository.failStale(
                List.of(ExecutionStatus.PENDING, ExecutionStatus.PROCESSING), ExecutionStatus.FAILED,
                now.minus(properties.execution().staleAfter()), now, "Abandoned: the report server stopped while processing"));
        if (count != null && count > 0) {
            log.warn("Marked {} stale report executions as FAILED", count);
        }
    }
}
