package com.sol.office_app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sol.office_app.exception.GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Every signed-in user may call these; services restrict them to the caller's own data. */
    private static final String[] SELF_SERVICE = {
            "/api/v1/auth/logout",
            "/api/v1/users/me",
            "/api/v1/users/change-password",
            "/api/v1/users/allowed-branches",
            "/api/v1/users/user-limits",
            "/api/v1/users/disallowed-branches",
            "/api/v1/report-histories/*/download",
            "/api/v1/export-monitor/export"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtUtils jwtUtils, ObjectMapper objectMapper,
                                           CorsConfigurationSource corsConfigurationSource) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                        .requestMatchers("/api/health", "/error").permitAll()
                        // STOMP CONNECT frames are authenticated by StompAuthChannelInterceptor
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers(SELF_SERVICE).authenticated()
                        .requestMatchers("/api/v1/**").access(new PrivilegeRuleAuthorizationManager())
                        .anyRequest().denyAll()
                )
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> writeProblem(objectMapper, res,
                                GlobalExceptionHandler.problem(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Please sign in.")))
                        .accessDeniedHandler((req, res, ex) -> writeProblem(objectMapper, res,
                                GlobalExceptionHandler.problem(HttpStatus.FORBIDDEN, "FORBIDDEN",
                                        "You do not have permission to perform this action."))))
                .addFilterBefore(new JwtAuthFilter(jwtUtils), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowCredentials(true);
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setExposedHeaders(List.of("Content-Disposition"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private static void writeProblem(ObjectMapper mapper, HttpServletResponse response, ProblemDetail body) throws IOException {
        response.setStatus(body.getStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), body);
    }
}
