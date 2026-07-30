package com.example.intellishopapp.model.dto

/** Body of POST /remove_share/ — dismiss one offer a sender shared with this user. */
data class ShareRemoveRequest(
    val from_username: String,
    val discount_id: String
)
