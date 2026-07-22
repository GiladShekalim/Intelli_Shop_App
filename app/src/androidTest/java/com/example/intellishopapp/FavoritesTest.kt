package com.example.intellishopapp

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
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
 * The Coupons tab with no saved coupons shows the empty state. Requires the
 * server (the catalog is fetched before it can decide the list is empty).
 */
@RunWith(AndroidJUnit4::class)
class FavoritesTest {

    @get:Rule
    val scenario = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun ensureGuest() {
        SessionManager.getInstance().clear()
    }

    @Test
    fun guest_withNoFavorites_showsEmptyState() {
        onView(withId(R.id.main_LAY_tabCoupons)).perform(click())
        waitDisplayed(R.id.favorites_LBL_empty)
        onView(withId(R.id.favorites_LBL_empty)).check(matches(isDisplayed()))
    }

    private fun waitDisplayed(id: Int) {
        val end = System.currentTimeMillis() + 15000
        while (System.currentTimeMillis() < end) {
            try {
                onView(withId(id)).check(matches(isDisplayed()))
                return
            } catch (e: Throwable) {
                Thread.sleep(300)
            }
        }
        onView(withId(id)).check(matches(isDisplayed()))
    }
}
