package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.data.database.UserEntity
import com.example.ui.viewmodel.MainViewModel

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    user: UserEntity,
    onNavigateToAdmin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Chats, 1: Profile, 2: Settings

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("main_bottom_nav_bar")
            ) {
                // Chats Tab Click
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Filled.Forum else Icons.Outlined.Forum,
                            contentDescription = "Chats Navigation Tab"
                        )
                    },
                    label = { Text("Chats") },
                    modifier = Modifier.testTag("nav_chats_tab")
                )

                // Profile Tab Click
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 1) Icons.Filled.AccountCircle else Icons.Outlined.AccountCircle,
                            contentDescription = "Profile Navigation Tab"
                        )
                    },
                    label = { Text("Profile") },
                    modifier = Modifier.testTag("nav_profile_tab")
                )

                // Settings Tab Click
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 2) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Settings Navigation Tab"
                        )
                    },
                    label = { Text("Settings") },
                    modifier = Modifier.testTag("nav_settings_tab")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> ChatsTab(
                    viewModel = viewModel,
                    user = user,
                    modifier = Modifier.fillMaxSize()
                )
                1 -> ProfileTab(
                    viewModel = viewModel,
                    user = user,
                    modifier = Modifier.fillMaxSize()
                )
                2 -> SettingsTab(
                    viewModel = viewModel,
                    user = user,
                    onNavigateToAdmin = onNavigateToAdmin,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
