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

    // --- favorites (local-first mirror of the backend, like the old web client) ---

    fun favoriteIds(): Set<String> = get()?.knownFavoriteIds ?: emptySet()

    fun isFavorite(discountId: String): Boolean = favoriteIds().contains(discountId)

    fun addFavorite(discountId: String) {
        get()?.let { save(it.copy(knownFavoriteIds = it.knownFavoriteIds + discountId)) }
    }

    fun removeFavorite(discountId: String) {
        get()?.let { save(it.copy(knownFavoriteIds = it.knownFavoriteIds - discountId)) }
    }

    // --- app theme (Day/Night), app-wide, not tied to a user ---

    fun isNightMode(): Boolean = prefs.getBoolean(Constants.Prefs.NIGHT_MODE, false)

    fun setNightMode(on: Boolean) {
        prefs.edit().putBoolean(Constants.Prefs.NIGHT_MODE, on).apply()
    }

    // --- per-user preferences (statuses + categories), stored locally (no backend) ---
    // Keyed by email so a user's registration choices survive logout on this device.

    private data class StoredPrefs(val statuses: List<String>, val hobbies: List<String>)

    private fun prefsKey(email: String) = "userprefs_" + email.lowercase()

    fun savePreferences(email: String, statuses: List<String>, hobbies: List<String>) {
        if (email.isBlank()) return
        prefs.edit().putString(prefsKey(email), gson.toJson(StoredPrefs(statuses, hobbies))).apply()
    }

    fun loadPreferences(email: String): Pair<List<String>, List<String>>? {
        val json = prefs.getString(prefsKey(email), null) ?: return null
        val stored = runCatching { gson.fromJson(json, StoredPrefs::class.java) }.getOrNull() ?: return null
        return stored.statuses to stored.hobbies
    }

    /** Update the live session AND the persistent per-user store together. */
    fun updatePreferences(statuses: List<String>, hobbies: List<String>) {
        get()?.let { session ->
            save(session.copy(status = statuses, hobbies = hobbies))
            savePreferences(session.email, statuses, hobbies)
        }
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
