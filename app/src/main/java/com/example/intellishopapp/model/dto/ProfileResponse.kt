package com.example.intellishopapp.model.dto

/** Response of GET /profile/ (JSON) — the user's current profile for the editors. */
data class ProfileResponse(
    val status: String? = null,
    val username: String? = null,
    val email: String? = null,
    val statuses: List<String>? = null,
    val hobbies: List<String>? = null
)
