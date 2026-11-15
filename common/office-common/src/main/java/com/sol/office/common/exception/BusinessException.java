package com.sol.office.common.exception;

import org.springframework.http.HttpStatus;

/**
 * A request that is well-formed but violates a business rule.
 */
public class BusinessException extends OfficeException {
    public BusinessException(String errorCode, String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, errorCode, message);
    }
}
