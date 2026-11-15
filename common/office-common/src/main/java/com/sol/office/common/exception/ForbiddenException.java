package com.sol.office.common.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends OfficeException {
    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }
}
