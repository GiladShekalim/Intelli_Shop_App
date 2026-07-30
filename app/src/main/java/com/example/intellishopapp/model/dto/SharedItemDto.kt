package com.example.intellishopapp.model.dto

/**
 * One coupon shared to the current user. All fields nullable so a partial document
 * never crashes parsing; the UI skips entries missing a username or discount_id.
 */
data class SharedItemDto(
    val from_user_id: String? = null,
    val from_username: String? = null,
    val discount_id: String? = null
)
