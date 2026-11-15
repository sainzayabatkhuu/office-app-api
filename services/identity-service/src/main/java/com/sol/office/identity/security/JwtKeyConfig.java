package com.sol.office.identity.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.sol.office.identity.config.IdentityProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * RS256 signing key. Only identity-service holds the private key; other services and the gateway verify
 * tokens with the public key published at {@code /.well-known/jwks.json}.
 */
@Slf4j
@Configuration
public class JwtKeyConfig {

    @Bean
    public RSAKey signingKey(IdentityProperties properties, ResourceLoader resourceLoader) throws IOException, NoSuchAlgorithmException {
        IdentityProperties.Jwt jwt = properties.jwt();
        RSAPublicKey publicKey;
        RSAPrivateKey privateKey;
        if (StringUtils.hasText(jwt.privateKeyLocation()) && StringUtils.hasText(jwt.publicKeyLocation())) {
            try (InputStream priv = resourceLoader.getResource(jwt.privateKeyLocation()).getInputStream();
                 InputStream pub = resourceLoader.getResource(jwt.publicKeyLocation()).getInputStream()) {
                privateKey = RsaKeyConverters.pkcs8().convert(priv);
                publicKey = RsaKeyConverters.x509().convert(pub);
            }
            log.info("Loaded JWT signing key '{}' from {}", jwt.keyId(), jwt.privateKeyLocation());
        } else {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            publicKey = (RSAPublicKey) pair.getPublic();
            privateKey = (RSAPrivateKey) pair.getPrivate();
            log.warn("No JWT key configured (office.identity.jwt.*-key-location): using an EPHEMERAL key. "
                    + "Tokens become invalid on restart and differ between instances. Do not run like this outside dev.");
        }
        return new RSAKey.Builder(publicKey).privateKey(privateKey).keyID(jwt.keyId()).build();
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource(RSAKey signingKey) {
        return new ImmutableJWKSet<>(new JWKSet(signingKey));
    }

    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    /** identity-service validates its own tokens locally instead of fetching its own JWKS over HTTP. */
    @Bean
    public JwtDecoder jwtDecoder(RSAKey signingKey, IdentityProperties properties) throws JOSEException {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(signingKey.toRSAPublicKey()).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.jwt().issuer()));
        return decoder;
    }
}
