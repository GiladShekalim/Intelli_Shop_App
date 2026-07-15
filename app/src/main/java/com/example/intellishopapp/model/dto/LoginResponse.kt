package com.example.intellishopapp.model.dto

data class LoginResponse(
    val status: String,
    val redirect: String?,
    val user_id: String
)
