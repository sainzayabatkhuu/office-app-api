package com.sol.office_app.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class JwtAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        if (!JwtUtils.isTokenValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        String username = JwtUtils.getUsername(token);
        Boolean multiBrnchAccess = JwtUtils.getMultiBrnchAccess(token);
        String branch = JwtUtils.getBrnch(token);
        List<String> permissions = JwtUtils.getRolePermissions(token);
        Map<String, List<String>> rules = JwtUtils.getRules(token);

        CustomUserPrincipal userPrincipal = new CustomUserPrincipal(username, branch, multiBrnchAccess, permissions, rules);

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                        userPrincipal,
                        null,
                        permissions.stream()
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList())
                );

        SecurityContextHolder.getContext().setAuthentication(authToken);
        filterChain.doFilter(request, response);
    }
}