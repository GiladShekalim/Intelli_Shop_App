package com.example.intellishopapp.repository

import com.example.intellishopapp.model.dto.AiFilterRequest
import com.example.intellishopapp.model.dto.CouponDto
import com.example.intellishopapp.model.dto.FilterRequest
import com.example.intellishopapp.network.RetrofitClient
import com.example.intellishopapp.utilities.ApiResult

/** Runs coupon searches/filters against the backend's /filtered_discounts/ endpoint. */
class SearchRepository {

    private val api get() = RetrofitClient.getInstance().apiService

    suspend fun search(request: FilterRequest): ApiResult<List<CouponDto>> {
        return try {
            val response = api.filteredDiscounts(request)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                ApiResult.Success(body.discounts)
            } else {
                ApiResult.Error("Search failed", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    /** Ask the AI helper to turn free text into a FilterRequest (statuses/interests/…). */
    suspend fun aiFilters(userText: String): ApiResult<FilterRequest> {
        return try {
            val aiResponse = api.aiFilterHelper(AiFilterRequest(userText))
            val aiBody = aiResponse.body()
            if (!aiResponse.isSuccessful || aiBody?.filters == null) {
                ApiResult.Error("AI helper failed", aiResponse.code())
            } else {
                ApiResult.Success(aiBody.filters)
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }
}
