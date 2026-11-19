package com.sol.office.report.service;

import com.sol.office.common.api.PageResponse;
import com.sol.office.common.exception.BusinessException;
import com.sol.office.common.exception.ForbiddenException;
import com.sol.office.common.exception.NotFoundException;
import com.sol.office.common.security.CurrentUser;
import com.sol.office.report.domain.ExecutionMode;
import com.sol.office.report.domain.ExecutionStatus;
import com.sol.office.report.domain.ReportDefinition;
import com.sol.office.report.domain.ReportExecution;
import com.sol.office.report.domain.ReportFormat;
import com.sol.office.report.domain.ReportParameter;
import com.sol.office.report.domain.ReportPermission;
import com.sol.office.report.dto.ReportDtos.ExecuteRequest;
import com.sol.office.report.dto.ReportDtos.ExecutionResponse;
import com.sol.office.report.engine.ParameterConverter;
import com.sol.office.report.engine.SystemParameters;
import com.sol.office.report.mapper.ReportMapper;
import com.sol.office.report.messaging.ReportMessagePublisher;
import com.sol.office.report.repository.ReportDefinitionRepository;
import com.sol.office.report.repository.ReportExecutionRepository;
import com.sol.office.report.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportExecutionService {

    public static final String HISTORY_ALL = "REPORT_HISTORY_ALL";

    private final ReportDefinitionRepository definitionRepository;
    private final ReportExecutionRepository executionRepository;
    private final ReportExecutor executor;
    private final ReportMessagePublisher publisher;
    private final FileStorage storage;
    private final ReportMapper mapper;
    private final TransactionTemplate tx;
    private final Clock clock;

    public record Download(Resource resource, String fileName, String contentType) {
    }

    /**
     * Validates and records the request, then runs it now (SYNC) or queues it (ASYNC).
     * Not transactional itself: the execution row must be committed before a worker can pick it up.
     */
    public ExecutionResponse request(CurrentUser user, Long reportId, ExecuteRequest request) {
        ReportExecution created = tx.execute(s -> createExecution(user, reportId, request));
        String id = created.getId();

        if (created.getMode() == ExecutionMode.ASYNC) {
            try {
                publisher.requestExecution(id);
            } catch (AmqpException e) {
                log.error("Could not queue execution {}", id, e);
                executor.fail(id, "Could not queue the report; try again later");
                throw new BusinessException("QUEUE_UNAVAILABLE", "Background processing is unavailable right now");
            }
        } else {
            executor.execute(id);
        }
        return tx.execute(s -> mapper.toResponse(executionRepository.findById(id).orElseThrow()));
    }

    private ReportExecution createExecution(CurrentUser user, Long reportId, ExecuteRequest request) {
        ReportDefinition report = definitionRepository.findWithPermissionsById(reportId)
                .filter(ReportDefinition::isActive)
                .orElseThrow(() -> NotFoundException.of("Report", reportId));
        List<ReportPermission> applicable = ReportAccess.applicable(report, user);
        if (applicable.isEmpty()) {
            throw new ForbiddenException("You are not allowed to run this report");
        }

        ReportFormat format = request.format() != null ? request.format() : report.getDefaultFormat();
        if (!report.allowedFormatSet().contains(format)) {
            throw new BusinessException("FORMAT_NOT_ALLOWED", "Format " + format + " is not allowed for this report");
        }

        ReportExecution execution = new ReportExecution();
        execution.setId(UUID.randomUUID().toString());
        execution.setReport(report);
        execution.setReportCode(report.getCode());
        execution.setReportTitle(report.getTitle());
        execution.setTemplateVersion(report.getTemplateVersion());
        execution.setRequestedBy(user.username());
        execution.setBranchCode(user.branchCode());
        execution.setFormat(format);
        execution.setMode(ReportAccess.effectiveMode(report, applicable));
        execution.setStatus(ExecutionStatus.PENDING);
        execution.setParameters(resolveParameters(report, user,
                request.parameters() == null ? Map.of() : request.parameters()));
        execution.setRequestedAt(clock.instant());
        return executionRepository.save(execution);
    }

    /**
     * Merges client values, defaults and server-controlled values; validates them by converting once.
     * Unknown keys from the client are ignored.
     */
    Map<String, String> resolveParameters(ReportDefinition report, CurrentUser user, Map<String, String> supplied) {
        Map<String, String> resolved = new LinkedHashMap<>();
        for (ReportParameter p : report.getParameters()) {
            String name = p.getName();
            String value;
            if (SystemParameters.SERVER_ONLY.contains(name)) {
                value = switch (name) {
                    case SystemParameters.REPORT_TITLE -> report.getTitle();
                    case SystemParameters.MAKER -> user.username();
                    case SystemParameters.MAKER_NAME -> user.fullName();
                    default -> null;
                };
            } else if ((SystemParameters.BRANCH.equals(name) || ParameterConverter.isBranchList(p.getJavaType()))
                    && !user.multiBranchAccess()) {
                // single-branch users only ever see their own branch, whatever the client sends
                value = user.branchCode();
            } else if (ParameterConverter.isBranchList(p.getJavaType())) {
                // nothing chosen means all branches (BranchList treats ALL as no filter)
                value = supplied.get(name);
                if (!StringUtils.hasText(value)) {
                    value = StringUtils.hasText(p.getDefaultValue()) ? p.getDefaultValue() : "ALL";
                }
            } else {
                value = supplied.get(name);
                if (!StringUtils.hasText(value)) {
                    value = SystemParameters.BRANCH.equals(name) ? user.branchCode() : p.getDefaultValue();
                }
            }
            if (!StringUtils.hasText(value)) {
                if (p.isRequired()) {
                    throw new BusinessException("REQUIRED_PARAMETER", "'" + p.getLabel() + "' is required");
                }
                continue;
            }
            ParameterConverter.convert(p.getLabel(), value, p.getJavaType());
            resolved.put(name, value);
        }
        return resolved;
    }

    // ---- history

    @Transactional(readOnly = true)
    public PageResponse<ExecutionResponse> search(CurrentUser user, boolean all, String requestedBy, String reportCode,
                                                  ExecutionStatus status, Instant from, Instant to, Pageable pageable) {
        String owner;
        if (all) {
            if (!user.hasAuthority(HISTORY_ALL)) {
                throw new ForbiddenException("You can only see your own report history");
            }
            owner = StringUtils.hasText(requestedBy) ? requestedBy : null;
        } else {
            owner = user.username();
        }
        return PageResponse.of(executionRepository.search(owner, emptyToNull(reportCode), status, from, to, pageable),
                mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ExecutionResponse get(CurrentUser user, String id) {
        return mapper.toResponse(loadVisible(user, id));
    }

    @Transactional(readOnly = true)
    public Download download(CurrentUser user, String id) {
        ReportExecution execution = loadVisible(user, id);
        if (execution.getStatus() != ExecutionStatus.SUCCESS) {
            throw new BusinessException("OUTPUT_NOT_AVAILABLE", "Output is not available (status " + execution.getStatus() + ")");
        }
        return new Download(storage.load(execution.getOutputStorageKey()), execution.getOutputFileName(),
                execution.getFormat().contentType());
    }

    private ReportExecution loadVisible(CurrentUser user, String id) {
        ReportExecution execution = executionRepository.findById(id).orElseThrow(() -> NotFoundException.of("Execution", id));
        if (!execution.getRequestedBy().equals(user.username()) && !user.hasAuthority(HISTORY_ALL)) {
            // Same answer as a missing id: don't reveal that someone else's execution exists.
            throw NotFoundException.of("Execution", id);
        }
        return execution;
    }

    private static String emptyToNull(String s) {
        return StringUtils.hasText(s) ? s : null;
    }
}
