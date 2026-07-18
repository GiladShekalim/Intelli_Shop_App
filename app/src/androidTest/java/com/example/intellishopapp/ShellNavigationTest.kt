package com.example.intellishopapp

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Shell navigation tests: the bottom 3-tab bar switches fragments and the burger
 * menu opens. Runs on a device/emulator against MainActivity (the launcher shell).
 */
@RunWith(AndroidJUnit4::class)
class ShellNavigationTest {

    @get:Rule
    val scenario = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun shellLaunches_showsHomeContent() {
        onView(withId(R.id.home_LBL_title)).check(matches(isDisplayed()))
    }

    @Test
    fun couponsTab_showsCouponsContent() {
        onView(withId(R.id.main_LAY_tabCoupons)).perform(click())
        onView(withId(R.id.favorites_LBL_title)).check(matches(isDisplayed()))
    }

    @Test
    fun profileTab_showsProfileContent() {
        onView(withId(R.id.main_LAY_tabProfile)).perform(click())
        onView(withId(R.id.profile_LBL_title)).check(matches(isDisplayed()))
    }

    @Test
    fun returnToHomeTab_showsHomeContent() {
        onView(withId(R.id.main_LAY_tabProfile)).perform(click())
        onView(withId(R.id.main_LAY_tabHome)).perform(click())
        onView(withId(R.id.home_LBL_title)).check(matches(isDisplayed()))
    }

    @Test
    fun burger_opensMenu_withSignIn() {
        onView(withId(R.id.main_BTN_burger)).perform(click())
        onView(withText(R.string.menu_sign_in))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))
    }

    @Test
    fun e2e_navigateAllTabsThenOpenMenu() {
        onView(withId(R.id.home_LBL_title)).check(matches(isDisplayed()))
        onView(withId(R.id.main_LAY_tabCoupons)).perform(click())
        onView(withId(R.id.favorites_LBL_title)).check(matches(isDisplayed()))
        onView(withId(R.id.main_LAY_tabProfile)).perform(click())
        onView(withId(R.id.profile_LBL_title)).check(matches(isDisplayed()))
        onView(withId(R.id.main_LAY_tabHome)).perform(click())
        onView(withId(R.id.home_LBL_title)).check(matches(isDisplayed()))
        onView(withId(R.id.main_BTN_burger)).perform(click())
        onView(withText(R.string.menu_sign_in)).inRoot(isPlatformPopup()).check(matches(isDisplayed()))
    }
}
