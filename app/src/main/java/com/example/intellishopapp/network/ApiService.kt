package com.example.intellishopapp.network

import com.example.intellishopapp.model.dto.AiFilterRequest
import com.example.intellishopapp.model.dto.AiFilterResponse
import com.example.intellishopapp.model.dto.FavoriteRequest
import com.example.intellishopapp.model.dto.FavoriteResponse
import com.example.intellishopapp.model.dto.FavoritesResponse
import com.example.intellishopapp.model.dto.FilterRequest
import com.example.intellishopapp.model.dto.FilteredDiscountsResponse
import com.example.intellishopapp.model.dto.GoogleLoginRequest
import com.example.intellishopapp.model.dto.GoogleLoginResponse
import com.example.intellishopapp.model.dto.HistoryResponse
import com.example.intellishopapp.model.dto.ProfileUpdateResponse
import com.example.intellishopapp.model.dto.ShowAllDiscountsResponse
import com.example.intellishopapp.model.dto.LoginRequest
import com.example.intellishopapp.model.dto.LoginResponse
import com.example.intellishopapp.model.dto.RegisterRequest
import com.example.intellishopapp.model.dto.RegisterResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
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

    @POST("ai_filter_helper/")
    suspend fun aiFilterHelper(@Body body: AiFilterRequest): Response<AiFilterResponse>

    // profile_view reads form fields and returns JSON when Accept: application/json.
    @FormUrlEncoded
    @POST("profile/")
    suspend fun changePassword(
        @Header("Accept") accept: String,
        @Field("action") action: String,
        @Field("current_password") currentPassword: String,
        @Field("new_password") newPassword: String,
        @Field("confirm_password") confirmPassword: String
    ): Response<ProfileUpdateResponse>

    @GET("favorites/")
    @Headers("Accept: application/json")
    suspend fun getFavorites(): Response<FavoritesResponse>

    @POST("add_history/")
    suspend fun addHistory(@Body body: FavoriteRequest): Response<FavoriteResponse>

    @GET("history/")
    @Headers("Accept: application/json")
    suspend fun getHistory(): Response<HistoryResponse>

    @POST("add_favorite/")
    suspend fun addFavorite(@Body body: FavoriteRequest): Response<FavoriteResponse>

    @POST("remove_favorite/")
    suspend fun removeFavorite(@Body body: FavoriteRequest): Response<FavoriteResponse>
}
