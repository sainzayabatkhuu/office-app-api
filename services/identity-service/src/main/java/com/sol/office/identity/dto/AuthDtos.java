package com.sol.office.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank @Size(max = 50) String username,
            @NotBlank @Size(max = 128) String password) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record TokenResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn,
            long refreshExpiresIn,
            boolean mustChangePassword) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 128) String newPassword) {
    }

    public record PreferencesDto(
            @Size(max = 5) String language,
            @Size(max = 30) String theme,
            @Size(max = 20) String dateFormat,
            @Size(max = 10) String fontSize) {
    }

    public record ProfileResponse(
            Long id,
            String username,
            String employeeCode,
            String firstName,
            String lastName,
            String displayName,
            String email,
            String branchCode,
            String branchName,
            String departmentName,
            String positionName,
            boolean multiBranchAccess,
            boolean mustChangePassword,
            Set<String> roles,
            Set<String> privileges,
            PreferencesDto preferences) {
    }
}
