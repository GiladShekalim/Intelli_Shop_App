package com.example.intellishopapp.utilities

import android.content.Context
import android.content.SharedPreferences
import com.example.intellishopapp.model.UserSession
import com.google.gson.Gson

/**
 * Holds the current UserSession, mirrored to SharedPreferences so the login
 * survives app restarts. App-scoped singleton.
 */
class SessionManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(Constants.Prefs.FILE, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun save(session: UserSession) {
        prefs.edit().putString(Constants.Prefs.SESSION, gson.toJson(session)).apply()
    }

    fun get(): UserSession? {
        val json = prefs.getString(Constants.Prefs.SESSION, null) ?: return null
        return runCatching { gson.fromJson(json, UserSession::class.java) }.getOrNull()
    }

    fun isLoggedIn(): Boolean = get() != null

    fun clear() {
        prefs.edit().remove(Constants.Prefs.SESSION).apply()
    }

    companion object {
        @Volatile
        private var instance: SessionManager? = null

        fun init(context: Context): SessionManager =
            instance ?: synchronized(this) {
                instance ?: SessionManager(context.applicationContext).also { instance = it }
            }

        fun getInstance(): SessionManager =
            instance ?: throw IllegalStateException(
                "SessionManager not initialized. Call init(context) in App.onCreate()."
            )
    }
}
