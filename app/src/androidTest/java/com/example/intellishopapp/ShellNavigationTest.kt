package com.example.intellishopapp

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.intellishopapp.utilities.SessionManager
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Shell navigation for a guest: Home and Coupons are browsable; Profile gates to
 * Login; the bottom tabs switch cleanly with no leftover between screens.
 */
@RunWith(AndroidJUnit4::class)
class ShellNavigationTest {

    @get:Rule
    val scenario = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun ensureGuest() {
        SessionManager.getInstance().clear()
    }

    @Test
    fun shellLaunches_showsHome() {
        onView(withId(R.id.home_LAY_scroll)).check(matches(isDisplayed()))
    }

    @Test
    fun couponsTab_isBrowsableByGuest() {
        onView(withId(R.id.main_LAY_tabCoupons)).perform(click())
        onView(withId(R.id.favorites_LBL_title)).check(matches(isDisplayed()))
    }

    @Test
    fun guestProfileTab_opensLogin() {
        onView(withId(R.id.main_LAY_tabProfile)).perform(click())
        onView(withId(R.id.login_ET_email)).check(matches(isDisplayed()))
    }

    @Test
    fun returnToHome_showsHome() {
        onView(withId(R.id.main_LAY_tabCoupons)).perform(click())
        onView(withId(R.id.main_LAY_tabHome)).perform(click())
        onView(withId(R.id.home_LAY_scroll)).check(matches(isDisplayed()))
    }

    @Test
    fun onLogin_pressHome_leavesLoginAndShowsHome() {
        onView(withId(R.id.main_LAY_tabProfile)).perform(click())
        onView(withId(R.id.login_ET_email)).check(matches(isDisplayed()))
        onView(withId(R.id.main_LAY_tabHome)).perform(click())
        onView(withId(R.id.login_ET_email)).check(doesNotExist())
        onView(withId(R.id.home_LAY_scroll)).check(matches(isDisplayed()))
    }

    @Test
    fun onLogin_pressCoupons_leavesLoginAndShowsCoupons() {
        onView(withId(R.id.main_LAY_tabProfile)).perform(click())
        onView(withId(R.id.login_ET_email)).check(matches(isDisplayed()))
        onView(withId(R.id.main_LAY_tabCoupons)).perform(click())
        onView(withId(R.id.login_ET_email)).check(doesNotExist())
        onView(withId(R.id.favorites_LBL_title)).check(matches(isDisplayed()))
    }

    @Test
    fun loginOverlay_hidesSearchBar() {
        onView(withId(R.id.main_LAY_search)).check(matches(isDisplayed()))
        onView(withId(R.id.main_LAY_tabProfile)).perform(click())
        onView(withId(R.id.main_LAY_search)).check(matches(not(isDisplayed())))
    }

    @Test
    fun burger_opensMenu_withSignIn() {
        onView(withId(R.id.main_BTN_burger)).perform(click())
        onView(withText(R.string.menu_sign_in)).inRoot(isPlatformPopup()).check(matches(isDisplayed()))
    }

    @Test
    fun searchBar_showsBanner() {
        onView(withId(R.id.main_LAY_search)).perform(click())
        onView(withId(R.id.main_LBL_banner)).check(matches(isDisplayed()))
        onView(withId(R.id.main_LBL_banner)).check(matches(withText(R.string.search_soon)))
    }

    @Test
    fun roundTrip_homeCouponsHome_landsOnCleanHome() {
        onView(withId(R.id.home_LAY_scroll)).check(matches(isDisplayed()))
        onView(withId(R.id.main_LAY_tabCoupons)).perform(click())
        onView(withId(R.id.favorites_LBL_title)).check(matches(isDisplayed()))
        onView(withId(R.id.home_LAY_scroll)).check(matches(not(isDisplayed())))
        onView(withId(R.id.main_LAY_tabHome)).perform(click())
        onView(withId(R.id.home_LAY_scroll)).check(matches(isDisplayed()))
        onView(withId(R.id.favorites_LBL_title)).check(matches(not(isDisplayed())))
    }
}
