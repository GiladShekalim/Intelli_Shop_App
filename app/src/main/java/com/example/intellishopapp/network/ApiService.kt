package com.example.intellishopapp.network

import com.example.intellishopapp.model.dto.LoginRequest
import com.example.intellishopapp.model.dto.LoginResponse
import com.example.intellishopapp.model.dto.RegisterRequest
import com.example.intellishopapp.model.dto.RegisterResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    // GET the register page once to seed the csrftoken cookie before posting to login.
    @GET("register/")
    suspend fun primeCsrf(): Response<ResponseBody>

    @POST("login/")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @POST("register/")
    suspend fun register(@Body body: RegisterRequest): Response<RegisterResponse>
}
