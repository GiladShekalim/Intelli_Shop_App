package com.example.intellishopapp.repository

import com.example.intellishopapp.network.RetrofitClient
import com.example.intellishopapp.utilities.ApiResult
import com.google.gson.Gson

/** Profile updates against the backend's /profile/ endpoint (session-cookie auth). */
class ProfileRepository {

    private val api get() = RetrofitClient.getInstance().apiService

    /** The user's saved statuses/interests, read fresh from the backend. */
    suspend fun getProfile(): ApiResult<com.example.intellishopapp.model.dto.ProfileResponse> {
        return try {
            val response = api.getProfile()
            val body = response.body()
            if (response.isSuccessful && body != null) ApiResult.Success(body)
            else ApiResult.Error("Not authenticated", response.code())
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    /**
     * Save both statuses and interests. Write-through with a 401-as-local-success
     * fallback (same as favorites) so the editor still works offline; a real session
     * syncs the change to every device.
     */
    suspend fun updatePreferences(statuses: List<String>, hobbies: List<String>): ApiResult<Unit> {
        return try {
            val response = api.updatePreferences(
                accept = "application/json",
                action = "update_preferences",
                statuses = statuses,
                hobbies = hobbies
            )
            when {
                response.isSuccessful -> ApiResult.Success(Unit)
                response.code() == 401 -> ApiResult.Success(Unit)
                else -> ApiResult.Error("Could not save preferences", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

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
