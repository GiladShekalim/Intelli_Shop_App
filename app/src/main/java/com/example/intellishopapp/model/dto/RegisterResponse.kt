package com.example.intellishopapp.model.dto

data class RegisterResponse(
    val status: String,
    val message: String?,
    val user_id: String?
)
