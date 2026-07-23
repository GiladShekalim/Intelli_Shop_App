package com.example.intellishopapp.network

import com.example.intellishopapp.model.dto.FavoriteRequest
import com.example.intellishopapp.model.dto.FavoriteResponse
import com.example.intellishopapp.model.dto.FilterRequest
import com.example.intellishopapp.model.dto.FilteredDiscountsResponse
import com.example.intellishopapp.model.dto.GoogleLoginRequest
import com.example.intellishopapp.model.dto.GoogleLoginResponse
import com.example.intellishopapp.model.dto.ShowAllDiscountsResponse
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

    @POST("google_login/")
    suspend fun googleLogin(@Body body: GoogleLoginRequest): Response<GoogleLoginResponse>

    @GET("show_all_discounts/")
    suspend fun showAllDiscounts(): Response<ShowAllDiscountsResponse>

    @POST("filtered_discounts/")
    suspend fun filteredDiscounts(@Body body: FilterRequest): Response<FilteredDiscountsResponse>

    @POST("add_favorite/")
    suspend fun addFavorite(@Body body: FavoriteRequest): Response<FavoriteResponse>

    @POST("remove_favorite/")
    suspend fun removeFavorite(@Body body: FavoriteRequest): Response<FavoriteResponse>
}
