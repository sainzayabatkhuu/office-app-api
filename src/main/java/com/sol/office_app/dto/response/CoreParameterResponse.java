package com.sol.office_app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CoreParameterResponse(
        String type,
        String code,
        String name,
        String delFlg
) {
}
