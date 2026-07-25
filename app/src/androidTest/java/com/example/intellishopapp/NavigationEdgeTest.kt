package com.example.intellishopapp

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
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
 * Edge cases and flows that must NOT work for a guest: every gated coupon action
 * routes to Login, Back peels overlays one layer at a time (never the app), and
 * re-tapping the current tab is a harmless no-op. Requires the server.
 */
@RunWith(AndroidJUnit4::class)
class NavigationEdgeTest {

    @get:Rule
    val scenario = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun ensureGuest() {
        SessionManager.getInstance().clear()
    }

    private fun openFirstDetail() {
        waitForHero()
        onView(withId(R.id.home_LAY_hero))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
        onView(withId(R.id.detail_LBL_title)).check(matches(isDisplayed()))
        // The sheet slides up via ViewPropertyAnimator (not tracked by Espresso);
        // wait until the lowest action button has fully arrived before tapping.
        waitCompletelyDisplayed(R.id.detail_BTN_offer)
    }

    @Test
    fun guestGoToSite_showsNotificationOnly_noLogin() {
        openFirstDetail()
        onView(withId(R.id.detail_BTN_site)).perform(click())
        onView(withId(R.id.main_LBL_banner)).check(matches(withText(R.string.gate_site)))
        onView(withId(R.id.login_ET_email)).check(doesNotExist())
    }

    @Test
    fun guestGoToOffer_showsNotificationOnly_noLogin() {
        openFirstDetail()
        onView(withId(R.id.detail_BTN_offer)).perform(click())
        onView(withId(R.id.main_LBL_banner)).check(matches(withText(R.string.gate_offer)))
        onView(withId(R.id.login_ET_email)).check(doesNotExist())
    }

    @Test
    fun guestProfileTab_neverShowsProfilePage() {
        onView(withId(R.id.main_LAY_tabProfile)).perform(click())
        onView(withId(R.id.profile_LBL_title)).check(matches(not(isDisplayed())))
        onView(withId(R.id.login_ET_email)).check(matches(isDisplayed()))
    }

    @Test
    fun backFromLogin_returnsToHome() {
        onView(withId(R.id.main_LAY_tabProfile)).perform(click())
        onView(withId(R.id.login_ET_email)).check(matches(isDisplayed()))
        pressBack()
        onView(withId(R.id.login_ET_email)).check(doesNotExist())
        onView(withId(R.id.home_LAY_scroll)).check(matches(isDisplayed()))
    }

    @Test
    fun backFromRegister_returnsToLoginNotHome() {
        onView(withId(R.id.main_LAY_tabProfile)).perform(click())
        onView(withId(R.id.login_LBL_registerLink)).perform(click())
        onView(withId(R.id.register_ET_username)).check(matches(isDisplayed()))
        pressBack()
        // Peels one layer: back to Login, not all the way to Home.
        onView(withId(R.id.register_ET_username)).check(doesNotExist())
        onView(withId(R.id.login_ET_email)).check(matches(isDisplayed()))
    }

    @Test
    fun backFromDetail_returnsToHome() {
        openFirstDetail()
        pressBack()
        waitUntilGone(R.id.detail_LBL_title)
        onView(withId(R.id.home_LAY_scroll)).check(matches(isDisplayed()))
    }

    @Test
    fun reTapHomeTab_staysOnHome() {
        onView(withId(R.id.main_LAY_tabHome)).perform(click())
        onView(withId(R.id.main_LAY_tabHome)).perform(click())
        onView(withId(R.id.home_LAY_scroll)).check(matches(isDisplayed()))
    }

    @Test
    fun openDetailThenProfileThenTab_landsOnCleanPage() {
        // Guest opens a detail, then Profile leaves the detail and shows Login; a
        // second tab press must leave Login and show the chosen page cleanly.
        openFirstDetail()
        onView(withId(R.id.main_LAY_tabProfile)).perform(click())
        onView(withId(R.id.detail_LBL_title)).check(doesNotExist())
        onView(withId(R.id.login_ET_email)).check(matches(isDisplayed()))
        onView(withId(R.id.main_LAY_tabHome)).perform(click())
        onView(withId(R.id.login_ET_email)).check(doesNotExist())
        onView(withId(R.id.home_LAY_scroll)).check(matches(isDisplayed()))
    }

    private fun waitForHero() {
        val end = System.currentTimeMillis() + 15000
        while (System.currentTimeMillis() < end) {
            try {
                onView(withId(R.id.home_LAY_hero)).check(matches(hasMinimumChildCount(1)))
                return
            } catch (e: Throwable) {
                Thread.sleep(400)
            }
        }
    }

    private fun waitCompletelyDisplayed(id: Int) {
        val end = System.currentTimeMillis() + 4000
        while (System.currentTimeMillis() < end) {
            try {
                onView(withId(id)).check(matches(isCompletelyDisplayed()))
                return
            } catch (e: Throwable) {
                Thread.sleep(100)
            }
        }
        onView(withId(id)).check(matches(isCompletelyDisplayed()))
    }

    private fun waitUntilGone(id: Int) {
        val end = System.currentTimeMillis() + 5000
        while (System.currentTimeMillis() < end) {
            try {
                onView(withId(id)).check(doesNotExist())
                return
            } catch (e: Throwable) {
                Thread.sleep(200)
            }
        }
        onView(withId(id)).check(doesNotExist())
    }
}
