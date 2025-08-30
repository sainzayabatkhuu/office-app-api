package com.sol.office_app.service;

import com.sol.office_app.config.JwtUtils;
import com.sol.office_app.dto.request.LoginDto;
import com.sol.office_app.dto.response.LoginResponse;
import com.sol.office_app.entity.Role;
import com.sol.office_app.entity.User;
import com.sol.office_app.repository.RoleRepository;
import com.sol.office_app.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuthenticationService {

    @Value("${security.jwt.login.attemps}")
    private int MAX_FAILED_ATTEMPTS;
    @Value("${security.jwt.lock.time}")
    private long LOCK_TIME_DURATION; // 15 minutes
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtUtils jwtUtils;

    public AuthenticationService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.jwtUtils = jwtUtils;
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

        User user = userRepository.findByUsername(input.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
        try {
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

        Set<Role> roles = user.getRoles();
        Map<String, Set<String>> rules = new HashMap<>();
        rules.put("get", new HashSet<>());
        rules.put("post", new HashSet<>());
        rules.put("put", new HashSet<>());
        rules.put("delete", new HashSet<>());
        roles.forEach(role -> {
            role.getPrivileges().forEach(p -> {
                System.out.println(p);
                String[] pragments = p.getName().split(":");
                String method = pragments[0];
                Set<String> urls = Arrays.stream(pragments[1].split(",")).collect(Collectors.toSet());
                Set<String> original = rules.get(method);
                original.addAll(urls);
                System.out.println(urls);
                rules.put(method, original);
            });
        });

        String jwtToken = jwtUtils.generateToken(
                user.getUsername(),

                user.getRoles().stream().map(Role::getName).collect(Collectors.toList()),
                rules);

        return new LoginResponse(jwtToken,jwtUtils.getExpirationTime(), null);
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
