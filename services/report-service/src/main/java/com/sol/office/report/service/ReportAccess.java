package com.sol.office.report.service;

import com.sol.office.common.security.CurrentUser;
import com.sol.office.report.domain.ExecutionMode;
import com.sol.office.report.domain.ReportDefinition;
import com.sol.office.report.domain.ReportPermission;

import java.util.List;
import java.util.Objects;

/**
 * Who may run a report, and how.
 */
final class ReportAccess {

    private ReportAccess() {
    }

    static List<ReportPermission> applicable(ReportDefinition report, CurrentUser user) {
        return report.getPermissions().stream()
                .filter(p -> p.appliesTo(user.roles(), user.branchCode()))
                .toList();
    }

    /** ASYNC wins if any applicable permission forces it; otherwise the report's own mode. */
    static ExecutionMode effectiveMode(ReportDefinition report, List<ReportPermission> applicable) {
        boolean forcedAsync = applicable.stream()
                .map(ReportPermission::getExecutionMode)
                .filter(Objects::nonNull)
                .anyMatch(m -> m == ExecutionMode.ASYNC);
        return forcedAsync ? ExecutionMode.ASYNC : report.getExecutionMode();
    }
}
