package com.jnetaol.securemessenger

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jnetaol.securemessenger.logger.DebugLogger
import com.jnetaol.securemessenger.ui.screens.*
import com.jnetaol.securemessenger.ui.theme.SecureMessengerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                enableEdgeToEdge()
            }
        } catch (e: Exception) {
            DebugLogger.e("MainActivity", "onCreate", "SM-MA-ERR-001", "EdgeToEdge failed", e)
        }
        DebugLogger.d("MainActivity", "onCreate", "SM-MA-001")
        try {
            setContent {
                SecureMessengerTheme {
                    val mainViewModel: MainViewModel = viewModel()
                    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
                    val snackbarHostState = remember { SnackbarHostState() }

                    LaunchedEffect(Unit) {
                        try {
                            mainViewModel.toastMessage.collect { message ->
                                snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
                            }
                        } catch (e: Exception) {
                            DebugLogger.e("MainActivity", "toastCollect", "SM-MA-ERR-002", "Toast collect failed", e)
                        }
                    }

                    Scaffold(
                        snackbarHost = {
                            SnackbarHost(snackbarHostState) { data ->
                                Snackbar(
                                    snackbarData = data,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.background
                    ) { padding ->
                        Box(Modifier.padding(padding)) {
                            var screenError by remember { mutableStateOf<String?>(null) }
                            if (screenError != null) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            "Error [SM-MA-ERR-003]",
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            screenError ?: "Unknown error",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 14.sp
                                        )
                                        Spacer(Modifier.height(16.dp))
                                        Button(onClick = {
                                            screenError = null
                                            currentScreen = Screen.Home
                                        }) {
                                            Text("Go Home")
                                        }
                                    }
                                }
                            } else {
                                when (currentScreen) {
                                    Screen.Home -> HomeScreen(
                                        viewModel = mainViewModel,
                                        onNavigateToChat = { contactId ->
                                            mainViewModel.selectContact(contactId)
                                            currentScreen = Screen.Chat
                                        },
                                        onNavigateToPair = { currentScreen = Screen.Pair },
                                        onNavigateToSettings = { currentScreen = Screen.Settings },
                                        onNavigateToAbout = { currentScreen = Screen.About }
                                    )
                                    Screen.Pair -> PairScreen(
                                        viewModel = mainViewModel,
                                        onBack = { currentScreen = Screen.Home }
                                    )
                                    Screen.Chat -> ChatScreen(
                                        viewModel = mainViewModel,
                                        onBack = { currentScreen = Screen.Home }
                                    )
                                    Screen.Settings -> SettingsScreen(
                                        viewModel = mainViewModel,
                                        onBack = { currentScreen = Screen.Home }
                                    )
                                    Screen.About -> AboutScreen(
                                        viewModel = mainViewModel,
                                        onBack = { currentScreen = Screen.Home }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            DebugLogger.e("MainActivity", "onCreate", "SM-MA-ERR-004", "setContent failed", e)
            setContent {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "App Error [SM-MA-ERR-004]\n${e.message}",
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        DebugLogger.d("MainActivity", "onDestroy", "SM-MA-002")
        super.onDestroy()
    }

    private enum class Screen { Home, Pair, Chat, Settings, About }
}
