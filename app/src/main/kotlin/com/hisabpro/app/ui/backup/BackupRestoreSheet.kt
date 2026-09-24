package com.hisabpro.app.ui.backup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Backward compatibility wrapper for BackupRestoreSheet.
 * Hosts the robust BackupScreen layout.
 */
@Composable
fun BackupRestoreSheet(
    viewModel: BackupViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize()
    ) {
        BackupScreen(
            viewModel = viewModel,
            onBack = onDismiss
        )
    }
}
