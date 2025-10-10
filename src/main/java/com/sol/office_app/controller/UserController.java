package com.sol.office_app.controller;

import com.sol.office_app.common.Constant;
import com.sol.office_app.config.SecurityRule;
import com.sol.office_app.dto.*;
import com.sol.office_app.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(Constant.USER_URL_PREFIX)
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping()
    @SecurityRule
    public ResponseEntity<Page<UserDTO>> index(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "10") int size){
        return new ResponseEntity<>(userService.findAll(PageRequest.of(page, size)), HttpStatus.OK);
    }

    @GetMapping("/{id:\\d+}")
    @SecurityRule
    public ResponseEntity<UserDTO> get(@PathVariable("id") Long id){
        return userService.get(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping
    @SecurityRule
    public ResponseEntity<UserDTO> save(@RequestBody UserDTO user) {
        Optional<UserDTO> savedUser = userService.save(user);
        return savedUser.map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PutMapping("/{id:\\d+}")
    @SecurityRule
    public ResponseEntity<UserDTO> update(@PathVariable Long id, @RequestBody UserDTO user) {
        Optional<UserDTO> updatedUserOpt = userService.update(id, user);
        return updatedUserOpt
                .map(userDTO -> new ResponseEntity<>(userDTO, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/{id:\\d+}")
    @SecurityRule
    public ResponseEntity<UserDTO> delete(@PathVariable Long id) {
        userService.delete(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

//    @PostMapping("/me/refresh-token")
//    public ResponseEntity<LoginResponse> refreshToken(Principal principal) {
//        return ResponseEntity.ok(userService.refreshToken(principal));
//    }

    @GetMapping("/me")
    public ResponseEntity<ProfileDTO> me(Principal principal) {
        ProfileDTO objProfile = userService.me(principal);
        return ResponseEntity.ok(objProfile);
    }

    @PutMapping("/me")
    public ResponseEntity<ProfileDTO> me(Principal principal, @RequestBody Map<String, String> body) {
        Optional<ProfileDTO> objProfile = userService.saveSettings(principal,
                body.get("amountFormat"),
                body.get("dateFormat"),
                body.get("fontSize"),
                body.get("theme"),
                body.get("showDash"),
                body.get("alertOnHome"),
                body.get("numbermask")
        );
        return objProfile
                .map(userDTO -> new ResponseEntity<>(userDTO, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{id:\\d+}/reset-password")
    @SecurityRule
    public ResponseEntity<UserDTO> resetPassword(@PathVariable Long id, @RequestBody UserDTO user) {
        Optional<UserDTO> updatedUserOpt = userService.update(id, user);
        return updatedUserOpt
                .map(userDTO -> new ResponseEntity<>(userDTO, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> loginHistory(
            Principal principal,
            @RequestBody Map<String, String> body
    ) {
        Optional<NotificationMessage> changePassword = userService.changePassword(
                principal,
                body.get("currentPassword"),
                body.get("password"),
                body.get("confirmPassword"));
        return ResponseEntity.ok(changePassword);
    }

//    @GetMapping("/login-history")
//    public ResponseEntity<?> loginHistory(Principal principal) {
//        LoginDetailsForUserDTO objLoginDetailsForUser = userService.loginHistory(principal);
//        return ResponseEntity.ok(objLoginDetailsForUser);
//    }

    @GetMapping("/user-role-mapping")
    public ResponseEntity<Page<UserRolesMappingDTO>> userRoleMapping(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort) {
        Page<UserRolesMappingDTO> userRolesMappings = userService.userRoleMapping(principal, PageRequest.of(page, size));
        return ResponseEntity.ok(userRolesMappings);
    }

    @GetMapping("/user-limits")
    public ResponseEntity<Profile> userLimits(Principal principal) {
        Profile objProfile = userService.userLimits(principal);
        return ResponseEntity.ok(objProfile);
    }

    @GetMapping("/disallowed-branches")
    public ResponseEntity<Profile> disallowedBranches(Principal principal) {
        Profile objProfile = userService.disallowedBranches(principal);
        return ResponseEntity.ok(objProfile);
    }

    @GetMapping("/allowed-branches")
    public ResponseEntity<Page<BranchDTO>> allowedBranches(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10")  int size,
            @RequestParam(defaultValue = "branchCode,asc") String[] sort) {

        List<Sort.Order> orders = new ArrayList<>();
        if (sort[0].contains(",")) {
            for (String sortOrder : sort) {
                String[] _sort = sortOrder.split(",");
                orders.add(new Sort.Order(Sort.Direction.fromString(_sort[1]), _sort[0]));
            }
        } else {
            orders.add(new Sort.Order(Sort.Direction.fromString(sort[1]), sort[0]));
        }

        Page<BranchDTO> allowedBranches = userService.allowedBranches(principal, PageRequest.of(page, size, Sort.by(orders)));
        return new ResponseEntity<>(allowedBranches, HttpStatus.OK);
    }
}
