package com.sol.office_app.config;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Aspect
@Component
public class SecurityRuleAspect {

    @Around("@annotation(SecurityRule)")
    public Object validateAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String method = request.getMethod();                   // e.g., "GET"
        String path = request.getRequestURI();                 // e.g., "/api/users/5"

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String principalUserId = null;
        Map<String, List<String>> rules = new HashMap<>();

        if (principal instanceof CustomUserPrincipal user) {
            principalUserId = user.getName();
            rules = user.getRules();
        } else {
            throw new RuntimeException("Principal is missing or invalid");
        }

        if (!rules.get(method.toLowerCase()).contains(path))
            throw new RuntimeException("Not authorized");

        return joinPoint.proceed();
    }
}