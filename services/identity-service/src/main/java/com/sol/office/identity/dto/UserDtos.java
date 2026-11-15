package com.sol.office.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;

public final class UserDtos {

    private UserDtos() {
    }

    /** Grants a sign-in account to an existing employee. */
    public record CreateUserRequest(
            @NotNull Long employeeId,
            @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "[a-zA-Z0-9._-]+") String username,
            /** Optional; a random temporary password is generated when omitted. */
            @Size(min = 8, max = 128) String password,
            @NotEmpty Set<String> roleCodes,
            boolean multiBranchAccess) {
    }

    public record UpdateUserRequest(
            @NotEmpty Set<String> roleCodes,
            boolean enabled,
            boolean multiBranchAccess) {
    }

    public record UserResponse(
            Long id,
            String username,
            String employeeCode,
            String displayName,
            String branchCode,
            boolean enabled,
            boolean locked,
            Instant lockedUntil,
            Instant lastLoginAt,
            boolean mustChangePassword,
            boolean multiBranchAccess,
            Set<String> roles) {
    }

    /** Returned once; the plain password is never stored or shown again. */
    public record TemporaryPasswordResponse(String username, String temporaryPassword) {
    }
}
