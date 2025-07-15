package com.sol.office_app.dto;

import com.sol.office_app.entity.ReportRolePermission;

import java.io.Serializable;
import java.util.List;

public record ReportDTO(
        Long id,
        String name,
        String title,
        List<ReportRolePermissionDTO> rolePermissions,
        List<ReportParameterDTO> parameters
) implements Serializable { }
