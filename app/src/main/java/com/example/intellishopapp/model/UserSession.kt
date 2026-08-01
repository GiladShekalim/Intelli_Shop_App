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
    // Clubs the user holds (coupon.club_name keys, e.g. "hot"/"adif"); empty = no filter.
    val memberships: List<String> = emptyList(),
    val knownFavoriteIds: Set<String> = emptySet(),
    val isGoogle: Boolean = false
)
