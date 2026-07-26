package com.example.intellishopapp.repository

import com.example.intellishopapp.model.dto.FavoriteRequest
import com.example.intellishopapp.network.RetrofitClient
import com.example.intellishopapp.utilities.ApiResult

/**
 * Coupon action history (copy / go to site / go to offer), per user. Write-through
 * to the backend with a local mirror; a 401 (no server session) is treated as a
 * local-only success, matching the favorites approach.
 */
class HistoryRepository {

    private val api get() = RetrofitClient.getInstance().apiService

    suspend fun add(discountId: String): ApiResult<Unit> {
        return try {
            val response = api.addHistory(FavoriteRequest(discountId))
            when {
                response.isSuccessful -> ApiResult.Success(Unit)
                response.code() == 401 -> ApiResult.Success(Unit) // keep it local
                else -> ApiResult.Error("Failed to record history", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    /** Read history from the backend. 401 (no server session) -> Error (keep local). */
    suspend fun get(): ApiResult<List<String>> {
        return try {
            val response = api.getHistory()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                ApiResult.Success(body.history ?: emptyList())
            } else {
                ApiResult.Error("Not authenticated", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }
}
