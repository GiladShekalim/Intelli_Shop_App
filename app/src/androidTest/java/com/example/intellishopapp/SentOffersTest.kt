package com.example.intellishopapp

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
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

    @Test
    fun favoriteHeartOnSharedCard_works() {
        // The heart on a Sent Offers card uses the same single favorite path as every
        // other list; tapping it saves (member session, 401-safe local write).
        RetrofitClient.getInstance().clearCookies()
        SessionManager.getInstance().setReceivedShares(
            listOf(SharedItemDto(from_user_id = "u1", from_username = "alice", discount_id = "1"))
        )
        openSentOffers()
        waitForText("alice")
        onView(
            org.hamcrest.Matchers.allOf(
                withId(R.id.section_LAY_row),
                androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA(withId(R.id.sent_LAY_sections))
            )
        ).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                0, clickChildViewWithId(R.id.item_BTN_favorite)
            )
        )
        val end = System.currentTimeMillis() + 6000
        while (System.currentTimeMillis() < end) {
            try {
                onView(withId(R.id.main_LBL_banner)).check(matches(withText(R.string.detail_saved))); break
            } catch (e: Throwable) { Thread.sleep(150) }
        }
        onView(withId(R.id.main_LBL_banner)).check(matches(withText(R.string.detail_saved)))
    }

    @Test
    fun longPressOffer_opensRemoveDialog() {
        SessionManager.getInstance().setReceivedShares(
            listOf(SharedItemDto(from_user_id = "u1", from_username = "alice", discount_id = "1"))
        )
        openSentOffers()
        waitForText("alice")
        // Home is behind the overlay and reuses section_LAY_row, so scope to this page.
        onView(
            org.hamcrest.Matchers.allOf(
                withId(R.id.section_LAY_row),
                androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA(withId(R.id.sent_LAY_sections))
            )
        ).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                0, androidx.test.espresso.action.ViewActions.longClick()
            )
        )
        onView(withText(R.string.sent_remove_title))
            .inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog())
            .check(matches(isDisplayed()))
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
