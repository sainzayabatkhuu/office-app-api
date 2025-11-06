package com.sol.office_app.service;

import com.sol.office_app.config.JwtUtils;
import com.sol.office_app.domain.request.LoginDto;
import com.sol.office_app.domain.response.LoginResponse;
import com.sol.office_app.entity.Role;
import com.sol.office_app.entity.User;
import com.sol.office_app.repository.RoleRepository;
import com.sol.office_app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.time.*;
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
    public LoginResponse authenticate(LoginDto input) {
        Optional<User> optionalUser = userRepository.findByUsername(input.username());

        if (optionalUser.isEmpty()) {
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "User you are referring doesn't exist");
        }

        User user = optionalUser.get();

        if (user.getLockTime() != null && System.currentTimeMillis() - user.getLockTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() < LOCK_TIME_DURATION) {
            throw new HttpClientErrorException(HttpStatus.FORBIDDEN, "Your account is temporarily locked. Wait for 15 minutes before attempting!");
        }

        if (!user.isAccountNonLocked()) {
            throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Your account is permanently locked. Contact admin to resolve the issue!");
        }

        BCryptPasswordEncoder bp = new BCryptPasswordEncoder();
        if (!bp.matches(input.password(), user.getPassword())) {
            user.setFailedAttempt(user.getFailedAttempt() + 1);
            if (user.getFailedAttempt() >= 3) {
                user.setLockTime(LocalDateTime.now());
            }
            if (user.getFailedAttempt() >= 10) {
                user.setAccountNonLocked(false);
            }
            userRepository.save(user);
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Username or Password is not correct. ");
        } else {
            user.setLockTime(null);
            user.setFailedAttempt(0);
            userRepository.save(user);
        }

        Set<Role> roles = user.getRoles();
        Map<String, Set<String>> rules = new HashMap<>();
        rules.put("get", new HashSet<>());
        rules.put("post", new HashSet<>());
        rules.put("put", new HashSet<>());
        rules.put("delete", new HashSet<>());
        roles.forEach(role -> {
            role.getPrivileges().forEach(p -> {
                String[] pragments = p.getValue().split(":");
                String method = pragments[0];
                Set<String> urls = Arrays.stream(pragments[1].split(",")).collect(Collectors.toSet());
                Set<String> original = rules.get(method);
                original.addAll(urls);
                System.out.println(urls);
                rules.put(method, original);
            });
        });
        //permissions надад эний оронд Privilege байгаа тэр нь value, description

        String jwtToken = jwtUtils.generateToken(
                user.getUsername(),
                user.getBranch().getSolId(),
                user.isMultiBrnchAccess(),
                user.getRoles().stream().map(Role::getName).collect(Collectors.toList()),
                rules);

        return new LoginResponse(jwtToken, jwtUtils.getExpirationTime(), null);
    }
}
