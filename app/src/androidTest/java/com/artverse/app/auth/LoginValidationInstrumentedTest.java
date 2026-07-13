package com.artverse.app.auth;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.view.View;

import androidx.test.espresso.matcher.BoundedMatcher;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.artverse.app.R;
import com.google.android.material.textfield.TextInputLayout;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * System-level UI test for the login form's client-side validation
 * (Chapter 5, System Testing - use case: "Log in with invalid credentials").
 * Both cases return before any Firebase call fires (see
 * LoginActivity.attemptLogin()), so this runs safely on any configured
 * device/emulator without needing network access or a seeded test account.
 */
@RunWith(AndroidJUnit4.class)
public class LoginValidationInstrumentedTest {

    @Rule
    public ActivityScenarioRule<LoginActivity> activityRule =
            new ActivityScenarioRule<>(LoginActivity.class);

    @Test
    public void invalidEmail_showsFieldError_andDoesNotProceed() {
        onView(withId(R.id.etEmail)).perform(clearText(), typeText("not-an-email"), closeSoftKeyboard());
        onView(withId(R.id.etPassword)).perform(clearText(), typeText("password123"), closeSoftKeyboard());
        onView(withId(R.id.btnLogin)).perform(click());

        onView(withId(R.id.tilEmail)).check(matches(hasErrorText("Enter a valid email address")));
    }

    @Test
    public void validEmail_shortPassword_showsFieldError() {
        onView(withId(R.id.etEmail)).perform(clearText(), typeText("dinithi@email.com"), closeSoftKeyboard());
        onView(withId(R.id.etPassword)).perform(clearText(), typeText("abc"), closeSoftKeyboard());
        onView(withId(R.id.btnLogin)).perform(click());

        onView(withId(R.id.tilPassword)).check(matches(hasErrorText("Password must be at least 6 characters")));
    }

    /** Matches a TextInputLayout whose current error text equals the expected string. */
    private static Matcher<View> hasErrorText(String expected) {
        return new BoundedMatcher<View, TextInputLayout>(TextInputLayout.class) {
            @Override
            protected boolean matchesSafely(TextInputLayout item) {
                CharSequence error = item.getError();
                return error != null && expected.contentEquals(error);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("TextInputLayout with error text: " + expected);
            }
        };
    }
}
