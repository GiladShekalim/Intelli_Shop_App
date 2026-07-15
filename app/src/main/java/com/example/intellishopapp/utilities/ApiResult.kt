package com.example.intellishopapp.utilities

/**
 * Result wrapper returned by repository calls: either a value or a typed error.
 */
sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
}
