package com.example.test.data.api

import android.util.Log
import com.example.test.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrencyExchangeService @Inject constructor() {

    private companion object {
        const val TAG = "CurrencyExchangeService"
        const val CACHE_VALIDITY_MS = 60 * 60 * 1000L // 1 hour
        const val RETRY_BACKOFF_MS = 60 * 1000L // 1 minute after a failed fetch
        const val USD_BASE = "USD"
    }

    // Values come from the root .env file via app/build.gradle.kts
    private val directApiKey = BuildConfig.EXCHANGE_RATE_API_KEY
    private val directApiBaseUrl = BuildConfig.EXCHANGE_RATE_API_BASE.trimEnd('/')

    // One or more backend URLs, tried in order. Supports comma-separated values so
    // the same build works over USB (adb reverse -> 127.0.0.1) and over Wi-Fi.
    private val backendBaseUrls: List<String> = BuildConfig.CURRENCY_BACKEND_BASE_URL
        .split(",")
        .map { it.trim().trimEnd('/') }
        .filter { it.isNotEmpty() }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var currencyRates: Map<String, Double> = emptyMap()

    @Volatile
    private var lastFetchTime: Long = 0L

    @Volatile
    private var lastAttemptTime: Long = 0L

    @Volatile
    private var lastUpdated: String = "Not available"

    // Used to prefetch rates in the background at app start, so screens that
    // format dual-currency amounts never wait on the network.
    private val warmUpScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Used only when both the backend and the direct API are unreachable.
    // Values are units of each currency per 1 USD.
    private val fallbackRates = mapOf(
        "USD" to 1.0,
        "INR" to 88.0,
        "IRR" to 8285000.0,
        "EUR" to 0.92,
        "GBP" to 0.78,
        "JPY" to 150.0,
        "AUD" to 1.5
    )

    /**
     * Prefetches rates in the background so later calls return cached values
     * immediately instead of blocking the UI thread.
     */
    fun warmUp() {
        warmUpScope.launch {
            if (shouldRefreshRates()) {
                fetchExchangeRates()
            }
        }
    }

    suspend fun getExchangeRate(from: String, to: String): Double {
        if (from == to) return 1.0

        if (shouldRefreshRates()) {
            fetchExchangeRates()
        }

        val fromRate = currencyRates[from.uppercase()]
        val toRate = currencyRates[to.uppercase()]

        if (fromRate != null && toRate != null && fromRate > 0.0 && toRate > 0.0) {
            return toRate / fromRate
        }

        return fallbackRate(from.uppercase(), to.uppercase())
    }

    suspend fun convertCurrency(amount: Double, from: String, to: String): Double {
        return amount * getExchangeRate(from, to)
    }

    private fun shouldRefreshRates(): Boolean {
        val now = System.currentTimeMillis()
        val cacheIsFresh = currencyRates.isNotEmpty() &&
            (now - lastFetchTime) <= CACHE_VALIDITY_MS
        if (cacheIsFresh) return false

        // If the last attempt failed (offline / API down), don't retry on every
        // render; wait out the backoff so the UI is not blocked repeatedly.
        return (now - lastAttemptTime) > RETRY_BACKOFF_MS
    }

    private suspend fun fetchExchangeRates() {
        lastAttemptTime = System.currentTimeMillis()
        withContext(Dispatchers.IO) {
            val parsedRates = runCatching { fetchFromBackend() }
                .recoverCatching { backendError ->
                    Log.w(TAG, "Backend unavailable (${backendError.message}), trying direct API")
                    fetchFromDirectApi()
                }
                .getOrNull()

            if (parsedRates == null) {
                Log.w(TAG, "Unable to fetch exchange rates; keeping previous/fallback rates")
                return@withContext
            }

            currencyRates = parsedRates.rates
            lastUpdated = parsedRates.lastUpdated
            lastFetchTime = System.currentTimeMillis()
        }
    }

    /**
     * Calls the local backend. The backend keeps the real API key server-side
     * and caches responses.
     */
    private fun fetchFromBackend(): ParsedRates {
        check(backendBaseUrls.isNotEmpty()) { "No backend URL configured" }

        var lastError: Throwable? = null
        backendBaseUrls.forEach { baseUrl ->
            try {
                return parseRates(execute("$baseUrl/api/rates/latest?base=$USD_BASE"))
            } catch (e: Exception) {
                lastError = e
                Log.w(TAG, "Backend $baseUrl unavailable (${e.message})")
            }
        }

        throw lastError ?: IllegalStateException("No backend URL configured")
    }

    /**
     * Fallback: calls ExchangeRate-API directly using the .env key.
     * Useful when the Python backend is not running.
     */
    private fun fetchFromDirectApi(): ParsedRates {
        check(directApiKey.isNotBlank()) { "EXCHANGE_RATE_API_KEY is missing in .env" }
        val url = "$directApiBaseUrl/$directApiKey/latest/$USD_BASE"
        return parseRates(execute(url))
    }

    private fun execute(url: String): String {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code} from $url")
            }
            return response.body?.string()
                ?: throw IllegalStateException("Empty response from $url")
        }
    }

    private fun parseRates(json: String): ParsedRates {
        val root = JSONObject(json)

        val result = root.optString("result", "success")
        if (result != "success") {
            throw IllegalStateException(
                root.optString("error-type", "Exchange rate API returned an error")
            )
        }

        // ExchangeRate-API uses "conversion_rates"; the backend mirrors that shape.
        val ratesObject = root.optJSONObject("conversion_rates")
            ?: root.optJSONObject("rates")
            ?: throw IllegalStateException("Missing conversion_rates in response")

        val rates = buildMap {
            ratesObject.keys().forEach { code ->
                val value = ratesObject.optDouble(code, Double.NaN)
                if (!value.isNaN() && value > 0.0) {
                    put(code.uppercase(), value)
                }
            }
        }

        if (rates.isEmpty()) {
            throw IllegalStateException("Exchange rate response contained no rates")
        }

        val updated = root.optString("time_last_update_utc")
            .ifBlank { root.optString("time_last_update") }
            .ifBlank { "Not available" }

        return ParsedRates(rates, updated)
    }

    private fun fallbackRate(from: String, to: String): Double {
        val fromRate = fallbackRates[from] ?: return 1.0
        val toRate = fallbackRates[to] ?: return 1.0
        return toRate / fromRate
    }

    fun getLatestUpdateTime(): String = lastUpdated

    private data class ParsedRates(
        val rates: Map<String, Double>,
        val lastUpdated: String
    )
}
