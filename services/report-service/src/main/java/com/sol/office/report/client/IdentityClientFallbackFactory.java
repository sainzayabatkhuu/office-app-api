package com.sol.office.report.client;

import com.sol.office.common.exception.OfficeException;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class IdentityClientFallbackFactory implements FallbackFactory<IdentityClient> {

    @Override
    public IdentityClient create(Throwable cause) {
        return () -> {
            if (cause instanceof FeignException.Forbidden) {
                throw new OfficeException(HttpStatus.FORBIDDEN, "FORBIDDEN", "ROLE_VIEW privilege is required to assign roles");
            }
            log.warn("identity-service call failed: {}", cause.toString());
            throw new OfficeException(HttpStatus.SERVICE_UNAVAILABLE, "IDENTITY_UNAVAILABLE",
                    "Role list is unavailable right now; try again shortly");
        };
    }
}
