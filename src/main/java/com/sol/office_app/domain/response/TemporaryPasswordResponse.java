package com.sol.office_app.domain.response;

/** Shown to the administrator once; the plain password is never stored. */
public record TemporaryPasswordResponse(String username, String temporaryPassword) {
}
