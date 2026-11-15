package com.sol.office.common.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "office.security")
public class OfficeSecurityProperties {

    /** Paths reachable without a token, in addition to the defaults below. */
    private List<String> publicPaths = new ArrayList<>();

    /** Always public: health probes and API docs. */
    public static final List<String> DEFAULT_PUBLIC_PATHS = List.of(
            "/actuator/health/**", "/actuator/info", "/actuator/prometheus",
            "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/error");
}
