package com.sol.office.identity.service;

import com.sol.office.common.exception.NotFoundException;
import com.sol.office.common.security.CurrentUser;
import com.sol.office.identity.domain.AppUser;
import com.sol.office.identity.domain.UserPreferences;
import com.sol.office.identity.dto.AuthDtos.PreferencesDto;
import com.sol.office.identity.dto.AuthDtos.ProfileResponse;
import com.sol.office.identity.mapper.IdentityMapper;
import com.sol.office.identity.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final AppUserRepository userRepository;
    private final IdentityMapper mapper;

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(CurrentUser currentUser) {
        return mapper.toProfile(load(currentUser));
    }

    @Transactional
    public PreferencesDto updatePreferences(CurrentUser currentUser, PreferencesDto request) {
        UserPreferences prefs = load(currentUser).getPreferences();
        if (request.language() != null) prefs.setLanguage(request.language());
        prefs.setTheme(request.theme());
        prefs.setDateFormat(request.dateFormat());
        prefs.setFontSize(request.fontSize());
        return mapper.toDto(prefs);
    }

    private AppUser load(CurrentUser currentUser) {
        return userRepository.findWithDetailsById(currentUser.userId())
                .orElseThrow(() -> NotFoundException.of("User", currentUser.username()));
    }
}
