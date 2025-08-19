package com.sol.office_app.exception;

import com.sol.office_app.dto.NotificationMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<NotificationMessage> handleIllegalArgumentException(IllegalArgumentException ex) {
        NotificationMessage message = new NotificationMessage(
                "error",
                "",
                ex.getMessage(),
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<NotificationMessage> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        NotificationMessage message = new NotificationMessage(
                "error",
                "",
                "Parameter '" + ex.getName() + "' must be of type " + ex.getRequiredType().getSimpleName(),
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<NotificationMessage> handleGenericException(Exception ex) {
        NotificationMessage message = new NotificationMessage(
                "error",
                "Internal Server Error",
                ex.getMessage(),
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(message);
    }
}
