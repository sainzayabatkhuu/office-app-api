package com.sol.office_app.mapper;

import com.sol.office_app.dto.UserDTO;
import com.sol.office_app.entity.User;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class UserDTOMapper implements Function<User, UserDTO> {

    @Override
    public UserDTO apply(User user) {
        UserDTO obj = new UserDTO();
        obj.setId(user.getId());
        obj.setEmail(user.getEmail());
        obj.setUsername(user.getUsername());
        obj.setFirstname(user.getFirstname());
        obj.setLastname(user.getLastname());

        obj.setAmountFormat(user.getAmountFormat());
        obj.setDateFormat(user.getDateFormat());
        obj.setFontSize(user.getFontSize());
        obj.setThemeName(user.getThemeName());
        obj.setShowDash(user.getShowDash());
        obj.setAlertOnHome(user.getAlertOnHome());
        obj.setNumberMask(user.getNumberMask());

        obj.setUserLanguage(user.getUserLanguage());
        obj.setMultiBrnchAccess(Boolean.TRUE.equals(user.getMultiBrnchAccessOrNull()));
        obj.setBranchCode(user.getBranch() == null ? null : user.getBranch().getSolId());
        obj.setRoleNames(user.getRoles().stream().map(r -> r.getName()).sorted().toList());
        obj.setEnabled(user.isEnabled());
        obj.setFailedAttempts(user.getFailedAttempt());
        obj.setLockTime(user.getLockTime());
        obj.setLastPasswordChangeDate(user.getLastPasswordChangeDate());
        obj.setCreatedDate(user.getCreatedDate());
        obj.setCreatedBy(user.getCreatedBy());
        obj.setLastModifiedDate(user.getLastModifiedDate());
        obj.setLastModifiedBy(user.getLastModifiedBy());
        obj.setModifiedCount(user.getModifiedCount());

        return obj;
    }
}
