package com.sol.office_app.mapper;

import com.sol.office_app.dto.PrivilegeDTO;
import com.sol.office_app.entity.Privilege;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class PrivilegeDTOMapper implements Function<Privilege, PrivilegeDTO> {
    @Override
    public PrivilegeDTO apply(Privilege privilege) {
        return new PrivilegeDTO(privilege.getId(),privilege.getName());
    }
}
