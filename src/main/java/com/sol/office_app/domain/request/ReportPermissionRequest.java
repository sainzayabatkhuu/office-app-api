package com.sol.office_app.domain.request;

/**
 * @param branchId        branch solId; null = all branches
 * @param runInBackground "V" = generate immediately, "B" = queue in the background
 */
public record ReportPermissionRequest(Long reportId, Long roleId, String branchId, String runInBackground) {
}
