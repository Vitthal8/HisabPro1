package com.hisabpro.app.ui.purchases

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hisabpro.app.data.model.InvoiceStatus
import com.hisabpro.app.data.model.Item
import com.hisabpro.app.data.model.KhataEntry
import com.hisabpro.app.data.model.KhataEntryType
import com.hisabpro.app.data.model.Party
import com.hisabpro.app.data.model.PartyType
import com.hisabpro.app.data.model.PurchaseBill
import com.hisabpro.app.data.model.StockReason
import com.hisabpro.app.data.repository.ItemRepository
import com.hisabpro.app.data.repository.PartyRepository
import com.hisabpro.app.data.repository.PurchaseRepository
import com.hisabpro.app.data.repository.SettingsRepository
import com.hisabpro.app.ui.reports.ReportExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class PurchaseFilter(val label: String) {
    ALL("All Purchases"),
    UNPAID("Unpaid / Credit"),
    PAID("Paid"),
    ITC_ELIGIBLE("ITC Eligible")
}

data class PurchaseUiState(
    val purchases: List<PurchaseBill> = emptyList(),
    val filteredPurchases: List<PurchaseBill> = emptyList(),
    val totalPurchasesAmount: Double = 0.0,
    val totalItcAmount: Double = 0.0,
    val totalSupplierPayables: Double = 0.0,
    val totalBillCount: Int = 0,
    val selectedFilter: PurchaseFilter = PurchaseFilter.ALL,
    val searchQuery: String = "",
    val selectedPurchase: PurchaseBill? = null
)

class PurchaseViewModel(application: Application) : AndroidViewModel(application) {

    private val purchaseRepo = PurchaseRepository.getInstance(application.applicationContext)
    private val partyRepo = PartyRepository.getInstance(application.applicationContext)
    private val itemRepo = ItemRepository.getInstance(application.applicationContext)

    val suppliers: StateFlow<List<Party>> = partyRepo.parties
    val inventoryItems: StateFlow<List<Item>> = itemRepo.items

    private val _selectedFilter = MutableStateFlow(PurchaseFilter.ALL)
    val selectedFilter: StateFlow<PurchaseFilter> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedPurchase = MutableStateFlow<PurchaseBill?>(null)
    val selectedPurchase: StateFlow<PurchaseBill?> = _selectedPurchase.asStateFlow()

