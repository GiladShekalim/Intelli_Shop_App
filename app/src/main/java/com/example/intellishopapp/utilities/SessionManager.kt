package com.example.intellishopapp.utilities

import android.content.Context
import android.content.SharedPreferences
import com.example.intellishopapp.model.UserSession
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

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

    /** Replace the local favorite mirror with the authoritative set from the backend. */
    fun setFavorites(ids: List<String>) {
        get()?.let { save(it.copy(knownFavoriteIds = ids.toSet())) }
    }

    fun removeFavorite(discountId: String) {
        get()?.let { save(it.copy(knownFavoriteIds = it.knownFavoriteIds - discountId)) }
    }

    // --- app theme (Day/Night), app-wide, not tied to a user ---

    fun isNightMode(): Boolean = prefs.getBoolean(Constants.Prefs.NIGHT_MODE, false)

    fun setNightMode(on: Boolean) {
        prefs.edit().putBoolean(Constants.Prefs.NIGHT_MODE, on).apply()
    }

    // --- in-app notifications (the sliding banner), on by default ---

    fun isNotificationsEnabled(): Boolean =
        prefs.getBoolean(Constants.Prefs.NOTIFICATIONS, true)

    fun setNotificationsEnabled(on: Boolean) {
        prefs.edit().putBoolean(Constants.Prefs.NOTIFICATIONS, on).apply()
    }

    // --- Google account photo, kept per email so it survives sign-out on this device ---

    private fun photoKey(email: String) = "photo_" + email.lowercase()

    fun savePhotoUrl(email: String, url: String?) {
        if (email.isBlank() || url.isNullOrBlank()) return
        prefs.edit().putString(photoKey(email), url).apply()
    }

    fun getPhotoUrl(email: String): String? =
        if (email.isBlank()) null else prefs.getString(photoKey(email), null)

    // --- per-user preferences (statuses + categories), stored locally (no backend) ---
    // Keyed by email so a user's registration choices survive logout on this device.

    // memberships defaults to empty so prefs stored before this field round-trip cleanly.
    private data class StoredPrefs(
        val statuses: List<String>,
        val hobbies: List<String>,
        val memberships: List<String> = emptyList()
    )

    /** A user's three selection dimensions, as loaded from the local store. */
    data class Prefs(
        val statuses: List<String>,
        val hobbies: List<String>,
        val memberships: List<String>
    )

    private fun prefsKey(email: String) = "userprefs_" + email.lowercase()

    fun savePreferences(
        email: String, statuses: List<String>, hobbies: List<String>,
        memberships: List<String> = emptyList()
    ) {
        if (email.isBlank()) return
        prefs.edit()
            .putString(prefsKey(email), gson.toJson(StoredPrefs(statuses, hobbies, memberships)))
            .apply()
    }

    fun loadPreferences(email: String): Prefs? {
        val json = prefs.getString(prefsKey(email), null) ?: return null
        val stored = runCatching { gson.fromJson(json, StoredPrefs::class.java) }.getOrNull() ?: return null
        return Prefs(stored.statuses, stored.hobbies, stored.memberships ?: emptyList())
    }

    /** The clubs the signed-in user holds; empty means "no membership filter". */
    fun memberships(): List<String> = get()?.memberships ?: emptyList()

    /** Update the live session AND the persistent per-user store together. */
    fun updatePreferences(
        statuses: List<String>, hobbies: List<String>, memberships: List<String>
    ) {
        get()?.let { session ->
            save(session.copy(status = statuses, hobbies = hobbies, memberships = memberships))
            savePreferences(session.email, statuses, hobbies, memberships)
        }
    }

    // --- coupon action history (copy / go to site / go to offer), per-user, local ---

    private fun historyKey(email: String) = "history_" + email.lowercase()

    /** Record that the user acted on a coupon; most-recent first, de-duplicated. */
    fun addHistory(discountId: String) {
        val email = get()?.email ?: return
        if (discountId.isBlank()) return
        val list = getHistory().toMutableList()
        list.remove(discountId)
        list.add(0, discountId)
        prefs.edit().putString(historyKey(email), gson.toJson(list.take(100))).apply()
    }

    fun getHistory(): List<String> {
        val email = get()?.email ?: return emptyList()
        val json = prefs.getString(historyKey(email), null) ?: return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return runCatching { gson.fromJson<List<String>>(json, type) }.getOrNull() ?: emptyList()
    }

    /** Replace the local history mirror with the authoritative list from the backend. */
    fun setHistory(ids: List<String>) {
        val email = get()?.email ?: return
        prefs.edit().putString(historyKey(email), gson.toJson(ids.take(100))).apply()
    }

    // --- redeemed coupons (copy / site / offer), per-user mirror of /redeemed/ ---

    private fun redeemedKey(email: String) = "redeemed_" + email.lowercase()

    fun addRedeemed(discountId: String) {
        val email = get()?.email ?: return
        if (discountId.isBlank()) return
        val list = getRedeemed().toMutableList()
        list.remove(discountId)
        list.add(0, discountId)
        prefs.edit().putString(redeemedKey(email), gson.toJson(list.take(100))).apply()
    }

    fun getRedeemed(): List<String> {
        val email = get()?.email ?: return emptyList()
        val json = prefs.getString(redeemedKey(email), null) ?: return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return runCatching { gson.fromJson<List<String>>(json, type) }.getOrNull() ?: emptyList()
    }

    fun setRedeemed(ids: List<String>) {
        val email = get()?.email ?: return
        prefs.edit().putString(redeemedKey(email), gson.toJson(ids.take(100))).apply()
    }

    // --- coupons shared to this user, per-user mirror of /received_shares/ ---

    private fun sharesKey(email: String) = "shares_" + email.lowercase()

    fun setReceivedShares(shares: List<com.example.intellishopapp.model.dto.SharedItemDto>) {
        val email = get()?.email ?: return
        prefs.edit().putString(sharesKey(email), gson.toJson(shares.take(200))).apply()
    }

    fun getReceivedShares(): List<com.example.intellishopapp.model.dto.SharedItemDto> {
        val email = get()?.email ?: return emptyList()
        val json = prefs.getString(sharesKey(email), null) ?: return emptyList()
        val type = object : TypeToken<List<com.example.intellishopapp.model.dto.SharedItemDto>>() {}.type
        return runCatching {
            gson.fromJson<List<com.example.intellishopapp.model.dto.SharedItemDto>>(json, type)
        }.getOrNull() ?: emptyList()
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
