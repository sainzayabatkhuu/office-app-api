package com.sol.office_app.mapper;

import com.sol.office_app.dto.UserRolesMappingDTO;
import com.sol.office_app.entity.UserRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class UserRolesMappingDTOMapper implements Function<UserRoles, UserRolesMappingDTO> {

    @Autowired
    private UserDTOMapper userDTOMapper;
    @Autowired
    private RoleDTOMapper roleDTOMapper;
    @Autowired
    private BranchDTOMapper branchDTOMapper;

    @Override
    public UserRolesMappingDTO apply(UserRoles userRoles) {
        return new UserRolesMappingDTO(userRoles.getId(),
                userRoles.getBranch().getSolId(),
                userRoles.getRole().getName(),
                userRoles.getRole().getDescription()
        );
    }
}
