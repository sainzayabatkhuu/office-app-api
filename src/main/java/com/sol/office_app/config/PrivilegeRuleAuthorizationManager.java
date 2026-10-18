package com.sol.office_app.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Grants an API request when one of the caller's privilege rules (from the token, e.g. {@code get:/reports/*})
 * matches the request method and path. Rule paths are relative to {@code /api/v1}; a rule also covers its
 * sub-paths, so {@code get:/roles} allows {@code GET /roles} and {@code GET /roles/5}.
 */
public class PrivilegeRuleAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    static final String API_PREFIX = "/api/v1";
    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        Authentication auth = authentication.get();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserPrincipal principal)) {
            return new AuthorizationDecision(false);
        }
        HttpServletRequest request = context.getRequest();
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (!path.startsWith(API_PREFIX)) {
            return new AuthorizationDecision(false);
        }
        String relative = path.substring(API_PREFIX.length());
        List<String> patterns = principal.getRules().getOrDefault(request.getMethod().toLowerCase(Locale.ROOT), List.of());
        return new AuthorizationDecision(patterns.stream().anyMatch(p -> matches(p, relative)));
    }

    static boolean matches(String pattern, String path) {
        if (MATCHER.match(pattern, path)) {
            return true;
        }
        String subPaths = pattern.endsWith("/") ? pattern + "**" : pattern + "/**";
        return MATCHER.match(subPaths, path);
    }
}
