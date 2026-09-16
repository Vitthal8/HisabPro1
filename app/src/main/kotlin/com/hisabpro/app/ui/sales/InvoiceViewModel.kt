package com.hisabpro.app.ui.sales

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hisabpro.app.data.model.GstMode
import com.hisabpro.app.data.model.Invoice
import com.hisabpro.app.data.model.InvoiceItem
import com.hisabpro.app.data.model.InvoiceStatus
import com.hisabpro.app.data.model.InvoiceType
import com.hisabpro.app.data.repository.InvoiceRepository
import com.hisabpro.app.data.repository.ItemRepository
import com.hisabpro.app.data.repository.PartyRepository
import com.hisabpro.app.data.repository.SettingsRepository
import com.hisabpro.app.util.InvoicePdfGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SalesUiState(
    val invoices: List<Invoice> = emptyList(),
    val filteredInvoices: List<Invoice> = emptyList(),
    val totalSalesVolume: Double = 0.0,
    val totalTaxCollected: Double = 0.0,
    val totalPendingDue: Double = 0.0,
    val totalPaidSales: Double = 0.0,
    val searchQuery: String = "",
    val typeFilter: InvoiceType? = null,
    val statusFilter: InvoiceStatus? = null,
    val selectedInvoice: Invoice? = null
)

class InvoiceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = InvoiceRepository.getInstance(application.applicationContext)
    private val partyRepository = PartyRepository.getInstance(application.applicationContext)
    private val itemRepository = ItemRepository.getInstance(application.applicationContext)

    private val _searchQuery = MutableStateFlow("")
    private val _typeFilter = MutableStateFlow<InvoiceType?>(null)
    private val _statusFilter = MutableStateFlow<InvoiceStatus?>(null)
    private val _selectedInvoiceId = MutableStateFlow<String?>(null)

    val parties = partyRepository.parties

    val uiState: StateFlow<SalesUiState> = combine(
        repository.invoices,
        _searchQuery,
        _typeFilter,
        _statusFilter,
        _selectedInvoiceId
    ) { invoices, query, typeFilter, statusFilter, selectedId ->
        var totalSales = 0.0
        var totalTax = 0.0
        var totalDue = 0.0
        var totalPaid = 0.0

        for (inv in invoices) {
            // Include tax invoices and bills in sales metrics (quotations are estimates)
            if (inv.type != InvoiceType.PROFORMA) {
                totalSales += inv.grandTotal
                totalTax += inv.totalTax
                totalDue += inv.dueAmount
                totalPaid += inv.paidAmount
            }
        }

        val filtered = invoices.filter { inv ->
            val matchesType = typeFilter == null || inv.type == typeFilter
            val matchesStatus = statusFilter == null || inv.paymentStatus == statusFilter
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                inv.invoiceNumber.contains(query, ignoreCase = true) ||
                        inv.customerName.contains(query, ignoreCase = true) ||
                        inv.customerPhone.contains(query, ignoreCase = true) ||
                        inv.items.any { it.description.contains(query, ignoreCase = true) }
            }
            matchesType && matchesStatus && matchesQuery
        }

        val selected = invoices.find { it.id == selectedId }

        SalesUiState(
            invoices = invoices,
            filteredInvoices = filtered,
            totalSalesVolume = totalSales,
            totalTaxCollected = totalTax,
            totalPendingDue = totalDue,
            totalPaidSales = totalPaid,
            searchQuery = query,
            typeFilter = typeFilter,
            statusFilter = statusFilter,
            selectedInvoice = selected
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SalesUiState()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTypeFilter(type: InvoiceType?) {
        _typeFilter.value = type
    }

    fun setStatusFilter(status: InvoiceStatus?) {
        _statusFilter.value = status
    }

    fun selectInvoice(invoice: Invoice?) {
        _selectedInvoiceId.value = invoice?.id
    }

    fun getNextInvoiceNumber(type: InvoiceType, prefixOverride: String? = null): String {
        return repository.generateNextInvoiceNumber(type, prefixOverride)
    }

    fun createInvoice(invoice: Invoice): Invoice {
        val saved = repository.addInvoice(invoice)
        // If customer exists in Khata and there is pending credit due, optionally update party ledger
        if (!invoice.customerId.isNullOrBlank() && invoice.dueAmount > 0) {
            partyRepository.addKhataEntry(
                partyId = invoice.customerId,
                amount = invoice.dueAmount,
                type = com.hisabpro.app.data.model.KhataEntryType.YOU_GAVE,
                dateMillis = invoice.dateMillis,
                billNumber = invoice.invoiceNumber,
                note = "Sale Invoice #${invoice.invoiceNumber} credit balance"
            )
        }
        return saved
    }

    fun updateInvoice(invoice: Invoice) {
        repository.updateInvoice(invoice)
        _selectedInvoiceId.value = invoice.id
    }

    fun duplicateInvoice(invoiceId: String): Invoice? {
        val dup = repository.duplicateInvoice(invoiceId)
        if (dup != null) {
            _selectedInvoiceId.value = dup.id
        }
        return dup
    }

    fun markAsPaid(invoice: Invoice) {
        val updated = invoice.copy(
            paidAmount = invoice.grandTotal,
            paymentStatus = InvoiceStatus.PAID
        )
        repository.updateInvoice(updated)
        _selectedInvoiceId.value = updated.id
    }

    fun deleteInvoice(invoiceId: String) {
        val invoice = repository.invoices.value.find { it.id == invoiceId }
        if (invoice != null) {
            invoice.items.forEach { lineItem ->
                itemRepository.restoreStockForInvoiceItem(
                    itemNameOrId = lineItem.description,
                    quantity = lineItem.quantity,
                    invoiceNumber = invoice.invoiceNumber
                )
            }
        }
        repository.deleteInvoice(invoiceId)
        if (_selectedInvoiceId.value == invoiceId) {
            _selectedInvoiceId.value = null
        }
    }

    fun sharePdf(context: Context, invoice: Invoice, targetWhatsApp: Boolean = false) {
        val profile = SettingsRepository.getInstance(context).profile.value
        InvoicePdfGenerator.sharePdf(context, invoice, targetWhatsApp, profile)
    }

    fun shareWhatsAppSummary(context: Context, invoice: Invoice) {
        val profile = SettingsRepository.getInstance(context).profile.value
        InvoicePdfGenerator.sharePdf(context, invoice, targetWhatsApp = true, profile)
    }
}
