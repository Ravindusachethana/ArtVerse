package com.artverse.app.auth;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.espresso.intent.rule.IntentsRule;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.artverse.app.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * System-level UI test for role-based routing at sign-up (Chapter 5, System
 * Testing - use case: "Choose a role"). Pure navigation, no Firebase call
 * involved, so it is safe to run without network access.
 */
@RunWith(AndroidJUnit4.class)
public class RoleSelectionNavigationTest {

    @Rule
    public IntentsRule intentsRule = new IntentsRule();

    @Rule
    public ActivityScenarioRule<RoleSelectionActivity> activityRule =
            new ActivityScenarioRule<>(RoleSelectionActivity.class);

    @Test
    public void tappingArtLoverCard_opensCustomerRegistration() {
        onView(withId(R.id.cardCustomer)).perform(click());
        intended(hasComponent(CustomerRegisterActivity.class.getName()));
    }

    @Test
    public void tappingArtistCard_opensArtistRegistration() {
        onView(withId(R.id.cardArtist)).perform(click());
        intended(hasComponent(ArtistRegisterActivity.class.getName()));
    }
}
