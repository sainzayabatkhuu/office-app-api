package com.sol.office_app.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class JwtUtils {

    private final SecretKey key;
    private final long jwtExpiration;

    public JwtUtils(@Value("${security.jwt.secret-key}") String secret,
                    @Value("${security.jwt.expiration-time}") long jwtExpiration) {
        byte[] bytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("security.jwt.secret-key (JWT_SECRET) must be at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.jwtExpiration = jwtExpiration;
    }

    /** Claims of a valid, unexpired token; empty for anything else. */
    public Optional<Claims> parse(String token) {
        try {
            return Optional.of(Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** Access-token lifetime in milliseconds. */
    public long getExpirationTime() {
        return jwtExpiration;
    }

    public String generateToken(String username, String branch, Boolean multiBrnchAccess,
                                List<String> rolePermissions, Map<String, Set<String>> rules) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("brnch", branch);
        claims.put("multiBrnchAccess", multiBrnchAccess);
        claims.put("roles_permissions", rolePermissions);
        claims.put("rules", rules);
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims()
                .add(claims)
                .subject(username)
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date(now))
                .expiration(new Date(now + jwtExpiration))
                .and()
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    @SuppressWarnings("unchecked")
    public static CustomUserPrincipal toPrincipal(Claims claims) {
        return new CustomUserPrincipal(
                claims.getSubject(),
                (String) claims.get("brnch"),
                (Boolean) claims.get("multiBrnchAccess"),
                Optional.ofNullable((List<String>) claims.get("roles_permissions")).orElse(List.of()),
                Optional.ofNullable((Map<String, List<String>>) claims.get("rules")).orElse(Map.of()));
    }
}
