package com.sol.office_app.service;

import com.sol.office_app.common.GeneralService;
import com.sol.office_app.domain.request.UserRequest;
import com.sol.office_app.domain.response.NotificationMessage;
import com.sol.office_app.domain.response.TemporaryPasswordResponse;
import com.sol.office_app.entity.Role;
import com.sol.office_app.exception.ApiException;
import com.sol.office_app.repository.RefreshTokenRepository;
import com.sol.office_app.repository.RoleRepository;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import com.sol.office_app.dto.*;
import com.sol.office_app.entity.Branch;
import com.sol.office_app.entity.User;
import com.sol.office_app.entity.UserRoles;
import com.sol.office_app.mapper.BranchDTOMapper;
import com.sol.office_app.mapper.UserDTOMapper;
import com.sol.office_app.mapper.UserRolesDTOMapper;
import com.sol.office_app.mapper.UserRolesMappingDTOMapper;
import com.sol.office_app.repository.BranchRepository;
import com.sol.office_app.repository.CoreParameterRepository;
import com.sol.office_app.repository.UserRepository;
import com.sol.office_app.repository.UserRolesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService implements GeneralService<UserDTO, Long> {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int DEFAULT_PASSWORD_EXPIRY_DAYS = 90;
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("mn", "en");

    @Autowired
    private UserDTOMapper userDTOMapper;
    @Autowired
    private BranchDTOMapper branchDTOMapper;
    @Autowired
    private UserRolesMappingDTOMapper userRolesMappingDTOMapper;
    @Autowired
    private UserRolesDTOMapper userRolesDTOMapper;


    @Autowired
    private BranchRepository branchRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserRolesRepository userRolesRepository;
    @Autowired
    private CoreParameterRepository coreParameterRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @PersistenceContext
    private EntityManager entityManager;

//    @Autowired
//    private UserLoginHistoryService userLoginHistoryService;


    @Override
    public Page<UserDTO> findAll(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        return users.map(userDTOMapper);
    }

    @Override
    public Page<UserDTO> search(String query, Pageable pageable) {
        return null;
    }

    @Override
    public Optional<UserDTO> get(Long id) {
        return userRepository.findById(id).map(userDTOMapper);
    }

    /** Interface method kept for {@link GeneralService}; the controller uses {@link #create}. */
    public Optional<UserDTO> save(UserDTO user) {
        throw new UnsupportedOperationException("Use create(UserRequest)");
    }

    public Optional<UserDTO> update(Long id, UserDTO updatedUser) {
        throw new UnsupportedOperationException("Use update(Long, UserRequest)");
    }

    /** Creates a user who must change the (temporary) password at first sign-in. */
    @Transactional
    public TemporaryPasswordResponse create(UserRequest request) {
        if (isBlank(request.username()) || isBlank(request.email())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "Username and email are required.");
        }
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw ApiException.conflict("Username already exists: " + request.username());
        }
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw ApiException.conflict("Email already exists: " + request.email());
        }
        String password = isBlank(request.password()) ? generateTemporaryPassword() : request.password();
        validatePasswordPolicy(password);

        User user = new User();
        user.setUsername(request.username().trim());
        user.setPassword(passwordEncoder.encode(password));
        user.setAccountNonLocked(true);
        user.setFailedAttempt(0);
        user.setTokenExpired(false);
        user.setFontSize("font-s");
        user.setLastPasswordChangeDate(LocalDate.now().minusDays(1)); // in the past = must change now
        apply(user, request);
        userRepository.save(user);
        assignRoles(user, request.roleNames());
        return new TemporaryPasswordResponse(user.getUsername(), password);
    }

    @Transactional
    public Optional<UserDTO> update(Long id, UserRequest request) {
        Optional<User> existing = userRepository.findById(id);
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        User user = existing.get();
        if (!isBlank(request.email()) && !request.email().equals(user.getEmail())
                && userRepository.findByEmail(request.email()).isPresent()) {
            throw ApiException.conflict("Email already exists: " + request.email());
        }
        boolean wasEnabled = user.isEnabled();
        apply(user, request);
        if (wasEnabled && !user.isEnabled()) {
            refreshTokenRepository.revokeAllForUser(user.getId(), LocalDateTime.now());
        }
        userRepository.saveAndFlush(user);
        if (request.roleNames() != null) {
            assignRoles(user, request.roleNames());
            entityManager.flush();
            entityManager.refresh(user);
        }
        return Optional.of(userDTOMapper.apply(user));
    }

    /** New temporary password; unlocks the account and signs the user out everywhere. */
    @Transactional
    public TemporaryPasswordResponse resetPassword(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> ApiException.notFound("User", id));
        String password = generateTemporaryPassword();
        user.setPassword(passwordEncoder.encode(password));
        user.setLastPasswordChangeDate(LocalDate.now().minusDays(1));
        user.setFailedAttempt(0);
        user.setLockTime(null);
        userRepository.save(user);
        refreshTokenRepository.revokeAllForUser(user.getId(), LocalDateTime.now());
        return new TemporaryPasswordResponse(user.getUsername(), password);
    }

    @Transactional
    public UserDTO unlock(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> ApiException.notFound("User", id));
        user.setFailedAttempt(0);
        user.setLockTime(null);
        return userDTOMapper.apply(userRepository.save(user));
    }

    private void apply(User user, UserRequest request) {
        if (!isBlank(request.email())) user.setEmail(request.email().trim());
        user.setFirstname(request.firstname());
        user.setLastname(request.lastname());
        user.setUserLanguage(isBlank(request.userLanguage()) ? "mn" : request.userLanguage());
        user.setMultiBrnchAccess(Boolean.TRUE.equals(request.multiBrnchAccess()));
        user.setIsActive(request.enabled() == null || request.enabled());
        if (isBlank(request.branchCode())) {
            if (user.getBranch() == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "Branch is required.");
            }
        } else {
            user.setBranch(branchRepository.findById(request.branchCode())
                    .orElseThrow(() -> ApiException.notFound("Branch", request.branchCode())));
        }
    }

    /**
     * users_roles is mapped twice (User.roles and the UserRoles entity, which requires branch_code), so roles
     * are written as UserRoles rows at the user's branch; User.roles is only used for reading.
     */
    private void assignRoles(User user, Set<String> roleNames) {
        if (roleNames == null) {
            return;
        }
        List<Role> roles = roleRepository.findByNameIn(roleNames);
        if (roles.size() != roleNames.size()) {
            throw ApiException.badRequest("UNKNOWN_ROLE", "One or more roles do not exist: " + roleNames);
        }
        userRolesRepository.deleteByUserId(user.getId());
        for (Role role : roles) {
            UserRoles link = new UserRoles();
            link.setUser(user);
            link.setRole(role);
            link.setBranch(user.getBranch());
            userRolesRepository.save(link);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** At least 8 characters with letters and digits. */
    static void validatePasswordPolicy(String password) {
        if (password == null || password.length() < 8
                || password.chars().noneMatch(Character::isLetter)
                || password.chars().noneMatch(Character::isDigit)) {
            throw ApiException.badRequest("WEAK_PASSWORD",
                    "Password must be at least 8 characters and contain letters and digits.");
        }
    }

    /** Readable random password that satisfies the policy (no 0/O, 1/l). */
    static String generateTemporaryPassword() {
        String letters = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
        String digits = "23456789";
        String all = letters + digits;
        StringBuilder sb = new StringBuilder()
                .append(letters.charAt(RANDOM.nextInt(letters.length())))
                .append(digits.charAt(RANDOM.nextInt(digits.length())));
        for (int i = 0; i < 10; i++) {
            sb.append(all.charAt(RANDOM.nextInt(all.length())));
        }
        return sb.toString();
    }

    @Override
    public void delete(Long id) {
        userRepository.deleteById(id);
    }

//    @Override
//    public LoginResponse refreshToken(Principal principal) {
//        var user = userRepository.findByEmail(principal.getName()).orElseThrow();
//
//        var accessToken = jwtService.generateToken(user);
//
//        var refreshToken = jwtService.generateToken(user);
//
//        return new LoginResponse(accessToken, refreshToken, 0L);
//    }


    public ProfileDTO me(Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> ApiException.notFound("User", principal.getName()));
        return toProfile(user);
    }

    /** Saves the signed-in user's display preferences; absent keys keep their current value. */
    public Optional<ProfileDTO> saveSettings(Principal principal, Map<String, String> body) {
        Optional<User> found = userRepository.findByUsername(principal.getName());
        if (found.isEmpty()) {
            return Optional.empty();
        }
        User user = found.get();
        if (body.containsKey("amountFormat")) user.setAmountFormat(body.get("amountFormat"));
        if (body.containsKey("dateFormat")) user.setDateFormat(body.get("dateFormat"));
        if (body.containsKey("fontSize")) user.setFontSize(body.get("fontSize"));
        if (body.containsKey("theme")) user.setThemeName(body.get("theme"));
        if (body.containsKey("showDash")) user.setShowDash(body.get("showDash"));
        if (body.containsKey("alertOnHome")) user.setAlertOnHome(body.get("alertOnHome"));
        if (body.containsKey("numbermask")) user.setNumberMask(body.get("numbermask"));
        if (body.containsKey("userLanguage")) {
            String language = body.get("userLanguage");
            if (!SUPPORTED_LANGUAGES.contains(language)) {
                throw ApiException.badRequest("VALIDATION_FAILED", "userLanguage must be one of " + SUPPORTED_LANGUAGES);
            }
            user.setUserLanguage(language);
        }
        return Optional.of(toProfile(userRepository.save(user)));
    }

    /**
     * {@code lastPasswordChangeDate} holds the password EXPIRY date (set to today + expiry days on change),
     * so the password must be changed once that date has passed or when it was never set.
     */
    private ProfileDTO toProfile(User user) {
        LocalDate expiry = user.getLastPasswordChangeDate();
        return new ProfileDTO(
                user.getUsername(),
                user.getFirstname(),
                user.getLastname(),
                user.getEmail(),
                user.getFontSize() == null ? null : user.getFontSize().toLowerCase(),
                user.getThemeName(),
                !user.isEnabled() ? "Unenabled" : "Enabled",
                user.getAmountFormat(),
                user.getDateFormat(),
                user.getShowDash(),
                user.getAlertOnHome(),
                user.getNumberMask(),
                user.isMultiBrnchAccess(),
                user.getBranch() == null ? null : user.getBranch().getSolId(),
                user.getUserLanguage(),
                null,
                expiry,
                expiry == null || LocalDate.now().isAfter(expiry)
        );
    }

    @Transactional
    public Optional<NotificationMessage> changePassword(Principal principal,
                                                        String currentPassword,
                                                        String password,
                                                        String confirmPassword) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> ApiException.notFound("User", principal.getName()));
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw ApiException.badRequest("INVALID_CURRENT_PASSWORD", "Current password is incorrect.");
        }
        if (password == null || !password.equals(confirmPassword)) {
            throw ApiException.badRequest("PASSWORD_MISMATCH", "New password and confirmation do not match.");
        }
        if (password.equals(currentPassword)) {
            throw ApiException.badRequest("PASSWORD_REUSED", "New password must differ from the current one.");
        }
        validatePasswordPolicy(password);

        int passwordExpiryDays = coreParameterRepository.findOneByParamType("password_expiry_days")
                .map(p -> Integer.parseInt(p.getParamCode()))
                .orElse(DEFAULT_PASSWORD_EXPIRY_DAYS);
        user.setPassword(passwordEncoder.encode(password));
        user.setLastPasswordChangeDate(LocalDate.now().plusDays(passwordExpiryDays));
        userRepository.save(user);
        // other sessions must sign in again with the new password
        refreshTokenRepository.revokeAllForUser(user.getId(), LocalDateTime.now());
        return Optional.of(new NotificationMessage("success", "Password changed", "Password has been changed.", Map.of()));
    }

