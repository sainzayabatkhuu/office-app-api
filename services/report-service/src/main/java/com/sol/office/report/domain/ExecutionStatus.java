package com.sol.office.report.domain;

public enum ExecutionStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED,
    /** Output removed by the retention job. */
    EXPIRED;

    public boolean isFinished() {
        return this == SUCCESS || this == FAILED || this == EXPIRED;
    }
}
