package com.example.intellishopapp.repository

import com.example.intellishopapp.model.dto.FavoriteRequest
import com.example.intellishopapp.network.RetrofitClient
import com.example.intellishopapp.utilities.ApiResult

/**
 * Redeemed coupons (copy / go to site / go to offer) -> the "Redeemed Offers" page.
 * Write-through with a local mirror; a 401 is local-only success. Shares its result
 * handling with the other id-list repositories via [RepoSupport].
 */
class RedeemRepository {

    private val api get() = RetrofitClient.getInstance().apiService

    suspend fun add(discountId: String): ApiResult<Unit> =
        RepoSupport.writeThrough("Failed to record redemption") { api.addRedeemed(FavoriteRequest(discountId)) }

    suspend fun get(): ApiResult<List<String>> =
        RepoSupport.readList({ it.redeemed }) { api.getRedeemed() }
}
