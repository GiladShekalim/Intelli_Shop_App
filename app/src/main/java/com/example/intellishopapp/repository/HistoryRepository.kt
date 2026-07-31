package com.example.intellishopapp.repository

import com.example.intellishopapp.model.dto.FavoriteRequest
import com.example.intellishopapp.network.RetrofitClient
import com.example.intellishopapp.utilities.ApiResult

/**
 * Coupon views ("Recently Viewed"), per user. Write-through with a local mirror; a
 * 401 is treated as local-only success. Result-shape handling is shared in
 * [RepoSupport] so it is not duplicated across the id-list repositories.
 */
class HistoryRepository {

    private val api get() = RetrofitClient.getInstance().apiService

    suspend fun add(discountId: String): ApiResult<Unit> =
        RepoSupport.writeThrough("Failed to record history") { api.addHistory(FavoriteRequest(discountId)) }

    suspend fun get(): ApiResult<List<String>> =
        RepoSupport.readList({ it.history }) { api.getHistory() }
}
