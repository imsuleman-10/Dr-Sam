package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AuthState
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel(
                factory = MainViewModelFactory(application)
            )

            val authState by viewModel.authState.collectAsState()
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            // State variable for Admin dashboard visibility
            var isAdminScreenActive by remember { mutableStateOf(false) }

            MyApplicationTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Crossfade(
                        targetState = authState,
                        label = "MainAuthCrossfade"
                    ) { state ->
                        when (state) {
                            is AuthState.Authenticated -> {
                                if (isAdminScreenActive && state.user.role == "admin") {
                                    AdminDashboardScreen(
                                        viewModel = viewModel,
                                        currentUser = state.user,
                                        onBack = { isAdminScreenActive = false }
                                    )
                                } else {
                                    MainScreen(
                                        viewModel = viewModel,
                                        user = state.user,
                                        onNavigateToAdmin = { isAdminScreenActive = true }
                                    )
                                }
                            }
                            is AuthState.Loading,
                            is AuthState.Unauthenticated,
                            is AuthState.Error -> {
                                // Clear administrative flag just in case
                                isAdminScreenActive = false
                                AuthScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
