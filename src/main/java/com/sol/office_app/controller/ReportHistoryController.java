package com.sol.office_app.controller;

import com.sol.office_app.common.Constant;
import com.sol.office_app.dto.ReportHistoryDto;
import com.sol.office_app.service.ReportHistoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
        return reportHistoryService.findDynamic(executedBy,branch,startDate, endDate, PageRequest.of(page, size));
    }

}
