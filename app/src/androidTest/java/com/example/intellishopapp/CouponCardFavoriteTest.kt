package com.example.intellishopapp

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.intellishopapp.model.UserSession
import com.example.intellishopapp.network.RetrofitClient
import com.example.intellishopapp.utilities.SessionManager
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The heart on a coupon card: a guest gets a sign-in notification only (no
 * redirect); a member toggles the favorite with a relevant notification. Requires
 * the server (the hero cards load from it); cookies are cleared so the member's
 * write falls back to local-only (web parity) and touches no real user.
 */
@RunWith(AndroidJUnit4::class)
class CouponCardFavoriteTest {

    @get:Rule
    val scenario = ActivityScenarioRule(MainActivity::class.java)

    @After
    fun cleanUp() {
        SessionManager.getInstance().clear()
    }

    private fun tapFirstHeroHeart() {
        waitForHero()
        onView(withId(R.id.home_LAY_hero)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                0, clickChildViewWithId(R.id.item_BTN_favorite)
            )
        )
    }

    @Test
    fun guestHeart_showsNotificationOnly_noLogin() {
        SessionManager.getInstance().clear()
        tapFirstHeroHeart()
        onView(withId(R.id.main_LBL_banner)).check(matches(withText(R.string.gate_save)))
        onView(withId(R.id.login_ET_email)).check(doesNotExist())
    }

    @Test
    fun memberHeart_savesThenRemoves() {
        RetrofitClient.getInstance().clearCookies()
        SessionManager.getInstance().save(
            UserSession(userId = "test-member", email = "member@test.local", username = "Member")
        )
        tapFirstHeroHeart()
        waitForBanner(R.string.detail_saved)
        onView(withId(R.id.login_ET_email)).check(doesNotExist())
        tapFirstHeroHeart()
        waitForBanner(R.string.detail_removed)
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

    private fun waitForBanner(textRes: Int) {
        val end = System.currentTimeMillis() + 5000
        while (System.currentTimeMillis() < end) {
            try {
                onView(withId(R.id.main_LBL_banner)).check(matches(withText(textRes)))
                return
            } catch (e: Throwable) {
                Thread.sleep(150)
            }
        }
        onView(withId(R.id.main_LBL_banner)).check(matches(withText(textRes)))
    }
}
