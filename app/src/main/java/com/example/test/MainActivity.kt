package com.example.test

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.compose.AppTheme
import com.example.test.navigation.NavGraph
import com.example.test.navigation.Screen
import com.example.test.viewmodel.AuthState
import com.example.test.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                val authState by authViewModel.authState.collectAsStateWithLifecycle()

                // The start destination is resolved once from the persisted token.
                // Later login/logout changes are handled by navigation, so the nav
                // graph is never rebuilt mid-session.
                var startDestination by rememberSaveable { mutableStateOf<String?>(null) }

                LaunchedEffect(authState) {
                    if (startDestination == null) {
                        when (authState) {
                            AuthState.LoggedIn -> startDestination = Screen.Dashboard.route
                            AuthState.LoggedOut -> startDestination = Screen.SignIn.route
                            AuthState.Loading -> Unit
                        }
                    }
                }

                val destination = startDestination
                if (destination == null) {
                    AuthLoadingScreen()
                } else {
                    NavGraph(startDestination = destination)
                }
            }
        }
    }
}

@Composable
private fun AuthLoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
