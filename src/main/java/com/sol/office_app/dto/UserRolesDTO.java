package com.sol.office_app.dto;

public record UserRolesDTO(
        Long id,
        BranchDTO branch,
        RoleDTO role,
        UserDTO user
) {
}
