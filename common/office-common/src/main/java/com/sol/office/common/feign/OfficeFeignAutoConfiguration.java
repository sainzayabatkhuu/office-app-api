package com.sol.office.common.feign;

import com.sol.office.common.security.SecurityUtils;
import feign.RequestInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;

/**
 * Forwards the caller's access token on every Feign call so downstream services see the same user
 * (and apply their own authorization) instead of trusting the caller service blindly.
 */
@AutoConfiguration
@ConditionalOnClass(RequestInterceptor.class)
public class OfficeFeignAutoConfiguration {

    @Bean
    public RequestInterceptor bearerTokenRelayInterceptor() {
        return template -> SecurityUtils.currentUser()
                .filter(u -> u.token() != null)
                .ifPresent(u -> template.header(HttpHeaders.AUTHORIZATION, "Bearer " + u.token()));
    }
}
