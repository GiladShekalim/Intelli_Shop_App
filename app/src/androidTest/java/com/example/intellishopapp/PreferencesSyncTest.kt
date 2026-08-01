package com.example.intellishopapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.intellishopapp.network.RetrofitClient
import com.example.intellishopapp.repository.AuthRepository
import com.example.intellishopapp.repository.ProfileRepository
import com.example.intellishopapp.utilities.ApiResult
import com.example.intellishopapp.utilities.SessionManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Proves preferences are stored on the BACKEND per user, not just locally: edit via
 * the API, read them back, and confirm a fresh login (a "new device") sees the same
 * values. Uses lala@gmail.com/lala. Directly against the real server, no UI.
 */
@RunWith(AndroidJUnit4::class)
class PreferencesSyncTest {

    private val auth = AuthRepository()
    private val profile = ProfileRepository()

    @Before
    fun fresh() {
        RetrofitClient.getInstance().clearCookies()
        SessionManager.getInstance().clear()
    }

    @After
    fun clean() {
        RetrofitClient.getInstance().clearCookies()
        SessionManager.getInstance().clear()
    }

    private suspend fun loginLala() {
        assertTrue(auth.login("lala@gmail.com", "lala") is ApiResult.Success)
    }

    @Test
    fun updatePreferences_persistsAndReadsBack() = runBlocking {
        loginLala()
        assertTrue(
            profile.updatePreferences(listOf("Student"), listOf("Cars"), listOf("hot"))
                is ApiResult.Success
        )
        val p = profile.getProfile()
        assertTrue(p is ApiResult.Success)
        p as ApiResult.Success
        assertTrue(p.data.statuses == listOf("Student"))
        assertTrue(p.data.hobbies == listOf("Cars"))
        assertTrue(p.data.memberships == listOf("hot"))
    }

    @Test
    fun preferences_syncAcrossAFreshLogin() = runBlocking {
        loginLala()
        assertTrue(
            profile.updatePreferences(listOf("Renter"), listOf("Travel and Vacation"), listOf("adif"))
                is ApiResult.Success
        )
        // Fake a fresh device: drop the local session + cookie, then log in again.
        RetrofitClient.getInstance().clearCookies()
        SessionManager.getInstance().clear()
        loginLala()
        val p = profile.getProfile()
        assertTrue(p is ApiResult.Success)
        p as ApiResult.Success
        assertTrue(p.data.statuses!!.contains("Renter"))
        assertTrue(p.data.hobbies!!.contains("Travel and Vacation"))
    }
}
