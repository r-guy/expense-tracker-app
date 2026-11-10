package com.example.test.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userDataStore by preferencesDataStore(name = "user_prefs")

class UserDataStore(private val context: Context) {
    companion object {
        private val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_USER_ID = intPreferencesKey("user_id")
    }

    val authTokenFlow: Flow<String?> = context.userDataStore.data
        .map { prefs -> prefs[KEY_AUTH_TOKEN] }

    /** Local row id of the signed-in user, used to scope all financial data. */
    val userIdFlow: Flow<Int?> = context.userDataStore.data
        .map { prefs -> prefs[KEY_USER_ID] }

    /** Writes the token and the user id together, so they can never disagree. */
    suspend fun saveSession(token: String, userId: Int) {
        context.userDataStore.edit { prefs ->
            prefs[KEY_AUTH_TOKEN] = token
            prefs[KEY_USER_ID] = userId
        }
    }

    suspend fun clearSession() {
        context.userDataStore.edit { prefs ->
            prefs.remove(KEY_AUTH_TOKEN)
            prefs.remove(KEY_USER_ID)
        }
    }
}
