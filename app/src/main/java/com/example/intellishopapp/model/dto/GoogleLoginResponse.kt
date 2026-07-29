package com.example.intellishopapp.model.dto

data class GoogleLoginResponse(
    val status: String,
    val is_new: Boolean,
    val user_id: String? = null,
    val username: String? = null,
    val email: String? = null,
    val name: String? = null,
    // For an existing user: their saved profile, to personalize the home feed on
    // any device. Absent for new users (they set these during registration).
    val statuses: List<String>? = null,
    val hobbies: List<String>? = null
)
