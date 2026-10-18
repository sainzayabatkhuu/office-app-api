package com.sol.office_app.controller;

import com.sol.office_app.common.Constant;
import com.sol.office_app.domain.request.LoginDto;
import com.sol.office_app.domain.request.RefreshTokenRequest;
import com.sol.office_app.domain.response.LoginResponse;
import com.sol.office_app.service.AuthenticationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RequestMapping(Constant.AUTH_URL_PREFIX)
@RestController
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticate(@RequestBody LoginDto loginDto) {
        return ResponseEntity.ok(authenticationService.authenticate(loginDto));
    }

    /** New token pair for a refresh token; the old refresh token stops working. */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authenticationService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Principal principal, @RequestBody RefreshTokenRequest request) {
        authenticationService.logout(principal.getName(), request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
