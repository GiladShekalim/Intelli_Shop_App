package com.example.intellishopapp.repository

import com.example.intellishopapp.model.dto.GoogleLoginRequest
import com.example.intellishopapp.model.dto.GoogleLoginResponse
import com.example.intellishopapp.model.dto.LoginRequest
import com.example.intellishopapp.model.dto.LoginResponse
import com.example.intellishopapp.model.dto.RegisterRequest
import com.example.intellishopapp.model.dto.RegisterResponse
import com.example.intellishopapp.network.RetrofitClient
import com.example.intellishopapp.utilities.ApiResult
import com.google.gson.Gson

class AuthRepository {

    private val api get() = RetrofitClient.getInstance().apiService

    // GET the register page once so the csrftoken cookie is set before we POST to login.
    suspend fun ensureCsrfPrimed() {
        try {
            api.primeCsrf()
        } catch (e: Exception) {
            // best-effort priming; login will still be attempted
        }
    }

    suspend fun login(email: String, password: String): ApiResult<LoginResponse> {
        // Drop any stale session cookie so the backend treats this as a fresh login
        // (otherwise it 302-redirects an "already logged in" request to the HTML home page).
        RetrofitClient.getInstance().clearCookies()
        return try {
            val response = api.login(LoginRequest(email, password))
            val body = response.body()
            if (response.isSuccessful && body != null) {
                ApiResult.Success(body)
            } else {
                ApiResult.Error("Login failed", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun register(body: RegisterRequest): ApiResult<RegisterResponse> {
        // Same reason as login: a stale session cookie makes /register/ redirect to HTML.
        RetrofitClient.getInstance().clearCookies()
        return try {
            val response = api.register(body)
            val respBody = response.body()
            if (response.isSuccessful && respBody != null) {
                ApiResult.Success(respBody)
            } else {
                // Surface the backend's real message (e.g. "Email already exists").
                val msg = runCatching {
                    Gson().fromJson(response.errorBody()?.string(), RegisterResponse::class.java)?.message
                }.getOrNull()
                ApiResult.Error(msg ?: "Registration failed", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    /**
     * Is this username free to register? UX-only hint for the registration screen —
     * registration itself still rejects duplicates. Any error (network, etc.) is
     * surfaced so the caller can fail open and not block a legitimate sign-up.
     */
    suspend fun checkUsername(username: String): ApiResult<Boolean> {
        return try {
            val response = api.checkUsername(username)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                ApiResult.Success(body.available)
            } else {
                ApiResult.Error("Check failed", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun googleLogin(idToken: String): ApiResult<GoogleLoginResponse> {
        return try {
            val response = api.googleLogin(GoogleLoginRequest(idToken))
            val body = response.body()
            if (response.isSuccessful && body != null) {
                ApiResult.Success(body)
            } else {
                ApiResult.Error("Google sign-in failed", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }
}