    val uiState: StateFlow<PurchaseUiState> = combine(
        purchaseRepo.purchases,
        _selectedFilter,
        _searchQuery,
        _selectedPurchase
    ) { purchases, filter, query, selected ->
        val totalPurchases = purchases.sumOf { it.grandTotal }
        val totalItc = purchases.filter { it.itcEligible }.sumOf { it.totalTax }
        val totalPayables = purchases.sumOf { it.dueAmount }

        val filtered = purchases.filter { bill ->
            val matchesFilter = when (filter) {
                PurchaseFilter.ALL -> true
                PurchaseFilter.UNPAID -> !bill.isFullyPaid
                PurchaseFilter.PAID -> bill.isFullyPaid
                PurchaseFilter.ITC_ELIGIBLE -> bill.itcEligible
            }

            val matchesQuery = if (query.isBlank()) true else {
                val q = query.trim().lowercase()
                bill.purchaseNumber.lowercase().contains(q) ||
                        bill.vendorBillNumber.lowercase().contains(q) ||
                        bill.supplierName.lowercase().contains(q) ||
                        bill.items.any { it.description.lowercase().contains(q) }
            }

            matchesFilter && matchesQuery
        }

        PurchaseUiState(
            purchases = purchases,
            filteredPurchases = filtered,
            totalPurchasesAmount = totalPurchases,
            totalItcAmount = totalItc,
            totalSupplierPayables = totalPayables,
            totalBillCount = purchases.size,
            selectedFilter = filter,
            searchQuery = query,
            selectedPurchase = selected
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PurchaseUiState()
    )

    fun setFilter(filter: PurchaseFilter) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectPurchase(bill: PurchaseBill?) {
        _selectedPurchase.value = bill
    }

    fun generateNextNumber(): String {
        return purchaseRepo.generateNextPurchaseNumber()
    }

    fun savePurchaseBill(bill: PurchaseBill) {
        viewModelScope.launch {
            purchaseRepo.addPurchase(bill)

            // 1. Automatically increment Inventory stock for purchased items
            bill.items.forEach { pItem ->
                if (pItem.itemId != null) {
                    itemRepo.adjustStock(
                        itemId = pItem.itemId,
                        changeQty = pItem.quantity,
                        reason = StockReason.PURCHASE_IN,
                        note = "Inward Purchase: ${bill.purchaseNumber} (${bill.supplierName})"
                    )
                } else {
                    // Try to match by item name
                    val existing = itemRepo.items.value.find {
                        it.name.equals(pItem.description, ignoreCase = true)
                    }
                    if (existing != null) {
                        itemRepo.adjustStock(
                            itemId = existing.id,
                            changeQty = pItem.quantity,
                            reason = StockReason.PURCHASE_IN,
                            note = "Inward Purchase: ${bill.purchaseNumber} (${bill.supplierName})"
                        )
                    }
                }
            }

            // 2. If unpaid / partial balance, record in Supplier's Khata (You Owe / Payable)
            if (bill.dueAmount > 0.01) {
                val supplier = bill.supplierId?.let { sId ->
                    partyRepo.parties.value.find { it.id == sId }
                } ?: partyRepo.parties.value.find {
                    it.name.equals(bill.supplierName, ignoreCase = true) && it.type == PartyType.SUPPLIER
                }

                if (supplier != null) {
                    partyRepo.addKhataEntry(
                        partyId = supplier.id,
                        amount = bill.dueAmount,
                        type = KhataEntryType.YOU_GOT, // In Supplier khata, YOU_GOT means goods received on credit = payable
                        dateMillis = bill.dateMillis,
                        billNumber = bill.purchaseNumber,
                        note = "Purchase Bill ${bill.vendorBillNumber.ifBlank { bill.purchaseNumber }}"
                    )
                }
            }
        }
    }

    fun markAsPaid(bill: PurchaseBill) {
        viewModelScope.launch {
            purchaseRepo.markAsPaid(bill.id, bill.grandTotal)

            // If linked to a supplier, record settlement in Party Khata
            val supplier = bill.supplierId?.let { sId ->
                partyRepo.parties.value.find { it.id == sId }
            }
            if (supplier != null && bill.dueAmount > 0.01) {
                partyRepo.addKhataEntry(
                    partyId = supplier.id,
                    amount = bill.dueAmount,
                    type = KhataEntryType.YOU_GAVE, // YOU_GAVE clears payable
                    dateMillis = System.currentTimeMillis(),
                    billNumber = bill.purchaseNumber,
                    note = "Payment cleared for ${bill.purchaseNumber}"
                )
            }
            _selectedPurchase.value = bill.copy(
                paidAmount = bill.grandTotal,
                paymentStatus = InvoiceStatus.PAID
            )
        }
    }

    fun deletePurchase(billId: String) {
        viewModelScope.launch {
            val bill = purchaseRepo.purchases.value.find { it.id == billId }
            if (bill != null) {
                // Revert stock additions
                bill.items.forEach { pItem ->
                    val matchedItem = pItem.itemId?.let { id ->
                        itemRepo.items.value.find { it.id == id }
                    } ?: itemRepo.items.value.find { it.name.equals(pItem.description, ignoreCase = true) }

                    if (matchedItem != null) {
                        itemRepo.adjustStock(
                            itemId = matchedItem.id,
                            changeQty = -pItem.quantity,
                            reason = StockReason.MANUAL_ADJUSTMENT,
                            note = "Reverted from Deleted Purchase Bill ${bill.purchaseNumber}"
                        )
                    }
                }
            }
            purchaseRepo.deletePurchase(billId)
            if (_selectedPurchase.value?.id == billId) {
                _selectedPurchase.value = null
            }
        }
    }

    fun exportPurchasesCsv(context: Context) {
        val businessName = SettingsRepository.getInstance(context).profile.value.shopName.ifBlank { "HisabPro Store" }
        val uri = ReportExporter.exportPurchasesRegisterCsv(context, uiState.value.filteredPurchases, businessName)
        if (uri != null) {
            ReportExporter.shareCsvFile(context, uri, "Purchases Register - $businessName")
        }
    }

    fun exportSingleBillCsv(context: Context, bill: PurchaseBill) {
        val businessName = SettingsRepository.getInstance(context).profile.value.shopName.ifBlank { "HisabPro Store" }
        val uri = ReportExporter.exportSinglePurchaseBillCsv(context, bill, businessName)
        if (uri != null) {
            ReportExporter.shareCsvFile(context, uri, "Purchase Bill - ${bill.purchaseNumber}")
        }
    }
}
