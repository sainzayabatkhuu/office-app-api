package com.sol.office_app.controller;

import com.sol.office_app.common.Constant;
import com.sol.office_app.dto.ExportMonitorDTO;
import com.sol.office_app.dto.NotificationMessage;
import com.sol.office_app.service.ExportMonitorService;
import org.springframework.http.HttpStatus;
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

    @PostMapping("/export")
    public ResponseEntity<NotificationMessage> exportToExcel(@RequestBody List<String[]> data,
                                                             @RequestParam String functionId,
                                                             Authentication authentication
                              ) {
        return new ResponseEntity<>(exportMonitorService.exportDataToExcel(data, authentication, functionId), HttpStatus.OK);
    }
}
