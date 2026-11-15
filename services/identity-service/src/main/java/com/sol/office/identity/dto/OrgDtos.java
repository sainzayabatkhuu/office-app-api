package com.sol.office.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class OrgDtos {

    private OrgDtos() {
    }

    public record BranchRequest(
            @NotBlank @Size(max = 10) @Pattern(regexp = "[A-Za-z0-9_-]+") String code,
            @NotBlank @Size(max = 150) String name,
            @Size(max = 500) String address,
            @Size(max = 32) String phone,
            Long parentId,
            Boolean active) {
    }

    public record BranchResponse(
            Long id, String code, String name, String address, String phone,
            Long parentId, String parentCode, boolean active) {
    }

    public record DepartmentRequest(
            @NotBlank @Size(max = 20) @Pattern(regexp = "[A-Za-z0-9_-]+") String code,
            @NotBlank @Size(max = 150) String name,
            Long branchId,
            Long parentId,
            Boolean active) {
    }

    public record DepartmentResponse(
            Long id, String code, String name, Long branchId, String branchCode,
            Long parentId, String parentName, boolean active) {
    }

    public record PositionRequest(
            @NotBlank @Size(max = 20) @Pattern(regexp = "[A-Za-z0-9_-]+") String code,
            @NotBlank @Size(max = 150) String name,
            @Size(max = 500) String description,
            Boolean active) {
    }

    public record PositionResponse(Long id, String code, String name, String description, boolean active) {
    }
}
