package com.sol.office_app.dto;

import java.util.Collection;

public record RoleDTO(
        Long id,
        String name,
        String description,
        Collection<PrivilegeDTO> privileges
) {
}
