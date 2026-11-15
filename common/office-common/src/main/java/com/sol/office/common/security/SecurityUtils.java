package com.sol.office.common.security;

import com.sol.office.common.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<CurrentUser> currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return Optional.of(CurrentUser.fromJwt(jwtAuth.getToken()));
        }
        return Optional.empty();
    }

    public static CurrentUser requireCurrentUser() {
        return currentUser().orElseThrow(() -> new UnauthorizedException("UNAUTHENTICATED", "Authentication required"));
    }

    public static Optional<String> currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return Optional.empty();
        }
        return Optional.ofNullable(auth.getName());
    }
}
