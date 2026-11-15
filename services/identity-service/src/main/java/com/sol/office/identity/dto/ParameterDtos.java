package com.sol.office.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ParameterDtos {

    private ParameterDtos() {
    }

    public record CoreParameterRequest(
            @NotBlank @Size(max = 50) String type,
            @NotBlank @Size(max = 50) String code,
            @NotBlank @Size(max = 200) String name) {
    }

    /** {@code delFlg} is always "N": deleted parameters are removed, kept for the office-app-api contract. */
    public record CoreParameterResponse(Long id, String type, String code, String name, String delFlg) {
    }
}
