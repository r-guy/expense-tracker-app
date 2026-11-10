package com.example.test.data

import com.example.test.data.datastore.UserDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the local id of the signed-in user for the whole app.
 *
 * Every per-user query and insert is scoped by [userId]. While signed out the
 * value is [NO_USER] (0), which matches no real row.
 */
@Singleton
class UserSession @Inject constructor(
    dataStore: UserDataStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val userId: StateFlow<Int> = dataStore.userIdFlow
        .map { it ?: NO_USER }
        .distinctUntilChanged()
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = NO_USER
        )

    /** Suspends until the stored user id is available, then returns it. */
    suspend fun requireUserId(): Int = userId.filter { it != NO_USER }.first()

    /** Current id, or null when signed out / not loaded yet. */
    fun currentUserIdOrNull(): Int? = userId.value.takeIf { it != NO_USER }

    companion object {
        const val NO_USER = 0
    }
}
