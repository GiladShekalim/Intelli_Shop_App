package com.example.intellishopapp.network

/**
 * Plain, Gson-friendly representation of an OkHttp Cookie for persistence
 * (okhttp3.Cookie is not directly serializable).
 */
data class SerializableCookie(
    val name: String,
    val value: String,
    val domain: String,
    val path: String,
    val expiresAt: Long,
    val secure: Boolean,
    val httpOnly: Boolean
)
