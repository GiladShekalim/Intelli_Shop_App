package com.example.intellishopapp.model.dto

/**
 * Coupon wire model — exact backend field names, data only. All fields nullable
 * so varied/partial documents from Mongo never crash parsing; the UI/formatter
 * apply safe defaults.
 */
data class CouponDto(
    val discount_id: String?,
    val title: String?,
    val price: Double?,
    val discount_type: String?,
    val description: String?,
    val image_link: String?,
    val discount_link: String?,
    val terms_and_conditions: String?,
    val club_name: List<String>?,
    val category: List<String>?,
    val valid_until: String?,
    val usage_limit: Int?,
    val coupon_code: String?,
    val provider_link: String?,
    val consumer_statuses: List<String>?,
    // Currency symbol for a fixed-amount price ("$", "€"); null defaults to shekels (₪).
    val currency: String? = null
)
