package com.example.intellishopapp

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.intellishopapp.model.UserSession
import com.example.intellishopapp.model.dto.SharedItemDto
import com.example.intellishopapp.network.RetrofitClient
import com.example.intellishopapp.utilities.SessionManager
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The "Sent Offers by friends" page. A fake member session (no server cookie) makes
 * the backend read 401, so the page renders from the seeded local mirror — which is
 * exactly why the mirror exists (testable without a two-account round-trip).
 */
@RunWith(AndroidJUnit4::class)
class SentOffersTest {

    @get:Rule
    val scenario = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun signInMember() {
        RetrofitClient.getInstance().clearCookies()
        SessionManager.getInstance().save(
            UserSession(userId = "m", email = "sent_${System.nanoTime()}@test.local", username = "M")
        )
    }

    @After
    fun cleanUp() {
        SessionManager.getInstance().clear()
    }

    private fun openSentOffers() {
        onView(withId(R.id.main_LAY_tabProfile)).perform(click())
        onView(withId(R.id.profile_LBL_sentOffers)).perform(click())
    }

    @Test
    fun emptyState_whenNothingShared() {
        SessionManager.getInstance().setReceivedShares(emptyList())
        openSentOffers()
        waitDisplayed(R.id.sent_LBL_empty)
        onView(withId(R.id.sent_LBL_empty)).check(matches(isDisplayed()))
    }

    @Test
    fun groupsSharesUnderSenderName() {
        // Coupon "1" exists in the catalog; sender "alice" labels the section.
        SessionManager.getInstance().setReceivedShares(
            listOf(SharedItemDto(from_user_id = "u1", from_username = "alice", discount_id = "1"))
        )
        openSentOffers()
        waitForText("alice")
        onView(withText("alice")).check(matches(isDisplayed()))
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

    private fun waitForText(text: String) {
        val end = System.currentTimeMillis() + 15000
        while (System.currentTimeMillis() < end) {
            try {
                onView(withText(text)).check(matches(isDisplayed()))
                return
            } catch (e: Throwable) {
                Thread.sleep(300)
            }
        }
        onView(withText(text)).check(matches(isDisplayed()))
    }
}
