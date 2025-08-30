package com.sol.office_app.mapper;

import com.sol.office_app.dto.ReportHistoryDto;
import com.sol.office_app.entity.ReportHistory;

public class ReportHistoryMapper {
    public static ReportHistoryDto toDto(ReportHistory entity) {
        ReportHistoryDto dto = new ReportHistoryDto();
        dto.setId(entity.getId());
        dto.setExecutedBy(entity.getExecutedBy());
        dto.setBranchCode(entity.getBranchCode());
        dto.setReportFileName(entity.getReportFileName());
        dto.setReportTitleName(entity.getReportTitleName());
        dto.setParameters(entity.getParameters());
        dto.setReportOutputFile(entity.getReportOutputFile());
        dto.setRunInBackground(entity.getRunInBackground());
        dto.setStatus(entity.getStatus());
        dto.setStartedAt(entity.getStartedAt());
        dto.setFinishedAt(entity.getFinishedAt());
        return dto;
    }
}
