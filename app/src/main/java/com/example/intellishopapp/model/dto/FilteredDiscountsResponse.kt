package com.example.intellishopapp.model.dto

data class FilteredDiscountsResponse(
    val discounts: List<CouponDto>,
    val total_count: Int?,
    val search_type: String?
)
