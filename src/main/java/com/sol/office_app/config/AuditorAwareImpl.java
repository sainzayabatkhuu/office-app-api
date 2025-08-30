package com.sol.office_app.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

public class AuditorAwareImpl implements AuditorAware<String> {

    public AuditorAwareImpl() {}

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        System.out.println(authentication == null || !authentication.isAuthenticated());
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.of("sysadmin"); // default if not authenticated
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            return Optional.of(((UserDetails) principal).getUsername());
        }

        return Optional.of("sysadmin");
    }
}