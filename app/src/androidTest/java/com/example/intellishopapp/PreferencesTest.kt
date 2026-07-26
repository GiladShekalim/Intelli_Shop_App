package com.example.intellishopapp

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.intellishopapp.model.UserSession
import com.example.intellishopapp.network.RetrofitClient
import com.example.intellishopapp.utilities.SessionManager
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The local My Preferences / My Categories editors: they show all options and a
 * tap toggles + notifies + persists (no backend). Uses a directly-set session.
 */
@RunWith(AndroidJUnit4::class)
class PreferencesTest {

    @get:Rule
    val scenario = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun signInMember() {
        RetrofitClient.getInstance().clearCookies()
        SessionManager.getInstance().save(
            UserSession(userId = "test-member", email = "member@test.local", username = "M")
        )
    }

    @After
    fun cleanUp() {
        SessionManager.getInstance().clear()
    }

    @Test
    fun myPreferences_showsOptions() {
        onView(withId(R.id.main_LAY_tabProfile)).perform(click())
        onView(withId(R.id.profile_LBL_preferences)).perform(click())
        onView(withId(R.id.pref_LAY_grid)).check(matches(hasMinimumChildCount(1)))
    }

    @Test
    fun myCategories_toggle_notifiesAndPersists() {
        onView(withId(R.id.main_LAY_tabProfile)).perform(click())
        onView(withId(R.id.profile_LBL_categories)).perform(click())
        onView(allOf(withText("Consumerism"), isDescendantOfA(withId(R.id.pref_LAY_grid))))
            .perform(click())
        onView(withId(R.id.main_LBL_banner)).check(matches(withText("Added Consumerism")))
        assertTrue(SessionManager.getInstance().get()?.hobbies?.contains("Consumerism") == true)
    }

    @Test
    fun back_returnsToProfile() {
        onView(withId(R.id.main_LAY_tabProfile)).perform(click())
        onView(withId(R.id.profile_LBL_preferences)).perform(click())
        onView(withId(R.id.pref_BTN_close)).perform(click())
        onView(withId(R.id.profile_LBL_preferences)).check(matches(isDisplayed()))
    }
}
