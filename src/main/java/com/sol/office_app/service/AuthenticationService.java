package com.sol.office_app.service;

import com.sol.office_app.config.JwtUtils;
import com.sol.office_app.domain.request.LoginDto;
import com.sol.office_app.domain.response.LoginResponse;
import com.sol.office_app.entity.RefreshToken;
import com.sol.office_app.entity.Role;
import com.sol.office_app.entity.User;
import com.sol.office_app.exception.ApiException;
import com.sol.office_app.repository.RefreshTokenRepository;
import com.sol.office_app.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);
    /** Same message for unknown users and wrong passwords so usernames cannot be probed. */
    private static final String INVALID_CREDENTIALS = "Username or password is not correct.";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final int maxFailedAttempts;
    private final int permanentLockAttempts;
    private final long lockTimeDuration;
    private final long refreshTokenTtl;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public AuthenticationService(
            @Value("${security.jwt.login.attemps}") int maxFailedAttempts,
            @Value("${security.jwt.login.permanent-lock-attempts:10}") int permanentLockAttempts,
            @Value("${security.jwt.lock.time}") long lockTimeDuration,
            @Value("${security.jwt.refresh-expiration-time}") long refreshTokenTtl,
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtUtils jwtUtils) {
        this.maxFailedAttempts = maxFailedAttempts;
        this.permanentLockAttempts = permanentLockAttempts;
        this.lockTimeDuration = lockTimeDuration;
        this.refreshTokenTtl = refreshTokenTtl;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    /** Failed attempts must be saved even though the method throws, hence {@code noRollbackFor}. */
    @Transactional(noRollbackFor = ApiException.class)
    public LoginResponse authenticate(LoginDto input) {
        if (input == null || input.username() == null || input.password() == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "Username and password are required.");
        }
        User user = userRepository.findByUsername(input.username())
                .orElseThrow(() -> ApiException.unauthorized("INVALID_CREDENTIALS", INVALID_CREDENTIALS));

        checkCanSignIn(user);

        if (!passwordEncoder.matches(input.password(), user.getPassword())) {
            int attempts = user.getFailedAttempt() + 1;
            user.setFailedAttempt(attempts);
            if (attempts < permanentLockAttempts && attempts % maxFailedAttempts == 0) {
                user.setLockTime(LocalDateTime.now());
            }
            userRepository.save(user);
            throw ApiException.unauthorized("INVALID_CREDENTIALS", INVALID_CREDENTIALS);
        }

        user.setLockTime(null);
        user.setFailedAttempt(0);
        userRepository.save(user);
        return issueTokens(user);
    }

    /**
     * Exchanges a refresh token for a new pair. The old token is revoked (rotation); presenting a revoked
     * token again means it was probably stolen, so every session of that user is revoked.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public LoginResponse refresh(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            throw ApiException.unauthorized("INVALID_REFRESH_TOKEN", "Refresh token is invalid.");
        }
        LocalDateTime now = LocalDateTime.now();
        RefreshToken token = refreshTokenRepository.findByTokenHash(sha256(refreshTokenValue))
                .orElseThrow(() -> ApiException.unauthorized("INVALID_REFRESH_TOKEN", "Refresh token is invalid."));

        if (token.getRevokedAt() != null) {
            int revoked = refreshTokenRepository.revokeAllForUser(token.getUser().getId(), now);
            log.warn("Refresh token reuse detected for user id {}; revoked {} sessions", token.getUser().getId(), revoked);
            throw ApiException.unauthorized("INVALID_REFRESH_TOKEN", "Refresh token is invalid.");
        }
        if (!token.isActive(now)) {
            throw ApiException.unauthorized("REFRESH_TOKEN_EXPIRED", "Session expired. Please sign in again.");
        }
        User user = token.getUser();
        checkCanSignIn(user);
        token.setRevokedAt(now);
        return issueTokens(user);
    }

    @Transactional
    public void logout(String username, String refreshTokenValue) {
        if (refreshTokenValue == null) {
            return;
        }
        refreshTokenRepository.findByTokenHash(sha256(refreshTokenValue))
                .filter(t -> t.getUser().getUsername().equals(username))
                .filter(t -> t.getRevokedAt() == null)
                .ifPresent(t -> t.setRevokedAt(LocalDateTime.now()));
    }

    /** Signs a user out everywhere, e.g. after a password change or reset. */
    @Transactional
    public void revokeAllSessions(User user) {
        refreshTokenRepository.revokeAllForUser(user.getId(), LocalDateTime.now());
    }

    @Scheduled(cron = "0 30 2 * * *")
    @Transactional
    public void purgeExpiredRefreshTokens() {
        int deleted = refreshTokenRepository.deleteExpiredBefore(LocalDateTime.now().minusDays(1));
        log.info("Purged {} expired refresh tokens", deleted);
    }

    private void checkCanSignIn(User user) {
        if (!user.isEnabled()) {
            throw ApiException.unauthorized("ACCOUNT_DISABLED", "Your account is disabled. Contact the administrator.");
        }
        // The account_non_locked column is not reliable in existing data, so the permanent lock is derived
        // from the failure counter; an administrator unlock resets it.
        if (user.getFailedAttempt() >= permanentLockAttempts) {
            throw ApiException.unauthorized("ACCOUNT_LOCKED", "Your account is locked. Contact the administrator.");
        }
        if (user.getLockTime() != null) {
            LocalDateTime unlockAt = user.getLockTime().plus(Duration.ofMillis(lockTimeDuration));
            if (unlockAt.isAfter(LocalDateTime.now())) {
                long minutes = Math.max(1, Duration.between(LocalDateTime.now(), unlockAt).toMinutes());
                throw ApiException.unauthorized("ACCOUNT_TEMPORARILY_LOCKED",
                        "Too many failed attempts. Try again in " + minutes + " minute(s).");
            }
        }
    }

    private LoginResponse issueTokens(User user) {
        String accessToken = jwtUtils.generateToken(
                user.getUsername(),
                user.getBranch().getSolId(),
                user.isMultiBrnchAccess(),
                user.getRoles().stream().map(Role::getName).collect(Collectors.toList()),
                buildRules(user.getRoles()));

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String refreshValue = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        LocalDateTime now = LocalDateTime.now();
        RefreshToken refresh = new RefreshToken();
        refresh.setUser(user);
        refresh.setTokenHash(sha256(refreshValue));
        refresh.setCreatedAt(now);
        refresh.setExpiresAt(now.plus(Duration.ofMillis(refreshTokenTtl)));
        refreshTokenRepository.save(refresh);

        return new LoginResponse(accessToken, refreshValue, jwtUtils.getExpirationTime(), null);
    }

    /**
     * Privilege values look like {@code get:/reports/*,get:/reports/report-list}: comma-separated
     * {@code method:path} pairs. Paths are relative to {@code /api/v1}.
     */
    static Map<String, Set<String>> buildRules(Collection<Role> roles) {
        Map<String, Set<String>> rules = new HashMap<>();
        for (String method : List.of("get", "post", "put", "delete")) {
            rules.put(method, new TreeSet<>());
        }
        roles.forEach(role -> role.getPrivileges().forEach(p -> {
            if (p.getValue() == null) {
                return;
            }
            for (String entry : p.getValue().split(",")) {
                int colon = entry.indexOf(':');
                if (colon <= 0) {
                    continue;
                }
                String method = entry.substring(0, colon).trim().toLowerCase(Locale.ROOT);
                String path = entry.substring(colon + 1).trim();
                if (!path.isEmpty()) {
                    rules.computeIfAbsent(method, m -> new TreeSet<>()).add(path);
                }
            }
        }));
        return rules;
    }

    static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
