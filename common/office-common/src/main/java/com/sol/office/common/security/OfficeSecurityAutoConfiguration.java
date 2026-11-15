package com.sol.office.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.List;

/**
 * Default stateless resource-server setup for every servlet service. The gateway already validates the
 * token, but each service validates it again (zero-trust) because services are reachable inside the network.
 */
@AutoConfiguration(before = OAuth2ResourceServerAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableMethodSecurity
@EnableConfigurationProperties(OfficeSecurityProperties.class)
public class OfficeSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OfficeJwtAuthenticationConverter officeJwtAuthenticationConverter() {
        return new OfficeJwtAuthenticationConverter();
    }

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain officeSecurityFilterChain(HttpSecurity http,
                                                        OfficeSecurityProperties properties,
                                                        OfficeJwtAuthenticationConverter converter,
                                                        ObjectMapper objectMapper) throws Exception {
        List<String> publicPaths = new ArrayList<>(OfficeSecurityProperties.DEFAULT_PUBLIC_PATHS);
        publicPaths.addAll(properties.getPublicPaths());

        http.csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(publicPaths.toArray(String[]::new)).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(rs -> rs
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                        .authenticationEntryPoint(ProblemDetailAuthHandlers.entryPoint(objectMapper)))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(ProblemDetailAuthHandlers.entryPoint(objectMapper))
                        .accessDeniedHandler(ProblemDetailAuthHandlers.accessDenied(objectMapper)));
        return http.build();
    }
}
