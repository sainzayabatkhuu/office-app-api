package com.sol.office.identity.security;

import com.sol.office.common.security.JwtClaimNames;
import com.sol.office.identity.config.IdentityProperties;
import com.sol.office.identity.domain.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final JwtEncoder jwtEncoder;
    private final IdentityProperties properties;
    private final Clock clock;

    /** Requires the user's employee, branch, roles and privileges to be loaded. */
    public String issueAccessToken(AppUser user) {
        Instant now = clock.instant();
        IdentityProperties.Jwt jwt = properties.jwt();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwt.issuer())
                .subject(user.getUsername())
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiresAt(now.plus(jwt.accessTokenTtl()))
                .claim(JwtClaimNames.TOKEN_TYPE, "access")
                .claim(JwtClaimNames.USER_ID, user.getId())
                .claim(JwtClaimNames.FULL_NAME, user.getEmployee().getDisplayName())
                .claim(JwtClaimNames.BRANCH, user.getEmployee().getBranch().getCode())
                .claim(JwtClaimNames.MULTI_BRANCH, user.isMultiBranchAccess())
                .claim(JwtClaimNames.ROLES, List.copyOf(user.roleCodes()))
                .claim(JwtClaimNames.AUTHORITIES, List.copyOf(user.privilegeCodes()))
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(jwt.keyId()).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /** 256-bit random, URL-safe. */
    public String newRefreshTokenValue() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
