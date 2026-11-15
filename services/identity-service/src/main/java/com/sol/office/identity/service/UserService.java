package com.sol.office.identity.service;

import com.sol.office.common.api.PageResponse;
import com.sol.office.common.exception.BusinessException;
import com.sol.office.common.exception.ConflictException;
import com.sol.office.common.exception.NotFoundException;
import com.sol.office.common.security.CurrentUser;
import com.sol.office.identity.domain.AppUser;
import com.sol.office.identity.domain.Employee;
import com.sol.office.identity.domain.Role;
import com.sol.office.identity.dto.UserDtos.CreateUserRequest;
import com.sol.office.identity.dto.UserDtos.TemporaryPasswordResponse;
import com.sol.office.identity.dto.UserDtos.UpdateUserRequest;
import com.sol.office.identity.dto.UserDtos.UserResponse;
import com.sol.office.identity.mapper.IdentityMapper;
import com.sol.office.identity.repository.AppUserRepository;
import com.sol.office.identity.repository.EmployeeRepository;
import com.sol.office.identity.repository.RefreshTokenRepository;
import com.sol.office.identity.repository.RoleRepository;
import com.sol.office.identity.security.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AppUserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final IdentityMapper mapper;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> search(CurrentUser user, String q, String branchCode, Boolean enabled, Pageable pageable) {
        String branch = BranchScope.effectiveBranch(user, StringUtils.hasText(branchCode) ? branchCode : null);
        return PageResponse.of(userRepository.search(StringUtils.hasText(q) ? q.trim() : null, branch, enabled, pageable),
                mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse get(CurrentUser user, Long id) {
        AppUser account = load(id);
        BranchScope.check(user, account.getEmployee().getBranch().getCode());
        return mapper.toResponse(account);
    }

    /** Grants an account to an existing employee. Returns the temporary password exactly once. */
    @Transactional
    public TemporaryPasswordResponse create(CurrentUser user, CreateUserRequest request) {
        Employee employee = employeeRepository.findWithDetailsById(request.employeeId())
                .orElseThrow(() -> NotFoundException.of("Employee", request.employeeId()));
        BranchScope.check(user, employee.getBranch().getCode());
        if (!employee.getStatus().canSignIn()) {
            throw new BusinessException("EMPLOYEE_INACTIVE", "Employee status " + employee.getStatus() + " cannot have an account");
        }
        if (userRepository.existsById(employee.getId())) {
            throw new ConflictException("Employee already has an account");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("Username already taken: " + request.username());
        }
        if (request.multiBranchAccess() && !user.multiBranchAccess()) {
            throw new BusinessException("CANNOT_GRANT_MULTI_BRANCH", "You cannot grant access you don't have yourself");
        }

        String password = request.password();
        if (password == null) {
            password = PasswordPolicy.generateTemporary();
        } else {
            PasswordPolicy.validate(password);
        }

        AppUser account = new AppUser();
        account.setEmployee(employee);
        account.setUsername(request.username().toLowerCase());
        account.setPasswordHash(passwordEncoder.encode(password));
        account.setPasswordChangedAt(clock.instant());
        account.setMustChangePassword(true);
        account.setMultiBranchAccess(request.multiBranchAccess());
        account.setRoles(resolveRoles(request.roleCodes()));
        userRepository.save(account);
        return new TemporaryPasswordResponse(account.getUsername(), password);
    }

    @Transactional
    public UserResponse update(CurrentUser user, Long id, UpdateUserRequest request) {
        AppUser account = load(id);
        BranchScope.check(user, account.getEmployee().getBranch().getCode());
        if (account.getId().equals(user.userId()) && !request.enabled()) {
            throw new BusinessException("CANNOT_DISABLE_SELF", "You cannot disable your own account");
        }
        if (request.multiBranchAccess() && !user.multiBranchAccess()) {
            throw new BusinessException("CANNOT_GRANT_MULTI_BRANCH", "You cannot grant access you don't have yourself");
        }
        account.setRoles(resolveRoles(request.roleCodes()));
        account.setMultiBranchAccess(request.multiBranchAccess());
        if (account.isEnabled() && !request.enabled()) {
            refreshTokenRepository.revokeAllForUser(id, clock.instant());
        }
        account.setEnabled(request.enabled());
        return mapper.toResponse(account);
    }

    @Transactional
    public TemporaryPasswordResponse resetPassword(CurrentUser user, Long id) {
        AppUser account = load(id);
        BranchScope.check(user, account.getEmployee().getBranch().getCode());
        String password = PasswordPolicy.generateTemporary();
        Instant now = clock.instant();
        account.setPasswordHash(passwordEncoder.encode(password));
        account.setPasswordChangedAt(now);
        account.setMustChangePassword(true);
        clearLock(account);
        refreshTokenRepository.revokeAllForUser(id, now);
        return new TemporaryPasswordResponse(account.getUsername(), password);
    }

    @Transactional
    public UserResponse unlock(CurrentUser user, Long id) {
        AppUser account = load(id);
        BranchScope.check(user, account.getEmployee().getBranch().getCode());
        clearLock(account);
        return mapper.toResponse(account);
    }

    private static void clearLock(AppUser account) {
        account.setLocked(false);
        account.setFailedAttempts(0);
        account.setLockedUntil(null);
    }

    private Set<Role> resolveRoles(Set<String> codes) {
        List<Role> roles = roleRepository.findByCodeIn(codes);
        if (roles.size() != codes.size()) {
            Set<String> missing = new HashSet<>(codes);
            roles.forEach(r -> missing.remove(r.getCode()));
            throw new BusinessException("UNKNOWN_ROLE", "Unknown role(s): " + missing);
        }
        return new LinkedHashSet<>(roles);
    }

    private AppUser load(Long id) {
        return userRepository.findWithDetailsById(id).orElseThrow(() -> NotFoundException.of("User", id));
    }
}
