package com.sol.office.report.web;

import com.sol.office.common.security.SecurityUtils;
import com.sol.office.report.dto.ReportDtos.ExecuteRequest;
import com.sol.office.report.dto.ReportDtos.ExecutionResponse;
import com.sol.office.report.dto.ReportDtos.ReportDetail;
import com.sol.office.report.dto.ReportDtos.ReportSummary;
import com.sol.office.report.domain.ExecutionMode;
import com.sol.office.report.service.ReportCatalogService;
import com.sol.office.report.service.ReportExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * End-user side: browse reports available to my roles and run them.
 */
@Tag(name = "Reports")
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportCatalogService catalogService;
    private final ReportExecutionService executionService;

    @Operation(summary = "Reports the current user may run")
    @GetMapping
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public List<ReportSummary> available(@RequestParam(required = false) Long categoryId,
                                         @RequestParam(required = false) String q) {
        return catalogService.listAvailable(SecurityUtils.requireCurrentUser(), categoryId, q);
    }

    @Operation(summary = "Report details with the parameters to fill in")
    @GetMapping("/{id:\\d+}")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ReportDetail get(@PathVariable Long id) {
        return catalogService.getForUser(SecurityUtils.requireCurrentUser(), id);
    }

    @Operation(summary = "Run a report",
            description = "SYNC reports return 200 with the finished execution (download via downloadUrl). "
                    + "ASYNC reports return 202 with a PENDING execution; a notification arrives when it is ready.")
    @PostMapping("/{id:\\d+}/executions")
    @PreAuthorize("hasAuthority('REPORT_EXECUTE')")
    public ResponseEntity<ExecutionResponse> execute(@PathVariable Long id, @RequestBody ExecuteRequest request) {
        ExecutionResponse response = executionService.request(SecurityUtils.requireCurrentUser(), id, request);
        HttpStatus status = response.mode() == ExecutionMode.ASYNC ? HttpStatus.ACCEPTED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }
}
