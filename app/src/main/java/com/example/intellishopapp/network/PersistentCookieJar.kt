package com.example.intellishopapp.network

import android.content.SharedPreferences
import com.example.intellishopapp.utilities.Constants
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * CookieJar that keeps cookies (sessionid, csrftoken) across requests and app restarts.
 * In-memory cache mirrored to SharedPreferences as JSON.
 */
class PersistentCookieJar(private val prefs: SharedPreferences) : CookieJar {

    private val gson = Gson()
    private val cache: MutableMap<String, MutableList<Cookie>> = mutableMapOf()

    init {
        load()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val stored = cache.getOrPut(url.host) { mutableListOf() }
        for (cookie in cookies) {
            stored.removeAll { it.name == cookie.name }
            stored.add(cookie)
        }
        persist()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        val stored = cache[url.host] ?: return emptyList()
        stored.removeAll { it.expiresAt < now }
        return stored.filter { it.matches(url) }
    }

    /** Drops all cookies (session + csrf) — used on sign-out and before a fresh login. */
    fun clear() {
        cache.clear()
        prefs.edit().remove(Constants.Prefs.COOKIES).apply()
    }

    private fun persist() {
        val serializable = cache.mapValues { entry -> entry.value.map { it.toSerializable() } }
        prefs.edit().putString(Constants.Prefs.COOKIES, gson.toJson(serializable)).apply()
    }

    private fun load() {
        val json = prefs.getString(Constants.Prefs.COOKIES, null) ?: return
        val type = object : TypeToken<Map<String, List<SerializableCookie>>>() {}.type
        val stored: Map<String, List<SerializableCookie>> = gson.fromJson(json, type) ?: return
        for ((host, list) in stored) {
            cache[host] = list.map { it.toCookie() }.toMutableList()
        }
    }

    private fun Cookie.toSerializable() = SerializableCookie(
        name = name,
        value = value,
        domain = domain,
        path = path,
        expiresAt = expiresAt,
        secure = secure,
        httpOnly = httpOnly
    )

    private fun SerializableCookie.toCookie(): Cookie {
        val builder = Cookie.Builder()
            .name(name)
            .value(value)
            .domain(domain)
            .path(path)
            .expiresAt(expiresAt)
        if (secure) builder.secure()
        if (httpOnly) builder.httpOnly()
        return builder.build()
    }
}
