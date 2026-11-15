package com.sol.office.report.engine;

/**
 * Report could not be produced (template/data/database problem). The message is shown to the user.
 */
public class ReportGenerationException extends RuntimeException {
    public ReportGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
