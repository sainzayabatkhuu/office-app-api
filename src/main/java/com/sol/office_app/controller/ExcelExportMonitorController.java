package com.sol.office_app.controller;

import com.sol.office_app.config.CustomUserPrincipal;
import com.sol.office_app.dto.ExportSummaryDTO;
import com.sol.office_app.service.ReportHistoryService;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PathVariable;
import java.nio.charset.StandardCharsets;

import com.sol.office_app.common.Constant;
import com.sol.office_app.dto.ExportMonitorDTO;
import com.sol.office_app.domain.response.NotificationMessage;
import com.sol.office_app.service.ExportMonitorService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping(Constant.EXCEL_EXPORT_URL_PREFIX)
@RestController
public class ExcelExportMonitorController {

    private final ExportMonitorService exportMonitorService;

    public ExcelExportMonitorController(ExportMonitorService exportMonitorService) {
        this.exportMonitorService = exportMonitorService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<ExportMonitorDTO> index(
            @RequestParam(defaultValue = "") String referenceNumber,
            @RequestParam(defaultValue = "") String username,
            @RequestParam(defaultValue = "") String functionId,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String startDate,
            @RequestParam(defaultValue = "") String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size){
        return exportMonitorService.findDynamic(referenceNumber, username, functionId, status, startDate, endDate,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestedTime")));
    }

    @GetMapping(value = "/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ExportSummaryDTO summary(@RequestParam(defaultValue = "") String startDate,
                                    @RequestParam(defaultValue = "") String endDate) {
        return exportMonitorService.summary(startDate, endDate);
    }

    /** The exported workbook (the user who exported it, or users who may monitor exports). */
    @GetMapping("/{id:\\d+}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id, Authentication authentication) {
        ReportHistoryService.Download download =
                exportMonitorService.download(id, (CustomUserPrincipal) authentication.getPrincipal());
        return ResponseEntity.ok()
                .contentType(download.mediaType())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(download.fileName(), StandardCharsets.UTF_8).build().toString())
                .body(download.resource());
    }

    @PostMapping("/export")
    public ResponseEntity<NotificationMessage> exportToExcel(@RequestBody List<String[]> data,
                                                             @RequestParam String functionId,
                                                             Authentication authentication
                              ) {
        return new ResponseEntity<>(exportMonitorService.exportDataToExcel(data, authentication, functionId), HttpStatus.OK);
    }
}
