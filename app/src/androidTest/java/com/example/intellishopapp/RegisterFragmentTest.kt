package com.example.intellishopapp

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.intellishopapp.utilities.SessionManager
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Register shown in the shell (Login -> "Create account"). Fields, the toggle
 * grids, and a real registration that pops back to Login. Requires the server.
 * New users use a test_ email so they can be cleaned up.
 */
@RunWith(AndroidJUnit4::class)
class RegisterFragmentTest {

    @get:Rule
    val scenario = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun ensureGuest() {
        SessionManager.getInstance().clear()
    }

    private fun openRegister() {
        onView(withId(R.id.main_BTN_burger)).perform(click())
        onView(withText(R.string.menu_sign_in)).inRoot(isPlatformPopup()).perform(click())
        // Wait for the Login overlay to finish appearing (popup dismiss + fragment
        // commit are async) before reaching for its register link.
        waitDisplayed(R.id.login_LBL_registerLink)
        onView(withId(R.id.login_LBL_registerLink)).perform(click())
        waitDisplayed(R.id.register_ET_username)
    }

    private fun waitDisplayed(id: Int) {
        val end = System.currentTimeMillis() + 8000
        while (System.currentTimeMillis() < end) {
            try {
                onView(withId(id)).check(matches(isDisplayed()))
                return
            } catch (e: Throwable) {
                Thread.sleep(150)
            }
        }
        onView(withId(id)).check(matches(isDisplayed()))
    }

    @Test
    fun register_showsFields() {
        openRegister()
        onView(withId(R.id.register_ET_username)).check(matches(isDisplayed()))
    }

    @Test
    fun register_statusTogglesPresent() {
        openRegister()
        onView(withId(R.id.register_LAY_statusGrid)).check(matches(hasMinimumChildCount(1)))
        onView(withId(R.id.register_LAY_interestGrid)).check(matches(hasMinimumChildCount(1)))
    }

    @Test
    fun register_emptyFields_showsError() {
        openRegister()
        onView(withId(R.id.register_BTN_submit)).perform(scrollTo(), click())
        onView(withId(R.id.register_LBL_error)).check(matches(isDisplayed()))
    }

    @Test
    fun register_newUser_popsBackToLogin() {
        openRegister()
        val unique = "test_reg" + System.currentTimeMillis()
        onView(withId(R.id.register_ET_username)).perform(typeText(unique), closeSoftKeyboard())
        onView(withId(R.id.register_ET_email)).perform(typeText("$unique@example.com"), closeSoftKeyboard())
        onView(withId(R.id.register_ET_password)).perform(typeText("pw123"), closeSoftKeyboard())
        onView(withId(R.id.register_BTN_submit)).perform(scrollTo(), click())
        waitUntilGone(R.id.register_ET_username)
        // Login re-appears underneath after the Register overlay pops (async).
        waitDisplayed(R.id.login_ET_email)
        onView(withId(R.id.login_ET_email)).check(matches(isDisplayed()))
    }

    private fun waitUntilGone(id: Int) {
        val end = System.currentTimeMillis() + 12000
        while (System.currentTimeMillis() < end) {
            try {
                onView(withId(id)).check(doesNotExist())
                return
            } catch (e: Throwable) {
                Thread.sleep(300)
            }
        }
        onView(withId(id)).check(doesNotExist())
    }
}
