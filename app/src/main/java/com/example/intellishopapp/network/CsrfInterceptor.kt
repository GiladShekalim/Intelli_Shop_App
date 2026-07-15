package com.example.intellishopapp.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds the X-CSRFToken header (read from the csrftoken cookie) to POSTs that the
 * server protects with CSRF: login, register, profile. Other requests pass through.
 */
class CsrfInterceptor(private val cookieJar: PersistentCookieJar) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val needsCsrf = request.method == "POST" &&
            CSRF_PATHS.any { request.url.encodedPath.endsWith(it) }
        if (!needsCsrf) return chain.proceed(request)

        val token = cookieJar.loadForRequest(request.url)
            .firstOrNull { it.name == "csrftoken" }
            ?.value

        val outgoing = if (token != null) {
            request.newBuilder().addHeader("X-CSRFToken", token).build()
        } else {
            request
        }
        return chain.proceed(outgoing)
    }

    companion object {
        private val CSRF_PATHS = listOf("/login/", "/register/", "/profile/")
    }
}
