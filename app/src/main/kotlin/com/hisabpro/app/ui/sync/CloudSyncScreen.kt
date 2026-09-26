package com.hisabpro.app.ui.sync

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hisabpro.app.ui.theme.DeepNavyBlue
import com.hisabpro.app.ui.theme.Emerald700
import com.hisabpro.app.ui.theme.ExpenseRed
import com.hisabpro.app.ui.theme.PureWhite
import com.hisabpro.app.ui.theme.SaffronOrange
import com.hisabpro.app.ui.theme.Slate700
import com.hisabpro.app.ui.theme.Slate800
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudSyncScreen(
    viewModel: CloudSyncViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val loginInProgress by viewModel.loginInProgress.collectAsStateWithLifecycle()
    val otpSent by viewModel.otpSent.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()

    var authTab by remember { mutableIntStateOf(0) } // 0: Mobile OTP, 1: Email, 2: 1-Click
    var phoneInput by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }

    BackHandler {
        onBack()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Supabase Cloud Sync",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_sync_back")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Sync Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("card_cloud_sync_status"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    val heroGradientColors = when {
                        syncState.pendingQueueCount > 0 -> listOf(DeepNavyBlue, Color(0xFF1E293B))
                        syncState.isAuthenticated -> listOf(Color(0xFF064E3B), Color(0xFF065F46))
                        else -> listOf(DeepNavyBlue, Color(0xFF0F174A))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = heroGradientColors
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = when {
                                    syncState.isSyncing -> SaffronOrange
                                    syncState.pendingQueueCount > 0 -> SaffronOrange
                                    syncState.isAuthenticated -> Emerald700
                                    else -> Slate700
                                },
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    when {
                                        syncState.isSyncing -> {
                                            CircularProgressIndicator(
                                                color = PureWhite,
                                                strokeWidth = 3.dp,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                        syncState.pendingQueueCount > 0 -> {
                                            Icon(
                                                imageVector = Icons.Default.CloudUpload,
                                                contentDescription = null,
                                                tint = PureWhite,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                        syncState.isAuthenticated -> {
                                            Icon(
                                                imageVector = Icons.Default.CloudDone,
                                                contentDescription = null,
                                                tint = PureWhite,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                        else -> {
                                            Icon(
                                                imageVector = Icons.Default.CloudOff,
                                                contentDescription = null,
                                                tint = PureWhite,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = when {
                                    syncState.isSyncing -> "Synchronizing Records..."
                                    syncState.pendingQueueCount > 0 -> "${syncState.pendingQueueCount} Offline Changes Pending"
                                    syncState.isAuthenticated -> "All Records Synced to Cloud"
                                    else -> "Connect Account for Cloud Sync"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = PureWhite,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            val lastSyncText = if (syncState.pendingQueueCount > 0) {
                                "Saved locally on device. Ready to push."
                            } else if (syncState.lastSyncTimeMillis > 0) {
                                val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH)
                                "Last synced: ${sdf.format(Date(syncState.lastSyncTimeMillis))}"
                            } else {
                                "Not synced yet"
                            }

                            Text(
                                text = lastSyncText,
                                fontSize = 12.sp,
                                color = PureWhite.copy(alpha = 0.8f)
                            )

                            if (syncState.pendingQueueCount > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = SaffronOrange.copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        text = "${syncState.pendingQueueCount} unsynced records (Invoices/Parties/Items)",
                                        color = SaffronOrange,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { viewModel.triggerSync() },
                                enabled = !syncState.isSyncing && syncState.isAuthenticated,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (syncState.pendingQueueCount > 0) SaffronOrange else Emerald700,
                                    disabledContainerColor = Slate700.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_sync_now")
                            ) {
                                if (syncState.isSyncing) {
                                    CircularProgressIndicator(
                                        color = PureWhite,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Syncing...", fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(
                                        imageVector = if (syncState.pendingQueueCount > 0) Icons.Default.CloudUpload else Icons.Default.Sync,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val btnText = if (syncState.pendingQueueCount > 0) {
                                        "Sync ${syncState.pendingQueueCount} Changes to Cloud"
                                    } else {
                                        "Sync Now"
                                    }
                                    Text(btnText, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Authentication Section
            item {
                if (syncState.isAuthenticated) {
                    // Connected User Profile Card
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("card_user_session"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Emerald700.copy(alpha = 0.15f),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Emerald700,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Connected Account",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = syncState.userEmail ?: syncState.userPhone ?: "Connected Business",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            OutlinedButton(
                                onClick = { viewModel.signOut() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("btn_cloud_signout")
                            ) {
                                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Disconnect", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                } else {
                    // Sign In / Register Card
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("card_auth_form"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Sign In to Enable Cloud Sync",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Back up and access your business data across all your Android devices safely.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                            )

                            TabRow(
                                selectedTabIndex = authTab,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Tab(
                                    selected = authTab == 0,
                                    onClick = { authTab = 0; viewModel.clearError() },
                                    text = { Text("Mobile OTP", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                )
                                Tab(
                                    selected = authTab == 1,
                                    onClick = { authTab = 1; viewModel.clearError() },
                                    text = { Text("Email", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                )
                                Tab(
                                    selected = authTab == 2,
                                    onClick = { authTab = 2; viewModel.clearError() },
                                    text = { Text("1-Click Connect", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            if (authError != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = ExpenseRed.copy(alpha = 0.1f),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                ) {
                                    Text(
                                        text = authError ?: "",
                                        color = ExpenseRed,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            when (authTab) {
                                0 -> {
                                    // Phone OTP Tab
                                    OutlinedTextField(
                                        value = phoneInput,
                                        onValueChange = { phoneInput = it },
                                        label = { Text("10-Digit Mobile Number") },
                                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("input_sync_phone")
                                    )

                                    if (otpSent) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        OutlinedTextField(
                                            value = otpInput,
                                            onValueChange = { otpInput = it },
                                            label = { Text("6-Digit OTP") },
                                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                            singleLine = true,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth().testTag("input_sync_otp")
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = { viewModel.verifyPhoneOtp(phoneInput, otpInput) },
                                            enabled = !loginInProgress,
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange),
                                            modifier = Modifier.fillMaxWidth().testTag("btn_verify_otp")
                                        ) {
                                            Text("Verify OTP & Connect Cloud", fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = { viewModel.requestPhoneOtp(phoneInput) },
                                            enabled = !loginInProgress,
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = DeepNavyBlue),
                                            modifier = Modifier.fillMaxWidth().testTag("btn_send_otp")
                                        ) {
                                            Text("Send Verification OTP", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                1 -> {
                                    // Email & Password Tab
                                    OutlinedTextField(
                                        value = emailInput,
                                        onValueChange = { emailInput = it },
                                        label = { Text("Email Address") },
                                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("input_sync_email")
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = passwordInput,
                                        onValueChange = { passwordInput = it },
                                        label = { Text("Password") },
                                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("input_sync_password")
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { viewModel.signInWithEmail(emailInput, passwordInput) },
                                        enabled = !loginInProgress,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = DeepNavyBlue),
                                        modifier = Modifier.fillMaxWidth().testTag("btn_email_signin")
                                    ) {
                                        Text("Sign In with Supabase", fontWeight = FontWeight.Bold)
                                    }
                                }
                                2 -> {
                                    // 1-Click Quick Connect
                                    Text(
                                        text = "Instant 1-Click Sync for this business profile. Creates a secure hardware-anchored cloud tenant immediately.",
                                        fontSize = 12.sp,
                                        color = Slate700,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                    Button(
                                        onClick = { viewModel.quickConnectAccount("hisabpro_merchant@cloud.hisabpro") },
                                        enabled = !loginInProgress,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                                        modifier = Modifier.fillMaxWidth().testTag("btn_quick_connect")
                                    ) {
                                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("1-Click Connect & Sync", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Architecture Security & Guarantee Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "HisabPro Cloud Guarantees",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        FeatureCheckRow(
                            title = "100% Offline-First",
                            desc = "Room is your local source of truth. Create invoices and bills anytime without internet."
                        )
                        FeatureCheckRow(
                            title = "Deterministic Accounting Safety",
                            desc = "No floating point rounding errors. Strict Double-Entry paise calculation and ledger safety."
                        )
                        FeatureCheckRow(
                            title = "Tenant Isolation via RLS",
                            desc = "PostgreSQL Row Level Security ensures your accounting records are strictly accessible by your business alone."
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureCheckRow(title: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = Emerald700,
            modifier = Modifier.size(18.dp).padding(top = 2.dp)
        )
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
