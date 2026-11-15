package com.sol.office.identity.web;

import com.sol.office.common.api.MessageResponse;
import com.sol.office.common.security.SecurityUtils;
import com.sol.office.identity.dto.AuthDtos.ChangePasswordRequest;
import com.sol.office.identity.dto.AuthDtos.LoginRequest;
import com.sol.office.identity.dto.AuthDtos.PreferencesDto;
import com.sol.office.identity.dto.AuthDtos.ProfileResponse;
import com.sol.office.identity.dto.AuthDtos.RefreshRequest;
import com.sol.office.identity.dto.AuthDtos.TokenResponse;
import com.sol.office.identity.service.AuthService;
import com.sol.office.identity.service.AuthService.ClientInfo;
import com.sol.office.identity.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final ProfileService profileService;

    @Operation(summary = "Sign in with username and password")
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return authService.login(request.username().trim().toLowerCase(), request.password(), clientInfo(http));
    }

    @Operation(summary = "Exchange a refresh token for a new token pair (the old refresh token is revoked)")
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest http) {
        return authService.refresh(request.refreshToken(), clientInfo(http));
    }

    @Operation(summary = "Revoke the given refresh token")
    @PostMapping("/logout")
    public MessageResponse logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(SecurityUtils.requireCurrentUser(), request.refreshToken());
        return MessageResponse.of("Signed out");
    }

    @Operation(summary = "Current user's profile, roles and privileges")
    @GetMapping("/me")
    public ProfileResponse me() {
        return profileService.getProfile(SecurityUtils.requireCurrentUser());
    }

    @PutMapping("/me/password")
    public MessageResponse changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(SecurityUtils.requireCurrentUser(), request);
        return MessageResponse.of("Password changed. Other sessions have been signed out.");
    }

    @PutMapping("/me/preferences")
    public PreferencesDto updatePreferences(@Valid @RequestBody PreferencesDto request) {
        return profileService.updatePreferences(SecurityUtils.requireCurrentUser(), request);
    }

    static ClientInfo clientInfo(HttpServletRequest http) {
        String forwarded = http.getHeader("X-Forwarded-For");
        String ip = forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim() : http.getRemoteAddr();
        return new ClientInfo(ip, http.getHeader(HttpHeaders.USER_AGENT));
    }
}
