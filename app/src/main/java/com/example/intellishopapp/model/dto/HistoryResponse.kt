package com.example.intellishopapp.model.dto

/** Response from GET /history/ (Accept: application/json) — the user's action history ids. */
data class HistoryResponse(
    val history: List<String>?
)
