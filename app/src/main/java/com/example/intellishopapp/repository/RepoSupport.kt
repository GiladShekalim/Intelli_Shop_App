package com.example.intellishopapp.repository

import com.example.intellishopapp.utilities.ApiResult
import retrofit2.Response

/**
 * One place for the two HTTP result shapes that the per-user id-list repositories
 * (favorites, history, redeemed) all share, so the try/catch + 401 handling is not
 * copy-pasted into each. Keeps each repository a thin, single-responsibility wrapper
 * over its own endpoints.
 */
object RepoSupport {

    /**
     * Write-through call: success on 2xx; a 401 (no server session) is treated as a
     * local-only success so the caller's local mirror still updates (web parity).
     */
    suspend fun writeThrough(
        failMessage: String,
        call: suspend () -> Response<*>
    ): ApiResult<Unit> = try {
        val response = call()
        when {
            response.isSuccessful -> ApiResult.Success(Unit)
            response.code() == 401 -> ApiResult.Success(Unit)
            else -> ApiResult.Error(failMessage, response.code())
        }
    } catch (e: Exception) {
        ApiResult.Error(e.message ?: "Network error")
    }

    /** Read a string list from a JSON body; 401/error -> Error so the caller keeps local. */
    suspend fun <T> readList(
        extract: (T) -> List<String>?,
        call: suspend () -> Response<T>
    ): ApiResult<List<String>> = try {
        val response = call()
        val body = response.body()
        if (response.isSuccessful && body != null) {
            ApiResult.Success(extract(body) ?: emptyList())
        } else {
            ApiResult.Error("Not authenticated", response.code())
        }
    } catch (e: Exception) {
        ApiResult.Error(e.message ?: "Network error")
    }
}
