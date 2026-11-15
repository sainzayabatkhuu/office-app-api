package com.sol.office.report.web;

import com.sol.office.common.api.PageResponse;
import com.sol.office.common.security.SecurityUtils;
import com.sol.office.report.domain.ExecutionStatus;
import com.sol.office.report.dto.ReportDtos.ExecutionResponse;
import com.sol.office.report.service.ReportExecutionService;
import com.sol.office.report.service.ReportExecutionService.Download;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Tag(name = "Report history")
@RestController
@RequestMapping("/api/v1/report-executions")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('REPORT_VIEW')")
public class ReportExecutionController {

    private final ReportExecutionService executionService;

    @Operation(summary = "Report history; all=true (REPORT_HISTORY_ALL) includes other users")
    @GetMapping
    public PageResponse<ExecutionResponse> search(
            @RequestParam(defaultValue = "false") boolean all,
            @RequestParam(required = false) String requestedBy,
            @RequestParam(required = false) String reportCode,
            @RequestParam(required = false) ExecutionStatus status,
            @Parameter(description = "ISO-8601 instant") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @Parameter(description = "ISO-8601 instant, exclusive") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20, sort = "requestedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return executionService.search(SecurityUtils.requireCurrentUser(), all, requestedBy, reportCode, status, from, to, pageable);
    }

    @GetMapping("/{id}")
    public ExecutionResponse get(@PathVariable String id) {
        return executionService.get(SecurityUtils.requireCurrentUser(), id);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable String id) {
        Download download = executionService.download(SecurityUtils.requireCurrentUser(), id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(download.fileName(), StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType(download.contentType()))
                .body(download.resource());
    }
}
