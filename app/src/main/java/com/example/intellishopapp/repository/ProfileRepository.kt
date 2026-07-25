package com.example.intellishopapp.repository

import com.example.intellishopapp.network.RetrofitClient
import com.example.intellishopapp.utilities.ApiResult
import com.google.gson.Gson

/** Profile updates against the backend's /profile/ endpoint (session-cookie auth). */
class ProfileRepository {

    private val api get() = RetrofitClient.getInstance().apiService

    /** Change password. Returns the backend's message on success or a typed error. */
    suspend fun changePassword(
        current: String,
        new: String,
        confirm: String
    ): ApiResult<String> {
        return try {
            val response = api.changePassword(
                accept = "application/json",
                action = "update_password",
                currentPassword = current,
                newPassword = new,
                confirmPassword = confirm
            )
            val body = response.body()
            if (response.isSuccessful && body?.status == "success") {
                ApiResult.Success(body.message ?: "Password updated")
            } else {
                // Backend sends {status:error, message:...} for a bad current/mismatch.
                val msg = body?.message ?: runCatching {
                    Gson().fromJson(response.errorBody()?.string(), com.example.intellishopapp.model.dto.ProfileUpdateResponse::class.java)?.message
                }.getOrNull()
                ApiResult.Error(msg ?: "Could not update password", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }
}
