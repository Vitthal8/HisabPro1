package com.hisabpro.app.ui.backup

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hisabpro.app.data.backup.BackupConstants
import com.hisabpro.app.data.backup.BackupCounts
import com.hisabpro.app.data.backup.BackupManager
import com.hisabpro.app.data.backup.BackupRestoreResult
import com.hisabpro.app.data.backup.BackupSummary
import com.hisabpro.app.data.backup.BackupValidationResult
import com.hisabpro.app.data.backup.LocalBackupFile
import com.hisabpro.app.data.repository.InvoiceRepository
import com.hisabpro.app.data.repository.PartyRepository
import com.hisabpro.app.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class BackupUiState(
    val isLoading: Boolean = false,
    val progressMessage: String = "",
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val pendingRestore: BackupValidationResult.Valid? = null,
    val showConfirmRestoreDialog: Boolean = false,
    val createSafetyBackup: Boolean = true,
    val localBackups: List<LocalBackupFile> = emptyList(),
    val selectedTab: Int = 0, // 0 = Backup & Local Files, 1 = Restore
    val lastCreatedBackup: File? = null,
    val restoredCounts: BackupCounts? = null
)

class BackupViewModel(application: Application) : AndroidViewModel(application) {

    private val backupManager = BackupManager.getInstance(application)

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    init {
        loadLocalBackups()
    }

    fun setSelectedTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun setCreateSafetyBackup(enabled: Boolean) {
        _uiState.update { it.copy(createSafetyBackup = enabled) }
    }

    fun loadLocalBackups() {
        viewModelScope.launch {
            val list = backupManager.listLocalBackups()
            _uiState.update { it.copy(localBackups = list) }
        }
    }

    /**
     * Creates a complete local backup of all HisabPro business data.
     */
    fun createBackup(onSuccessShare: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    progressMessage = "Preparing business backup...",
                    statusMessage = null,
                    errorMessage = null
                )
            }

            val result = backupManager.createBackup(
                isSafetyBackup = false,
                onProgress = { msg ->
                    _uiState.update { it.copy(progressMessage = msg) }
                }
            )

            result.fold(
                onSuccess = { file ->
                    loadLocalBackups()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            progressMessage = "",
                            statusMessage = "Backup created successfully: ${file.name}",
                            lastCreatedBackup = file
                        )
                    }
                    if (onSuccessShare) {
                        shareBackupFile(file)
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            progressMessage = "",
                            errorMessage = "Backup failed: ${error.localizedMessage ?: "Unknown error"}"
                        )
                    }
                }
            )
        }
    }

    /**
     * Validates a chosen backup file (from Uri) and prompts the confirmation dialog if valid.
     */
    fun onBackupFilePicked(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    progressMessage = "Validating backup integrity and version...",
                    statusMessage = null,
                    errorMessage = null
                )
            }

            when (val validation = backupManager.validateBackupUri(uri)) {
                is BackupValidationResult.Valid -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            progressMessage = "",
                            pendingRestore = validation,
                            showConfirmRestoreDialog = true
                        )
                    }
                }
                is BackupValidationResult.IncompatibleVersion -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            progressMessage = "",
                            errorMessage = validation.message
                        )
                    }
                }
                is BackupValidationResult.Corrupted -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            progressMessage = "",
                            errorMessage = "Cannot restore file: ${validation.reason}"
                        )
                    }
                }
                is BackupValidationResult.InvalidFormat -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            progressMessage = "",
                            errorMessage = "Invalid backup format: ${validation.reason}"
                        )
                    }
                }
            }
        }
    }

    /**
     * Validates a chosen backup from local storage files list.
     */
    fun onLocalBackupSelectedForRestore(file: File) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    progressMessage = "Validating local backup ${file.name}...",
                    statusMessage = null,
                    errorMessage = null
                )
            }

            when (val validation = backupManager.validateBackupFile(file)) {
                is BackupValidationResult.Valid -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            progressMessage = "",
                            pendingRestore = validation,
                            showConfirmRestoreDialog = true
                        )
                    }
                }
                is BackupValidationResult.IncompatibleVersion -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            progressMessage = "",
                            errorMessage = validation.message
                        )
                    }
                }
                is BackupValidationResult.Corrupted -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            progressMessage = "",
                            errorMessage = "Cannot restore file: ${validation.reason}"
                        )
                    }
                }
                is BackupValidationResult.InvalidFormat -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            progressMessage = "",
                            errorMessage = "Invalid backup format: ${validation.reason}"
                        )
                    }
                }
            }
        }
    }

    /**
     * Confirms and performs the transactional restore.
     */
    fun confirmAndExecuteRestore() {
        val pending = _uiState.value.pendingRestore ?: return
        val createSafety = _uiState.value.createSafetyBackup

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    showConfirmRestoreDialog = false,
                    isLoading = true,
                    progressMessage = "Starting safe restore...",
                    statusMessage = null,
                    errorMessage = null
                )
            }

            val result = backupManager.restoreBackup(
                payload = pending.payload,
                createSafetyBackupFirst = createSafety,
                onProgress = { msg ->
                    _uiState.update { it.copy(progressMessage = msg) }
                }
            )

            when (result) {
                is BackupRestoreResult.Success -> {
                    loadLocalBackups()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            progressMessage = "",
                            statusMessage = "Restore successful! ${result.message}",
                            pendingRestore = null,
                            restoredCounts = result.restoredCounts
                        )
                    }
                }
                is BackupRestoreResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            progressMessage = "",
                            errorMessage = result.message,
                            pendingRestore = null
                        )
                    }
                }
            }
        }
    }

    fun dismissConfirmDialog() {
        _uiState.update {
            it.copy(
                showConfirmRestoreDialog = false,
                pendingRestore = null
            )
        }
    }

    fun shareBackupFile(file: File) {
        backupManager.shareBackupFile(getApplication(), file)
    }

    fun exportBackupToUri(sourceFile: File, destinationUri: Uri) {
        viewModelScope.launch {
            val success = backupManager.exportBackupToUri(sourceFile, destinationUri)
            if (success) {
                _uiState.update { it.copy(statusMessage = "Backup saved to device successfully.") }
            } else {
                _uiState.update { it.copy(errorMessage = "Failed to write backup file to chosen location.") }
            }
        }
    }

    fun deleteBackupFile(file: File) {
        viewModelScope.launch {
            val deleted = backupManager.deleteBackupFile(file)
            if (deleted) {
                loadLocalBackups()
                _uiState.update { it.copy(statusMessage = "Backup ${file.name} deleted.") }
            } else {
                _uiState.update { it.copy(errorMessage = "Could not delete backup file.") }
            }
        }
    }

    fun clearStatus() {
        _uiState.update {
            it.copy(
                statusMessage = null,
                errorMessage = null,
                restoredCounts = null
            )
        }
    }
}
