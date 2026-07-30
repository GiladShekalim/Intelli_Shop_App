package com.example.intellishopapp.repository

import com.example.intellishopapp.model.dto.ShareRemoveRequest
import com.example.intellishopapp.model.dto.ShareRequest
import com.example.intellishopapp.model.dto.SharedItemDto
import com.example.intellishopapp.model.dto.ShareResponse
import com.example.intellishopapp.network.RetrofitClient
import com.example.intellishopapp.utilities.ApiResult
import com.google.gson.Gson

/**
 * Sharing coupons between users. Unlike favorites/history, sharing is inherently
 * server-only (you can't fabricate what others sent you, or share as a guest), so
 * there is no 401-as-local-success here — both calls require a real session.
 */
class ShareRepository {

    private val api get() = RetrofitClient.getInstance().apiService

    /** Typed outcomes so the dialog can show the precise reason. */
    sealed class ShareResult {
        object Success : ShareResult()
        object UnknownUser : ShareResult()
        object SelfShare : ShareResult()
        data class Failed(val message: String) : ShareResult()
    }

    suspend fun share(toUsername: String, discountId: String): ShareResult {
        return try {
            val response = api.shareCoupon(ShareRequest(toUsername, discountId))
            when {
                response.isSuccessful -> ShareResult.Success
                response.code() == 404 -> ShareResult.UnknownUser
                response.code() == 400 -> {
                    val status = runCatching {
                        Gson().fromJson(response.errorBody()?.string(), ShareResponse::class.java)?.status
                    }.getOrNull()
                    if (status == "self") ShareResult.SelfShare else ShareResult.Failed("Could not share")
                }
                else -> ShareResult.Failed("Could not share")
            }
        } catch (e: Exception) {
            ShareResult.Failed(e.message ?: "Network error")
        }
    }

    /** Recipient dismisses one offer (by sender username + coupon). Needs a session. */
    suspend fun removeShare(fromUsername: String, discountId: String): ApiResult<Unit> {
        return try {
            val response = api.removeShare(ShareRemoveRequest(fromUsername, discountId))
            if (response.isSuccessful) ApiResult.Success(Unit)
            else ApiResult.Error("Could not remove", response.code())
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    /** Coupons shared to the current user. 401 (no session) -> Error. */
    suspend fun getReceived(): ApiResult<List<SharedItemDto>> {
        return try {
            val response = api.getReceivedShares()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                ApiResult.Success(body.received_shares ?: emptyList())
            } else {
                ApiResult.Error("Not authenticated", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }
}
