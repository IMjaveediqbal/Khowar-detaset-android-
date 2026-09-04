package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.community.CommunityScreen
import com.example.community.CommunityUiState
import com.example.ui.components.AppHeader
import com.example.ui.components.AppNavigationBar
import com.example.ui.screens.*
import com.example.ui.theme.KhowarDatasetTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.KhowarViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: KhowarViewModel = viewModel()
            val isDark by viewModel.isDarkTheme.collectAsState()
            val currentScreen by viewModel.currentScreen.collectAsState()
            val currentLanguage by viewModel.currentLanguage.collectAsState()
            val statusMessage by viewModel.statusMessage.collectAsState()
            val communityOpen by CommunityUiState.open.collectAsState()

            val lexiconQueue by viewModel.lexiconQueue.collectAsState()
            val sentenceQueue by viewModel.sentenceQueue.collectAsState()
            val speechQueue by viewModel.speechQueue.collectAsState()
            val storyQueue by viewModel.storyQueue.collectAsState()
            val totalQueueCount = lexiconQueue.size + sentenceQueue.size + speechQueue.size + storyQueue.size

            val snackbarHostState = remember { SnackbarHostState() }
            val coroutineScope = rememberCoroutineScope()

            LaunchedEffect(statusMessage) {
                statusMessage?.let { msg ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearStatusMessage()
                    }
                }
            }

            KhowarDatasetTheme(darkTheme = isDark) {
                Box(Modifier.fillMaxSize()) {
                    Scaffold(
                        topBar = { AppHeader(viewModel = viewModel) },
                        bottomBar = {
                            AppNavigationBar(
                                currentScreen = currentScreen,
                                onNavigate = { viewModel.navigateTo(it) },
                                lang = currentLanguage,
                                reviewQueueCount = totalQueueCount
                            )
                        },
                        floatingActionButton = {
                            ExtendedFloatingActionButton(
                                onClick = { CommunityUiState.show() },
                                icon = { Icon(Icons.Default.Groups, contentDescription = null) },
                                text = { Text("Community") }
                            )
                        },
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        Crossfade(
                            targetState = currentScreen,
                            label = "ScreenTransition",
                            modifier = Modifier.padding(innerPadding)
                        ) { screen ->
                            when (screen) {
                                AppScreen.HOME -> HomeScreen(viewModel = viewModel)
                                AppScreen.EXPLORE -> ExploreScreen(viewModel = viewModel)
                                AppScreen.CONTRIBUTE -> ContributorHubScreen(viewModel = viewModel)
                                AppScreen.VALIDATE -> ValidateScreen(viewModel = viewModel)
                                AppScreen.STATS -> StatsScreen(viewModel = viewModel)
                                AppScreen.RESEARCH -> ResearcherScreen(viewModel = viewModel)
                                AppScreen.ADMIN -> AdminScreen(viewModel = viewModel)
                                AppScreen.DOCS -> DocsScreen(viewModel = viewModel)
                                AppScreen.PROFILE -> ProfileScreen(viewModel = viewModel)
                            }
                        }
                    }

                    if (communityOpen) {
                        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                            CommunityScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
