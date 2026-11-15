package com.sol.office.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sol.office.common.web.GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

/**
 * Security filter-chain errors happen before MVC, so they bypass {@link GlobalExceptionHandler};
 * these handlers give them the same problem-detail body.
 */
final class ProblemDetailAuthHandlers {

    private ProblemDetailAuthHandlers() {
    }

    static AuthenticationEntryPoint entryPoint(ObjectMapper mapper) {
        return (request, response, ex) -> write(mapper, response,
                GlobalExceptionHandler.problem(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Missing or invalid access token"));
    }

    static AccessDeniedHandler accessDenied(ObjectMapper mapper) {
        return (request, response, ex) -> write(mapper, response,
                GlobalExceptionHandler.problem(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission to perform this action"));
    }

    private static void write(ObjectMapper mapper, HttpServletResponse response, ProblemDetail body) throws IOException {
        response.setStatus(body.getStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), body);
    }
}
