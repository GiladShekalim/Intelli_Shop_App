package com.example.intellishopapp.model.dto

/** Response of GET /received_shares/ — coupons others shared to this user, newest first. */
data class ReceivedSharesResponse(
    val received_shares: List<SharedItemDto>? = null
)
