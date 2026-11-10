package com.example.test.data.auth

import android.content.Context
import com.example.test.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the Better Auth session cookies between app launches.
 *
 * Neon Auth (Managed Better Auth) authenticates with an HTTP-only session
 * cookie; bearer tokens are not enabled on the managed server, so the cookie
 * is the credential that must survive restarts.
 */
@Singleton
class AuthCookieJar @Inject constructor(
    @ApplicationContext context: Context
) : CookieJar {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val baseUrl: HttpUrl? = BuildConfig.NEON_AUTH_BASE_URL.toHttpUrlOrNull()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return

        synchronized(this) {
            val merged = readAll().associateBy { it.name }.toMutableMap()
            cookies.forEach { merged[it.name] = it }
            writeAll(merged.values)
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        return synchronized(this) {
            readAll().filter { it.expiresAt > now && it.matches(url) }
        }
    }

    fun clear() {
        synchronized(this) { prefs.edit().remove(KEY_COOKIES).apply() }
    }

    fun hasSessionCookies(): Boolean = synchronized(this) { readAll().isNotEmpty() }

    private fun readAll(): List<Cookie> {
        val url = baseUrl ?: return emptyList()
        val raw = prefs.getString(KEY_COOKIES, null) ?: return emptyList()

        return raw.split('\n')
            .filter { it.isNotBlank() }
            .mapNotNull { Cookie.parse(url, it) }
    }

    private fun writeAll(cookies: Collection<Cookie>) {
        val serialized = cookies.joinToString("\n") { it.toString() }
        prefs.edit().putString(KEY_COOKIES, serialized).apply()
    }

    private companion object {
        const val PREFS_NAME = "auth_session"
        const val KEY_COOKIES = "cookies"
    }
}
