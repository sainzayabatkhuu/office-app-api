package com.sol.office_app.controller;

import com.sol.office_app.common.Constant;
import com.sol.office_app.domain.request.LoginDto;
import com.sol.office_app.domain.response.LoginResponse;
import com.sol.office_app.service.AuthenticationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping(Constant.AUTH_URL_PREFIX)
@RestController
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

//    @PostMapping("/signup")
//    public ResponseEntity<User> register(@RequestBody RegisterUserDto registerUserDto) {
//        User registeredUser = authenticationService.signup(registerUserDto);
//
//        return ResponseEntity.ok(registeredUser);
//    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticate( @RequestBody LoginDto loginDto) {
        LoginResponse loginResponse = authenticationService.authenticate( loginDto);
        return ResponseEntity.ok(loginResponse);
    }
}
