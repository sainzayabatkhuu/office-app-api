package com.sol.office_app.domain.response;

import com.sol.office_app.dto.UserDTO;

/**
 * @param expiresIn access-token lifetime in milliseconds
 */
public record LoginResponse(String accessToken, String refreshToken, Long expiresIn, UserDTO user) {}
