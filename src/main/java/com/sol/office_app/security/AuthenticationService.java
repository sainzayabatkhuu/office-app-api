package com.sol.office_app.security;

import com.sol.office_app.dto.LoginDto;
import com.sol.office_app.dto.LoginResponse;
import com.sol.office_app.entity.User;
import com.sol.office_app.repository.RoleRepository;
import com.sol.office_app.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class AuthenticationService {

    @Value("${security.jwt.login.attemps}")
    private int MAX_FAILED_ATTEMPTS;
    @Value("${security.jwt.lock.time}")
    private long LOCK_TIME_DURATION; // 15 minutes
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
//    private final UserLoginHistoryRepository userLoginHistoryRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
//    private final UserLoginHistoryService userLoginHistoryService;

    public AuthenticationService(
            UserRepository userRepository,
            AuthenticationManager authenticationManager,
            PasswordEncoder passwordEncoder,
            RoleRepository roleRepository,
            //UserLoginHistoryRepository userLoginHistoryRepository, UserLoginHistoryService userLoginHistoryService,
            JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
//        this.userLoginHistoryRepository = userLoginHistoryRepository;
        this.jwtService = jwtService;
//        this.userLoginHistoryService = userLoginHistoryService;
    }

//    public User signup(RegisterUserDto input) {
//        User user = new User();
//        user.setEmail(input.getEmail());
//        user.setPassword(passwordEncoder.encode(input.password()));
//        user.setFontSize("small");
//        //user.addRole(roleRepository.findByName("ROLE_VIEWER"));
//        user.addRole(roleRepository.findByName("ROLE_SYSTADM"));
//        return userRepository.save(user);
//    }
//
    public LoginResponse authenticate(HttpServletRequest req, LoginDto input) {

        User user = userRepository.findByEmail(input.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            input.username(),
                            input.password()
                    )
            );
            // update font-size and theme name
            if (user.getFontSize() == null || user.getFontSize().equalsIgnoreCase(""))
                user.setFontSize("small");
            if (user.getThemeName() == null || user.getThemeName().equalsIgnoreCase(""))
                user.setThemeName("CADER_GREEN");
            user.setExpirationTime(LocalDateTime.now());

            userRepository.save(user);
            userRepository.flush();

            //userLoginHistoryService.logLogin(user, req.getRemoteAddr(), true);
            // don't forget it. if you forget this one, you will face with Deadlock error
            //userLoginHistoryRepository.flush();
        } catch (BadCredentialsException ex) {
            if(user.isEnabled() && user.isAccountNonLocked()) {
                if (user.getFailedAttempt() < MAX_FAILED_ATTEMPTS - 1) {
                    user.setFailedAttempt(user.getFailedAttempt() + 1);
                } else {
                    user.setAccountNonLocked(false);
                    user.setLockTime(LocalDateTime.now());
                    throw new RuntimeException("Your account has been locked due to " + MAX_FAILED_ATTEMPTS + " failed attempts."
                            + " It will be unlocked after 15 minutes.");
                }
                user = userRepository.save(user);
                userRepository.flush();

                //userLoginHistoryService.logLogin(user, req.getRemoteAddr(), false);
                //userLoginHistoryRepository.flush();

            } else if (!user.isAccountNonLocked()) {
                if (this.unlockWhenTimeExpired(user)) {
                    throw new RuntimeException("Your account has been unlocked. Please try to login again.");
                }
            }
            throw new RuntimeException("invalid access");
        }

        String jwtToken = jwtService.generateToken(user);

        return new LoginResponse(jwtToken,jwtService.getExpirationTime(), null);
    }

    public boolean unlockWhenTimeExpired(User user) {

        OffsetDateTime offsetDateTime = user.getLockTime().atOffset(ZoneOffset.systemDefault().getRules().getOffset(user.getLockTime()));
        Instant instant = offsetDateTime.toInstant();
        long lockTimeInMillis = instant.toEpochMilli();

        long currentTimeInMillis = System.currentTimeMillis();

        if (lockTimeInMillis + LOCK_TIME_DURATION < currentTimeInMillis) {
            user.setAccountNonLocked(true);
            user.setLockTime(null);
            user.setFailedAttempt(0);
            userRepository.save(user);
            return true;
        }

        return false;
    }
}
