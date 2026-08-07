package com.artverse.app.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * Unit tests for the shared form-validation rules used across registration,
 * login, and artwork forms (Chapter 5, Unit Testing - module: ValidationUtil).
 * Runs on Robolectric because isValidEmail() delegates to
 * android.util.Patterns, which is a stub under plain JUnit.
 */
@RunWith(RobolectricTestRunner.class)
public class ValidationUtilTest {

    // --- isValidEmail ---

    @Test
    public void isValidEmail_acceptsWellFormedAddress() {
        assertTrue(ValidationUtil.isValidEmail("dinithi@email.com"));
    }

    @Test
    public void isValidEmail_rejectsMissingAtSymbol() {
        assertFalse(ValidationUtil.isValidEmail("dinithi.email.com"));
    }

    @Test
    public void isValidEmail_rejectsEmptyString() {
        assertFalse(ValidationUtil.isValidEmail(""));
    }

    @Test
    public void isValidEmail_rejectsNull() {
        assertFalse(ValidationUtil.isValidEmail(null));
    }

    // --- isValidPassword ---

    @Test
    public void isValidPassword_acceptsSixOrMoreCharacters() {
        assertTrue(ValidationUtil.isValidPassword("secret6"));
    }

    @Test
    public void isValidPassword_rejectsFewerThanSixCharacters() {
        assertFalse(ValidationUtil.isValidPassword("abc12"));
    }

    @Test
    public void isValidPassword_rejectsNull() {
        assertFalse(ValidationUtil.isValidPassword(null));
    }

    // --- isNotEmpty ---

    @Test
    public void isNotEmpty_rejectsBlankAndWhitespaceOnly() {
        assertFalse(ValidationUtil.isNotEmpty(""));
        assertFalse(ValidationUtil.isNotEmpty("   "));
    }

    @Test
    public void isNotEmpty_acceptsNonBlankText() {
        assertTrue(ValidationUtil.isNotEmpty("Sunset Over Kandy"));
    }

    // --- isValidPrice ---

    @Test
    public void isValidPrice_acceptsPositiveDecimal() {
        assertTrue(ValidationUtil.isValidPrice("24000.50"));
    }

    @Test
    public void isValidPrice_rejectsZero() {
        assertFalse(ValidationUtil.isValidPrice("0"));
    }

    @Test
    public void isValidPrice_rejectsNegativeValue() {
        assertFalse(ValidationUtil.isValidPrice("-500"));
    }

    @Test
    public void isValidPrice_rejectsNonNumericText() {
        assertFalse(ValidationUtil.isValidPrice("free"));
    }

    @Test
    public void isValidPrice_rejectsEmptyString() {
        assertFalse(ValidationUtil.isValidPrice(""));
    }
}
