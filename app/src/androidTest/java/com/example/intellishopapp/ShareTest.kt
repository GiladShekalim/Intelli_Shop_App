package com.example.intellishopapp

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
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
 * Sharing UI surfaces: a guest gets the Sign-Up gate (no dialog), a member gets the
 * recipient dialog. The abuse guards (self-share, unknown user) are covered
 * deterministically against the real backend in [ShareRepositoryTest].
 */
@RunWith(AndroidJUnit4::class)
class ShareTest {

    @get:Rule
    val scenario = ActivityScenarioRule(MainActivity::class.java)

    @After
    fun cleanUp() {
        RetrofitClient.getInstance().clearCookies()
        SessionManager.getInstance().clear()
    }

    private fun openFirstDetail() {
        waitForHero()
        onView(withId(R.id.home_LAY_hero))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
        waitCompletelyDisplayed(R.id.detail_BTN_share)
    }

    @Test
    fun guestShare_showsSignUpGate_noDialog() {
        SessionManager.getInstance().clear()
        openFirstDetail()
        onView(withId(R.id.detail_BTN_share)).perform(click())
        onView(withId(R.id.main_LBL_banner)).check(matches(withText(R.string.gate_share)))
        onView(withText(R.string.share_dialog_title)).check(doesNotExist())
    }

    @Test
    fun memberShare_opensDialog() {
        RetrofitClient.getInstance().clearCookies()
        SessionManager.getInstance().save(
            UserSession(userId = "m", email = "m@test.local", username = "Member")
        )
        openFirstDetail()
        onView(withId(R.id.detail_BTN_share)).perform(click())
        onView(withText(R.string.share_dialog_title)).inRoot(isDialog()).check(matches(isDisplayed()))
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
        val end = System.currentTimeMillis() + 8000
        while (System.currentTimeMillis() < end) {
            try {
                onView(withId(id)).check(matches(isCompletelyDisplayed()))
                return
            } catch (e: Throwable) {
                Thread.sleep(120)
            }
        }
        onView(withId(id)).check(matches(isCompletelyDisplayed()))
    }
}
