package com.example.intellishopapp.model

/**
 * The logged-in user's state. Fully populated at registration; after a bare login
 * only userId and email are known until a profile fetch fills the rest.
 */
data class UserSession(
    val userId: String,
    val email: String,
    val username: String? = null,
    val status: List<String> = emptyList(),
    val age: Int? = null,
    val location: String? = null,
    val hobbies: List<String> = emptyList(),
    val knownFavoriteIds: Set<String> = emptySet()
)
