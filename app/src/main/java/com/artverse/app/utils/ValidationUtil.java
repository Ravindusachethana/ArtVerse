package com.artverse.app.utils;

import android.util.Patterns;

/** Shared form-validation logic reused across registration and artwork forms. */
public final class ValidationUtil {

    private ValidationUtil() { }

    public static boolean isValidEmail(CharSequence email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static boolean isValidPassword(CharSequence password) {
        return password != null && password.length() >= 6;
    }

    public static boolean isNotEmpty(CharSequence value) {
        return value != null && value.toString().trim().length() > 0;
    }

    public static boolean isValidPrice(CharSequence value) {
        if (!isNotEmpty(value)) return false;
        try {
            double v = Double.parseDouble(value.toString().trim());
            return v > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
