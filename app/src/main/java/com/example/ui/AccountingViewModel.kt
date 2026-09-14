package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.Business
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.data.model.InvoiceType
import com.example.data.model.Item
import com.example.data.model.Party
import com.example.data.model.PartyType
import com.example.data.model.Payment
import com.example.data.model.PaymentMode
import com.example.data.repository.AccountingRepository
import com.example.util.AppStrings
import com.example.util.IndianAccountingUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AccountingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AccountingRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AccountingRepository(database)
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    val business: StateFlow<Business?> = repository.primaryBusiness
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val parties: StateFlow<List<Party>> = repository.allParties
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val invoices: StateFlow<List<Invoice>> = repository.allInvoices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payments: StateFlow<List<Payment>> = repository.allPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val items: StateFlow<List<Item>> = repository.allItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayInvoices: StateFlow<List<Invoice>> = repository.getTodayInvoices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val journalEntries = repository.allJournalEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Language state: defaults to en, syncs with business language if present
    private val _currentLanguage = MutableStateFlow("en")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    fun setLanguage(lang: String) {
        _currentLanguage.value = lang
        viewModelScope.launch {
            val current = business.value
            if (current != null) {
                repository.saveBusiness(current.copy(language = lang))
            }
        }
    }

    fun getString(key: String): String {
        return AppStrings.get(key, _currentLanguage.value)
    }

    fun saveBusiness(
        name: String,
        phone: String,
        address: String,
        state: String,
        gstEnabled: Boolean,
        gstin: String,
        pan: String,
        isCompositionScheme: Boolean
    ) {
        viewModelScope.launch {
            val current = business.value
            val toSave = current?.copy(
                name = name,
                phone = phone,
                address = address,
                state = state,
                gstEnabled = gstEnabled,
                gstin = gstin,
                pan = pan,
                isCompositionScheme = isCompositionScheme
            ) ?: Business(
                name = name,
                phone = phone,
                address = address,
                state = state,
                gstEnabled = gstEnabled,
                gstin = gstin,
                pan = pan,
                isCompositionScheme = isCompositionScheme,
                language = _currentLanguage.value
            )
            repository.saveBusiness(toSave)
        }
    }

    fun toggleGstMode(enabled: Boolean) {
        viewModelScope.launch {
            val current = business.value ?: return@launch
            repository.saveBusiness(current.copy(gstEnabled = enabled))
        }
    }

    fun addParty(
        name: String,
        phone: String,
        address: String,
        gstin: String,
        type: PartyType,
        openingBalance: Double,
        isReceivable: Boolean // true = Dr (Customer owes us), false = Cr (We owe)
    ) {
        viewModelScope.launch {
            val balance = if (isReceivable) openingBalance else -openingBalance
            repository.addParty(
                Party(
                    name = name,
                    phone = phone,
                    address = address,
                    gstin = gstin,
                    type = type,
                    openingBalance = balance,
                    currentBalance = balance
                )
            )
        }
    }

    suspend fun getNextInvoiceNumber(): String {
        val seq = repository.getNextInvoiceSequence()
        return IndianAccountingUtils.generateInvoiceNumber(seq)
    }

    fun saveInvoice(
        invoiceNo: String,
        partyId: Long,
        partyName: String,
        items: List<InvoiceItem>,
        paidAmount: Double,
        paymentMode: PaymentMode,
        isGst: Boolean,
        notes: String,
        onSuccess: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val subtotal = items.sumOf { it.qty * it.rate - it.discount }
            var cgst = 0.0
            var sgst = 0.0
            var igst = 0.0

            if (isGst) {
                items.forEach { item ->
                    val lineSub = (item.qty * item.rate) - item.discount
                    cgst += lineSub * (item.cgstRate / 100.0)
                    sgst += lineSub * (item.sgstRate / 100.0)
                }
            }

            val total = subtotal + cgst + sgst + igst
            val invoice = Invoice(
                invoiceNo = invoiceNo,
                partyId = partyId,
                partyName = partyName,
                type = InvoiceType.SALE,
                subtotal = subtotal,
                cgst = cgst,
                sgst = sgst,
                igst = igst,
                total = total,
                paidAmount = paidAmount,
                paymentMode = paymentMode,
                notes = notes,
                isGst = isGst
            )

            val invoiceId = repository.saveInvoice(invoice, items)
            onSuccess(invoiceId)
        }
    }

    fun recordPayment(
        partyId: Long,
        partyName: String,
        amount: Double,
        mode: PaymentMode,
        referenceNo: String,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.recordPaymentReceived(
                partyId = partyId,
                partyName = partyName,
                amount = amount,
                mode = mode,
                referenceNo = referenceNo,
                notes = notes
            )
            onSuccess()
        }
    }

    fun addExpense(
        category: String,
        amount: Double,
        description: String,
        mode: PaymentMode,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.recordExpense(category, amount, description, mode)
            onSuccess()
        }
    }

    fun getPartyInvoices(partyId: Long) = repository.getInvoicesForParty(partyId)
    fun getPartyPayments(partyId: Long) = repository.getPaymentsForParty(partyId)
    suspend fun getInvoiceItems(invoiceId: Long): List<InvoiceItem> = repository.getItemsForInvoiceSync(invoiceId)
}
