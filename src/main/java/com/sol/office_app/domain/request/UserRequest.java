package com.sol.office_app.domain.request;

import java.util.Set;

/**
 * Create / update a user. {@code password} is only used on create; when empty a temporary password is
 * generated and returned once.
 */
public record UserRequest(
        String username,
        String email,
        String firstname,
        String lastname,
        String userLanguage,
        String branchCode,
        Boolean multiBrnchAccess,
        Boolean enabled,
        Set<String> roleNames,
        String password) {
}
