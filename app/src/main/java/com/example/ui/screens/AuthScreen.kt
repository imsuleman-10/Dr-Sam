package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.ui.viewmodel.AuthState
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AuthScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val authState by viewModel.authState.collectAsState()
    var isSignUp by remember { mutableStateOf(false) }
    
    // Form Inputs
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // Signup specific fields (optional but helpful on profile)
    var dob by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    val genderOptions = listOf("Male", "Female", "Other")
    var work by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }

    // Email verification dialog
    var showVerificationNotice by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var generatedCode by remember { mutableStateOf("") }
    var enteredCode by remember { mutableStateOf("") }
    var isSendingEmail by remember { mutableStateOf(false) }
    var emailSendError by remember { mutableStateOf("") }
    var enterCodeError by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Hospital Header logo with pulsate visual
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.LocalHospital,
                    contentDescription = "Dr SAM App Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Dr SAM",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Your AI Healthcare Companion",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Form container card
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isSignUp) "Create Account" else "Welcome Back",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (authState is AuthState.Error) {
                        Text(
                            text = (authState as AuthState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Fields
                    if (isSignUp) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Full Name") },
                            leadingIcon = { Icon(Icons.Outlined.Person, "Name icon") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("name_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Outlined.Email, "Email icon") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("email_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Outlined.Lock, "Lock icon") },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (isSignUp) {
                        Spacer(modifier = Modifier.height(8.dp))

                        // Custom DOB / Gender / Work for Complete Profile Onboarding
                        OutlinedTextField(
                            value = dob,
                            onValueChange = { dob = it },
                            label = { Text("Date of Birth (YYYY-MM-DD)") },
                            placeholder = { Text("e.g. 1995-10-15") },
                            leadingIcon = { Icon(Icons.Filled.DateRange, "Date icon") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Gender",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
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

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = work,
                            onValueChange = { work = it },
                            label = { Text("Occupation / Work") },
                            leadingIcon = { Icon(Icons.Filled.Work, "Occupation") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = country,
                            onValueChange = { country = it },
                            label = { Text("Country") },
                            leadingIcon = { Icon(Icons.Filled.Public, "Country icon") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (authState is AuthState.Loading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    } else {
                        Button(
                            onClick = {
                                if (isSignUp) {
                                    if (name.isBlank() || email.isBlank() || password.isBlank()) {
                                        viewModel.signup(name, email, password)
                                    } else {
                                        // Generate and dispatch real SMTP email verification code
                                        val code = (100001..999999).random().toString()
                                        generatedCode = code
                                        enteredCode = ""
                                        emailSendError = ""
                                        enterCodeError = ""
                                        isSendingEmail = true
                                        showVerificationNotice = true
                                        coroutineScope.launch {
                                            val success = com.example.util.GmailSmtpSender.sendVerificationEmail(email, code)
                                            isSendingEmail = false
                                            if (!success) {
                                                emailSendError = "Gmail App Password delivery failed. Confirm internet connection and try again."
                                            }
                                        }
                                    }
                                } else {
                                    viewModel.login(email, password)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("auth_submit_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (isSignUp) "Verify Email & Register" else "Sign In",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = {
                        viewModel.clearAuthError()
                        isSignUp = !isSignUp
                    }) {
                        Text(
                            text = if (isSignUp) "Already have an account? Sign In" else "New to Dr SAM? Create Account",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Admin fast trigger suggestion info
                    if (!isSignUp) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Admin login email: sulemanzaheer09@gmail.com",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Professional clinical disclaimer notice
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "Disclaimer notice symbol",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "IMPORTANT DISCLAIMER: Dr SAM is an AI assistant and not a licensed medical doctor. Guidance is for informational and educational use only.",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }

    // Email verification interactive dialog trigger
    if (showVerificationNotice) {
        AlertDialog(
            onDismissRequest = { 
                if (!isSendingEmail) {
                    showVerificationNotice = false
                }
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.VerifiedUser, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Email Verification Required")
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSendingEmail) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Dispatching secure code via Google SMTP...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Text(
                            text = "A 6-digit verification code has been dispatched to:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = email,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (emailSendError.isNotBlank()) {
                            Text(
                                text = emailSendError,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        OutlinedTextField(
                            value = enteredCode,
                            onValueChange = { 
                                if (it.length <= 6) {
                                    enteredCode = it
                                    enterCodeError = ""
                                }
                            },
                            label = { Text("6-Digit Verification Code") },
                            placeholder = { Text("e.g. 123456") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (enterCodeError.isNotBlank()) {
                            Text(
                                text = enterCodeError,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                        
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(
                                onClick = {
                                    val code = (100001..999999).random().toString()
                                    generatedCode = code
                                    enteredCode = ""
                                    emailSendError = ""
                                    enterCodeError = ""
                                    isSendingEmail = true
                                    coroutineScope.launch {
                                        val success = com.example.util.GmailSmtpSender.sendVerificationEmail(email, code)
                                        isSendingEmail = false
                                        if (!success) {
                                            emailSendError = "SMTP Delivery failed. Check connection."
                                        }
                                    }
                                }
                            ) {
                                Text("Resend Code", fontSize = 13.sp)
                            }

                            TextButton(
                                onClick = {
                                    // Bypass mode as backup option
                                    showVerificationNotice = false
                                    viewModel.signup(
                                        name = name,
                                        email = email,
                                        password = password,
                                        dob = dob,
                                        gender = gender,
                                        work = work,
                                        country = country,
                                        bio = bio
                                    )
                                }
                            ) {
                                Text("Bypass Verification", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (!isSendingEmail) {
                    Button(
                        onClick = {
                            if (enteredCode.trim() == generatedCode && generatedCode.isNotBlank()) {
                                showVerificationNotice = false
                                viewModel.signup(
                                    name = name,
                                    email = email,
                                    password = password,
                                    dob = dob,
                                    gender = gender,
                                    work = work,
                                    country = country,
                                    bio = bio
                                )
                            } else {
                                enterCodeError = "Invalid verification code. Please confirm code in inbox."
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Verify & Complete Register")
                    }
                }
            },
            dismissButton = {
                if (!isSendingEmail) {
                    TextButton(
                        onClick = { 
                            showVerificationNotice = false 
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}
