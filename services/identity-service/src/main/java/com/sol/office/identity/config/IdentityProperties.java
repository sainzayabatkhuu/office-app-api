package com.sol.office.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "office.identity")
public record IdentityProperties(@DefaultValue Jwt jwt, @DefaultValue Login login, @DefaultValue Password password) {

    public record Jwt(
            @DefaultValue("office-identity") String issuer,
            @DefaultValue("office-key-1") String keyId,
            String privateKeyLocation,
            String publicKeyLocation,
            @DefaultValue("30m") Duration accessTokenTtl,
            @DefaultValue("7d") Duration refreshTokenTtl) {
    }

    public record Login(
            @DefaultValue("5") int maxFailedAttempts,
            @DefaultValue("15m") Duration lockDuration,
            @DefaultValue("10") int permanentLockAfter) {
    }

    public record Password(@DefaultValue("90") int expiryDays) {
    }
}
