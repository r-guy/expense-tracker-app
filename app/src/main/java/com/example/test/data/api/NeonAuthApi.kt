package com.example.test.data.api

import com.example.test.BuildConfig
import com.example.test.data.auth.AuthCookieJar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** A user as returned by Neon Auth. */
data class AuthUser(
    val id: String,
    val email: String,
    val name: String?,
    val emailVerified: Boolean
)

/** Thrown when the auth server rejects a request; the message is user-friendly. */
class AuthException(message: String) : Exception(message)

/**
 * Client for Neon Auth (Managed Better Auth) over its REST API.
 *
 * Session state is carried by the cookie jar; [signOut] calls the server so the
 * session is revoked, then drops the local cookies.
 */
@Singleton
class NeonAuthApi @Inject constructor(
    private val cookieJar: AuthCookieJar
) {
    private val baseUrl = BuildConfig.NEON_AUTH_BASE_URL.trimEnd('/')

    private val client = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun signUp(name: String, email: String, password: String): Result<AuthUser> =
        withContext(Dispatchers.IO) {
            runCatching {
                requireConfigured()
                val body = JSONObject().apply {
                    put("name", name)
                    put("email", email)
                    put("password", password)
                }
                parseUser(post("/sign-up/email", body))
            }
        }

    suspend fun signIn(email: String, password: String): Result<AuthUser> =
        withContext(Dispatchers.IO) {
            runCatching {
                requireConfigured()
                val body = JSONObject().apply {
                    put("email", email)
                    put("password", password)
                }
                parseUser(post("/sign-in/email", body))
            }
        }

    /**
     * Revokes the server session and clears the stored cookies.
     * Always clears locally, even if the network call fails.
     */
    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        val result = runCatching {
            requireConfigured()
            post("/sign-out", JSONObject())
        }
        cookieJar.clear()
        result.map { }
    }

    /**
     * Returns the current user, `null` when the server says the session is gone,
     * and a failure when the server could not be reached.
     */
    suspend fun getSession(): Result<AuthUser?> = withContext(Dispatchers.IO) {
        runCatching {
            requireConfigured()

            val request = Request.Builder()
                .url("$baseUrl/get-session")
                .get()
                .header("Origin", TRUSTED_ORIGIN)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 401) return@runCatching null

                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw AuthException(errorMessage(body))
                if (body.isBlank() || body == "null") return@runCatching null

                val user = JSONObject(body).optJSONObject("user")
                    ?: return@runCatching null
                user.toAuthUser()
            }
        }
    }

    private fun post(path: String, body: JSONObject): String {
        val request = Request.Builder()
            .url("$baseUrl$path")
            .post(body.toString().toRequestBody(JSON))
            .header("Origin", TRUSTED_ORIGIN)
            .build()

        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw AuthException(errorMessage(text))
            return text
        }
    }

    private fun parseUser(json: String): AuthUser {
        val user = JSONObject(json).optJSONObject("user")
            ?: throw AuthException("Unexpected response from the auth server")
        return user.toAuthUser()
    }

    private fun JSONObject.toAuthUser(): AuthUser = AuthUser(
        id = optString("id"),
        email = optString("email"),
        name = optString("name").ifBlank { null },
        emailVerified = optBoolean("emailVerified", false)
    )

    private fun errorMessage(body: String): String {
        val message = runCatching { JSONObject(body).optString("message") }.getOrNull()
        return when {
            !message.isNullOrBlank() -> message
            else -> "Authentication failed"
        }
    }

    private fun requireConfigured() {
        if (baseUrl.isBlank()) {
            throw AuthException("NEON_AUTH_BASE_URL is not configured in .env")
        }
    }

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()

        /**
         * Managed Auth requires an Origin header (CSRF guard). Native clients have
         * no browser origin, so they send this trusted localhost origin.
         */
        const val TRUSTED_ORIGIN = "http://localhost:3000"
    }
}
