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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The My Preferences / My Categories editors: options render, a tap toggles a
 * selection, and Save persists it (optimistically to the session; the backend
 * round-trip is proven in PreferencesSyncTest). Uses a directly-set session.
 */
@RunWith(AndroidJUnit4::class)
class PreferencesTest {

    @get:Rule
    val scenario = ActivityScenarioRule(MainActivity::class.java)

    @After
    fun cleanUp() {
        SessionManager.getInstance().clear()
    }

    private fun signIn(
        statuses: List<String> = emptyList(),
        hobbies: List<String> = emptyList(),
        memberships: List<String> = emptyList(),
        google: Boolean = false
    ) {
        RetrofitClient.getInstance().clearCookies()
        SessionManager.getInstance().save(
            UserSession(
                userId = "m", email = "member@test.local", username = "M",
                status = statuses, hobbies = hobbies, memberships = memberships, isGoogle = google
            )
        )
    }

    private fun openCategories() {
        onView(withId(R.id.main_LAY_tabProfile)).perform(click())
        onView(withId(R.id.profile_LBL_categories)).perform(click())
    }

    private fun openMemberships() {
        onView(withId(R.id.main_LAY_tabProfile)).perform(click())
        onView(withId(R.id.profile_LBL_memberships)).perform(click())
    }

    @Test
    fun myMemberships_showsOptions() {
        signIn()
        openMemberships()
        onView(withId(R.id.pref_LAY_grid)).check(matches(hasMinimumChildCount(1)))
    }

    @Test
    fun tapMembership_thenSave_storesTheClubKey() {
        // The grid shows the label "HOT" but stores the key "hot".
        signIn()
        openMemberships()
        onView(allOf(withText("HOT"), isDescendantOfA(withId(R.id.pref_LAY_grid)))).perform(click())
        onView(withId(R.id.pref_BTN_save)).perform(click())
        assertTrue(SessionManager.getInstance().memberships().contains("hot"))
    }

    @Test
    fun editingMemberships_preservesStatusesAndCategories() {
        signIn(statuses = listOf("Student"), hobbies = listOf("Cars"))
        openMemberships()
        onView(allOf(withText("Adif"), isDescendantOfA(withId(R.id.pref_LAY_grid)))).perform(click())
        onView(withId(R.id.pref_BTN_save)).perform(click())
        val s = SessionManager.getInstance().get()
        assertTrue(s?.memberships?.contains("adif") == true)
        assertTrue(s?.status?.contains("Student") == true)
        assertTrue(s?.hobbies?.contains("Cars") == true)
    }

    @Test
    fun myPreferences_showsOptions() {
        signIn()
        onView(withId(R.id.main_LAY_tabProfile)).perform(click())
        onView(withId(R.id.profile_LBL_preferences)).perform(click())
        onView(withId(R.id.pref_LAY_grid)).check(matches(hasMinimumChildCount(1)))
    }

    @Test
    fun tapOption_thenSave_addsToSelection() {
        signIn()
        openCategories()
        onView(allOf(withText("Consumerism"), isDescendantOfA(withId(R.id.pref_LAY_grid)))).perform(click())
        onView(withId(R.id.pref_BTN_save)).perform(click())
        assertTrue(SessionManager.getInstance().get()?.hobbies?.contains("Consumerism") == true)
    }

    @Test
    fun tapSelectedOption_thenSave_removesIt() {
        // Pre-seed a category; tapping it deselects, and Save persists the removal.
        signIn(hobbies = listOf("Consumerism"))
        openCategories()
        onView(allOf(withText("Consumerism"), isDescendantOfA(withId(R.id.pref_LAY_grid)))).perform(click())
        onView(withId(R.id.pref_BTN_save)).perform(click())
        assertFalse(SessionManager.getInstance().get()?.hobbies?.contains("Consumerism") == true)
    }

    @Test
    fun save_showsConfirmationAndReturnsToProfile() {
        signIn()
        openCategories()
        onView(withId(R.id.pref_BTN_save)).perform(click())
        onView(withId(R.id.main_LBL_banner)).check(matches(withText(R.string.pref_saved)))
        onView(withId(R.id.profile_LBL_categories)).check(matches(isDisplayed()))
    }

    @Test
    fun googleMember_canEditPreferences() {
        // Alternative auth flow: a Google-signed-in user edits the same way.
        signIn(google = true)
        openCategories()
        onView(allOf(withText("Cars"), isDescendantOfA(withId(R.id.pref_LAY_grid)))).perform(click())
        onView(withId(R.id.pref_BTN_save)).perform(click())
        assertTrue(SessionManager.getInstance().get()?.hobbies?.contains("Cars") == true)
    }

    @Test
    fun deselectingEverything_thenSave_clearsSelection() {
        signIn(hobbies = listOf("Cars", "Insurance"))
        openCategories()
        onView(allOf(withText("Cars"), isDescendantOfA(withId(R.id.pref_LAY_grid)))).perform(click())
        onView(allOf(withText("Insurance"), isDescendantOfA(withId(R.id.pref_LAY_grid)))).perform(click())
        onView(withId(R.id.pref_BTN_save)).perform(click())
        assertTrue(SessionManager.getInstance().get()?.hobbies?.isEmpty() == true)
    }

    @Test
    fun editingCategories_preservesStatuses() {
        // Only the edited dimension changes; the other is preserved on save.
        signIn(statuses = listOf("Student"), hobbies = emptyList())
        openCategories()
        onView(allOf(withText("Cars"), isDescendantOfA(withId(R.id.pref_LAY_grid)))).perform(click())
        onView(withId(R.id.pref_BTN_save)).perform(click())
        val s = SessionManager.getInstance().get()
        assertTrue(s?.status?.contains("Student") == true)
        assertTrue(s?.hobbies?.contains("Cars") == true)
    }

    @Test
    fun back_returnsToProfile() {
        signIn()
        onView(withId(R.id.main_LAY_tabProfile)).perform(click())
        onView(withId(R.id.profile_LBL_preferences)).perform(click())
        onView(withId(R.id.pref_BTN_close)).perform(click())
        onView(withId(R.id.profile_LBL_preferences)).check(matches(isDisplayed()))
    }
}
