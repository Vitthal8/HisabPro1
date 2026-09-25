package com.hisabpro.app.ui.sales

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
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
import com.hisabpro.app.data.repository.TransactionRepository
import com.hisabpro.app.domain.accounting.AccountingEngine
import com.hisabpro.app.domain.usecase.CreateInvoiceUseCase
import com.hisabpro.app.domain.usecase.DeleteInvoiceUseCase
import com.hisabpro.app.domain.usecase.UpdateInvoiceUseCase
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

class InvoiceViewModel @JvmOverloads constructor(
    application: Application,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
) : AndroidViewModel(application) {

    companion object {
        private const val KEY_SELECTED_INVOICE_ID = "key_selected_invoice_id"
    }

    private val repository = InvoiceRepository.getInstance(application.applicationContext)
    private val partyRepository = PartyRepository.getInstance(application.applicationContext)
    private val itemRepository = ItemRepository.getInstance(application.applicationContext)
    private val transactionRepository = TransactionRepository.getInstance(application.applicationContext)

    // Domain Use Cases
    private val createInvoiceUseCase = CreateInvoiceUseCase(
        invoiceRepository = repository,
        itemRepository = itemRepository,
        partyRepository = partyRepository,
        transactionRepository = transactionRepository
    )
    private val deleteInvoiceUseCase = DeleteInvoiceUseCase(
        invoiceRepository = repository,
        itemRepository = itemRepository
    )
    private val updateInvoiceUseCase = UpdateInvoiceUseCase(
        invoiceRepository = repository,
        itemRepository = itemRepository
    )

    private val _searchQuery = MutableStateFlow("")
    private val _typeFilter = MutableStateFlow<InvoiceType?>(null)
    private val _statusFilter = MutableStateFlow<InvoiceStatus?>(null)
    private val _selectedInvoiceId = savedStateHandle.getStateFlow<String?>(KEY_SELECTED_INVOICE_ID, null)

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
            totalSalesVolume = AccountingEngine.roundToTwoDecimals(totalSales),
            totalTaxCollected = AccountingEngine.roundToTwoDecimals(totalTax),
            totalPendingDue = AccountingEngine.roundToTwoDecimals(totalDue),
            totalPaidSales = AccountingEngine.roundToTwoDecimals(totalPaid),
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
        savedStateHandle[KEY_SELECTED_INVOICE_ID] = invoice?.id
    }

    fun getNextInvoiceNumber(type: InvoiceType, prefixOverride: String? = null): String {
        return repository.generateNextInvoiceNumber(type, prefixOverride)
    }

    fun createInvoice(invoice: Invoice): Invoice {
        return createInvoiceUseCase.execute(invoice).getOrElse {
            repository.addInvoice(invoice)
        }
    }

    fun updateInvoice(invoice: Invoice) {
        updateInvoiceUseCase.execute(invoice)
        savedStateHandle[KEY_SELECTED_INVOICE_ID] = invoice.id
    }

    fun duplicateInvoice(invoiceId: String): Invoice? {
        val dup = repository.duplicateInvoice(invoiceId)
        if (dup != null) {
            savedStateHandle[KEY_SELECTED_INVOICE_ID] = dup.id
        }
        return dup
    }

    fun markAsPaid(invoice: Invoice) {
        val updated = invoice.copy(
            paidAmount = invoice.grandTotal,
            paymentStatus = InvoiceStatus.PAID
        )
        repository.updateInvoice(updated)
        savedStateHandle[KEY_SELECTED_INVOICE_ID] = updated.id
    }

    fun deleteInvoice(invoiceId: String) {
        deleteInvoiceUseCase.execute(invoiceId)
        if (savedStateHandle.get<String?>(KEY_SELECTED_INVOICE_ID) == invoiceId) {
            savedStateHandle[KEY_SELECTED_INVOICE_ID] = null
        }
    }

    fun sharePdf(context: Context, invoice: Invoice, targetWhatsApp: Boolean = false) {
        val profile = SettingsRepository.getInstance(context).profile.value
        InvoicePdfGenerator.sharePdf(context, invoice, targetWhatsApp, profile)
    }

    fun printPdf(context: Context, invoice: Invoice) {
        val profile = SettingsRepository.getInstance(context).profile.value
        InvoicePdfGenerator.printPdf(context, invoice, profile)
    }

    fun shareWhatsAppSummary(context: Context, invoice: Invoice) {
        val profile = SettingsRepository.getInstance(context).profile.value
        InvoicePdfGenerator.sharePdf(context, invoice, targetWhatsApp = true, profile)
    }
}
