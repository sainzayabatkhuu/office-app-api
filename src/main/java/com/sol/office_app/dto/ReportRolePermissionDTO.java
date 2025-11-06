package com.sol.office_app.dto;

public record ReportRolePermissionDTO(
        Long id,
        String brnchId,
        String brnchName,
        Long roleId,
        String roleName,
        String isBackground,
        String delFlg
) {
}
