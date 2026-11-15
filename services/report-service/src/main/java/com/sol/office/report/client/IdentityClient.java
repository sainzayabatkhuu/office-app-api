package com.sol.office.report.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Calls identity-service through Eureka with the caller's token (see office-common's Feign interceptor),
 * so identity-service applies its own authorization (ROLE_VIEW).
 */
@FeignClient(name = "identity-service", fallbackFactory = IdentityClientFallbackFactory.class)
public interface IdentityClient {

    @GetMapping("/api/v1/roles")
    List<RoleSummary> listRoles();

    record RoleSummary(Long id, String code, String name) {
    }
}
