package com.sol.office_app.mapper;

import com.sol.office_app.dto.PrivilegeDTO;
import com.sol.office_app.dto.ReportDTO;
import com.sol.office_app.dto.ReportRolePermissionDTO;
import com.sol.office_app.entity.Report;
import com.sol.office_app.entity.ReportRolePermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ReportDTOMapper implements Function<Report, ReportDTO> {

    @Autowired
    private ReportRolePermissionDTOMapper reportRolePermissionDTOMapper;

    @Override
    public ReportDTO apply(Report report) {
        List<ReportRolePermissionDTO> privilegeDTOs = (report.getRolePermissions() != null)
                ? report.getRolePermissions().stream().map(reportRolePermissionDTOMapper).collect(Collectors.toList())
                : null;
        return new ReportDTO(report.getId(),report.getFileName(), report.getTitle(), privilegeDTOs, null);
    }
}
