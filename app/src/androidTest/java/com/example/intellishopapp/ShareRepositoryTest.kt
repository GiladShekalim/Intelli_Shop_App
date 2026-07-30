package com.example.intellishopapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.intellishopapp.model.dto.RegisterRequest
import com.example.intellishopapp.network.RetrofitClient
import com.example.intellishopapp.repository.AuthRepository
import com.example.intellishopapp.repository.ShareRepository
import com.example.intellishopapp.repository.ShareRepository.ShareResult
import com.example.intellishopapp.utilities.ApiResult
import com.example.intellishopapp.utilities.SessionManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The share abuse guards, verified directly against the real backend with a real
 * session (no UI, so no flaky chained-navigation timing). Coupon "1" exists in the
 * catalog; "lala" is a seeded account. Proves: you cannot share as a guest, with
 * yourself, or to a non-existent user — and a valid share succeeds.
 */
@RunWith(AndroidJUnit4::class)
class ShareRepositoryTest {

    private val auth = AuthRepository()
    private val share = ShareRepository()

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
    fun share_asGuest_isRejected() = runBlocking {
        // No session -> the backend refuses (401) and the repo reports a failure.
        val result = share.share("lala", "1")
        assertTrue(result is ShareResult.Failed)
    }

    @Test
    fun share_withSelf_isRejected() = runBlocking {
        loginLala()
        assertTrue(share.share("lala", "1") is ShareResult.SelfShare)
    }

    @Test
    fun share_withUnknownUser_isRejected() = runBlocking {
        loginLala()
        assertTrue(share.share("zzz_nobody_9xk", "1") is ShareResult.UnknownUser)
    }

    @Test
    fun share_toRealRecipient_succeeds() = runBlocking {
        // Register a throwaway recipient, then share to them from lala.
        val name = "shr_" + System.currentTimeMillis()
        val reg = auth.register(
            RegisterRequest(
                username = name, password = "pw1234", email = "$name@ex.com",
                status = emptyList(), age = 20, location = "x", hobbies = emptyList()
            )
        )
        assertTrue(reg is ApiResult.Success)
        loginLala()
        assertTrue(share.share(name, "1") is ShareResult.Success)
    }

    @Test
    fun recipient_canRemoveAReceivedShare() = runBlocking {
        // lala shares to a fresh recipient; the recipient sees it, removes it, and it's gone.
        val name = "shr_" + System.currentTimeMillis()
        assertTrue(
            auth.register(
                RegisterRequest(
                    username = name, password = "pw1234", email = "$name@ex.com",
                    status = emptyList(), age = 20, location = "x", hobbies = emptyList()
                )
            ) is ApiResult.Success
        )
        loginLala()
        assertTrue(share.share(name, "1") is ShareResult.Success)

        // Log in as the recipient and confirm the share is there.
        assertTrue(auth.login("$name@ex.com", "pw1234") is ApiResult.Success)
        val before = share.getReceived()
        assertTrue(before is ApiResult.Success && before.data.any { it.discount_id == "1" })

        // Remove it, then confirm it's gone from the backend.
        assertTrue(share.removeShare("lala", "1") is ApiResult.Success)
        val after = share.getReceived()
        assertTrue(after is ApiResult.Success && after.data.none { it.discount_id == "1" })
    }
}
