package com.sol.office_app.config;

import java.util.List;
import java.util.Locale;

/**
 * Checks a caller's privilege rules from service code, for decisions that depend on the data
 * (e.g. "own record, or may see everyone's").
 */
public final class PrivilegeRules {

    private PrivilegeRules() {
    }

    /** @param path relative to {@code /api/v1}, e.g. {@code /report-histories} */
    public static boolean allows(CustomUserPrincipal principal, String method, String path) {
        List<String> patterns = principal.getRules().getOrDefault(method.toLowerCase(Locale.ROOT), List.of());
        return patterns.stream().anyMatch(p -> PrivilegeRuleAuthorizationManager.matches(p, path));
    }
}
