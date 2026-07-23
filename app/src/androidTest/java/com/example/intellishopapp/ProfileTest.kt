package com.example.intellishopapp

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.intellishopapp.model.UserSession
import com.example.intellishopapp.network.RetrofitClient
import com.example.intellishopapp.utilities.SessionManager
import org.junit.After
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
        onView(withId(R.id.main_LAY_tabProfile)).perform(click())
        onView(withId(R.id.profile_BTN_signOut)).perform(click())
        // Back on Home...
        onView(withId(R.id.home_LAY_scroll)).check(matches(isDisplayed()))
        // ...and now a guest: Profile gates to Login again.
        onView(withId(R.id.main_LAY_tabProfile)).perform(click())
        onView(withId(R.id.login_ET_email)).check(matches(isDisplayed()))
    }
}
