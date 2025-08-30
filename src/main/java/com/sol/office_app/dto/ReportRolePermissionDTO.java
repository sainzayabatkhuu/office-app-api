package com.sol.office_app.dto;

public record ReportRolePermissionDTO(
        Long id,

        RoleDTO role,
        String canPrint,
        String canDownload
) {
}
