package com.example.intellishopapp.network

import android.content.Context
import com.example.intellishopapp.utilities.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Single OkHttp/Retrofit stack for the app. Owns the persistent cookie jar so the
 * session survives restarts. Initialized once from App.onCreate().
 */
class RetrofitClient private constructor(context: Context) {

    private val cookieJar = PersistentCookieJar(
        context.getSharedPreferences(Constants.Prefs.FILE, Context.MODE_PRIVATE)
    )

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        // Real networks (and a busy dev server) are slow; the 10s defaults are too tight.
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(CsrfInterceptor(cookieJar))
        .addInterceptor(HttpLoggingInterceptor().apply {
            // BASIC (method/url/status/timing) instead of dumping large response bodies.
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    val apiService: ApiService = Retrofit.Builder()
        .baseUrl(Constants.Api.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)

    /** Clears the persisted session/csrf cookies (sign-out, or before a fresh login). */
    fun clearCookies() = cookieJar.clear()

    companion object {
        @Volatile
        private var instance: RetrofitClient? = null

        fun init(context: Context): RetrofitClient =
            instance ?: synchronized(this) {
                instance ?: RetrofitClient(context.applicationContext).also { instance = it }
            }

        fun getInstance(): RetrofitClient =
            instance ?: throw IllegalStateException(
                "RetrofitClient not initialized. Call init(context) in App.onCreate()."
            )
    }
}
