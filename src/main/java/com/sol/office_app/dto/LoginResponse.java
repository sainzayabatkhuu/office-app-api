package com.sol.office_app.dto;

public record LoginResponse(String accessToken,Long expiresIn, UserDTO user) {}
