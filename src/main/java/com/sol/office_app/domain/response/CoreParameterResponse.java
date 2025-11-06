package com.sol.office_app.domain.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CoreParameterResponse(
        Long id,
        String type,
        String code,
        String name,
        String delFlg
) {
}
