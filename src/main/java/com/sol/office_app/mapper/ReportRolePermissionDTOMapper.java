package com.sol.office_app.mapper;

import com.sol.office_app.dto.ReportRolePermissionDTO;
import com.sol.office_app.dto.RoleDTO;
import com.sol.office_app.entity.ReportRolePermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class ReportRolePermissionDTOMapper implements Function<ReportRolePermission, ReportRolePermissionDTO> {

    @Autowired
    private RoleDTOMapper roleDTOMapper;

    @Override
    public ReportRolePermissionDTO apply(ReportRolePermission role) {

        RoleDTO roleDTO = (role.getRole() != null)
                ? roleDTOMapper.apply(role.getRole())
                : null;

        return new ReportRolePermissionDTO(
                role.getId(),
                roleDTO.id(),
                roleDTO.name(),
                role.getRunInBackground()
        );
    }
}
