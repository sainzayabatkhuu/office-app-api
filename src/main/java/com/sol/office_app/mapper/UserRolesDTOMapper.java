package com.sol.office_app.mapper;

import com.sol.office_app.dto.UserRolesDTO;
import com.sol.office_app.entity.UserRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class UserRolesDTOMapper implements Function<UserRoles, UserRolesDTO> {

    @Autowired
    private UserDTOMapper userDTOMapper;
    @Autowired
    private RoleDTOMapper roleDTOMapper;
    @Autowired
    private BranchDTOMapper branchDTOMapper;

    @Override
    public UserRolesDTO apply(UserRoles userRoles) {
        return new UserRolesDTO(userRoles.getId(),
                branchDTOMapper.apply(userRoles.getBranch()),
                roleDTOMapper.apply(userRoles.getRole()),
                userDTOMapper.apply(userRoles.getUser())
        );
    }
}
