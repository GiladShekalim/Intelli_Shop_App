package com.example.intellishopapp.model.dto

/** Response of GET /redeemed/ — discount ids the user redeemed, most recent first. */
data class RedeemedResponse(
    val redeemed: List<String>? = null
)
