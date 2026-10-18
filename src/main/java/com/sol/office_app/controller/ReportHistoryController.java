package com.sol.office_app.controller;

import com.sol.office_app.common.Constant;
import com.sol.office_app.config.CustomUserPrincipal;
import com.sol.office_app.dto.ReportHistoryDto;
import com.sol.office_app.service.ReportHistoryService;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping(Constant.REPORT_HISTORY_URL_PREFIX)
public class ReportHistoryController {
    private final ReportHistoryService reportHistoryService;

    public ReportHistoryController(ReportHistoryService reportHistoryService) {
        this.reportHistoryService = reportHistoryService;
    }

    @GetMapping
    public Page<ReportHistoryDto> getAll(
            @RequestParam(defaultValue = "") String executedBy,
            @RequestParam(defaultValue = "") String branch,
            @RequestParam(defaultValue = "") String startDate,
            @RequestParam(defaultValue = "") String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return reportHistoryService.findDynamic(executedBy, branch, startDate, endDate,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startedAt")));
    }

    /** The generated report file (owner, or users who may view report histories). */
    @GetMapping("/{id:\\d+}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id, Authentication authentication) {
        ReportHistoryService.Download download =
                reportHistoryService.download(id, (CustomUserPrincipal) authentication.getPrincipal());
        return ResponseEntity.ok()
                .contentType(download.mediaType())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(download.fileName(), StandardCharsets.UTF_8).build().toString())
                .body(download.resource());
    }
}
