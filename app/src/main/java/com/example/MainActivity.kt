package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

@Composable
private fun RbacDeniedScreen(title: String, message: String) {
    Box(Modifier.fillMaxSize()) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
                Text(message, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

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
            val currentUser by viewModel.currentUser.collectAsState()
            val communityOpen by CommunityUiState.open.collectAsState()

            val lexiconQueue by viewModel.lexiconQueue.collectAsState()
            val sentenceQueue by viewModel.sentenceQueue.collectAsState()
            val speechQueue by viewModel.speechQueue.collectAsState()
            val storyQueue by viewModel.storyQueue.collectAsState()
            val totalQueueCount = lexiconQueue.size + sentenceQueue.size + speechQueue.size + storyQueue.size
            val role = currentUser?.role
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

            fun navigateWithRbac(target: AppScreen) {
                val allowed = when (target) {
                    AppScreen.VALIDATE -> RbacPolicy.can(role, RbacPermission.VALIDATE_COMMUNITY)
                    AppScreen.RESEARCH -> RbacPolicy.can(role, RbacPermission.ACCESS_RESEARCH_HUB)
                    AppScreen.ADMIN -> RbacPolicy.can(role, RbacPermission.MANAGE_USERS)
                    else -> true
                }
                if (allowed) viewModel.navigateTo(target)
                else coroutineScope.launch { snackbarHostState.showSnackbar("Your role does not have access to this workspace.") }
            }

            KhowarDatasetTheme(darkTheme = isDark) {
                Box(Modifier.fillMaxSize()) {
                    Scaffold(
                        topBar = { AppHeader(viewModel = viewModel) },
                        bottomBar = {
                            AppNavigationBar(currentScreen = currentScreen, onNavigate = ::navigateWithRbac, lang = currentLanguage, reviewQueueCount = totalQueueCount)
                        },
                        floatingActionButton = {
                            ExtendedFloatingActionButton(onClick = { CommunityUiState.show() }, icon = { Icon(Icons.Default.Groups, contentDescription = null) }, text = { Text("Community") })
                        },
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        Crossfade(targetState = currentScreen, label = "ScreenTransition", modifier = Modifier.padding(innerPadding)) { screen ->
                            when (screen) {
                                AppScreen.HOME -> HomeScreen(viewModel = viewModel)
                                AppScreen.EXPLORE -> ExploreScreen(viewModel = viewModel)
                                AppScreen.CONTRIBUTE -> ContributorHubScreen(viewModel = viewModel)
                                AppScreen.VALIDATE -> if (RbacPolicy.can(role, RbacPermission.VALIDATE_COMMUNITY)) ValidatorWorkflowScreen(viewModel = viewModel) else RbacDeniedScreen("Validation restricted", "A Validator, Expert, Admin or Super Admin role is required.")
                                AppScreen.STATS -> StatsScreen(viewModel = viewModel)
                                AppScreen.RESEARCH -> if (RbacPolicy.can(role, RbacPermission.ACCESS_RESEARCH_HUB)) ResearcherScreen(viewModel = viewModel) else RbacDeniedScreen("Research workspace restricted", "Researcher, Expert, Admin or Super Admin access is required.")
                                AppScreen.ADMIN -> if (RbacPolicy.can(role, RbacPermission.MANAGE_USERS)) AdminScreen(viewModel = viewModel) else RbacDeniedScreen("Administration restricted", "Administrator privileges are required.")
                                AppScreen.DOCS -> DocsScreen(viewModel = viewModel)
                                AppScreen.PROFILE -> ProfileScreen(viewModel = viewModel)
                            }
                        }
                    }
                    if (communityOpen) {
                        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { CommunityScreen(viewModel = viewModel) }
                    }
                }
            }
        }
    }
}
