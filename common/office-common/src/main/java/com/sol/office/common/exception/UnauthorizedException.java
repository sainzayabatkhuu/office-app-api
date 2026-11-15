package com.sol.office.common.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends OfficeException {
    public UnauthorizedException(String errorCode, String message) {
        super(HttpStatus.UNAUTHORIZED, errorCode, message);
    }
}
