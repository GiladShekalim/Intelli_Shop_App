package com.example.intellishopapp.model.dto

/** Response of GET /check_username/ — whether the typed username is free to register. */
data class UsernameCheckResponse(
    val available: Boolean = false
)
