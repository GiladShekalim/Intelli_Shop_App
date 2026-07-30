package com.example.intellishopapp.model.dto

/** Response of POST /share_coupon/. `status` carries "self"/"not_found"/"bad_coupon" on errors. */
data class ShareResponse(
    val status: String? = null,
    val error: String? = null
)
