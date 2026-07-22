package com.example.intellishopapp.repository

import com.example.intellishopapp.model.dto.FavoriteRequest
import com.example.intellishopapp.model.dto.FavoriteResponse
import com.example.intellishopapp.network.RetrofitClient
import com.example.intellishopapp.utilities.ApiResult
import retrofit2.Response

/**
 * Favorites are local-first (mirrored in the session) with write-through to the
 * backend, matching the old web client. As there, a 401 (no server session) is
 * treated as a local-only success so the local set still updates.
 */
class FavoriteRepository {

    private val api get() = RetrofitClient.getInstance().apiService

    suspend fun add(discountId: String): ApiResult<Unit> =
        runCatching { toResult(api.addFavorite(FavoriteRequest(discountId))) }
            .getOrElse { ApiResult.Error(it.message ?: "Network error") }

    suspend fun remove(discountId: String): ApiResult<Unit> =
        runCatching { toResult(api.removeFavorite(FavoriteRequest(discountId))) }
            .getOrElse { ApiResult.Error(it.message ?: "Network error") }

    private fun toResult(response: Response<FavoriteResponse>): ApiResult<Unit> = when {
        response.isSuccessful -> ApiResult.Success(Unit)
        // Web parity: not authenticated server-side -> keep it local-only.
        response.code() == 401 -> ApiResult.Success(Unit)
        else -> ApiResult.Error("Failed to update favorites", response.code())
    }
}
