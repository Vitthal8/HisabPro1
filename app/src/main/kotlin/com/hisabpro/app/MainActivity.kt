package com.hisabpro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.hisabpro.app.ui.HisabViewModel
import com.hisabpro.app.ui.MainScreen
import com.hisabpro.app.ui.party.PartyViewModel
import com.hisabpro.app.ui.theme.HisabProTheme

class MainActivity : ComponentActivity() {

    private val hisabViewModel: HisabViewModel by viewModels()
    private val partyViewModel: PartyViewModel by viewModels()
    private val invoiceViewModel: com.hisabpro.app.ui.sales.InvoiceViewModel by viewModels()
    private val itemViewModel: com.hisabpro.app.ui.items.ItemViewModel by viewModels()
    private val reportsViewModel: com.hisabpro.app.ui.reports.ReportsViewModel by viewModels()
    private val purchaseViewModel: com.hisabpro.app.ui.purchases.PurchaseViewModel by viewModels()

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
                    purchaseViewModel = purchaseViewModel
                )
            }
        }
    }
}
