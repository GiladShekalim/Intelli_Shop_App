package com.example.intellishopapp.model.dto

/** Body for POST /ai_filter_helper/ — free text the AI turns into filters. */
data class AiFilterRequest(val user_text: String)

/**
 * Response from /ai_filter_helper/. The extracted [filters] reuse the same shape
 * as [FilterRequest] (statuses / interests / price_range / percentage_range).
 */
data class AiFilterResponse(
    val filters: FilterRequest?,
    val success: Boolean?,
    val error: String?
)
