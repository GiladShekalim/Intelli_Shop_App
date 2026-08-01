package com.example.intellishopapp

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.intellishopapp.model.UserSession
import com.example.intellishopapp.network.RetrofitClient
import com.example.intellishopapp.repository.CouponRepository
import com.example.intellishopapp.utilities.ApiResult
import com.example.intellishopapp.utilities.SessionManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
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

    @Test
    fun reopeningFavorites_reusesTheList_noRebuild() {
        // Fake a signed-in member with one saved coupon that exists in the catalog.
        // Cookies are cleared so the backend favorites read 401s and the local set stays.
        RetrofitClient.getInstance().clearCookies()
        SessionManager.getInstance().save(
            UserSession(userId = "flick", email = "flick@test.local", username = "Flick")
        )
        val realId = runBlocking {
            (CouponRepository().getAllCoupons() as ApiResult.Success).data.first().discount_id!!
        }
        SessionManager.getInstance().setFavorites(listOf(realId))
        onView(withId(R.id.main_LAY_tabCoupons)).perform(click())
        waitForChildren(R.id.favorites_RCV_list) // wait until the list is actually populated
        var first: RecyclerView.Adapter<*>? = null
        scenario.scenario.onActivity {
            first = it.findViewById<RecyclerView>(R.id.favorites_RCV_list).adapter
        }
        assertNotNull("the favorites list should have rendered", first)
        // Leave the tab and come back: the saved set is unchanged.
        onView(withId(R.id.main_LAY_tabHome)).perform(click())
        onView(withId(R.id.main_LAY_tabCoupons)).perform(click())
        waitForChildren(R.id.favorites_RCV_list)
        Thread.sleep(1500) // let the quiet background refresh settle
        var second: RecyclerView.Adapter<*>? = null
        scenario.scenario.onActivity {
            second = it.findViewById<RecyclerView>(R.id.favorites_RCV_list).adapter
        }
        // Same adapter instance = the list was not rebuilt = no image-reload flicker.
        assertSame("re-opening rebuilt the list (flicker)", first, second)
        SessionManager.getInstance().setFavorites(emptyList())
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
