package com.hisabpro.app.ui.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hisabpro.app.data.backup.BackupConstants
import com.hisabpro.app.data.backup.BackupCounts
import com.hisabpro.app.data.backup.BackupManager
import com.hisabpro.app.data.backup.LocalBackupFile
import com.hisabpro.app.ui.theme.DeepNavyBlue
import com.hisabpro.app.ui.theme.DeepNavyLight
import com.hisabpro.app.ui.theme.Emerald700
import com.hisabpro.app.ui.theme.Emerald800
import com.hisabpro.app.ui.theme.IncomeGreen
import com.hisabpro.app.ui.theme.PureWhite
import com.hisabpro.app.ui.theme.ExpenseRed
import com.hisabpro.app.ui.theme.ExpenseRedDark
import com.hisabpro.app.ui.theme.SaffronLight
import com.hisabpro.app.ui.theme.SaffronOrange
import com.hisabpro.app.ui.theme.Slate200
import com.hisabpro.app.ui.theme.Slate400
import com.hisabpro.app.ui.theme.Slate600
import com.hisabpro.app.ui.theme.Slate700
import com.hisabpro.app.ui.theme.Slate800
import com.hisabpro.app.ui.theme.Slate900
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BackupViewModel = viewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var fileToExport by remember { mutableStateOf<File?>(null) }
    var fileToDelete by remember { mutableStateOf<File?>(null) }

    // SAF Document Picker for Restore
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onBackupFilePicked(uri)
        }
    }

    // SAF Document Creator for Saving Backup to user's device
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null && fileToExport != null) {
            viewModel.exportBackupToUri(fileToExport!!, uri)
            fileToExport = null
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = SaffronLight,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = SaffronOrange,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Local Backup & Restore",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Slate900
                        )
                        Text(
                            text = "Offline-first • 100% Data Ownership",
                            fontSize = 12.sp,
                            color = Slate600
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Slate600
                    )
                }
            }

            // Tabs
            TabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = DeepNavyBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab]),
                        color = SaffronOrange,
                        height = 3.dp
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.setSelectedTab(0) },
                    text = {
                        Text(
                            "Create & Manage",
                            fontWeight = if (uiState.selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    },
                    icon = {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.setSelectedTab(1) },
                    text = {
                        Text(
                            "Restore Data",
                            fontWeight = if (uiState.selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    },
                    icon = {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                )
            }

            // Status or Error Banners
            if (!uiState.statusMessage.isNullOrBlank()) {
                Surface(
                    color = Emerald700.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald700, modifier = Modifier.size(20.dp))
                        Text(
                            text = uiState.statusMessage!!,
                            color = Emerald800,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearStatus() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Emerald700, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            if (!uiState.errorMessage.isNullOrBlank()) {
                Surface(
                    color = ExpenseRed.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(20.dp))
                        Text(
                            text = uiState.errorMessage!!,
                            color = ExpenseRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearStatus() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = ExpenseRed, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Progress Banner
            if (uiState.isLoading) {
                Surface(
                    color = DeepNavyLight,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = DeepNavyBlue,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = uiState.progressMessage.ifBlank { "Processing..." },
                            color = DeepNavyBlue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Tab Content
            when (uiState.selectedTab) {
                0 -> CreateBackupTabContent(
                    uiState = uiState,
                    onCreateBackup = { shareAfter -> viewModel.createBackup(onSuccessShare = shareAfter) },
                    onShareFile = { file -> viewModel.shareBackupFile(file) },
                    onSaveFile = { file ->
                        fileToExport = file
                        createDocumentLauncher.launch(file.name)
                    },
                    onRestoreFile = { file -> viewModel.onLocalBackupSelectedForRestore(file) },
                    onDeleteFile = { file -> fileToDelete = file },
                    onRefresh = { viewModel.loadLocalBackups() }
                )
                1 -> RestoreBackupTabContent(
                    uiState = uiState,
                    onPickFile = {
                        openDocumentLauncher.launch(arrayOf("*/*", "application/json", "application/octet-stream"))
                    },
                    onRestoreLocalFile = { file -> viewModel.onLocalBackupSelectedForRestore(file) }
                )
            }
        }
    }

    // Pre-Restore Confirmation Dialog (Prevents Accidental Destructive Restore)
    if (uiState.showConfirmRestoreDialog && uiState.pendingRestore != null) {
        val summary = uiState.pendingRestore!!.summary
        AlertDialog(
            onDismissRequest = { viewModel.dismissConfirmDialog() },
            icon = {
                Icon(Icons.Default.Warning, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(36.dp))
            },
            title = {
                Text(
                    text = "Confirm Database Restore",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Restoring this backup will replace current local data with records from the selected backup file.",
                        fontSize = 13.sp,
                        color = Slate700
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "📦 ${summary.businessName.ifBlank { "HisabPro Business" }}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Slate900
                            )
                            Text(
                                text = "📅 Backup Date: ${summary.createdAtFormatted}",
                                fontSize = 12.sp,
                                color = Slate700
                            )
                            Text(
                                text = "📊 Records to restore: ${summary.counts.invoices} Invoices • ${summary.counts.parties} Parties • ${summary.counts.items} Items • ${summary.counts.payments} Payments • ${summary.counts.expenses} Expenses",
                                fontSize = 11.sp,
                                color = DeepNavyBlue,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Safety Backup Checkbox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setCreateSafetyBackup(!uiState.createSafetyBackup) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = uiState.createSafetyBackup,
                            onCheckedChange = { viewModel.setCreateSafetyBackup(it) },
                            colors = CheckboxDefaults.colors(checkedColor = DeepNavyBlue)
                        )
                        Text(
                            text = "Create automatic safety backup of current data first (Recommended)",
                            fontSize = 12.sp,
                            color = Slate800,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = "⚠️ This operation is atomic: if any problem occurs during restore, your current data is safely kept intact without any loss.",
                        fontSize = 11.sp,
                        color = Emerald800
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmAndExecuteRestore() },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                    modifier = Modifier.testTag("btn_confirm_restore_backup")
                ) {
                    Text("Confirm & Restore", color = PureWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissConfirmDialog() }) {
                    Text("Cancel", color = Slate700)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (fileToDelete != null) {
        val file = fileToDelete!!
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("Delete Backup?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete backup '${file.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBackupFile(file)
                        fileToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Delete", color = PureWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CreateBackupTabContent(
    uiState: BackupUiState,
    onCreateBackup: (shareAfter: Boolean) -> Unit,
    onShareFile: (File) -> Unit,
    onSaveFile: (File) -> Unit,
    onRestoreFile: (File) -> Unit,
    onDeleteFile: (File) -> Unit,
    onRefresh: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Primary Action Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Create Local Backup",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Slate900
                            )
                            Text(
                                text = "Includes all businesses, parties, items, bills & settings",
                                fontSize = 11.sp,
                                color = Slate600
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = null,
                            tint = SaffronOrange,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onCreateBackup(false) },
                            enabled = !uiState.isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = DeepNavyBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_create_backup")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create Backup", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = { onCreateBackup(true) },
                            enabled = !uiState.isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_create_and_share_backup")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share Backup", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Section: Existing Local Backups
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SAVED BACKUPS ON THIS DEVICE (${uiState.localBackups.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate700
                )
                IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh list", tint = Slate700, modifier = Modifier.size(16.dp))
                }
            }
        }

        if (uiState.localBackups.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = Slate600, modifier = Modifier.size(32.dp))
                        Text("No local backups found", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Slate800)
                        Text(
                            "Tap 'Create Backup' above to create your first encrypted, versioned HisabPro backup.",
                            fontSize = 12.sp,
                            color = Slate600,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(uiState.localBackups) { backupFile ->
                BackupFileItemCard(
                    backup = backupFile,
                    onShare = { onShareFile(backupFile.file) },
                    onSave = { onSaveFile(backupFile.file) },
                    onRestore = { onRestoreFile(backupFile.file) },
                    onDelete = { onDeleteFile(backupFile.file) }
                )
            }
        }
    }
}

@Composable
private fun RestoreBackupTabContent(
    uiState: BackupUiState,
    onPickFile: () -> Unit,
    onRestoreLocalFile: (File) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = DeepNavyLight,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = DeepNavyBlue, modifier = Modifier.size(20.dp))
                            }
                        }
                        Column {
                            Text("Select Backup File", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Slate900)
                            Text("Pick .hisabpro or .json from device, Drive, WhatsApp or Downloads", fontSize = 11.sp, color = Slate600)
                        }
                    }

                    Button(
                        onClick = onPickFile,
                        enabled = !uiState.isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("btn_select_backup_file")
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose Backup File to Restore", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Information & Safety Features
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "🛡️ HisabPro Safe Restore Protection",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Slate800
                    )
                    Text(
                        text = "1. Pre-validation checks format, schema version & SHA-256 integrity.\n" +
                                "2. An automatic safety backup is generated before any replacement.\n" +
                                "3. Room transactions ensure that if any step fails, the entire database rolls back and no existing data is ever corrupted.",
                        fontSize = 11.5.sp,
                        color = Slate700,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Quick restore from existing local files
        if (uiState.localBackups.isNotEmpty()) {
            item {
                Text(
                    text = "OR RESTORE FROM LOCAL BACKUP HISTORY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate700,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(uiState.localBackups) { backupFile ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = backupFile.summary?.businessName?.ifBlank { backupFile.fileName } ?: backupFile.fileName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Slate900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ENGLISH).format(Date(backupFile.lastModifiedMillis))
                            Text(
                                text = "$dateStr • ${formatFileSize(backupFile.fileSizeBytes)}",
                                fontSize = 11.sp,
                                color = Slate600
                            )
                        }

                        Button(
                            onClick = { onRestoreLocalFile(backupFile.file) },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepNavyBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Restore", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackupFileItemCard(
    backup: LocalBackupFile,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = backup.summary?.businessName?.ifBlank { backup.fileName } ?: backup.fileName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val dateFormatted = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH).format(Date(backup.lastModifiedMillis))
                    Text(
                        text = "$dateFormatted • ${formatFileSize(backup.fileSizeBytes)}",
                        fontSize = 11.5.sp,
                        color = Slate600
                    )
                }

                if (backup.fileName.contains("SafetyBackup")) {
                    Surface(
                        color = Emerald700.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Safety Auto-Backup",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald800,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (backup.summary != null) {
                val c = backup.summary.counts
                Text(
                    text = "${c.invoices} Invoices • ${c.parties} Parties • ${c.items} Items • ${c.payments} Payments",
                    fontSize = 11.sp,
                    color = DeepNavyBlue,
                    fontWeight = FontWeight.Medium
                )
            }

            HorizontalDivider(color = Slate200, thickness = 0.8.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = DeepNavyBlue, modifier = Modifier.size(18.dp))
                }

                IconButton(onClick = onSave, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Save to device", tint = Slate700, modifier = Modifier.size(18.dp))
                }

                IconButton(onClick = onRestore, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Restore, contentDescription = "Restore", tint = Emerald700, modifier = Modifier.size(18.dp))
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ExpenseRed, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.ENGLISH, "%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format(Locale.ENGLISH, "%.1f MB", mb)
}
