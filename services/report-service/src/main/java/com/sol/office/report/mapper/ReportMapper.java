package com.sol.office.report.mapper;

import com.sol.office.report.domain.ExecutionStatus;
import com.sol.office.report.domain.ReportCategory;
import com.sol.office.report.domain.ReportDefinition;
import com.sol.office.report.domain.ReportExecution;
import com.sol.office.report.domain.ReportParameter;
import com.sol.office.report.domain.ReportPermission;
import com.sol.office.report.dto.ReportDtos.CategoryResponse;
import com.sol.office.report.dto.ReportDtos.ExecutionResponse;
import com.sol.office.report.dto.ReportDtos.ParameterDto;
import com.sol.office.report.dto.ReportDtos.PermissionResponse;
import com.sol.office.report.dto.ReportDtos.ReportSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface ReportMapper {

    String DOWNLOAD_PATH = "/api/v1/report-executions/%s/download";

    CategoryResponse toResponse(ReportCategory category);

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "allowedFormats", expression = "java(report.allowedFormatSet())")
    ReportSummary toSummary(ReportDefinition report);

    ParameterDto toDto(ReportParameter parameter);

    PermissionResponse toResponse(ReportPermission permission);

    @Mapping(target = "reportId", source = "report.id")
    @Mapping(target = "downloadUrl", expression = "java(downloadUrl(execution))")
    ExecutionResponse toResponse(ReportExecution execution);

    default String downloadUrl(ReportExecution execution) {
        return execution.getStatus() == ExecutionStatus.SUCCESS ? DOWNLOAD_PATH.formatted(execution.getId()) : null;
    }
}
