package com.example.intellishopapp

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.allOf
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.intellishopapp.utilities.SessionManager
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Search over the shell: open from the top bar, run a text query against the
 * backend, open a result, and dismiss. Uses "hot" which matches most of the
 * sample data. Requires the server.
 */
@RunWith(AndroidJUnit4::class)
class SearchTest {

    @get:Rule
    val scenario = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun ensureGuest() {
        SessionManager.getInstance().clear()
    }

    private fun openSearch() {
        onView(withId(R.id.main_LAY_search)).perform(click())
        onView(withId(R.id.search_ET_query)).check(matches(isDisplayed()))
    }

    @Test
    fun searchBar_opensSearchWithPrompt() {
        openSearch()
        onView(withId(R.id.search_LBL_empty)).check(matches(isDisplayed()))
    }

    @Test
    fun shortQuery_showsMinCharsBannerNoResults() {
        openSearch()
        onView(withId(R.id.search_ET_query)).perform(typeText("h"), closeSoftKeyboard())
        onView(withId(R.id.search_BTN_go)).perform(click())
        onView(withId(R.id.main_LBL_banner)).check(matches(withText(R.string.search_min_chars)))
        onView(withId(R.id.search_RCV_results)).check(matches(not(isDisplayed())))
    }

    @Test
    fun textQuery_showsResults() {
        openSearch()
        onView(withId(R.id.search_ET_query)).perform(typeText("hot"), closeSoftKeyboard())
        onView(withId(R.id.search_BTN_go)).perform(click())
        waitForChildren(R.id.search_RCV_results)
        onView(withId(R.id.search_RCV_results)).check(matches(hasMinimumChildCount(1)))
    }

    @Test
    fun hebrewQuery_showsResults() {
        openSearch()
        // Injected directly (no keyboard needed) to prove Hebrew search works end to
        // end: "הנחה" (discount) matches most of the Hebrew coupon data.
        onView(withId(R.id.search_ET_query)).perform(replaceText("הנחה"), closeSoftKeyboard())
        onView(withId(R.id.search_BTN_go)).perform(click())
        waitForChildren(R.id.search_RCV_results)
        onView(withId(R.id.search_RCV_results)).check(matches(hasMinimumChildCount(1)))
    }

    @Test
    fun aiHelper_emptyQuery_showsBanner() {
        openSearch()
        onView(withId(R.id.search_BTN_ai)).perform(click())
        onView(withId(R.id.main_LBL_banner)).check(matches(withText(R.string.search_ai_empty)))
    }

    @Test
    fun aiHelper_completesToTerminalState() {
        openSearch()
        onView(withId(R.id.search_ET_query))
            .perform(replaceText("cheap electronics for students"), closeSoftKeyboard())
        onView(withId(R.id.search_BTN_ai)).perform(click())
        // AI round-trips through Groq, then runs a filtered search; either way the
        // spinner must resolve (results or the empty/failed message).
        waitUntilGone(R.id.search_PRG_loading)
    }

    @Test
    fun searchResult_opensDetail() {
        openSearch()
        onView(withId(R.id.search_ET_query)).perform(typeText("hot"), closeSoftKeyboard())
        onView(withId(R.id.search_BTN_go)).perform(click())
        waitForChildren(R.id.search_RCV_results)
        onView(withId(R.id.search_RCV_results))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
        onView(withId(R.id.detail_LBL_title)).check(matches(isDisplayed()))
    }

    @Test
    fun filters_openShowsCategoryAndStatusGrids() {
        openSearch()
        onView(withId(R.id.search_BTN_filters)).perform(click())
        onView(withId(R.id.search_LAY_interestGrid)).check(matches(hasMinimumChildCount(1)))
        onView(withId(R.id.search_LAY_statusGrid)).check(matches(hasMinimumChildCount(1)))
    }

    @Test
    fun filters_applyCategory_showsResults() {
        openSearch()
        onView(withId(R.id.search_BTN_filters)).perform(click())
        onView(allOf(withText("Consumerism"), isDescendantOfA(withId(R.id.search_LAY_interestGrid))))
            .perform(scrollTo(), click())
        onView(withId(R.id.search_BTN_apply)).perform(scrollTo(), click())
        waitForChildren(R.id.search_RCV_results)
        onView(withId(R.id.search_RCV_results)).check(matches(hasMinimumChildCount(1)))
    }

    @Test
    fun backFromSearch_returnsToHome() {
        openSearch()
        pressBack()
        onView(withId(R.id.search_ET_query)).check(doesNotExist())
        onView(withId(R.id.home_LAY_scroll)).check(matches(isDisplayed()))
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
    }

    private fun waitUntilGone(id: Int) {
        val end = System.currentTimeMillis() + 40000
        while (System.currentTimeMillis() < end) {
            try {
                onView(withId(id)).check(matches(not(isDisplayed())))
                return
            } catch (e: Throwable) {
                Thread.sleep(300)
            }
        }
        onView(withId(id)).check(matches(not(isDisplayed())))
    }
}
