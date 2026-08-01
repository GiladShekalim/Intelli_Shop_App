package com.example.intellishopapp.model.dto

data class LoginResponse(
    val status: String,
    val redirect: String?,
    val user_id: String,
    // The signed-in user's saved profile, used to personalize the home feed on any
    // device. Null on older responses.
    val statuses: List<String>? = null,
    val hobbies: List<String>? = null,
    val memberships: List<String>? = null
)
