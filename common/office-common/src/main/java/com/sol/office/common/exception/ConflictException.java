package com.sol.office.common.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends OfficeException {
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, "CONFLICT", message);
    }
}
