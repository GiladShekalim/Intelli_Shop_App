package com.example.intellishopapp

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
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
 * Guest Home: the coupon list is browsable by anyone; a guest tapping a coupon
 * is prompted to sign up and sent to Login. Requires the Django server running.
 */
@RunWith(AndroidJUnit4::class)
class HomeGuestTest {

    @get:Rule
    val scenario = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun ensureGuest() {
        SessionManager.getInstance().clear()
    }

    @Test
    fun home_showsCouponList() {
        onView(withId(R.id.home_LAY_couponList)).check(matches(isDisplayed()))
    }

    @Test
    fun home_loadsCouponsFromBackend() {
        waitForCoupons()
        onView(withId(R.id.home_LAY_couponList)).check(matches(hasMinimumChildCount(1)))
    }

    @Test
    fun guestTapCoupon_opensLogin() {
        waitForCoupons()
        onView(withId(R.id.home_LAY_couponList))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
        onView(withId(R.id.login_ET_email)).check(matches(isDisplayed()))
    }

    private fun waitForCoupons() {
        val end = System.currentTimeMillis() + 15000
        while (System.currentTimeMillis() < end) {
            try {
                onView(withId(R.id.home_LAY_couponList)).check(matches(hasMinimumChildCount(1)))
                return
            } catch (e: Throwable) {
                Thread.sleep(400)
            }
        }
    }
}
