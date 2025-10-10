package com.sol.office_app.service;

import com.sol.office_app.common.GeneralService;
import com.sol.office_app.config.JwtUtils;
import com.sol.office_app.dto.*;
import com.sol.office_app.entity.Branch;
import com.sol.office_app.entity.CoreParameter;
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

    @Value("${security.jwt.expiration-time}")
    private long jwtExpiration;

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
    private JwtUtils jwtUtils;

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

    public Optional<UserDTO> save(UserDTO user) {
        User newUser = new User();
        newUser.setEmail(user.getEmail());
        newUser.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(newUser);

        return Optional.of(userDTOMapper.apply(newUser));
    }

    public Optional<UserDTO> update(Long id, UserDTO updatedUser) {
        Optional<User> existingUserOpt = userRepository.findById(id);
        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            existingUser.setEmail(updatedUser.getEmail());
            //existingUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
            userRepository.save(existingUser);
            return Optional.of(userDTOMapper.apply(existingUser));
        } else {
            return Optional.empty();
        }
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
        ProfileDTO objProfile = null;

        if(principal != null) {
            User user = userRepository.findByUsername(principal.getName())
                    .orElseThrow();

            OffsetDateTime offsetDateTime = user.getExpirationTime().atOffset(ZoneOffset.systemDefault().getRules().getOffset(user.getExpirationTime()));
            Instant instant = offsetDateTime.toInstant();
            Long epochMilli = instant.toEpochMilli();

            objProfile = new ProfileDTO(
                    user.getUsername(),
                    user.getFirstname(),
                    user.getLastname(),
                    user.getEmail(),
                    user.getFontSize().toLowerCase(),
                    user.getThemeName(),
                    !user.isEnabled() ? "Unenabled" : "Enabled",
                    user.getAccountFormat(),
                    user.getDateFormat(),
                    user.getShowDash(),
                    user.getAlertOnHome(),
                    user.getNumberMask(),
                    user.isMultiBrnchAccess(),
                    user.getBranch().getSolId(),
                    user.getUserLanguage(),
                    (epochMilli + jwtExpiration),
                    user.getLastPasswordChangeDate(),
                    LocalDate.now().isAfter(user.getLastPasswordChangeDate())
            );

        }
        return objProfile;
    }

    public Optional<ProfileDTO> saveSettings(Principal principal,
                                             String accountFormat,
                                             String dateFormat,
                                             String fontSize,
                                             String themeName,
                                             String showDash,
                                             String alertOnHome,
                                             String numberMask
                                             ) {
        Optional<User> user = userRepository.findByUsername(principal.getName());
        if(user.isPresent()) {
            User existingUser = user.get();
            existingUser.setAccountFormat(accountFormat);
            existingUser.setDateFormat(dateFormat);
            existingUser.setFontSize(fontSize);
            existingUser.setThemeName(themeName);
            existingUser.setShowDash(showDash);
            existingUser.setAlertOnHome(alertOnHome);
            existingUser.setNumberMask(numberMask);

            userRepository.save(existingUser);

            OffsetDateTime offsetDateTime = existingUser.getExpirationTime().atOffset(ZoneOffset.systemDefault().getRules().getOffset(existingUser.getExpirationTime()));
            Instant instant = offsetDateTime.toInstant();
            Long epochMilli = instant.toEpochMilli();
            return Optional.of(new ProfileDTO(
                    existingUser.getUsername(),
                    existingUser.getFirstname(),
                    existingUser.getLastname(),
                    existingUser.getEmail(),
                    existingUser.getFontSize().toLowerCase(),
                    existingUser.getThemeName(),
                    !existingUser.isEnabled() ? "Unenabled" : "Enabled",
                    existingUser.getAccountFormat(),
                    existingUser.getDateFormat(),
                    existingUser.getShowDash(),
                    existingUser.getAlertOnHome(),
                    existingUser.getNumberMask(),
                    existingUser.isMultiBrnchAccess(),
                    existingUser.getBranch().getSolId(),
                    existingUser.getUserLanguage(),
                    (epochMilli + jwtExpiration),
                    existingUser.getLastPasswordChangeDate(),
                    LocalDate.now().isAfter(existingUser.getLastPasswordChangeDate())
            ));
        } else {
            return Optional.empty();
        }
    }

    public Optional<NotificationMessage> changePassword(Principal principal,
                                                        String currentPassword,
                                                        String password,
                                                        String confirmPassword) {
        Optional<User> user = userRepository.findByUsername(principal.getName());
        coreParameterRepository.findOneByParamType("password_expiry_days");
        int passwordExpiryDays = Integer.parseInt(
                coreParameterRepository.findOneByParamType("password_expiry_days")
                        .orElseThrow(() -> new RuntimeException("Parameter not found"))
                        .getParamCode()
        );
        if(user.isPresent()) {
            User existingUser = user.get();
            if (!passwordEncoder.matches(currentPassword, existingUser.getPassword())) {
                throw new IllegalArgumentException("Old password is incorrect");
            }
            existingUser.setPassword(passwordEncoder.encode(password));
            existingUser.setLastPasswordChangeDate(LocalDate.now().plusDays(passwordExpiryDays));

            userRepository.save(existingUser);
            return Optional.of(new NotificationMessage("success","Password changed","Password has been changed.", Map.of()));
        } else {
            throw new IllegalArgumentException("Contact to admin");
        }
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
