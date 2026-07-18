package com.example.intellishopapp.repository

import com.example.intellishopapp.model.dto.CouponDto
import com.example.intellishopapp.network.RetrofitClient
import com.example.intellishopapp.utilities.ApiResult

class CouponRepository {

    private val api get() = RetrofitClient.getInstance().apiService

    suspend fun getAllCoupons(): ApiResult<List<CouponDto>> {
        return try {
            val response = api.showAllDiscounts()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                ApiResult.Success(body.discounts)
            } else {
                ApiResult.Error("Failed to load coupons", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }
}
