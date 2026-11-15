package com.sol.office.report.web;

import com.sol.office.common.api.PageResponse;
import com.sol.office.common.security.SecurityUtils;
import com.sol.office.report.dto.ExportDtos.ExportMonitorResponse;
import com.sol.office.report.dto.ExportDtos.ExportResult;
import com.sol.office.report.dto.ExportDtos.ExportSummaryResponse;
import com.sol.office.report.service.ExportMonitorService;
import com.sol.office.report.service.ReportExecutionService.Download;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Tag(name = "Excel exports")
@RestController
@RequestMapping("/api/v1/export-monitor")
@RequiredArgsConstructor
public class ExportMonitorController {

    private final ExportMonitorService service;

    @Operation(summary = "Exports of all users, newest first")
    @GetMapping
    @PreAuthorize("hasAuthority('EXPORT_MONITOR')")
    public PageResponse<ExportMonitorResponse> search(
            @RequestParam(required = false) String referenceNumber,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String functionId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @PageableDefault(size = 10, sort = "requestedTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.search(referenceNumber, username, functionId, status, startDate, endDate, pageable);
    }

    @Operation(summary = "Totals by status, function and user; defaults to the last 30 days")
    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('EXPORT_MONITOR')")
    public ExportSummaryResponse summary(@RequestParam(required = false) String startDate,
                                         @RequestParam(required = false) String endDate) {
        return service.summary(startDate, endDate);
    }

    @Operation(summary = "The workbook (own exports, or any with EXPORT_MONITOR)")
    @GetMapping("/{id:\\d+}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        Download download = service.download(SecurityUtils.requireCurrentUser(), id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(download.fileName(), StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType(download.contentType()))
                .body(download.resource());
    }

    @Operation(summary = "Export grid rows to Excel; the first row is the header")
    @PostMapping("/export")
    public ExportResult export(@RequestParam String functionId, @RequestBody List<String[]> rows) {
        return service.export(SecurityUtils.requireCurrentUser(), functionId, rows);
    }
}
