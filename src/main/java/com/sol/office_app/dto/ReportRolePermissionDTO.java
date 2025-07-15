package com.sol.office_app.dto;

public record ReportRolePermissionDTO(
        Long id,

        RoleDTO role,
        boolean canPrint,
        boolean canDownload
) {
}
