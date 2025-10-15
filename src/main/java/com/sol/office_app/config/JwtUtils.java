package com.sol.office_app.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.*;

@Service
public class JwtUtils {
    @Value("${security.jwt.secret-key}")
    private static final String SECRET = "your-256-bit-secret-your-256-bit-secret"; // 32+ characters

    private static final SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());

    @Value("${security.jwt.expiration-time}")
    private long jwtExpiration;

    public static Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public static String getUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public static String getBrnch(String token) {
        return (String) extractAllClaims(token).get("brnch");
    }

    @SuppressWarnings("unchecked")
    public static Boolean getMultiBrnchAccess(String token) {
        return (Boolean) extractAllClaims(token).get("multiBrnchAccess");
    }

    @SuppressWarnings("unchecked")
    public static List<String> getRolePermissions(String token) {
        return (List<String>) extractAllClaims(token).get("roles_permissions");
    }

    @SuppressWarnings("unchecked")
    public static Map<String, List<String>> getRules(String token) {
        return (Map<String, List<String>>) extractAllClaims(token).get("rules");
    }

    public static boolean isTokenValid(String token) {
        try {
            return extractAllClaims(token).getExpiration().after(new java.util.Date());
        } catch (Exception e) {
            return false;
        }
    }

    public long getExpirationTime() {
        return jwtExpiration;
    }

    private String buildToken(
            String username,
            Map<String, Object> extraClaims
    ) {

        return Jwts.builder()
                .claims()
                .add(extraClaims)
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .and()
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public String generateToken(String username,String branch, Boolean multiBrnchAccess, List<String> rolePermissions, Map<String, Set<String>> rules) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("brnch", branch);
        claims.put("multiBrnchAccess", multiBrnchAccess);
        claims.put("roles_permissions", rolePermissions);
        claims.put("rules", rules);
        return buildToken(username, claims);
    }
}