package com.example.test.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test.data.api.NeonAuthApi
import com.example.test.data.datastore.UserDataStore
import com.example.test.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/** Whether a session is stored on this device. */
enum class AuthState { Loading, LoggedIn, LoggedOut }

/**
 * Owns the persisted login state.
 *
 * The auth cookie and local user id live on disk, so the session survives app
 * restarts. On startup the stored session is revalidated with Neon Auth before
 * a start screen is chosen: if the server says the session is gone, the app goes
 * to Sign In. If the server cannot be reached, the stored session is trusted.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val dataStore: UserDataStore,
    private val neonAuthApi: NeonAuthApi,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState

    init {
        resolveInitialState()
    }

    private fun resolveInitialState() {
        viewModelScope.launch {
            val token = dataStore.authTokenFlow.first()
            if (token.isNullOrBlank()) {
                _authState.value = AuthState.LoggedOut
                return@launch
            }

            // Bound the check so a slow network never blocks the splash screen.
            val result = withTimeoutOrNull(SESSION_CHECK_TIMEOUT_MS) {
                neonAuthApi.getSession()
            }

            _authState.value = when {
                // Definitive answer from the server: the session is gone.
                result != null && result.isSuccess && result.getOrNull() == null -> {
                    authRepository.clearLocalSession()
                    AuthState.LoggedOut
                }
                // Offline or timeout: keep the stored session.
                else -> AuthState.LoggedIn
            }
        }
    }

    private companion object {
        const val SESSION_CHECK_TIMEOUT_MS = 6_000L
    }
}
