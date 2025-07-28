package com.sol.office_app.service;

import com.sol.office_app.config.JwtUtils;
import com.sol.office_app.dto.ProfileDTO;
import com.sol.office_app.dto.UserDTO;
import com.sol.office_app.entity.User;
import com.sol.office_app.mapper.BranchDTOMapper;
import com.sol.office_app.mapper.UserDTOMapper;
import com.sol.office_app.mapper.UserRolesDTOMapper;
import com.sol.office_app.repository.BranchRepository;
import com.sol.office_app.repository.UserRepository;
import com.sol.office_app.repository.UserRolesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    @Value("${security.jwt.expiration-time}")
    private long jwtExpiration;

    @Autowired
    private UserDTOMapper userDTOMapper;
    @Autowired
    private BranchDTOMapper branchDTOMapper;
    @Autowired
    private BranchRepository branchRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserRolesRepository userRolesRepository;
    @Autowired
    private UserRolesDTOMapper userRolesDTOMapper;

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

    public Optional<UserDTO> delete(Long id, UserDTO updatedUser) {
        Optional<User> existingUserOpt = userRepository.findById(id);
        if (existingUserOpt.isPresent()) {
            userRepository.deleteById(id);
            return Optional.of(userDTOMapper.apply(existingUserOpt.get()));
        } else {
            return Optional.empty();
        }
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
                (epochMilli + jwtExpiration)
            );

        }
        return objProfile;
    }
//
//    @Override
//    public Optional<ProfileDTO> saveSettings(Principal principal, ProfileDTO profileDTO) {
//
//        Optional<User> user = userRepository.findByEmail(principal.getName());
//        if(user.isPresent()) {
//            User existingUser = user.get();
//            existingUser.setAccountFormat(profileDTO.getAccount_format());
//            existingUser.setDateFormat(profileDTO.getDate_format());
//            existingUser.setFontSize(profileDTO.getFont_size());
//            existingUser.setThemeName(profileDTO.getTheme_name());
//            existingUser.setShowDash(profileDTO.getShow_dash());
//            existingUser.setAlertOnHome(profileDTO.getAlert_on_home());
//            existingUser.setNumberMask(profileDTO.getNumber_mask());
//
//            userRepository.save(existingUser);
//            return Optional.of(profileDTO);
//        } else {
//            return Optional.empty();
//        }
//    }
//
//    @Override
//    public Optional<ChangePasswordDTO> changePassword(Principal principal, ChangePasswordDTO changePasswordDTO) {
//        Optional<User> user = userRepository.findByEmail(principal.getName());
//        if(user.isPresent()) {
//            User existingUser = user.get();
//            if (!passwordEncoder.matches(changePasswordDTO.getCurrentPassword(), existingUser.getPassword())) {
//                throw new IllegalArgumentException("Old password is incorrect");
//            }
//            existingUser.setPassword(passwordEncoder.encode(changePasswordDTO.getPassword()));
//
//            userRepository.save(existingUser);
//            return Optional.of(changePasswordDTO);
//        } else {
//            return Optional.empty();
//        }
//    }
//
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
//    @Override
//    public Page<UserRolesMappingDTO> userRoleMapping(Principal principal, Pageable pageable) {
//        Page<UserRoles> userRoles = userRolesRepository.findAll(pageable);
//        return userRoles.map(userRolesMappingDTOMapper);
//    }
//
//    @Override
//    public Page<BranchDTO> allowedBranches(Principal principal, Pageable pageable) {
//        Optional<User> user = userRepository.findByEmail(principal.getName());
//        if(user.isPresent()) {
//            List<Branch> branches = branchRepository.findByUserEmail(principal.getName());
//            List<BranchDTO> branchDTOs = branches.stream()
//                    .flatMap(branch -> branch.getChildrenBranches().stream())
//                    .map(branchDTOMapper)
//                    .collect(Collectors.toList());
//
//            return new PageImpl<>(branchDTOs, pageable, branchDTOs.size());
//        }
//
//        return null;
//    }
}
