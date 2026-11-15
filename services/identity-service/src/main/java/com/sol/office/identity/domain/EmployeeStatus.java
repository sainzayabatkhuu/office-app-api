package com.sol.office.identity.domain;

public enum EmployeeStatus {
    ACTIVE,
    ON_LEAVE,
    SUSPENDED,
    TERMINATED;

    /** Only these employees may sign in. */
    public boolean canSignIn() {
        return this == ACTIVE || this == ON_LEAVE;
    }
}
