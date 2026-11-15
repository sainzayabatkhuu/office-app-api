package com.sol.office.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base class for expected, client-facing errors. Rendered as RFC 7807 problem details.
 */
@Getter
public class OfficeException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    public OfficeException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }
}
