package com.sol.office.identity.service;

import com.sol.office.common.exception.BusinessException;
import com.sol.office.common.exception.UnauthorizedException;
import com.sol.office.common.security.CurrentUser;
import com.sol.office.identity.config.IdentityProperties;
import com.sol.office.identity.domain.AppUser;
import com.sol.office.identity.domain.LoginHistory;
import com.sol.office.identity.domain.RefreshToken;
import com.sol.office.identity.dto.AuthDtos.ChangePasswordRequest;
import com.sol.office.identity.dto.AuthDtos.TokenResponse;
import com.sol.office.identity.repository.AppUserRepository;
import com.sol.office.identity.repository.LoginHistoryRepository;
import com.sol.office.identity.repository.RefreshTokenRepository;
import com.sol.office.identity.security.PasswordPolicy;
import com.sol.office.identity.security.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /** Same message for unknown user and wrong password, so usernames cannot be enumerated. */
    private static final String INVALID_CREDENTIALS = "Username or password is incorrect";
    /** BCrypt hash of a random string, compared against when the username does not exist. */
    private static final String TIMING_DUMMY_HASH = "$2a$10$n4OrVXWtdC9ybtZFff4ciOXB4nZuunheNFZjLo2Xc70ApJ/qs86mq";

    private final AppUserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final IdentityProperties properties;
    private final Clock clock;

    public record ClientInfo(String ip, String userAgent) {
    }

    /**
     * Failed attempts must be persisted even though the method throws, hence {@code noRollbackFor}.
     */
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public TokenResponse login(String username, String password, ClientInfo client) {
        Instant now = clock.instant();
        AppUser user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            // Burn comparable time to a real check so response timing does not reveal unknown usernames.
            passwordEncoder.matches(password, TIMING_DUMMY_HASH);
            recordLogin(username, null, false, "UNKNOWN_USER", client, now);
            throw new UnauthorizedException("INVALID_CREDENTIALS", INVALID_CREDENTIALS);
        }

        checkCanSignIn(user, now, client);

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            registerFailedAttempt(user, now);
            recordLogin(username, user.getId(), false, "BAD_PASSWORD", client, now);
            throw new UnauthorizedException("INVALID_CREDENTIALS", INVALID_CREDENTIALS);
        }

        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(now);
        recordLogin(username, user.getId(), true, null, client, now);
        return issueTokens(user, client, now);
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public TokenResponse refresh(String refreshTokenValue, ClientInfo client) {
        Instant now = clock.instant();
        RefreshToken token = refreshTokenRepository.findByTokenHash(TokenService.sha256(refreshTokenValue))
                .orElseThrow(() -> new UnauthorizedException("INVALID_REFRESH_TOKEN", "Refresh token is invalid"));

        if (token.getRevokedAt() != null) {
            // A rotated token was presented again: it may have been stolen. Kill every session of this user.
            int revoked = refreshTokenRepository.revokeAllForUser(token.getUser().getId(), now);
            log.warn("Refresh token reuse detected for user id {}; revoked {} active tokens", token.getUser().getId(), revoked);
            throw new UnauthorizedException("INVALID_REFRESH_TOKEN", "Refresh token is invalid");
        }
        if (!token.isActive(now)) {
            throw new UnauthorizedException("REFRESH_TOKEN_EXPIRED", "Session expired, please sign in again");
        }

        AppUser user = userRepository.findByUsername(token.getUser().getUsername())
                .orElseThrow(() -> new UnauthorizedException("INVALID_REFRESH_TOKEN", "Refresh token is invalid"));
        checkCanSignIn(user, now, client);

        token.setRevokedAt(now);
        return issueTokens(user, client, now);
    }

    @Transactional
    public void logout(CurrentUser currentUser, String refreshTokenValue) {
        refreshTokenRepository.findByTokenHash(TokenService.sha256(refreshTokenValue))
                .filter(t -> t.getUser().getId().equals(currentUser.userId()))
                .filter(t -> t.getRevokedAt() == null)
                .ifPresent(t -> t.setRevokedAt(clock.instant()));
    }

    @Transactional
    public void changePassword(CurrentUser currentUser, ChangePasswordRequest request) {
        AppUser user = userRepository.findById(currentUser.userId())
                .orElseThrow(() -> new UnauthorizedException("UNAUTHENTICATED", "User no longer exists"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException("INVALID_CURRENT_PASSWORD", "Current password is incorrect");
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw new BusinessException("PASSWORD_REUSED", "New password must differ from the current one");
        }
        PasswordPolicy.validate(request.newPassword());
        Instant now = clock.instant();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordChangedAt(now);
        user.setMustChangePassword(false);
        refreshTokenRepository.revokeAllForUser(user.getId(), now);
    }

    /** Housekeeping: expired refresh tokens are useless, keep the table small. */
    @Scheduled(cron = "0 30 2 * * *")
    @Transactional
    public void purgeExpiredRefreshTokens() {
        int deleted = refreshTokenRepository.deleteExpiredBefore(clock.instant().minus(Duration.ofDays(1)));
        log.info("Purged {} expired refresh tokens", deleted);
    }

    private void checkCanSignIn(AppUser user, Instant now, ClientInfo client) {
        String reason = null;
        String message = null;
        if (!user.isEnabled()) {
            reason = "ACCOUNT_DISABLED";
            message = "Your account is disabled. Contact the administrator.";
        } else if (user.isLocked()) {
            reason = "ACCOUNT_LOCKED";
            message = "Your account is locked. Contact the administrator.";
        } else if (user.isTemporarilyLocked(now)) {
            long minutes = Math.max(1, Duration.between(now, user.getLockedUntil()).toMinutes());
            reason = "ACCOUNT_TEMPORARILY_LOCKED";
            message = "Too many failed attempts. Try again in " + minutes + " minute(s).";
        } else if (!user.getEmployee().getStatus().canSignIn()) {
            reason = "EMPLOYEE_INACTIVE";
            message = "Your employment status does not allow sign-in.";
        }
        if (reason != null) {
            recordLogin(user.getUsername(), user.getId(), false, reason, client, now);
            throw new UnauthorizedException(reason, message);
        }
    }

    private void registerFailedAttempt(AppUser user, Instant now) {
        IdentityProperties.Login policy = properties.login();
        int attempts = user.getFailedAttempts() + 1;
        user.setFailedAttempts(attempts);
        if (attempts >= policy.permanentLockAfter()) {
            user.setLocked(true);
        } else if (attempts % policy.maxFailedAttempts() == 0) {
            user.setLockedUntil(now.plus(policy.lockDuration()));
        }
    }

    private TokenResponse issueTokens(AppUser user, ClientInfo client, Instant now) {
        IdentityProperties.Jwt jwt = properties.jwt();
        String refreshValue = tokenService.newRefreshTokenValue();
        RefreshToken refresh = new RefreshToken();
        refresh.setUser(user);
        refresh.setTokenHash(TokenService.sha256(refreshValue));
        refresh.setCreatedAt(now);
        refresh.setExpiresAt(now.plus(jwt.refreshTokenTtl()));
        refresh.setClientIp(client.ip());
        refreshTokenRepository.save(refresh);

        return new TokenResponse(
                tokenService.issueAccessToken(user),
                refreshValue,
                "Bearer",
                jwt.accessTokenTtl().toSeconds(),
                jwt.refreshTokenTtl().toSeconds(),
                user.isMustChangePassword() || isPasswordExpired(user, now));
    }

    private boolean isPasswordExpired(AppUser user, Instant now) {
        int expiryDays = properties.password().expiryDays();
        return expiryDays > 0 && user.getPasswordChangedAt() != null
                && user.getPasswordChangedAt().plus(Duration.ofDays(expiryDays)).isBefore(now);
    }

    private void recordLogin(String username, Long userId, boolean success, String reason, ClientInfo client, Instant now) {
        LoginHistory entry = new LoginHistory();
        entry.setUsername(username.length() > 50 ? username.substring(0, 50) : username);
        entry.setUserId(userId);
        entry.setSuccess(success);
        entry.setFailureReason(reason);
        entry.setClientIp(client.ip());
        String ua = client.userAgent();
        entry.setUserAgent(ua != null && ua.length() > 300 ? ua.substring(0, 300) : ua);
        entry.setCreatedAt(now);
        loginHistoryRepository.save(entry);
    }
}
