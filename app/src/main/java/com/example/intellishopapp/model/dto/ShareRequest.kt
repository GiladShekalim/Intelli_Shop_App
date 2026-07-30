package com.example.intellishopapp.model.dto

/** Body of POST /share_coupon/ — send a coupon to a user by username. */
data class ShareRequest(
    val to_username: String,
    val discount_id: String
)
