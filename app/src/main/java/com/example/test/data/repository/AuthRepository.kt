package com.example.test.data.repository

import com.example.test.data.UserDataSeeder
import com.example.test.data.api.AuthUser
import com.example.test.data.api.NeonAuthApi
import com.example.test.data.dao.UserDao
import com.example.test.data.datastore.UserDataStore
import com.example.test.model.User
import javax.inject.Inject

/**
 * Real authentication against Neon Auth (Managed Better Auth).
 *
 * The auth server owns credentials and sessions; this repository only maps the
 * authenticated email to a local row id, which is what scopes the on-device
 * financial data (accounts, categories, transactions, budgets, notifications).
 */
class AuthRepository @Inject constructor(
    private val neonAuthApi: NeonAuthApi,
    private val dataStore: UserDataStore,
    private val userDao: UserDao,
    private val userDataSeeder: UserDataSeeder
) {
    suspend fun signIn(email: String, password: String): Result<Unit> =
        neonAuthApi.signIn(email, password).mapCatching { user ->
            startSession(user)
        }

    suspend fun signUp(username: String, email: String, password: String): Result<Unit> =
        neonAuthApi.signUp(username, email, password).mapCatching { user ->
            startSession(user)
        }

    /**
     * Revokes the session on the auth server and clears it locally.
     * Local data is kept so signing back in restores it.
     */
    suspend fun signOut() {
        runCatching { neonAuthApi.signOut() }
        dataStore.clearSession()
    }

    /** Drops the local session without calling the server (used for stale sessions). */
    suspend fun clearLocalSession() {
        dataStore.clearSession()
    }

    private suspend fun startSession(user: AuthUser) {
        val localUserId = localUserIdFor(user)
        // The token value is the auth-server session token; it is stored so the
        // app can tell "signed in" apart from "signed out" across restarts.
        dataStore.saveSession(token = user.id, userId = localUserId)
        userDataSeeder.prepareUser(localUserId)
    }

    /**
     * Maps an authenticated email to a local user row. The row id is what all
     * financial data is scoped by, so the same email always gets the same data.
     * Passwords are never stored locally: authentication happens in Neon.
     */
    private suspend fun localUserIdFor(user: AuthUser): Int {
        userDao.getUserByEmail(user.email)?.let { existing ->
            // Accounts created before real auth stored a plaintext password.
            // Neon owns credentials now, so clear any leftover local copy.
            if (existing.password.isNotEmpty()) {
                userDao.insert(existing.copy(password = ""))
            }
            return existing.id
        }

        val newId = userDao.insert(
            User(
                username = user.name ?: user.email.substringBefore('@'),
                email = user.email,
                password = ""
            )
        )
        return newId.toInt()
    }
}
