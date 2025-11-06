package com.sol.office_app.controller;

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
            @RequestParam(defaultValue = "") String fetchSize,
            @RequestParam(defaultValue = "") String functionId,
            @RequestParam(defaultValue = "") String requestedTime,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size){
        return exportMonitorService.findDynamic(
                referenceNumber,
                username,
                fetchSize,
                functionId,
                requestedTime,
                status,
                PageRequest.of(page, size));
    }

    @PostMapping("/export")
    public ResponseEntity<NotificationMessage> exportToExcel(@RequestBody List<String[]> data,
                                                             @RequestParam String functionId,
                                                             Authentication authentication
                              ) {
        return new ResponseEntity<>(exportMonitorService.exportDataToExcel(data, authentication, functionId), HttpStatus.OK);
    }
}
