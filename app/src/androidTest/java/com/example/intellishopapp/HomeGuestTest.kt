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
 * Guest Home (Figma layout): a hero row + category sections are browsable by
 * anyone; a guest tapping a coupon opens the Details sheet (the sign-in gate is on
 * the actions inside it). Requires the Django server running.
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
    fun home_showsScrollableFeed() {
        onView(withId(R.id.home_LAY_scroll)).check(matches(isDisplayed()))
    }

    @Test
    fun home_loadsFeaturedCouponsFromBackend() {
        waitForHero()
        onView(withId(R.id.home_LAY_hero)).check(matches(hasMinimumChildCount(1)))
    }

    @Test
    fun home_showsBestMatchesGreeting() {
        waitForHero()
        onView(withId(R.id.home_LBL_bestMatches)).check(matches(isDisplayed()))
    }

    @Test
    fun home_showsTwoBigCardRows() {
        waitForHero()
        // With the full catalog, the 10 suggestions fill both rows of five.
        onView(withId(R.id.home_LAY_hero)).check(matches(hasMinimumChildCount(1)))
        onView(withId(R.id.home_LAY_hero2)).check(matches(hasMinimumChildCount(1)))
    }

    @Test
    fun guestTapCoupon_opensDetail() {
        waitForHero()
        onView(withId(R.id.home_LAY_hero))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
        onView(withId(R.id.detail_LBL_title)).check(matches(isDisplayed()))
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
}
