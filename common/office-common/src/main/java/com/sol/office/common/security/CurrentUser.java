package com.sol.office.common.security;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.Set;

/**
 * The authenticated caller, decoded from the access token.
 *
 * @param roles       role codes (e.g. {@code ADMIN}, {@code BRANCH_MANAGER})
 * @param authorities privilege codes (e.g. {@code REPORT_EXECUTE})
 */
public record CurrentUser(
        Long userId,
        String username,
        String fullName,
        String branchCode,
        boolean multiBranchAccess,
        Set<String> roles,
        Set<String> authorities,
        String token
) {
    public static CurrentUser fromJwt(Jwt jwt) {
        Number uid = jwt.getClaim(JwtClaimNames.USER_ID);
        Boolean mba = jwt.getClaim(JwtClaimNames.MULTI_BRANCH);
        return new CurrentUser(
                uid == null ? null : uid.longValue(),
                jwt.getSubject(),
                jwt.getClaimAsString(JwtClaimNames.FULL_NAME),
                jwt.getClaimAsString(JwtClaimNames.BRANCH),
                Boolean.TRUE.equals(mba),
                toSet(jwt.getClaimAsStringList(JwtClaimNames.ROLES)),
                toSet(jwt.getClaimAsStringList(JwtClaimNames.AUTHORITIES)),
                jwt.getTokenValue());
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public boolean hasAuthority(String authority) {
        return authorities.contains(authority);
    }

    /** True when the user may act on data that belongs to {@code branch}. */
    public boolean canAccessBranch(String branch) {
        return multiBranchAccess || branch == null || branch.equals(branchCode);
    }

    private static Set<String> toSet(Collection<String> values) {
        return values == null ? Set.of() : Set.copyOf(values);
    }

    @Override
    public String toString() {
        return "CurrentUser[" + username + "@" + branchCode + ", roles=" + roles + "]";
    }
}
