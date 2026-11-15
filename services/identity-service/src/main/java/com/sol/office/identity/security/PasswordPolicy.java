package com.sol.office.identity.security;

import com.sol.office.common.exception.BusinessException;

import java.security.SecureRandom;

public final class PasswordPolicy {

    private static final String LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordPolicy() {
    }

    /** At least 8 characters with both letters and digits. */
    public static void validate(String password) {
        if (password == null || password.length() < 8
                || password.chars().noneMatch(Character::isLetter)
                || password.chars().noneMatch(Character::isDigit)) {
            throw new BusinessException("WEAK_PASSWORD",
                    "Password must be at least 8 characters and contain both letters and digits");
        }
    }

    /** Random password that satisfies {@link #validate}; ambiguous characters (0/O, 1/l) are excluded. */
    public static String generateTemporary() {
        String all = LETTERS + DIGITS;
        StringBuilder sb = new StringBuilder();
        sb.append(LETTERS.charAt(RANDOM.nextInt(LETTERS.length())));
        sb.append(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        for (int i = 0; i < 10; i++) {
            sb.append(all.charAt(RANDOM.nextInt(all.length())));
        }
        return sb.toString();
    }
}
