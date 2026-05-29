package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.UserEntity
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTab(
    viewModel: MainViewModel,
    user: UserEntity,
    modifier: Modifier = Modifier
) {
    var name by remember(user) { mutableStateOf(user.name) }
    var dob by remember(user) { mutableStateOf(user.dob ?: "") }
    var work by remember(user) { mutableStateOf(user.work ?: "") }
    var bio by remember(user) { mutableStateOf(user.bio ?: "") }
    var country by remember(user) { mutableStateOf(user.country ?: "") }
    var height by remember(user) { mutableStateOf(user.height ?: "") }
    var weight by remember(user) { mutableStateOf(user.weight ?: "") }
    var gender by remember(user) { mutableStateOf(user.gender ?: "Male") }

    val genderOptions = listOf("Male", "Female", "Other")

    // Preset avatar icons
    val avatarOptions = listOf("local_hospital", "healing", "spa", "person")
    var selectedAvatar by remember(user) { mutableStateOf(user.profileImage ?: "local_hospital") }

    var isEditing by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    var showDialogAlert by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Upper Profile Card with avatar switcher
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Avatar display with active icon state
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    val avatarIcon = when (selectedAvatar) {
                        "healing" -> Icons.Filled.Healing
                        "spa" -> Icons.Filled.Spa
                        "person" -> Icons.Filled.Person
                        else -> Icons.Filled.LocalHospital
                    }
                    Icon(
                        imageVector = avatarIcon,
                        contentDescription = "Selected medical avatar",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(50.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )

                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(user.role.uppercase()) },
                        icon = {
                            Icon(
                                imageVector = if (user.role == "admin") Icons.Filled.Shield else Icons.Filled.VerifiedUser,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                }

                // Preset selector when editing
                if (isEditing) {
                    Text(
                        text = "Choose Medical Avatar Style:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        avatarOptions.forEach { option ->
                            val isChosen = selectedAvatar == option
                            val icon = when (option) {
                                "healing" -> Icons.Filled.Healing
                                "spa" -> Icons.Filled.Spa
                                "person" -> Icons.Filled.Person
                                else -> Icons.Filled.LocalHospital
                            }
                            IconButton(
                                onClick = { selectedAvatar = option },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(
                                        if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .size(36.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isChosen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Details Editor Screen Layout
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "General Health Profiles",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(
                        onClick = {
                            if (isEditing) {
                                // Save profile changes trigger
                                val edited = user.copy(
                                    name = name.trim(),
                                    dob = dob.trim(),
                                    work = work.trim(),
                                    bio = bio.trim(),
                                    country = country.trim(),
                                    height = height.trim(),
                                    weight = weight.trim(),
                                    gender = gender,
                                    profileImage = selectedAvatar
                                )
                                viewModel.updateProfile(edited)
                                isEditing = false
                                showDialogAlert = true
                            } else {
                                isEditing = true
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isEditing) Icons.Filled.CheckCircle else Icons.Filled.Edit,
                            contentDescription = "Edit Profile",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Divider()

                if (isEditing) {
                    // Editable fields
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = dob,
                        onValueChange = { dob = it },
                        label = { Text("Date of Birth") },
                        placeholder = { Text("YYYY-MM-DD") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Gender",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            genderOptions.forEach { option ->
                                val selected = gender == option
                                FilterChip(
                                    selected = selected,
                                    onClick = { gender = option },
                                    label = { Text(option) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = work,
                        onValueChange = { work = it },
                        label = { Text("Occupation / Work") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = country,
                        onValueChange = { country = it },
                        label = { Text("Country") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = height,
                            onValueChange = { height = it },
                            label = { Text("Height (cm or ft)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = weight,
                            onValueChange = { weight = it },
                            label = { Text("Weight (kg or lbs)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Personal Bio (Allergies, Medical notes)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                } else {
                    // Display details
                    ProfileRow(title = "Full Name", value = name, icon = Icons.Filled.Person)
                    ProfileRow(title = "DOB", value = dob.ifBlank { "Not specified" }, icon = Icons.Filled.DateRange)
                    ProfileRow(title = "Gender", value = gender, icon = Icons.Filled.Wc)
                    ProfileRow(title = "Occupation", value = work.ifBlank { "Not specified" }, icon = Icons.Filled.Work)
                    ProfileRow(title = "Country", value = country.ifBlank { "Not specified" }, icon = Icons.Filled.Public)
                    ProfileRow(
                        title = "Measurements",
                        value = if (height.isNotBlank() || weight.isNotBlank()) "H: $height | W: $weight" else "Not specified",
                        icon = Icons.Filled.MonitorWeight
                    )
                    ProfileRow(title = "Bio / Medical Alerts", value = bio.ifBlank { "No allergies or notes declared." }, icon = Icons.Filled.Assignment)
                }
            }
        }
    }

    if (showDialogAlert) {
        AlertDialog(
            onDismissRequest = { showDialogAlert = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Verified, "Verified", tint = MaterialTheme.colorScheme.primary)
                    Text("Profile Saved")
                }
            },
            text = { Text("Your health profile has been securely saved in the local database. Dr SAM will use these medical fields to adapt responses custom to you.") },
            confirmButton = {
                Button(onClick = { showDialogAlert = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun ProfileRow(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
