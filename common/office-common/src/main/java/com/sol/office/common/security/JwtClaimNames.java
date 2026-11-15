package com.sol.office.common.security;

/**
 * Claim names in access tokens issued by identity-service. Every service reads them through
 * {@link CurrentUser}, so renaming a claim only happens here.
 */
public final class JwtClaimNames {
    public static final String USER_ID = "uid";
    public static final String FULL_NAME = "name";
    public static final String BRANCH = "branch";
    public static final String MULTI_BRANCH = "mba";
    public static final String ROLES = "roles";
    public static final String AUTHORITIES = "authorities";
    public static final String TOKEN_TYPE = "typ";

    private JwtClaimNames() {
    }
}
