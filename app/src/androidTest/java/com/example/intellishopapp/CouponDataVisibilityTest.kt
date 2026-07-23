package com.example.intellishopapp

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.intellishopapp.utilities.SessionManager
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies the data that should be visible actually is: coupon cards carry a
 * title, a discount, and an image slot; the detail shows title, description,
 * code, terms, and the hero image. Asserts the image *view* is present (the
 * placeholder counts) so it holds even if the network can't fetch the bitmap.
 * Requires the server.
 */
@RunWith(AndroidJUnit4::class)
class CouponDataVisibilityTest {

    @get:Rule
    val scenario = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun ensureGuest() {
        SessionManager.getInstance().clear()
    }

    @Test
    fun homeCards_showTitleDiscountAndImage() {
        waitForHero()
        onView(withId(R.id.home_LAY_hero))
            .check(matches(hasDescendant(allOf(withId(R.id.item_LBL_title), withNonEmptyText()))))
        onView(withId(R.id.home_LAY_hero))
            .check(matches(hasDescendant(allOf(withId(R.id.item_LBL_discount), withNonEmptyText()))))
        onView(withId(R.id.home_LAY_hero))
            .check(matches(hasDescendant(allOf(withId(R.id.item_IMG_logo), hasDrawable()))))
    }

    @Test
    fun couponDetail_showsAllExpectedFields() {
        waitForHero()
        onView(withId(R.id.home_LAY_hero))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
        onView(withId(R.id.detail_LBL_title)).check(matches(isDisplayed()))
        waitCompletelyDisplayed(R.id.detail_BTN_offer)

        onView(withId(R.id.detail_LBL_couponTitle)).check(matches(withNonEmptyText()))
        onView(withId(R.id.detail_LBL_description)).check(matches(withNonEmptyText()))
        onView(withId(R.id.detail_LBL_code)).check(matches(withNonEmptyText()))
        onView(withId(R.id.detail_LBL_terms)).check(matches(withNonEmptyText()))
        onView(withId(R.id.detail_IMG_hero)).check(matches(allOf(isDisplayed(), hasDrawable())))
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
}
