package com.example.intellishopapp

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.intellishopapp.network.RetrofitClient
import com.example.intellishopapp.utilities.SessionManager
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Proves favorites are read from the BACKEND per user (not just local): a real
 * login, save a coupon, wipe the local mirror to fake a fresh device, then the
 * Coupons tab still shows it (re-fetched from the server). Uses lala@gmail.com/lala.
 */
@RunWith(AndroidJUnit4::class)
class FavoritesSyncTest {

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
    fun favoritesReadFromBackendAfterLocalWipe() {
        // Real login (sets the server session cookie).
        onView(withId(R.id.main_LAY_tabProfile)).perform(click())
        onView(withId(R.id.login_ET_email)).perform(typeText("lala@gmail.com"), closeSoftKeyboard())
        onView(withId(R.id.login_ET_password)).perform(typeText("lala"), closeSoftKeyboard())
        onView(withId(R.id.login_BTN_submit)).perform(click())
        waitUntilGone(R.id.login_ET_email)

        // Save the first coupon (real backend write-through).
        waitForHero()
        onView(withId(R.id.home_LAY_hero))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
        // Settle on the favorite button itself: it is always present, whereas the
        // offer button is hidden for coupons that carry no offer link (the top
        // personalized coupon varies per user).
        waitCompletelyDisplayed(R.id.detail_BTN_favorite)
        onView(withId(R.id.detail_BTN_favorite)).perform(click())
        waitForBanner(R.string.detail_saved)

        // Fake a fresh device: wipe ONLY the local favorite mirror (keep session+cookie).
        SessionManager.getInstance().setFavorites(emptyList())

        // Coupons tab re-fetches favorites from the backend -> the coupon still shows.
        onView(withId(R.id.main_LAY_tabCoupons)).perform(click())
        waitForChildren(R.id.favorites_RCV_list)
        onView(withId(R.id.favorites_RCV_list)).check(matches(hasMinimumChildCount(1)))
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
                onView(withId(id)).check(androidx.test.espresso.assertion.ViewAssertions.doesNotExist())
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
}
