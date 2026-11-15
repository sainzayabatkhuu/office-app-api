package com.sol.office.report.dto;

import com.sol.office.report.domain.ExecutionMode;
import com.sol.office.report.domain.ExecutionStatus;
import com.sol.office.report.domain.ReportFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ReportDtos {

    private ReportDtos() {
    }

    // ---- categories

    public record CategoryRequest(
            @NotBlank @Size(max = 30) @Pattern(regexp = "[A-Z0-9_]+") String code,
            @NotBlank @Size(max = 150) String name,
            int sortOrder) {
    }

    public record CategoryResponse(Long id, String code, String name, int sortOrder) {
    }

    // ---- catalog

    public record ReportCreateRequest(
            @NotBlank @Size(max = 50) @Pattern(regexp = "[A-Z0-9_]+", message = "use UPPER_SNAKE_CASE") String code,
            @Valid @NotNull ReportMetadataRequest metadata) {
    }

    public record ReportMetadataRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 1000) String description,
            Long categoryId,
            @NotEmpty Set<ReportFormat> allowedFormats,
            @NotNull ReportFormat defaultFormat,
            @NotNull ExecutionMode executionMode,
            boolean active) {
    }

    public record ReportSummary(
            Long id,
            String code,
            String title,
            String description,
            Long categoryId,
            String categoryName,
            Set<ReportFormat> allowedFormats,
            ReportFormat defaultFormat,
            ExecutionMode executionMode,
            boolean active,
            int templateVersion,
            String templateFileName,
            Instant updatedAt) {
    }

    public record ReportDetail(
            ReportSummary report,
            List<ParameterDto> parameters,
            List<PermissionResponse> permissions) {
    }

    public record ParameterDto(
            String name,
            String label,
            String javaType,
            String inputType,
            boolean required,
            String defaultValue,
            boolean systemParam,
            int sortOrder) {
    }

    public record ParameterUpdate(
            @NotBlank String name,
            @NotBlank @Size(max = 200) String label,
            boolean required,
            @Size(max = 500) String defaultValue,
            int sortOrder) {
    }

    public record PermissionRequest(
            @NotBlank @Size(max = 50) String roleCode,
            @Size(max = 10) String branchCode,
            ExecutionMode executionMode) {
    }

    public record PermissionResponse(Long id, String roleCode, String branchCode, ExecutionMode executionMode) {
    }

    // ---- execution

    public record ExecuteRequest(ReportFormat format, Map<String, String> parameters) {
    }

    public record ExecutionResponse(
            String id,
            Long reportId,
            String reportCode,
            String reportTitle,
            String requestedBy,
            String branchCode,
            ReportFormat format,
            ExecutionMode mode,
            ExecutionStatus status,
            Map<String, String> parameters,
            String outputFileName,
            Long outputSize,
            Integer pageCount,
            String errorMessage,
            Instant requestedAt,
            Instant startedAt,
            Instant finishedAt,
            Long durationMs,
            String downloadUrl) {
    }
}
