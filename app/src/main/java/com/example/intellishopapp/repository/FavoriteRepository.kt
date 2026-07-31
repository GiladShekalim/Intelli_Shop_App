package com.example.intellishopapp.repository

import com.example.intellishopapp.model.dto.FavoriteRequest
import com.example.intellishopapp.network.RetrofitClient
import com.example.intellishopapp.utilities.ApiResult

/**
 * Favorites are local-first (mirrored in the session) with write-through to the
 * backend, matching the old web client. As there, a 401 (no server session) is
 * treated as a local-only success so the local set still updates. Result handling
 * is shared with the other id-list repositories via [RepoSupport].
 */
class FavoriteRepository {

    private val api get() = RetrofitClient.getInstance().apiService

    /** Read the user's favorites from the backend. 401 (no server session) -> Error. */
    suspend fun getFavorites(): ApiResult<List<String>> =
        RepoSupport.readList({ it.favorites }) { api.getFavorites() }

    suspend fun add(discountId: String): ApiResult<Unit> =
        RepoSupport.writeThrough("Failed to update favorites") { api.addFavorite(FavoriteRequest(discountId)) }

    suspend fun remove(discountId: String): ApiResult<Unit> =
        RepoSupport.writeThrough("Failed to update favorites") { api.removeFavorite(FavoriteRequest(discountId)) }
}
