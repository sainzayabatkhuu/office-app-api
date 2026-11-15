package com.sol.office.common.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps token claims to Spring authorities: privileges as-is ({@code REPORT_EXECUTE}) and roles with the
 * {@code ROLE_} prefix, so both {@code hasAuthority(..)} and {@code hasRole(..)} work in {@code @PreAuthorize}.
 */
public class OfficeJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        List<String> privileges = jwt.getClaimAsStringList(JwtClaimNames.AUTHORITIES);
        if (privileges != null) {
            privileges.forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
        }
        List<String> roles = jwt.getClaimAsStringList(JwtClaimNames.ROLES);
        if (roles != null) {
            roles.forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));
        }
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }
}
