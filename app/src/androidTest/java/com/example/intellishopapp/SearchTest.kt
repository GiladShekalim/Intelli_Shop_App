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
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.intellishopapp.utilities.SessionManager
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Search driven by the static top bar (AI Filter / field / Search live in the
 * shell). Tapping the field shows filters; the buttons run the search; applied
 * filters show as removable labels. Requires the server.
 */
@RunWith(AndroidJUnit4::class)
class SearchTest {

    @get:Rule
    val scenario = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun ensureGuest() {
        SessionManager.getInstance().clear()
    }

    private fun openFilters() {
        onView(withId(R.id.main_ET_search)).perform(click())
        onView(withId(R.id.search_LAY_interestGrid)).check(matches(isDisplayed()))
    }

    @Test
    fun tapField_showsFilters() {
        openFilters()
    }

    @Test
    fun textSearch_showsResults() {
        openFilters()
        onView(withId(R.id.main_ET_search)).perform(typeText("hot"), closeSoftKeyboard())
        onView(withId(R.id.main_BTN_search)).perform(click())
        waitForChildren(R.id.search_RCV_results)
        onView(withId(R.id.search_RCV_results)).check(matches(hasMinimumChildCount(1)))
    }

    @Test
    fun hebrewSearch_showsResults() {
        openFilters()
        onView(withId(R.id.main_ET_search)).perform(replaceText("הנחה"), closeSoftKeyboard())
        onView(withId(R.id.main_BTN_search)).perform(click())
        waitForChildren(R.id.search_RCV_results)
        onView(withId(R.id.search_RCV_results)).check(matches(hasMinimumChildCount(1)))
    }

    @Test
    fun shortQuery_showsMinCharsBanner() {
        openFilters()
        onView(withId(R.id.main_ET_search)).perform(typeText("h"), closeSoftKeyboard())
        onView(withId(R.id.main_BTN_search)).perform(click())
        onView(withId(R.id.main_LBL_banner)).check(matches(withText(R.string.search_min_chars)))
    }

    @Test
    fun aiSearch_completesToTerminalState() {
        openFilters()
        onView(withId(R.id.main_ET_search))
            .perform(replaceText("cheap electronics for students"), closeSoftKeyboard())
        onView(withId(R.id.main_BTN_ai)).perform(click())
        waitUntilGone(R.id.search_PRG_loading)
    }

    @Test
    fun categoryFilter_showsResultsAndRemovableLabel() {
        openFilters()
        onView(allOf(withText("Consumerism"), isDescendantOfA(withId(R.id.search_LAY_interestGrid))))
            .perform(scrollTo(), click())
        onView(withId(R.id.main_BTN_search)).perform(click())
        waitForChildren(R.id.search_RCV_results)
        // The applied filter shows as a label.
        onView(withId(R.id.search_LAY_labels)).check(matches(hasMinimumChildCount(1)))
        // Removing it (scoped to the labels row) clears the label and re-runs.
        onView(allOf(withText(containsString("Consumerism")), isDescendantOfA(withId(R.id.search_LAY_labels))))
            .perform(click())
        waitUntilGone(R.id.search_LAY_labelsScroll)
    }

    @Test
    fun searchResult_opensDetail() {
        openFilters()
        onView(withId(R.id.main_ET_search)).perform(typeText("hot"), closeSoftKeyboard())
        onView(withId(R.id.main_BTN_search)).perform(click())
        waitForChildren(R.id.search_RCV_results)
        onView(withId(R.id.search_RCV_results))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
        onView(withId(R.id.detail_LBL_title)).check(matches(isDisplayed()))
    }

    @Test
    fun backFromSearch_returnsToHome() {
        openFilters()
        onView(withId(R.id.main_ET_search)).perform(closeSoftKeyboard())
        pressBack()
        onView(withId(R.id.search_LAY_interestGrid)).check(doesNotExist())
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
