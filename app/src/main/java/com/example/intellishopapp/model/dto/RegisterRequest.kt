package com.example.intellishopapp.model.dto

data class RegisterRequest(
    val username: String,
    val password: String,
    val email: String,
    val status: List<String>,
    val age: Int,
    val location: String,
    val hobbies: List<String>
)
