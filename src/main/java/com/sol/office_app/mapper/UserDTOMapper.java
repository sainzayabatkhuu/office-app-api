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

        obj.setAccountFormat(user.getAccountFormat());
        obj.setDateFormat(user.getDateFormat());
        obj.setFontSize(user.getFontSize());
        obj.setThemeName(user.getThemeName());
        obj.setShowDash(user.getShowDash());
        obj.setAlertOnHome(user.getAlertOnHome());
        obj.setNumberMask(user.getNumberMask());

        //obj.setExpirationTime(user.getExpirationTime().toEpochSecond());

        return obj;
    }
}
