package com.sol.office_app.dto.response;

import com.sol.office_app.dto.UserDTO;

public record LoginResponse(String accessToken, Long expiresIn, UserDTO user) {}
