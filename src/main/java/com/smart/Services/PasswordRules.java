package com.smart.Services;

import java.nio.charset.StandardCharsets;

public final class PasswordRules {
    public static final String MESSAGE = "Use 8-72 characters, including a letter and a number (maximum 72 UTF-8 bytes).";
    private PasswordRules() { }

    public static boolean isValid(String password) {
        return password != null && password.length() >= 8 && password.length() <= 72
                && password.getBytes(StandardCharsets.UTF_8).length <= 72
                && password.matches("(?s).*[A-Za-z].*") && password.matches("(?s).*[0-9].*");
    }
}
