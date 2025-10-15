package com.sol.office_app.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.Principal;
import java.util.Optional;

public class AuditorAwareImpl implements AuditorAware<String> {

    public AuditorAwareImpl() {}

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.of("sysadmin"); // default if not authenticated
        }
        if (authentication.getPrincipal() instanceof String) {
            return Optional.of((String)authentication.getPrincipal());
        }
        Principal principal = (Principal) authentication.getPrincipal();
        return Optional.of(principal.getName());
    }
}