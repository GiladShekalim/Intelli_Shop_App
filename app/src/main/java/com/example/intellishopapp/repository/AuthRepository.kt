package com.example.intellishopapp.repository

import com.example.intellishopapp.model.dto.GoogleLoginRequest
import com.example.intellishopapp.model.dto.GoogleLoginResponse
import com.example.intellishopapp.model.dto.LoginRequest
import com.example.intellishopapp.model.dto.LoginResponse
import com.example.intellishopapp.model.dto.RegisterRequest
import com.example.intellishopapp.model.dto.RegisterResponse
import com.example.intellishopapp.network.RetrofitClient
import com.example.intellishopapp.utilities.ApiResult

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
        return try {
            val response = api.register(body)
            val respBody = response.body()
            if (response.isSuccessful && respBody != null) {
                ApiResult.Success(respBody)
            } else {
                ApiResult.Error("Registration failed", response.code())
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
