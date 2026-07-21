package com.example.intellishopapp

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
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
import com.example.intellishopapp.model.UserSession
import com.example.intellishopapp.utilities.SessionManager
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The happy path for a logged-in member: coupon actions run instead of gating.
 * The session is set directly (no network); the coupon list still loads from the
 * server, so the server must be running.
 */
@RunWith(AndroidJUnit4::class)
class CouponDetailMemberTest {

    @get:Rule
    val scenario = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun signInMember() {
        SessionManager.getInstance().save(
            UserSession(userId = "test-member", email = "member@test.local", username = "Member")
        )
    }

    @After
    fun signOut() {
        SessionManager.getInstance().clear()
    }

    private fun openFirstDetail() {
        waitForHero()
        onView(withId(R.id.home_LAY_hero))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
        onView(withId(R.id.detail_LBL_title)).check(matches(isDisplayed()))
        // Let the slide-up animation settle before tapping the action buttons.
        waitCompletelyDisplayed(R.id.detail_BTN_offer)
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

    @Test
    fun memberProfileTab_opensProfilePage() {
        onView(withId(R.id.main_LAY_tabProfile)).perform(click())
        onView(withId(R.id.profile_LBL_title)).check(matches(isDisplayed()))
        onView(withId(R.id.login_ET_email)).check(doesNotExist())
    }

    @Test
    fun memberSave_showsSavedBannerNotLogin() {
        openFirstDetail()
        onView(withId(R.id.detail_BTN_save)).perform(click())
        onView(withId(R.id.login_ET_email)).check(doesNotExist())
        onView(withId(R.id.detail_LAY_savedPill)).check(matches(isDisplayed()))
        onView(withId(R.id.main_LBL_banner)).check(matches(withText(R.string.detail_saved)))
    }

    @Test
    fun memberCopyCode_showsCopiedBannerNotLogin() {
        openFirstDetail()
        onView(withId(R.id.detail_BTN_copy)).perform(click())
        onView(withId(R.id.login_ET_email)).check(doesNotExist())
        onView(withId(R.id.main_LBL_banner)).check(matches(withText(R.string.detail_code_copied)))
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
