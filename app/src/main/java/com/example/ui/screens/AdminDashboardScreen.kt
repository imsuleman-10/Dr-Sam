package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.ReportEntity
import com.example.data.database.UserEntity
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: MainViewModel,
    currentUser: UserEntity,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.adminStats.collectAsState()
    val users by viewModel.adminUsers.collectAsState()
    val reports by viewModel.adminReports.collectAsState()

    var activeTabIdx by remember { mutableStateOf(0) } // 0: Overview, 1: Users, 2: Concerns
    var showDemoteWarning by remember { mutableStateOf<UserEntity?>(null) }

    // Load admin metrics
    LaunchedEffect(Unit) {
        viewModel.loadAdminDashboard()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Shield, "Admin", tint = MaterialTheme.colorScheme.primary)
                        Text("Admin Dashboard Hub", fontWeight = FontWeight.ExtraBold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back icon")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Interactive Tab Bar to split sub-sections
            TabRow(selectedTabIndex = activeTabIdx) {
                Tab(
                    selected = activeTabIdx == 0,
                    onClick = { activeTabIdx = 0 },
                    text = { Text("Metrics Overview") },
                    icon = { Icon(Icons.Filled.Analytics, null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = activeTabIdx == 1,
                    onClick = { activeTabIdx = 1 },
                    text = { Text("User list") },
                    icon = { Icon(Icons.Filled.People, null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = activeTabIdx == 2,
                    onClick = { activeTabIdx = 2 },
                    text = { Text("Log Concerns") },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (reports.isNotEmpty()) {
                                    Badge { Text("${reports.size}") }
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Warning, null, modifier = Modifier.size(18.dp))
                        }
                    }
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                when (activeTabIdx) {
                    0 -> OverviewPanel(stats = stats, usersCount = users.size, reportsCount = reports.size, viewModel = viewModel)
                    1 -> UsersListPanel(users = users, currentAdminId = currentUser.id, viewModel = viewModel)
                    2 -> ReportsListPanel(reports = reports, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun OverviewPanel(
    stats: Map<String, Int>?,
    usersCount: Int,
    reportsCount: Int,
    viewModel: MainViewModel
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Dr SAM Real-time Database Metrics (Local PostgreSQL Mock)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Total Users",
                value = "${stats?.get("total_users") ?: usersCount}",
                icon = Icons.Filled.Group,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Reported Concerns",
                value = "${stats?.get("total_reports") ?: reportsCount}",
                icon = Icons.Filled.Feedback,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Diagnostic Chats",
                value = "${stats?.get("total_chats") ?: 0}",
                icon = Icons.Filled.Forum,
                color = Color(0xFF0EA5E9),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Total Messages",
                value = "${stats?.get("total_messages") ?: 0}",
                icon = Icons.Filled.Message,
                color = Color(0xFFD946EF),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("API Usage Monitoring", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Divider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("API Service Provider:")
                    Text("Direct REST API (V1beta)", fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Target AI Model Mode:")
                    Text("gemini-3.5-flash", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Fallback Status:")
                    Text("Graceful Local Clinic Enabled", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }

        Button(
            onClick = { viewModel.loadAdminDashboard() },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Icon(Icons.Filled.Refresh, "Refresh stats")
            Spacer(modifier = Modifier.width(6.dp))
            Text("Refresh metrics dashboard")
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Text(text = value, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun UsersListPanel(
    users: List<UserEntity>,
    currentAdminId: Int,
    viewModel: MainViewModel
) {
    if (users.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No registered users found inside SQLite.")
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Manage Users Databases (${users.size} Accounts)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(users) { usr ->
                    val isSelf = usr.id == currentAdminId
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelf) MaterialTheme.colorScheme.primary.copy(alpha = 0.04f) else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(text = usr.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                    Badge(
                                        containerColor = if (usr.role == "admin") MaterialTheme.colorScheme.primary else Color.Gray,
                                        contentColor = Color.White
                                    ) {
                                        Text(usr.role.uppercase())
                                    }
                                }
                                Text(text = usr.email, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                if (usr.dob != null || usr.gender != null) {
                                    Text(
                                        text = "Gender: ${usr.gender ?: "?"} | DOB: ${usr.dob ?: "?"}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            if (!isSelf) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // Toggle Role Action
                                    IconButton(
                                        onClick = {
                                            val targetRole = if (usr.role == "admin") "user" else "admin"
                                            viewModel.adminUpdateUserRole(usr.id, targetRole)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (usr.role == "admin") Icons.Filled.SupervisorAccount else Icons.Filled.Shield,
                                            contentDescription = "Toggle admin rule",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    // Delete User Action
                                    IconButton(
                                        onClick = { viewModel.adminDeleteUser(usr.id) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Delete account",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "Current Self",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportsListPanel(
    reports: List<ReportEntity>,
    viewModel: MainViewModel
) {
    if (reports.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Filled.VerifiedUser, "Success info", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                Text("All quiet! No user issues compiled inside DB.")
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Logged Issues and Reports (${reports.size} items)", fontWeight = FontWeight.Bold, fontSize = 13.sp)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(reports) { rpt ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = "Reporter: ${rpt.userName}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(text = "ID: ${rpt.userId}", fontSize = 10.sp, color = Color.Gray)
                                }
                                IconButton(
                                    onClick = { viewModel.adminDeleteReport(rpt.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Dismiss feedback",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Divider()
                            Text(
                                text = rpt.issue,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

// Ext helper for icon size
fun ImageVectorSize(size: Int) = size.dp
