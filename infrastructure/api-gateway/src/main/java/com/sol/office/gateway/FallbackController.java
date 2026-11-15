package com.sol.office.gateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Target of the circuit-breaker filter: answers quickly when a downstream service is down
 * instead of letting the client wait for timeouts.
 */
@RestController
public class FallbackController {

    @RequestMapping("/fallback/{service}")
    public ProblemDetail fallback(@PathVariable String service) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                service + " is temporarily unavailable. Please try again shortly.");
        pd.setProperty("errorCode", "SERVICE_UNAVAILABLE");
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }
}
