package com.sol.office.report.engine;

import java.util.Set;

/**
 * Template parameters the server fills from the caller's identity. A client can never set these,
 * which matters for {@link #BRANCH}: it restricts single-branch users to their own branch's data.
 * Names match the templates already used by office-app-api.
 */
public final class SystemParameters {

    public static final String REPORT_TITLE = "reportTitle";
    public static final String MAKER = "maker";
    public static final String MAKER_NAME = "makerName";
    /** Branch filter. Multi-branch users may choose it; everyone else gets their own branch. */
    public static final String BRANCH = "branchId";

    /** Always server-filled. {@link #BRANCH} is handled separately because some users may set it. */
    public static final Set<String> SERVER_ONLY = Set.of(REPORT_TITLE, MAKER, MAKER_NAME);

    private SystemParameters() {
    }
}
