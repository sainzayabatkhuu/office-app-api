package com.sol.office.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public final class RoleDtos {

    private RoleDtos() {
    }

    public record RoleRequest(
            @NotBlank @Size(max = 50) @Pattern(regexp = "[A-Z0-9_]+", message = "use UPPER_SNAKE_CASE") String code,
            @NotBlank @Size(max = 150) String name,
            @Size(max = 500) String description,
            Set<String> privilegeCodes) {
    }

    public record RoleResponse(
            Long id, String code, String name, String description, boolean systemRole, Set<String> privileges) {
    }

    public record PrivilegeResponse(Long id, String code, String name, String module) {
    }
}
