package com.sol.office_app.controller;

import com.sol.office_app.common.Constant;
import com.sol.office_app.dto.BranchDTO;
import com.sol.office_app.dto.ReportRolePermissionDTO;
import com.sol.office_app.service.ReportRolePermissionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(Constant.REPORT_PERMISSION_URL_PREFIX)
public class ReportPermissionController {

    public final ReportRolePermissionService service;

    public ReportPermissionController(ReportRolePermissionService service) {
        this.service = service;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<ReportRolePermissionDTO> index(
            @RequestParam Long reportId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size){
        return service.findDynamic(reportId, PageRequest.of(page, size));
    }
}
