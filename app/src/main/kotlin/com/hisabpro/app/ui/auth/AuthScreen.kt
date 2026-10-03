package com.hisabpro.app.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hisabpro.app.ui.theme.DeepNavyBlue
import com.hisabpro.app.ui.theme.Emerald700
import com.hisabpro.app.ui.theme.ExpenseRed
import com.hisabpro.app.ui.theme.PureWhite
import com.hisabpro.app.ui.theme.SaffronOrange

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val authMode by viewModel.authMode.collectAsStateWithLifecycle()
    val email by viewModel.email.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()
    val confirmPassword by viewModel.confirmPassword.collectAsStateWithLifecycle()
    val businessName by viewModel.businessName.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val successMessage by viewModel.successMessage.collectAsStateWithLifecycle()
    val showConfigDialog by viewModel.showConfigDialog.collectAsStateWithLifecycle()

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DeepNavyBlue, Color(0xFF0F172A))
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .testTag("card_auth"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = { viewModel.openConfigDialog() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .testTag("btn_auth_server_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Server Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SaffronOrange,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "₹",
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "HisabPro",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepNavyBlue
                        )
                        Text(
                            text = "Smart Accounting & Billing for Indian Business",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Mode Title
                val modeTitle = when (authMode) {
                    AuthMode.LOGIN -> "Welcome Back"
                    AuthMode.REGISTER -> "Create Business Account"
                    AuthMode.FORGOT_PASSWORD -> "Recover Password"
                }
                Text(
                    text = modeTitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Error / Success Banners
                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ExpenseRed.copy(alpha = 0.1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .testTag("banner_auth_error")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = errorMessage ?: "",
                                color = ExpenseRed,
                                fontSize = 12.sp
                            )
                            if (errorMessage?.contains("Configure Server", ignoreCase = true) == true ||
                                errorMessage?.contains("not configured", ignoreCase = true) == true
                            ) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilledTonalButton(
                                        onClick = { viewModel.openConfigDialog() },
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = ExpenseRed.copy(alpha = 0.2f),
                                            contentColor = ExpenseRed
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("btn_error_configure_server")
                                    ) {
                                        Text("Configure Server (URL & Key)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                if (successMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Emerald700.copy(alpha = 0.15f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .testTag("banner_auth_success")
                    ) {
                        Text(
                            text = successMessage ?: "",
                            color = Emerald700,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Input Fields based on mode
                if (authMode == AuthMode.REGISTER) {
                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { viewModel.updateBusinessName(it) },
                        label = { Text("Business / Shop Name") },
                        leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_auth_business_name")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { viewModel.updateEmail(it) },
                    label = { Text("Email Address") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_auth_email")
                )

                if (authMode != AuthMode.FORGOT_PASSWORD) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { viewModel.updatePassword(it) },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle Password Visibility"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_auth_password")
                    )
                }

                if (authMode == AuthMode.REGISTER) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { viewModel.updateConfirmPassword(it) },
                        label = { Text("Confirm Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle Confirm Password Visibility"
                                )
                            }
                        },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_confirm_password")
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Button
                val buttonText = when (authMode) {
                    AuthMode.LOGIN -> "Sign In"
                    AuthMode.REGISTER -> "Create Account"
                    AuthMode.FORGOT_PASSWORD -> "Send Recovery Link"
                }
                val buttonTag = when (authMode) {
                    AuthMode.LOGIN -> "btn_login"
                    AuthMode.REGISTER -> "btn_register"
                    AuthMode.FORGOT_PASSWORD -> "btn_send_recovery"
                }

                Button(
                    onClick = {
                        when (authMode) {
                            AuthMode.LOGIN -> viewModel.signIn()
                            AuthMode.REGISTER -> viewModel.signUp()
                            AuthMode.FORGOT_PASSWORD -> viewModel.sendPasswordReset()
                        }
                    },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag(buttonTag)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = PureWhite,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = buttonText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Switch Modes
                when (authMode) {
                    AuthMode.LOGIN -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { viewModel.setAuthMode(AuthMode.FORGOT_PASSWORD) },
                                modifier = Modifier.testTag("btn_goto_forgot")
                            ) {
                                Text("Forgot Password?", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(
                                onClick = { viewModel.setAuthMode(AuthMode.REGISTER) },
                                modifier = Modifier.testTag("btn_goto_register")
                            ) {
                                Text("Create Account", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SaffronOrange)
                            }
                        }
                    }
                    AuthMode.REGISTER -> {
                        TextButton(
                            onClick = { viewModel.setAuthMode(AuthMode.LOGIN) },
                            modifier = Modifier.testTag("btn_goto_login_from_reg")
                        ) {
                            Text("Already have an account? Sign In", fontSize = 12.sp, color = SaffronOrange, fontWeight = FontWeight.Bold)
                        }
                    }
                    AuthMode.FORGOT_PASSWORD -> {
                        TextButton(
                            onClick = { viewModel.setAuthMode(AuthMode.LOGIN) },
                            modifier = Modifier.testTag("btn_goto_login_from_forgot")
                        ) {
                            Text("Back to Sign In", fontSize = 12.sp, color = SaffronOrange, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                OutlinedButton(
                    onClick = { viewModel.continueOffline() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = DeepNavyBlue
                    ),
                    border = BorderStroke(1.5.dp, DeepNavyBlue.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_continue_offline")
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Continue in Offline Mode",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Full billing, GST/non-GST invoices & ledger locally on this phone without cloud setup",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(
                    onClick = { viewModel.openConfigDialog() },
                    modifier = Modifier.testTag("btn_open_server_settings_link")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Configure Supabase Server (URL & Anon Key)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (showConfigDialog) {
            var urlInput by remember { mutableStateOf(viewModel.getSavedSupabaseUrl()) }
            var keyInput by remember { mutableStateOf(viewModel.getSavedSupabaseAnonKey()) }

            AlertDialog(
                onDismissRequest = { viewModel.closeConfigDialog() },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Cloud, contentDescription = null, tint = SaffronOrange)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Supabase Server Settings", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Configure your Supabase Cloud instance to enable multi-device sync, authentication, and backups.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            label = { Text("Supabase Project URL") },
                            placeholder = { Text("https://xyz.supabase.co") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_config_supabase_url")
                        )
                        OutlinedTextField(
                            value = keyInput,
                            onValueChange = { keyInput = it },
                            label = { Text("Supabase Anon Public Key") },
                            placeholder = { Text("eyJhbGciOi...") },
                            singleLine = false,
                            maxLines = 3,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_config_supabase_key")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.saveSupabaseConfig(urlInput.trim(), keyInput.trim())
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange),
                        modifier = Modifier.testTag("btn_save_supabase_config")
                    ) {
                        Text("Save & Apply", color = PureWhite, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.closeConfigDialog() },
                        modifier = Modifier.testTag("btn_cancel_supabase_config")
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
