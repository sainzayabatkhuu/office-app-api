package com.sol.office_app.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.NoSuchElementException;

/**
 * Every error is an RFC 7807 problem detail with two extra properties: {@code errorCode} (stable, for code)
 * and {@code message} (human readable; same as {@code detail}, kept for the existing front-end).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApi(ApiException ex) {
        return problem(ex.getStatus(), ex.getErrorCode(), ex.getMessage());
    }

    /** Legacy services throw these with an explicit status. */
    @ExceptionHandler(HttpStatusCodeException.class)
    public ProblemDetail handleHttpStatus(HttpStatusCodeException ex) {
        return problem(ex.getStatusCode(), "ERROR", ex.getStatusText());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        return problem(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ProblemDetail handleNotFound(NoSuchElementException ex) {
        return problem(HttpStatus.NOT_FOUND, "NOT_FOUND", "The requested record was not found.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String type = ex.getRequiredType() == null ? "?" : ex.getRequiredType().getSimpleName();
        return problem(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Parameter '" + ex.getName() + "' must be of type " + type);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission to perform this action.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT, "DATA_INTEGRITY", "The request conflicts with existing data.");
    }

    /** Unexpected errors: logged with the stack trace, but internals are not sent to the client. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected server error.");
    }

    public static ProblemDetail problem(HttpStatusCode status, String errorCode, String message) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, message);
        pd.setProperty("errorCode", errorCode);
        pd.setProperty("message", message);
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }
}
