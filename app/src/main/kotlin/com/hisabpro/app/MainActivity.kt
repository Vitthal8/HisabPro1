package com.hisabpro.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.hisabpro.app.ui.HisabViewModel
import com.hisabpro.app.ui.MainScreen
import com.hisabpro.app.ui.party.PartyViewModel
import com.hisabpro.app.ui.theme.HisabProTheme
import java.io.File

class MainActivity : ComponentActivity() {

    private val hisabViewModel: HisabViewModel by viewModels()
    private val partyViewModel: PartyViewModel by viewModels()
    private val invoiceViewModel: com.hisabpro.app.ui.sales.InvoiceViewModel by viewModels()
    private val itemViewModel: com.hisabpro.app.ui.items.ItemViewModel by viewModels()
    private val reportsViewModel: com.hisabpro.app.ui.reports.ReportsViewModel by viewModels()
    private val purchaseViewModel: com.hisabpro.app.ui.purchases.PurchaseViewModel by viewModels()
    private val backupViewModel: com.hisabpro.app.ui.backup.BackupViewModel by viewModels()

    private var pendingFileToExport: File? = null

    // Register SAF launchers at Activity initialization (legal lifecycle state)
    private val restoreFilePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            backupViewModel.onBackupFilePicked(uri)
        }
    }

    private val saveDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        val file = pendingFileToExport
        if (uri != null && file != null) {
            backupViewModel.exportBackupToUri(file, uri)
            pendingFileToExport = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HisabProTheme {
                MainScreen(
                    partyViewModel = partyViewModel,
                    hisabViewModel = hisabViewModel,
                    invoiceViewModel = invoiceViewModel,
                    itemViewModel = itemViewModel,
                    reportsViewModel = reportsViewModel,
                    purchaseViewModel = purchaseViewModel,
                    backupViewModel = backupViewModel,
                    onPickBackupFile = {
                        try {
                            restoreFilePickerLauncher.launch(arrayOf("*/*", "application/json", "application/octet-stream"))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    onSaveBackupToUri = { file ->
                        try {
                            pendingFileToExport = file
                            saveDocumentLauncher.launch(file.name)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                )
            }
        }
    }
}
