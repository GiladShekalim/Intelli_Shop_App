package com.example.intellishopapp

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.intellishopapp.utilities.SessionManager
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Coupon Details slide-up: opens for a guest, shows the store + action buttons,
 * gates the actions to Login, and closes back to Home. Requires the server.
 */
@RunWith(AndroidJUnit4::class)
class CouponDetailTest {

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
    }

    @Test
    fun detail_showsStoreAndActions() {
        openFirstDetail()
        onView(withId(R.id.detail_LBL_store)).check(matches(isDisplayed()))
        onView(withId(R.id.detail_BTN_save)).check(matches(isDisplayed()))
        onView(withId(R.id.detail_BTN_copy)).check(matches(isDisplayed()))
    }

    @Test
    fun guestSave_opensLogin() {
        openFirstDetail()
        onView(withId(R.id.detail_BTN_save)).perform(click())
        onView(withId(R.id.login_ET_email)).check(matches(isDisplayed()))
    }

    @Test
    fun guestCopyCode_opensLogin() {
        openFirstDetail()
        onView(withId(R.id.detail_BTN_copy)).perform(click())
        onView(withId(R.id.login_ET_email)).check(matches(isDisplayed()))
    }

    @Test
    fun close_returnsToHome() {
        openFirstDetail()
        onView(withId(R.id.detail_BTN_close)).perform(click())
        waitUntilGone(R.id.detail_LBL_title)
        onView(withId(R.id.home_LAY_scroll)).check(matches(isDisplayed()))
    }

    @Test
    fun couponTab_closesDetail() {
        openFirstDetail()
        onView(withId(R.id.main_LAY_tabCoupons)).perform(click())
        onView(withId(R.id.detail_LBL_title)).check(doesNotExist())
        onView(withId(R.id.favorites_LBL_title)).check(matches(isDisplayed()))
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
