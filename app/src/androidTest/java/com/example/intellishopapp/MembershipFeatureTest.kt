package com.example.intellishopapp

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.allOf
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.intellishopapp.logic.MembershipFilter
import com.example.intellishopapp.model.UserSession
import com.example.intellishopapp.network.RetrofitClient
import com.example.intellishopapp.repository.CouponRepository
import com.example.intellishopapp.utilities.ApiResult
import com.example.intellishopapp.utilities.SessionManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end + integration coverage for the membership filter:
 *  - INTEGRATION: the filter, run over the REAL backend catalog, keeps only the
 *    selected club (and no selection keeps everything).
 *  - E2E through the UI: with a membership set, Search shows a coupon of that club
 *    and hides one from another club; with no membership set, nothing is hidden.
 * Requires the server.
 */
@RunWith(AndroidJUnit4::class)
class MembershipFeatureTest {

    @get:Rule
    val scenario = ActivityScenarioRule(MainActivity::class.java)

    @After
    fun cleanUp() {
        SessionManager.getInstance().clear()
        RetrofitClient.getInstance().clearCookies()
    }

    private fun catalog() = runBlocking {
        (CouponRepository().getAllCoupons() as ApiResult.Success).data
    }

    /** A coupon code that exists only on the given club, so searching it is club-specific. */
    private fun codeForClub(club: String): String {
        val c = catalog().firstOrNull { coupon ->
            coupon.club_name?.contains(club) == true &&
                !coupon.coupon_code.isNullOrBlank() && coupon.coupon_code != "N/A"
        } ?: error("no coded coupon found for club '$club'")
        return c.coupon_code!!
    }

    private fun signInWith(memberships: List<String>) {
        SessionManager.getInstance().save(
            UserSession(userId = "mem", email = "mem@test.local", username = "Mem", memberships = memberships)
        )
    }

    private fun search(text: String) {
        onView(withId(R.id.main_ET_search)).perform(click())
        onView(withId(R.id.main_ET_search)).perform(replaceText(text), closeSoftKeyboard())
        onView(withId(R.id.main_BTN_search)).perform(click())
    }

    // --- INTEGRATION: filter over the real catalog ---

    @Test
    fun realCatalog_filteredToHot_isAllHotAndNonEmpty() {
        val all = catalog()
        val hot = MembershipFilter.apply(all, listOf("hot"))
        assertTrue("expected some HOT coupons in the catalog", hot.isNotEmpty())
        assertTrue("filter leaked a non-HOT coupon", hot.all { it.club_name?.contains("hot") == true })
        assertTrue("filter should narrow the catalog", hot.size < all.size)
    }

    @Test
    fun realCatalog_noMembership_isTheFullCatalog() {
        val all = catalog()
        assertTrue(MembershipFilter.apply(all, emptyList()).size == all.size)
    }

    // --- E2E through Search ---

    @Test
    fun search_showsOwnClubCoupon_whenMembershipMatches() {
        signInWith(listOf("hot"))
        search(codeForClub("hot"))
        waitForChildren(R.id.search_RCV_results)
        onView(withId(R.id.search_RCV_results)).check(matches(hasMinimumChildCount(1)))
    }

    @Test
    fun search_hidesOtherClubCoupon_whenMembershipExcludesIt() {
        // Holding only HOT, searching an ADIF-only code returns nothing to show.
        signInWith(listOf("hot"))
        search(codeForClub("adif"))
        waitDisplayed(R.id.search_LBL_empty)
        onView(withId(R.id.search_LBL_empty)).check(matches(isDisplayed()))
    }

    @Test
    fun search_noMembership_showsEveryClub() {
        // Edge case: empty selection = no filter, so an ADIF coupon still appears.
        signInWith(emptyList())
        search(codeForClub("adif"))
        waitForChildren(R.id.search_RCV_results)
        onView(withId(R.id.search_RCV_results)).check(matches(hasMinimumChildCount(1)))
    }

    @Test
    fun memberships_defaultEmpty_soFilterIsOffUntilChosen() {
        // A fresh member holds no memberships, so the filter passes everything.
        signInWith(emptyList())
        assertTrue(SessionManager.getInstance().memberships().isEmpty())
        assertFalse(MembershipFilter.apply(catalog(), SessionManager.getInstance().memberships()).isEmpty())
    }

    @Test
    fun e2e_pickMembershipInProfile_thenSearchHidesOtherClubs() {
        // Full journey: sign in with no memberships, choose HOT in the editor, save,
        // then an ADIF coupon no longer shows in Search.
        signInWith(emptyList())
        onView(withId(R.id.main_LAY_tabProfile)).perform(click())
        onView(withId(R.id.profile_LBL_memberships)).perform(click())
        onView(allOf(withText("HOT"), isDescendantOfA(withId(R.id.pref_LAY_grid)))).perform(click())
        onView(withId(R.id.pref_BTN_save)).perform(click())
        assertTrue(SessionManager.getInstance().memberships().contains("hot"))

        // Save returns to Profile (no search bar there); go to Home, then search.
        onView(withId(R.id.main_LAY_tabHome)).perform(click())
        search(codeForClub("adif"))
        waitDisplayed(R.id.search_LBL_empty)
        onView(withId(R.id.search_LBL_empty)).check(matches(isDisplayed()))
    }

    private fun waitForChildren(id: Int) {
        val end = System.currentTimeMillis() + 15000
        while (System.currentTimeMillis() < end) {
            try {
                onView(withId(id)).check(matches(hasMinimumChildCount(1))); return
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
                onView(withId(id)).check(matches(isDisplayed())); return
            } catch (e: Throwable) {
                Thread.sleep(300)
            }
        }
        onView(withId(id)).check(matches(isDisplayed()))
    }
}
