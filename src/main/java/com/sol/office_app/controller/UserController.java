package com.sol.office_app.controller;

import com.sol.office_app.domain.request.UserRequest;
import com.sol.office_app.domain.response.TemporaryPasswordResponse;

import com.sol.office_app.common.Constant;
import com.sol.office_app.domain.response.NotificationMessage;
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

    /** Users, optionally filtered (all filters are case-insensitive "contains", branch is exact). */
    @GetMapping()
    public ResponseEntity<Page<UserDTO>> index(@RequestParam(defaultValue = "") String username,
                                               @RequestParam(defaultValue = "") String name,
                                               @RequestParam(defaultValue = "") String branchCode,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "10") int size){
        return new ResponseEntity<>(userService.search(username, name, branchCode,
                PageRequest.of(page, size, Sort.by("username"))), HttpStatus.OK);
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<UserDTO> get(@PathVariable("id") Long id){
        return userService.get(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /** Returns the temporary password once; the user must change it at first sign-in. */
    @PostMapping
    public ResponseEntity<TemporaryPasswordResponse> save(@RequestBody UserRequest user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(user));
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<UserDTO> update(@PathVariable Long id, @RequestBody UserRequest user) {
        Optional<UserDTO> updatedUserOpt = userService.update(id, user);
        return updatedUserOpt
                .map(userDTO -> new ResponseEntity<>(userDTO, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/{id:\\d+}")
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
        Optional<ProfileDTO> objProfile = userService.saveSettings(principal, body);
        return objProfile
                .map(userDTO -> new ResponseEntity<>(userDTO, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /** Generates a temporary password (returned once), unlocks the account and ends its sessions. */
    @PutMapping("/{id:\\d+}/reset-password")
    public ResponseEntity<TemporaryPasswordResponse> resetPassword(@PathVariable Long id) {
        return ResponseEntity.ok(userService.resetPassword(id));
    }

    @PutMapping("/{id:\\d+}/unlock")
    public ResponseEntity<UserDTO> unlock(@PathVariable Long id) {
        return ResponseEntity.ok(userService.unlock(id));
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(
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
//    public ResponseEntity<?> changePassword(Principal principal) {
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
