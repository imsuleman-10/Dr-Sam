package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.SettingsEntity
import com.example.data.database.UserEntity
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTab(
    viewModel: MainViewModel,
    user: UserEntity,
    onNavigateToAdmin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.userSettings.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    var showPassInput by remember { mutableStateOf(false) }

    // Forms
    var selectProvider by remember(settings) { mutableStateOf(settings?.aiProvider ?: "Gemini") }
    var apiKey by remember(settings) { mutableStateOf(settings?.apiKeyEncrypted ?: "") }
    var personalizationPrompt by remember(settings) { mutableStateOf(settings?.personalizationPrompt ?: "") }

    val providerOptions = listOf("Gemini", "OpenAI", "OpenRouter", "Custom Endpoint")

    var notificationEnabled by remember { mutableStateOf(true) }
    var languageSelected by remember { mutableStateOf("English (US)") }

    var supabaseUrl by remember(settings) { mutableStateOf(settings?.supabaseUrl ?: "") }
    var supabaseAnonKey by remember(settings) { mutableStateOf(settings?.supabaseAnonKey ?: "") }
    var supabaseSyncEnabled by remember(settings) { mutableStateOf(settings?.supabaseSyncEnabled ?: false) }

    var testConnectionStatus by remember { mutableStateOf<String?>(null) }
    var testingConnection by remember { mutableStateOf(false) }

    val syncStatus by viewModel.syncStatus.collectAsState()

    // Dialogs
    var showReportDialog by remember { mutableStateOf(false) }
    var reportText by remember { mutableStateOf("") }
    var showDeleteAccountConfirm by remember { mutableStateOf(false) }
    var saveFeedbackText by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ADMIN DASHBOARD ENTRANCE (Admin Only)
        if (user.role == "admin") {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToAdmin() }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = "Admin shield logo",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dr SAM Admin Panel",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "Access user, logs, chat counters, and db stats",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.ArrowForwardIos,
                        contentDescription = "Navigate to admin",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Custom AI Settings Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.SettingsSuggest, "AI settings", tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Customize AI Brain",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Divider()

                // Provider selection
                Text(text = "AI Model Provider", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Column {
                    providerOptions.forEach { provider ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectProvider = provider
                                    viewModel.updateSettings(
                                        provider = provider,
                                        apiKey = apiKey,
                                        prompt = personalizationPrompt,
                                        darkMode = isDarkMode
                                    )
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectProvider == provider,
                                onClick = {
                                    selectProvider = provider
                                    viewModel.updateSettings(
                                        provider = provider,
                                        apiKey = apiKey,
                                        prompt = personalizationPrompt,
                                        darkMode = isDarkMode
                                    )
                                }
                            )
                            Text(
                                text = provider,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Overriding key input
                Text(text = "Custom API Overriding Key (Optional)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = {
                            apiKey = it
                            viewModel.updateSettings(
                                provider = selectProvider,
                                apiKey = it,
                                prompt = personalizationPrompt,
                                darkMode = isDarkMode
                            )
                        },
                        placeholder = { Text("Leaves empty for Dr SAM's out-of-box API key") },
                        visualTransformation = if (showPassInput) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            IconButton(onClick = { showPassInput = !showPassInput }) {
                                Icon(
                                    imageVector = if (showPassInput) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Custom AI prompt instruction behavior
                Text(text = "Personalization instructions", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                OutlinedTextField(
                    value = personalizationPrompt,
                    onValueChange = {
                        personalizationPrompt = it
                        viewModel.updateSettings(
                            provider = selectProvider,
                            apiKey = apiKey,
                            prompt = it,
                            darkMode = isDarkMode
                        )
                    },
                    placeholder = { Text("e.g. Answer briefly and professionally, or assume I am diabetic.") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )
            }
        }

        // Toggles / Configuration List
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Personalizations",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Divider()

                // Dark mode toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.DarkMode, "Dark Icon", tint = MaterialTheme.colorScheme.primary)
                        Text("Dark Theme Default")
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { checked ->
                            viewModel.updateSettings(
                                provider = selectProvider,
                                apiKey = apiKey,
                                prompt = personalizationPrompt,
                                darkMode = checked
                            )
                        }
                    )
                }

                // Notification toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.Notifications, "Notif Icon", tint = MaterialTheme.colorScheme.primary)
                        Text("Mute Alerts/Tips")
                    }
                    Switch(
                        checked = notificationEnabled,
                        onCheckedChange = { notificationEnabled = it }
                    )
                }

                // Language selection Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.Translate, "Lang Icon", tint = MaterialTheme.colorScheme.primary)
                        Text("App Language")
                    }
                    Text(
                        text = languageSelected,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Supabase Cloud Database Configuration Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudUpload,
                        contentDescription = "Cloud Icon",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Supabase Cloud Sync",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Text(
                    text = "Store your profile details, symptom logs, active chats, and medical audit files secure in your remote private cloud database.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Divider()

                // Toggle Sync
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Sync with Cloud", fontWeight = FontWeight.Bold)
                    Switch(
                        checked = supabaseSyncEnabled,
                        onCheckedChange = { checked ->
                            supabaseSyncEnabled = checked
                            viewModel.updateSettings(
                                provider = selectProvider,
                                apiKey = apiKey,
                                prompt = personalizationPrompt,
                                darkMode = isDarkMode,
                                supabaseUrl = supabaseUrl,
                                supabaseAnonKey = supabaseAnonKey,
                                supabaseSyncEnabled = checked
                            )
                        }
                    )
                }

                // URL Input
                OutlinedTextField(
                    value = supabaseUrl,
                    onValueChange = {
                        supabaseUrl = it
                        viewModel.updateSettings(
                            provider = selectProvider,
                            apiKey = apiKey,
                            prompt = personalizationPrompt,
                            darkMode = isDarkMode,
                            supabaseUrl = it,
                            supabaseAnonKey = supabaseAnonKey,
                            supabaseSyncEnabled = supabaseSyncEnabled
                        )
                    },
                    label = { Text("Supabase Cloud URL") },
                    placeholder = { Text("https://your-project.supabase.co") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Service Anon Key
                var showSupKeyInput by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = supabaseAnonKey,
                    onValueChange = {
                        supabaseAnonKey = it
                        viewModel.updateSettings(
                            provider = selectProvider,
                            apiKey = apiKey,
                            prompt = personalizationPrompt,
                            darkMode = isDarkMode,
                            supabaseUrl = supabaseUrl,
                            supabaseAnonKey = it,
                            supabaseSyncEnabled = supabaseSyncEnabled
                        )
                    },
                    label = { Text("Supabase Anon Key") },
                    placeholder = { Text("eyJhbGciOiJIUzI1NiIsInR5c...") },
                    visualTransformation = if (showSupKeyInput) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showSupKeyInput = !showSupKeyInput }) {
                            Icon(
                                imageVector = if (showSupKeyInput) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (supabaseUrl.isNotBlank() && supabaseAnonKey.isNotBlank()) {
                                testingConnection = true
                                testConnectionStatus = "Pinging Supabase REST API server..."
                                viewModel.testSupabaseConnection(supabaseUrl, supabaseAnonKey) { success ->
                                    testingConnection = false
                                    testConnectionStatus = if (success) {
                                        "Connected successfully! All tables on Supabase are ready to exchange medical logs."
                                    } else {
                                        "Failed to connect. Please check your internet, project URL, or authorization anon key."
                                    }
                                }
                            } else {
                                testConnectionStatus = "You must enter both Supabase URL and Anon Key to test."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !testingConnection
                    ) {
                        Icon(Icons.Filled.NetworkCheck, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test API", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.syncFromCloud()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = supabaseSyncEnabled
                    ) {
                        Icon(Icons.Filled.Sync, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pull Cloud DB", fontSize = 12.sp)
                    }
                }

                // Instructions button / link
                Text(
                    text = "💡 Tap here to view the schema setup commands to launch on your Supabase dashboard.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable {
                            testConnectionStatus = "Run the following SQL in your Supabase SQL Editor to initialize the necessary tables:\n\n" +
                                    "CREATE TABLE IF NOT EXISTS users (\n" +
                                    "  id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,\n" +
                                    "  name text,\n" +
                                    "  email text UNIQUE,\n" +
                                    "  password_hash text,\n" +
                                    "  role text DEFAULT 'user',\n" +
                                    "  profile_image text,\n" +
                                    "  dob text,\n" +
                                    "  gender text,\n" +
                                    "  work text,\n" +
                                    "  bio text,\n" +
                                    "  country text,\n" +
                                    "  height text,\n" +
                                    "  weight text,\n" +
                                    "  created_at bigint\n" +
                                    ");\n\n" +
                                    "CREATE TABLE IF NOT EXISTS chats (\n" +
                                    "  id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,\n" +
                                    "  user_id bigint,\n" +
                                    "  title text,\n" +
                                    "  created_at bigint\n" +
                                    ");\n\n" +
                                    "CREATE TABLE IF NOT EXISTS messages (\n" +
                                    "  id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,\n" +
                                    "  chat_id bigint,\n" +
                                    "  sender text,\n" +
                                    "  content text,\n" +
                                    "  timestamp bigint\n" +
                                    ");\n\n" +
                                    "CREATE TABLE IF NOT EXISTS reports (\n" +
                                    "  id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,\n" +
                                    "  user_id bigint,\n" +
                                    "  user_name text,\n" +
                                    "  issue text,\n" +
                                    "  created_at bigint\n" +
                                    ");"
                        }
                        .padding(top = 4.dp)
                )
            }
        }

        // feedback and security card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Security & Feedback",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Divider()

                // File system issue reports
                Button(
                    onClick = { showReportDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.ReportProblem, "Report", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Report Concern or App Issue")
                }

                // Log out Button
                Button(
                    onClick = { viewModel.logout() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                        contentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Logout, "Logout", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out Account")
                }

                // Delete Account
                Button(
                    onClick = { showDeleteAccountConfirm = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Delete, "Delete logo", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Account Permanently")
                }
            }
        }
    }

    // Report/Concern Dialogue
    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Submit Health/App Feedback") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Provide suggestions or medical app bugs below. It is stored securely for administrator reviews.")
                    OutlinedTextField(
                        value = reportText,
                        onValueChange = { reportText = it },
                        placeholder = { Text("Describe the issue or user complaint...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reportText.isNotBlank()) {
                            viewModel.submitReport(reportText.trim())
                            reportText = ""
                            showReportDialog = false
                            saveFeedbackText = "Thank you! Your feedback has been logged securely for the administrators."
                        }
                    }
                ) {
                    Text("Submit feedback")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Feedback Confirmation info
    if (saveFeedbackText.isNotBlank()) {
        AlertDialog(
            onDismissRequest = { saveFeedbackText = "" },
            title = { Text("Logged Successfully") },
            text = { Text(saveFeedbackText) },
            confirmButton = {
                Button(onClick = { saveFeedbackText = "" }) {
                    Text("OK")
                }
            }
        )
    }

    // Delete account dialog
    if (showDeleteAccountConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountConfirm = false },
            title = { Text("Delete Account permanently?") },
            text = { Text("WARNING: Doing this will wipe all your local health details, active lists, chat history files, and custom AI personalization preferences. This action is irreversible.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountConfirm = false
                        viewModel.deleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete irreversibly")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Supabase Sync Dialog
    if (syncStatus != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearSyncStatus() },
            title = { Text("Cloud synchronization") },
            text = { Text(syncStatus ?: "") },
            confirmButton = {
                Button(onClick = { viewModel.clearSyncStatus() }) {
                    Text("OK")
                }
            }
        )
    }

    // Supabase Connection Status Dialog
    if (testConnectionStatus != null) {
        AlertDialog(
            onDismissRequest = { testConnectionStatus = null },
            title = { Text("Supabase Status Detail") },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = testConnectionStatus ?: "",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(onClick = { testConnectionStatus = null }) {
                    Text("Dismiss")
                }
            }
        )
    }
}
