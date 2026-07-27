package com.example.intellishopapp

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
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
import com.example.intellishopapp.network.RetrofitClient
import com.example.intellishopapp.utilities.SessionManager
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Proves Coupon History is read from the BACKEND per user: real login, copy a
 * coupon (records to the server), wipe the local mirror, then Coupon History still
 * shows it (re-fetched). Uses lala@gmail.com/lala.
 */
@RunWith(AndroidJUnit4::class)
class HistorySyncTest {

    @get:Rule
    val scenario = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun freshStart() {
        RetrofitClient.getInstance().clearCookies()
        SessionManager.getInstance().clear()
    }

    @After
    fun signOut() {
        RetrofitClient.getInstance().clearCookies()
        SessionManager.getInstance().clear()
    }

    @Test
    fun historyReadFromBackendAfterLocalWipe() {
        onView(withId(R.id.main_LAY_tabProfile)).perform(click())
        onView(withId(R.id.login_ET_email)).perform(typeText("lala@gmail.com"), closeSoftKeyboard())
        onView(withId(R.id.login_ET_password)).perform(typeText("lala"), closeSoftKeyboard())
        onView(withId(R.id.login_BTN_submit)).perform(click())
        waitUntilGone(R.id.login_ET_email)

        // Copy a coupon code (records the action to the backend).
        openCouponWithCode()
        onView(withId(R.id.detail_BTN_copy)).perform(click())
        waitForBannerGone()
        waitForBanner(R.string.detail_code_copied)

        // Fake a fresh device: wipe the local history mirror (keep session + cookie).
        SessionManager.getInstance().setHistory(emptyList())

        // Coupon History re-fetches from the backend -> the coupon still shows.
        onView(withId(R.id.main_LAY_tabProfile)).perform(click())
        onView(withId(R.id.profile_LBL_myCoupons)).perform(click())
        waitForChildren(R.id.history_RCV_list)
        onView(withId(R.id.history_RCV_list)).check(matches(hasMinimumChildCount(1)))
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
        onView(withId(id)).check(matches(hasMinimumChildCount(1)))
    }

    private fun waitForBanner(textRes: Int) {
        val end = System.currentTimeMillis() + 6000
        while (System.currentTimeMillis() < end) {
            try {
                onView(withId(R.id.main_LBL_banner)).check(matches(withText(textRes)))
                return
            } catch (e: Throwable) {
                Thread.sleep(150)
            }
        }
    }

    private fun waitUntilGone(id: Int) {
        val end = System.currentTimeMillis() + 15000
        while (System.currentTimeMillis() < end) {
            try {
                onView(withId(id)).check(doesNotExist())
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