//    @Override
//    public LoginDetailsForUserDTO loginHistory(Principal principal) {
//
//        User user = userRepository.findByEmail(principal.getName())
//                .orElseThrow();
//
//        LoginDetailsForUserDTO loginDetailsForUserVO = new LoginDetailsForUserDTO();
//        loginDetailsForUserVO.setNoOfCumulative(userLoginHistoryService.getCumulativeLogins(user));
//        loginDetailsForUserVO.setNoOfSuccessive(userLoginHistoryService.getSuccessiveLogins(user, 5).size());
//        return loginDetailsForUserVO;
//    }
//

    public Page<UserRolesMappingDTO> userRoleMapping(Principal principal, Pageable pageable) {
        Page<UserRoles> userRoles = userRolesRepository.findAll(pageable);
        return userRoles.map(userRolesMappingDTOMapper);
    }

    public Page<BranchDTO> allowedBranches(Principal principal, Pageable pageable) {
        Optional<User> user = userRepository.findByUsername(principal.getName());
        if(user.isPresent()) {
            List<Branch> branches = branchRepository.findByUsername(principal.getName());
            List<BranchDTO> branchDTOs = branches.stream()
                    .flatMap(branch -> branch.getChildBranches().stream())
                    .map(branchDTOMapper)
                    .collect(Collectors.toList());

            return new PageImpl<>(branchDTOs, pageable, branchDTOs.size());
        }

        return null;
    }

    public Profile userLimits(Principal principal) {
        return null;
    }

    public Profile disallowedBranches(Principal principal) {
        return null;
    }
}
