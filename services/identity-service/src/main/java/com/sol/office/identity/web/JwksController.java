package com.sol.office.identity.web;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Public signing key(s) used by the gateway and other services to verify access tokens.
 */
@Hidden
@RestController
@RequiredArgsConstructor
public class JwksController {

    private final RSAKey signingKey;

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return new JWKSet(signingKey.toPublicJWK()).toJSONObject();
    }
}
