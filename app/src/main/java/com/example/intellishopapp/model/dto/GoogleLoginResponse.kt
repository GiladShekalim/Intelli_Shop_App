package com.example.intellishopapp.model.dto

data class GoogleLoginResponse(
    val status: String,
    val is_new: Boolean,
    val user_id: String? = null,
    val username: String? = null,
    val email: String? = null,
    val name: String? = null
)
