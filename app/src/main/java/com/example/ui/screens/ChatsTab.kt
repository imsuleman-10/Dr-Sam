package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.ChatEntity
import com.example.data.database.MessageEntity
import com.example.data.database.UserEntity
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatsTab(
    viewModel: MainViewModel,
    user: UserEntity,
    modifier: Modifier = Modifier
) {
    val chats by viewModel.chats.collectAsState()
    val activeChat by viewModel.activeChat.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var inputMessage by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    // State for toggleable drawer panel showing all past chats
    var showHistoryDrawer by remember { mutableStateOf(false) }

    // Dialog state for renaming chat
    var showRenameDialog by remember { mutableStateOf(false) }
    var chatToRename by remember { mutableStateOf<ChatEntity?>(null) }
    var newChatTitle by remember { mutableStateOf("") }

    // Dialog state for exporting chat as health report
    var showExportDialog by remember { mutableStateOf(false) }
    var chatToExport by remember { mutableStateOf<ChatEntity?>(null) }

    val listState = rememberLazyListState()

    // Smooth scroll down when new messages appear
    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val filteredChats = chats.filter {
        it.title.contains(searchQuery, ignoreCase = true)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Drawer Trigger Icon
                IconButton(onClick = { showHistoryDrawer = !showHistoryDrawer }) {
                    BadgedBox(
                        badge = {
                            if (chats.isNotEmpty()) {
                                Badge { Text("${chats.size}") }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (showHistoryDrawer) Icons.Filled.MenuOpen else Icons.Filled.History,
                            contentDescription = "Toggle past chats lists"
                        )
                    }
                }

                // Title Banner (Sleek branded badge)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "S",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Column {
                        Text(
                            text = activeChat?.title ?: "Dr SAM",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            modifier = Modifier.widthIn(max = 160.dp)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isGenerating) MaterialTheme.colorScheme.primary else Color.Gray)
                            )
                            Text(
                                text = if (isGenerating) "Dr SAM is writing..." else "AI Health Assistant",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                // Right: Actions Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (activeChat != null) {
                        IconButton(
                            onClick = {
                                chatToExport = activeChat
                                showExportDialog = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FileDownload,
                                contentDescription = "Export current health report",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            viewModel.selectChatEntity(null)
                            inputMessage = ""
                            showHistoryDrawer = false
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddComment,
                            contentDescription = "Start brand new medical assessment",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Central Chat Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (activeChat == null && messages.isEmpty()) {
                    // Modern greeting card layout
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocalHospital,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Greeting prompt (Hello, Suleman)
                        Text(
                            text = "Hello, ${user.name}",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "How can Dr SAM assist your symptoms today?",
                            style = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                        )

                        // Disclaimer Box with Sleek theme blue-tinted accent border and background
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text(
                                        text = "Clinical Disclaimer",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Dr SAM is an AI assistant and not a licensed medical doctor. All responses are educational and suggest precautions or OTC pain remedies carefully. Consult a physician for emergencies.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }

                        // Symptom Quick Suggestion buttons
                        Text(
                            text = "Or tap a common health topic:",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val prompts = listOf("Mild headache remedies", "Acid reflux precautions")
                            prompts.forEach { p ->
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            viewModel.sendMessage(p)
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = p,
                                        modifier = Modifier.padding(12.dp),
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Chat message bubbles
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        contentPadding = PaddingValues(top = 10.dp, bottom = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(messages) { msg ->
                            val isAi = msg.sender == "ai"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isAi) Arrangement.Start else Arrangement.End
                            ) {
                                if (isAi) {
                                    // AI Avatar
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                            .align(Alignment.Top),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.LocalHospital,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                Column(
                                    horizontalAlignment = if (isAi) Alignment.Start else Alignment.End,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Card(
                                        shape = RoundedCornerShape(
                                            topStart = if (isAi) 0.dp else 16.dp,
                                            topEnd = if (isAi) 16.dp else 0.dp,
                                            bottomStart = 16.dp,
                                            bottomEnd = 16.dp
                                        ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isAi) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary
                                        ),
                                        border = if (isAi) BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)) else null,
                                        modifier = Modifier.testTag(if (isAi) "ai_message_bubble" else "user_message_bubble"),
                                        elevation = CardDefaults.cardElevation(defaultElevation = if (isAi) 2.dp else 1.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = msg.content,
                                                fontSize = 14.sp,
                                                color = if (isAi) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary,
                                                lineHeight = 20.sp
                                            )
                                        }
                                    }

                                    // Action buttons for messages (e.g. Copy)
                                    if (isAi) {
                                        Row(
                                            modifier = Modifier.padding(top = 2.dp, start = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(msg.content))
                                                    Toast.makeText(context, "Medical guidance copied!", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.ContentCopy,
                                                    contentDescription = "Copy Response",
                                                    modifier = Modifier.size(14.dp),
                                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Writing typing indicators
                        if (isGenerating) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.LocalHospital,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))

                                    Card(
                                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        modifier = Modifier.padding(end = 40.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "Dr SAM is assessing symptoms...",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // SLIDING DRAWERS OVERLAY PANEL FOR CHATS HISTORY (Standard sliding state)
                androidx.compose.animation.AnimatedVisibility(
                    visible = showHistoryDrawer,
                    enter = slideInHorizontally() + fadeIn(),
                    exit = slideOutHorizontally() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(280.dp)
                            .background(MaterialTheme.colorScheme.surface)
                            .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
                            .padding(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Consultation Logs",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                IconButton(onClick = { showHistoryDrawer = false }) {
                                    Icon(Icons.Filled.Close, "Close drawer")
                                }
                            }

                            Divider()

                            // Search past consultations bar
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search chats...") },
                                leadingIcon = { Icon(Icons.Filled.Search, null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )

                            Button(
                                onClick = {
                                    viewModel.selectChatEntity(null)
                                    inputMessage = ""
                                    showHistoryDrawer = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Filled.Add, null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("New Diagnostic Chat")
                            }

                            // Past chats list scroll container
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(filteredChats) { chat ->
                                    val isSelected = activeChat?.id == chat.id
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.selectChatEntity(chat)
                                                showHistoryDrawer = false
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Forum,
                                                    contentDescription = null,
                                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = chat.title,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    maxLines = 1
                                                )
                                            }

                                            // Edit / Delete / Export past chat history triggers
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                IconButton(
                                                    onClick = {
                                                        chatToExport = chat
                                                        showExportDialog = true
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.FileDownload,
                                                        contentDescription = "Export diagnostic report",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        chatToRename = chat
                                                        newChatTitle = chat.title
                                                        showRenameDialog = true
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Edit,
                                                        contentDescription = "Rename chat",
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { viewModel.deleteChat(chat.id) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Delete,
                                                        contentDescription = "Delete chat",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Input Section (Sleek custom container with shadow & high-precision rounded details)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputMessage,
                    onValueChange = { inputMessage = it },
                    placeholder = { Text("Search symptoms, ask for precautions, OTC remedies...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Attach details",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    trailingIcon = {
                        if (inputMessage.isNotBlank()) {
                            IconButton(onClick = { inputMessage = "" }) {
                                Icon(Icons.Filled.Clear, "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                )

                IconButton(
                    onClick = {
                        if (inputMessage.isNotBlank()) {
                            viewModel.sendMessage(inputMessage)
                            inputMessage = ""
                        }
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (inputMessage.isNotBlank() && !isGenerating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .size(48.dp)
                        .testTag("send_button"),
                    enabled = inputMessage.isNotBlank() && !isGenerating
                ) {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = "Send advice message to Dr SAM",
                        tint = if (inputMessage.isNotBlank() && !isGenerating) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    // Rename past Diagnostic chat Dialog
    if (showRenameDialog && chatToRename != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Consultation") },
            text = {
                OutlinedTextField(
                    value = newChatTitle,
                    onValueChange = { newChatTitle = it },
                    label = { Text("Consultation Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newChatTitle.isNotBlank()) {
                            viewModel.renameChat(chatToRename!!.id, newChatTitle.trim())
                            showRenameDialog = false
                            chatToRename = null
                        }
                    }
                ) {
                    Text("Confirm Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Export Consultation Health Report selection Dialog
    if (showExportDialog && chatToExport != null) {
        AlertDialog(
            onDismissRequest = {
                showExportDialog = false
                chatToExport = null
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalHospital,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Clinical Export",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Compile and share a comprehensive medical summary for: \"${chatToExport!!.title}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "This report uses clinical extraction to automatically filter out small-talk (greetings and chit-chat), compiling only the patient's isolated physiological problems paired with Dr SAM's diagnostic solutions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val msgs = viewModel.getMessagesForChatSync(chatToExport!!.id)
                                if (msgs.isEmpty()) {
                                    Toast.makeText(context, "No advice history to export.", Toast.LENGTH_SHORT).show()
                                } else {
                                    com.example.util.ReportExporter.exportChatAsText(
                                        context = context,
                                        chatTitle = chatToExport!!.title,
                                        user = user,
                                        messages = msgs
                                    )
                                }
                                showExportDialog = false
                                chatToExport = null
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("TXT Format")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                val msgs = viewModel.getMessagesForChatSync(chatToExport!!.id)
                                if (msgs.isEmpty()) {
                                    Toast.makeText(context, "No advice history to export.", Toast.LENGTH_SHORT).show()
                                } else {
                                    com.example.util.ReportExporter.exportChatAsPdf(
                                        context = context,
                                        chatTitle = chatToExport!!.title,
                                        user = user,
                                        messages = msgs
                                    )
                                }
                                showExportDialog = false
                                chatToExport = null
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Clinical PDF")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showExportDialog = false
                        chatToExport = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", textAlign = TextAlign.Center)
                }
            }
        )
    }
}
