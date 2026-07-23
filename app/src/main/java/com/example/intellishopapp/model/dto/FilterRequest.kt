package com.example.intellishopapp.model.dto

/**
 * Body for POST /filtered_discounts/ — exact backend field names. Null fields are
 * omitted by Gson, so a text-only search sends just text_search. Backed only by
 * parameters the backend accepts (no new/unsupported filters).
 */
data class FilterRequest(
    val text_search: String? = null,
    val statuses: List<String>? = null,
    val interests: List<String>? = null,
    val price_range: PriceRange? = null,
    val percentage_range: PercentageRange? = null
)

data class PriceRange(
    val enabled: Boolean,
    val max_value: Double
)

data class PercentageRange(
    val enabled: Boolean,
    val max_value: Double? = null,
    val bucket: String? = null
)
