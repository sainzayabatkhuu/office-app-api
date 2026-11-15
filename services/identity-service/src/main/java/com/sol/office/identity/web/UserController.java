package com.sol.office.identity.web;

import com.sol.office.common.api.PageResponse;
import com.sol.office.common.security.SecurityUtils;
import com.sol.office.identity.dto.UserDtos.CreateUserRequest;
import com.sol.office.identity.dto.UserDtos.TemporaryPasswordResponse;
import com.sol.office.identity.dto.UserDtos.UpdateUserRequest;
import com.sol.office.identity.dto.UserDtos.UserResponse;
import com.sol.office.identity.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Users")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public PageResponse<UserResponse> search(@RequestParam(required = false) String q,
                                             @RequestParam(required = false) String branchCode,
                                             @RequestParam(required = false) Boolean enabled,
                                             @PageableDefault(size = 20, sort = "username", direction = Sort.Direction.ASC) Pageable pageable) {
        return userService.search(SecurityUtils.requireCurrentUser(), q, branchCode, enabled, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public UserResponse get(@PathVariable Long id) {
        return userService.get(SecurityUtils.requireCurrentUser(), id);
    }

    @Operation(summary = "Grant a sign-in account to an existing employee")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public TemporaryPasswordResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(SecurityUtils.requireCurrentUser(), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.update(SecurityUtils.requireCurrentUser(), id, request);
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public TemporaryPasswordResponse resetPassword(@PathVariable Long id) {
        return userService.resetPassword(SecurityUtils.requireCurrentUser(), id);
    }

    @PostMapping("/{id}/unlock")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public UserResponse unlock(@PathVariable Long id) {
        return userService.unlock(SecurityUtils.requireCurrentUser(), id);
    }
}
