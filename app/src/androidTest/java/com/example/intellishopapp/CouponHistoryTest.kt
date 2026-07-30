package com.example.intellishopapp

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.intellishopapp.model.UserSession
import com.example.intellishopapp.network.RetrofitClient
import com.example.intellishopapp.utilities.SessionManager
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Coupon History: coupons the member acted on show up, most recent first; an
 * empty history shows the empty state. A fresh email per run keeps it isolated.
 * Requires the server (the catalog loads from it).
 */
@RunWith(AndroidJUnit4::class)
class CouponHistoryTest {

    @get:Rule
    val scenario = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun signInMember() {
        RetrofitClient.getInstance().clearCookies()
        SessionManager.getInstance().save(
            UserSession(userId = "h", email = "hist_${System.nanoTime()}@test.local", username = "H")
        )
    }

    @After
    fun cleanUp() {
        SessionManager.getInstance().clear()
    }

    @Test
    fun emptyHistory_showsEmptyState() {
        onView(withId(R.id.main_LAY_tabProfile)).perform(click())
        onView(withId(R.id.profile_LBL_myCoupons)).perform(click())
        onView(withId(R.id.history_LBL_empty)).check(matches(isDisplayed()))
    }

    @Test
    fun copyAction_appearsInHistory() {
        // Open a coupon that has a code and copy it (records history).
        openCouponWithCode()
        onView(withId(R.id.detail_BTN_copy)).perform(click())
        waitForBannerGone()
        // Leave the detail, go to Profile -> Coupon History.
        onView(withId(R.id.main_LAY_tabProfile)).perform(click())
        onView(withId(R.id.profile_LBL_myCoupons)).perform(click())
        waitForChildren(R.id.history_RCV_list)
        onView(withId(R.id.history_RCV_list)).check(matches(hasMinimumChildCount(1)))
    }

    @Test
    fun openingWithoutRedeeming_staysEmpty() {
        // Merely opening a coupon (a "view") must NOT land it in Redeemed Offers.
        openCouponWithCode()
        onView(withId(R.id.detail_BTN_close)).perform(click())
        onView(withId(R.id.main_LAY_tabProfile)).perform(click())
        onView(withId(R.id.profile_LBL_myCoupons)).perform(click())
        val end = System.currentTimeMillis() + 12000
        while (System.currentTimeMillis() < end) {
            try {
                onView(withId(R.id.history_LBL_empty)).check(matches(isDisplayed())); break
            } catch (e: Throwable) { Thread.sleep(300) }
        }
        onView(withId(R.id.history_LBL_empty)).check(matches(isDisplayed()))
    }

    @Test
    fun favoriteFromHistoryRow_saves() {
        RetrofitClient.getInstance().clearCookies()
        openCouponWithCode()
        // Redeem it (copy) so it lands in Redeemed Offers, then leave.
        onView(withId(R.id.detail_BTN_copy)).perform(click())
        waitForBannerGone()
        onView(withId(R.id.detail_BTN_close)).perform(click())
        onView(withId(R.id.main_LAY_tabProfile)).perform(click())
        onView(withId(R.id.profile_LBL_myCoupons)).perform(click())
        waitForChildren(R.id.history_RCV_list)
        // The heart on a history row now works: tapping it saves the coupon.
        onView(withId(R.id.history_RCV_list)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                0, clickChildViewWithId(R.id.item_BTN_favorite)
            )
        )
        waitForBanner(R.string.detail_saved)
    }

    private fun waitForBanner(textRes: Int) {
        val end = System.currentTimeMillis() + 6000
        while (System.currentTimeMillis() < end) {
            try {
                onView(withId(R.id.main_LBL_banner))
                    .check(matches(androidx.test.espresso.matcher.ViewMatchers.withText(textRes)))
                return
            } catch (e: Throwable) {
                Thread.sleep(150)
            }
        }
        onView(withId(R.id.main_LBL_banner))
            .check(matches(androidx.test.espresso.matcher.ViewMatchers.withText(textRes)))
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


    /** The notification overlays mid-screen content; wait until it's gone. */
    private fun waitForBannerGone() {
        val end = System.currentTimeMillis() + 8000
        while (System.currentTimeMillis() < end) {
            try {
                onView(withId(R.id.main_LBL_banner)).check(matches(not(isDisplayed())))
                return
            } catch (e: Throwable) {
                Thread.sleep(200)
            }
        }
    }

}
