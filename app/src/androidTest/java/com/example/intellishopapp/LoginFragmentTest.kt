package com.example.intellishopapp

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
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
 * Login shown inside the shell (via the burger "Sign in"). Backend-dependent
 * cases use a known account (lala@gmail.com / lala). Requires the Django server.
 */
@RunWith(AndroidJUnit4::class)
class LoginFragmentTest {

    @get:Rule
    val scenario = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun ensureGuest() {
        SessionManager.getInstance().clear()
    }

    private fun openLogin() {
        onView(withId(R.id.main_BTN_burger)).perform(click())
        onView(withText(R.string.menu_sign_in)).inRoot(isPlatformPopup()).perform(click())
    }

    @Test
    fun login_showsFields() {
        openLogin()
        onView(withId(R.id.login_ET_email)).check(matches(isDisplayed()))
        onView(withId(R.id.login_BTN_submit)).check(matches(isDisplayed()))
        onView(withId(R.id.login_BTN_google)).check(matches(isDisplayed()))
    }

    @Test
    fun login_emptyFields_showsError() {
        openLogin()
        onView(withId(R.id.login_BTN_submit)).perform(click())
        onView(withId(R.id.login_LBL_error)).check(matches(isDisplayed()))
    }

    @Test
    fun login_wrongPassword_showsError() {
        openLogin()
        onView(withId(R.id.login_ET_email)).perform(typeText("lala@gmail.com"), closeSoftKeyboard())
        onView(withId(R.id.login_ET_password)).perform(typeText("wrongpass"), closeSoftKeyboard())
        onView(withId(R.id.login_BTN_submit)).perform(click())
        waitUntilDisplayed(R.id.login_LBL_error)
    }

    @Test
    fun login_validCredentials_returnsToShell() {
        openLogin()
        onView(withId(R.id.login_ET_email)).perform(typeText("lala@gmail.com"), closeSoftKeyboard())
        onView(withId(R.id.login_ET_password)).perform(typeText("lala"), closeSoftKeyboard())
        onView(withId(R.id.login_BTN_submit)).perform(click())
        waitUntilDisplayed(R.id.home_LAY_scroll)
    }

    @Test
    fun login_registerLink_opensRegister() {
        openLogin()
        onView(withId(R.id.login_LBL_registerLink)).perform(click())
        onView(withId(R.id.register_ET_username)).check(matches(isDisplayed()))
    }

    private fun waitUntilDisplayed(id: Int) {
        val end = System.currentTimeMillis() + 12000
        while (System.currentTimeMillis() < end) {
            try {
                onView(withId(id)).check(matches(isDisplayed()))
                return
            } catch (e: Throwable) {
                Thread.sleep(300)
            }
        }
        onView(withId(id)).check(matches(isDisplayed()))
    }
}
