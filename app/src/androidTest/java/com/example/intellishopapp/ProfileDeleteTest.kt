package com.example.intellishopapp

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.intellishopapp.model.dto.RegisterRequest
import com.example.intellishopapp.network.RetrofitClient
import com.example.intellishopapp.repository.AuthRepository
import com.example.intellishopapp.repository.ProfileRepository
import com.example.intellishopapp.utilities.ApiResult
import com.example.intellishopapp.utilities.SessionManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Deleting an account. WHITE BOX: register a throwaway user, delete via the
 * repository, and confirm the backend no longer has the user or its data (login
 * fails, profile read 401s). BLACK BOX / E2E: do it through the Profile UI and land
 * back in the guest state. Deletion is only treated as done once the backend
 * confirms the record is gone.
 */
@RunWith(AndroidJUnit4::class)
class ProfileDeleteTest {

    @get:Rule
    val scenario = ActivityScenarioRule(MainActivity::class.java)

    private val auth = AuthRepository()
    private val profile = ProfileRepository()

    @After
    fun cleanUp() {
        RetrofitClient.getInstance().clearCookies()
        SessionManager.getInstance().clear()
    }

    private fun newUser() = "del_" + System.currentTimeMillis()

    private suspend fun register(name: String) {
        assertTrue(
            auth.register(
                RegisterRequest(
                    username = name, password = "pw1234", email = "$name@ex.com",
                    status = emptyList(), age = 20, location = "x", hobbies = emptyList()
                )
            ) is ApiResult.Success
        )
    }

    // --- WHITE BOX: repository <-> backend contract ---

    @Test
    fun deleteAccount_reallyRemovesUserAndData() = runBlocking {
        val name = newUser()
        register(name)
        assertTrue(auth.login("$name@ex.com", "pw1234") is ApiResult.Success)
        // Delete returns Success only after the backend confirms the record is gone.
        assertTrue(profile.deleteAccount() is ApiResult.Success)
        // The user's data is gone: the profile read is unauthenticated now...
        assertTrue(profile.getProfile() is ApiResult.Error)
        // ...and the account can no longer log in.
        assertTrue(auth.login("$name@ex.com", "pw1234") is ApiResult.Error)
    }

    // --- BLACK BOX: the Profile delete flow through the UI ---

    @Test
    fun deleteAccount_uiFlow_returnsToGuest() {
        val name = newUser()
        runBlocking { register(name) }
        // Log in through the UI (sets the real session).
        onView(withId(R.id.main_LAY_tabProfile)).perform(click())
        onView(withId(R.id.login_ET_email)).perform(typeText("$name@ex.com"), closeSoftKeyboard())
        onView(withId(R.id.login_ET_password)).perform(typeText("pw1234"), closeSoftKeyboard())
        onView(withId(R.id.login_BTN_submit)).perform(click())
        // Login lands back on Home; open Profile now that we're signed in.
        waitDisplayed(R.id.home_LAY_scroll)
        onView(withId(R.id.main_LAY_tabProfile)).perform(click())
        // Wait for the Profile header (on-screen); the delete row is below the fold.
        waitDisplayed(R.id.profile_LBL_title)
        // Scroll to the lowest option and delete.
        onView(withId(R.id.profile_BTN_delete)).perform(scrollTo(), click())
        onView(withText(R.string.delete_account_confirm)).inRoot(isDialog()).perform(click())
        // After deletion the app returns to guest Home; tapping Profile now gates to Login.
        waitDisplayed(R.id.home_LAY_scroll)
        onView(withId(R.id.main_LAY_tabProfile)).perform(click())
        waitDisplayed(R.id.login_ET_email)
        onView(withId(R.id.login_ET_email)).check(matches(isDisplayed()))
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
