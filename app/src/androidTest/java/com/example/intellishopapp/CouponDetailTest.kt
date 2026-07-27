package com.example.intellishopapp

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
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
        // Let the slide-up animation settle before tapping the lowest buttons.
        waitCompletelyDisplayed(R.id.detail_BTN_offer)
    }

    /** Opens a coupon that HAS a code (text search matches coupon_code). */
    private fun openCouponWithCode() {
        onView(withId(R.id.main_ET_search)).perform(click())
        onView(withId(R.id.main_ET_search)).perform(replaceText("HOT29"), closeSoftKeyboard())
        onView(withId(R.id.main_BTN_search)).perform(click())
        waitForChildren(R.id.search_RCV_results)
        onView(withId(R.id.search_RCV_results))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
        waitCompletelyDisplayed(R.id.detail_BTN_offer)
    }

    @Test
    fun detail_showsStoreAndActions() {
        openFirstDetail()
        onView(withId(R.id.detail_LBL_store)).check(matches(isDisplayed()))
        onView(withId(R.id.detail_BTN_favorite)).check(matches(isDisplayed()))
        // Every coupon in the data has links, so these actions are always offered.
        onView(withId(R.id.detail_BTN_site)).check(matches(isDisplayed()))
        onView(withId(R.id.detail_BTN_offer)).check(matches(isDisplayed()))
    }

    @Test
    fun detail_hidesCopyWhenCouponHasNoCode() {
        // The highest-value coupon (first hero item) has no coupon_code.
        openFirstDetail()
        onView(withId(R.id.detail_BTN_copy)).check(matches(not(isDisplayed())))
    }

    @Test
    fun detail_showsCopyWhenCouponHasCode() {
        openCouponWithCode()
        onView(withId(R.id.detail_BTN_copy)).check(matches(isDisplayed()))
    }

    @Test
    fun guestSave_showsNotificationOnly_noLogin() {
        openFirstDetail()
        onView(withId(R.id.detail_BTN_favorite)).perform(click())
        onView(withId(R.id.main_LBL_banner)).check(matches(withText(R.string.gate_save)))
        onView(withId(R.id.login_ET_email)).check(doesNotExist())
    }

    @Test
    fun guestCopyCode_showsNotificationOnly_noLogin() {
        openCouponWithCode()
        onView(withId(R.id.detail_BTN_copy)).perform(click())
        onView(withId(R.id.main_LBL_banner)).check(matches(withText(R.string.gate_copy)))
        onView(withId(R.id.login_ET_email)).check(doesNotExist())
    }

    private fun waitForChildren(id: Int) {
        val end = System.currentTimeMillis() + 15000
        while (System.currentTimeMillis() < end) {
            try {
                onView(withId(id)).check(matches(hasMinimumChildCount(1)))
                return
            } catch (e: Throwable) {
                Thread.sleep(300)
            }
        }
        onView(withId(id)).check(matches(hasMinimumChildCount(1)))
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
