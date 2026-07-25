package com.example.intellishopapp

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.intellishopapp.model.UserSession
import com.example.intellishopapp.network.RetrofitClient
import com.example.intellishopapp.utilities.SessionManager
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Profile page for a signed-in member: shows name + email, and Sign Out returns
 * the app to the guest state (Profile then gates to Login again).
 */
@RunWith(AndroidJUnit4::class)
class ProfileTest {

    @get:Rule
    val scenario = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun signInMember() {
        RetrofitClient.getInstance().clearCookies()
        SessionManager.getInstance().save(
            UserSession(userId = "test-member", email = "member@test.local", username = "TestMember")
        )
    }

    @After
    fun cleanUp() {
        SessionManager.getInstance().clear()
        SessionManager.getInstance().setNightMode(false)
    }

    private fun openProfile() {
        onView(withId(R.id.main_LAY_tabProfile)).perform(click())
    }

    @Test
    fun profile_showsNameEmailAndSignOut() {
        onView(withId(R.id.main_LAY_tabProfile)).perform(click())
        onView(withId(R.id.profile_LBL_title)).check(matches(withText("TestMember")))
        onView(withId(R.id.profile_LBL_email)).check(matches(withText("member@test.local")))
        onView(withId(R.id.profile_BTN_signOut)).check(matches(isDisplayed()))
    }

    @Test
    fun signOut_returnsToGuestHome() {
        openProfile()
        onView(withId(R.id.profile_BTN_signOut)).perform(click())
        // Back on Home...
        onView(withId(R.id.home_LAY_scroll)).check(matches(isDisplayed()))
        // ...and now a guest: Profile gates to Login again.
        onView(withId(R.id.main_LAY_tabProfile)).perform(click())
        onView(withId(R.id.login_ET_email)).check(matches(isDisplayed()))
    }

    @Test
    fun nonGoogleMember_seesChangePassword() {
        openProfile()
        onView(withId(R.id.profile_LBL_password)).check(matches(isDisplayed()))
    }

    @Test
    fun googleMember_hidesChangePassword() {
        SessionManager.getInstance().save(
            UserSession(userId = "g", email = "g@test.local", username = "G", isGoogle = true)
        )
        openProfile()
        onView(withId(R.id.profile_LBL_password)).check(matches(not(isDisplayed())))
    }

    @Test
    fun changePassword_mismatch_staysOpen() {
        openProfile()
        onView(withId(R.id.profile_LBL_password)).perform(click())
        onView(withId(R.id.pw_ET_current)).inRoot(isDialog()).perform(typeText("old"), closeSoftKeyboard())
        onView(withId(R.id.pw_ET_new)).inRoot(isDialog()).perform(typeText("aaa111"), closeSoftKeyboard())
        onView(withId(R.id.pw_ET_confirm)).inRoot(isDialog()).perform(typeText("bbb222"), closeSoftKeyboard())
        onView(withText(R.string.pw_change)).inRoot(isDialog()).perform(click())
        // A mismatch surfaces as a field error and keeps the dialog open.
        onView(withId(R.id.pw_ET_current)).inRoot(isDialog()).check(matches(isDisplayed()))
    }

    @Test
    fun changePassword_cancel_dismissesDialog() {
        openProfile()
        onView(withId(R.id.profile_LBL_password)).perform(click())
        onView(withText(R.string.pw_cancel)).inRoot(isDialog()).perform(click())
        onView(withId(R.id.pw_ET_current)).check(doesNotExist())
    }

    @Test
    fun toggleNight_persistsPreference() {
        openProfile()
        onView(withId(R.id.profile_LBL_night)).perform(click())
        assertTrue(SessionManager.getInstance().isNightMode())
    }
}
