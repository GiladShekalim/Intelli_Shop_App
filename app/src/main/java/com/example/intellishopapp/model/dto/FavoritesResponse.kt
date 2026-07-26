package com.example.intellishopapp.model.dto

/** Response from GET /favorites/ (Accept: application/json) — the user's favorite ids. */
data class FavoritesResponse(
    val favorites: List<String>?
)
