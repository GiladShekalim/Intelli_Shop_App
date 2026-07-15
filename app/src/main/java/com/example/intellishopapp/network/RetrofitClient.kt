package com.example.intellishopapp.network

import android.content.Context
import com.example.intellishopapp.utilities.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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
        .addInterceptor(CsrfInterceptor(cookieJar))
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val apiService: ApiService = Retrofit.Builder()
        .baseUrl(Constants.Api.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)

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
