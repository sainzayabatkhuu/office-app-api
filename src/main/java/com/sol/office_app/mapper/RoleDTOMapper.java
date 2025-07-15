package com.sol.office_app.mapper;

import com.sol.office_app.dto.PrivilegeDTO;
import com.sol.office_app.dto.RoleDTO;
import com.sol.office_app.entity.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RoleDTOMapper implements Function<Role, RoleDTO> {
    @Autowired
    private PrivilegeDTOMapper privilegeDTOMapper;

    @Override
    public RoleDTO apply(Role role) {
        List<PrivilegeDTO> privilegeDTOs = (role.getPrivileges() != null)
                ? role.getPrivileges().stream().map(privilegeDTOMapper).collect(Collectors.toList())
                : null;

        return new RoleDTO(role.getId(), role.getName(), role.getDescription(), privilegeDTOs);
    }
}
