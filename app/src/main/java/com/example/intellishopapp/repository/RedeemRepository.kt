package com.example.intellishopapp.repository

import com.example.intellishopapp.model.dto.FavoriteRequest
import com.example.intellishopapp.network.RetrofitClient
import com.example.intellishopapp.utilities.ApiResult

/**
 * Redeemed coupons (copy / go to site / go to offer), per user. Write-through with a
 * local mirror; a 401 (no server session) is treated as a local-only success, like
 * favorites and history. Feeds the "Redeemed Offers" page.
 */
class RedeemRepository {

    private val api get() = RetrofitClient.getInstance().apiService

    suspend fun add(discountId: String): ApiResult<Unit> {
        return try {
            val response = api.addRedeemed(FavoriteRequest(discountId))
            when {
                response.isSuccessful -> ApiResult.Success(Unit)
                response.code() == 401 -> ApiResult.Success(Unit) // keep it local
                else -> ApiResult.Error("Failed to record redemption", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    /** Read redemptions from the backend. 401 -> Error (keep local mirror). */
    suspend fun get(): ApiResult<List<String>> {
        return try {
            val response = api.getRedeemed()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                ApiResult.Success(body.redeemed ?: emptyList())
            } else {
                ApiResult.Error("Not authenticated", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }
}
