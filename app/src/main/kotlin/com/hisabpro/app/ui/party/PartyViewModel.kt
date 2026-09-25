package com.hisabpro.app.ui.party

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.hisabpro.app.data.model.Category
import com.hisabpro.app.data.model.KhataEntry
import com.hisabpro.app.data.model.KhataEntryType
import com.hisabpro.app.data.model.Party
import com.hisabpro.app.data.model.PartyTag
import com.hisabpro.app.data.model.PartyType
import com.hisabpro.app.data.model.PartyWithBalance
import com.hisabpro.app.data.model.PaymentMode
import com.hisabpro.app.data.model.TransactionType
import com.hisabpro.app.data.repository.InvoiceRepository
import com.hisabpro.app.data.repository.PartyRepository
import com.hisabpro.app.data.repository.PurchaseRepository
import com.hisabpro.app.data.repository.TransactionRepository
import com.hisabpro.app.ui.payments.PaymentDirection
import com.hisabpro.app.util.PartyStatementPdfGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.io.File

enum class PartySortOption(val label: String) {
    MOST_DUE("Highest Balance First"),
    RECENT("Recent Activity"),
    NAME_ASC("Name (A to Z)"),
    SETTLED("Zero Balance / Settled")
}

data class PartyUiState(
    val parties: List<PartyWithBalance> = emptyList(),
    val filteredParties: List<PartyWithBalance> = emptyList(),
    val totalReceivable: Double = 0.0, // You'll Get
    val totalPayable: Double = 0.0,    // You'll Give
    val searchQuery: String = "",
    val typeFilter: PartyType? = null,
    val tagFilter: PartyTag? = null,
    val sortOption: PartySortOption = PartySortOption.MOST_DUE,
    val selectedParty: PartyWithBalance? = null,
    val selectedPartyEntries: List<KhataEntry> = emptyList()
)

class PartyViewModel @JvmOverloads constructor(
    application: Application,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
) : AndroidViewModel(application) {

    companion object {
        private const val KEY_SELECTED_PARTY_ID = "key_selected_party_id"
    }

    private val repository = PartyRepository.getInstance(application.applicationContext)
    private val invoiceRepository = InvoiceRepository.getInstance(application.applicationContext)
    private val purchaseRepository = PurchaseRepository.getInstance(application.applicationContext)
    private val transactionRepository = TransactionRepository.getInstance(application.applicationContext)

    // Domain Use Case
    private val recordPaymentUseCase = com.hisabpro.app.domain.usecase.RecordPaymentUseCase(
        partyRepository = repository,
        transactionRepository = transactionRepository,
        invoiceRepository = invoiceRepository
    )

    private val _searchQuery = MutableStateFlow("")
    private val _typeFilter = MutableStateFlow<PartyType?>(null)
    private val _tagFilter = MutableStateFlow<PartyTag?>(null)
    private val _sortOption = MutableStateFlow(PartySortOption.MOST_DUE)
    private val _selectedPartyId = savedStateHandle.getStateFlow<String?>(KEY_SELECTED_PARTY_ID, null)

    private data class FilterParams(
        val query: String,
        val typeFilter: PartyType?,
        val tagFilter: PartyTag?,
        val sortOption: PartySortOption,
        val selectedPartyId: String?
    )

    private val _filterParams = combine(
        _searchQuery,
        _typeFilter,
        _tagFilter,
        _sortOption,
        _selectedPartyId
    ) { query, typeFilter, tagFilter, sortOption, selectedPartyId ->
        FilterParams(query, typeFilter, tagFilter, sortOption, selectedPartyId)
    }

    val uiState: StateFlow<PartyUiState> = combine(
        repository.parties,
        repository.entries,
        _filterParams
    ) { parties, entries, filter ->
        val query = filter.query
        val typeFilter = filter.typeFilter
        val tagFilter = filter.tagFilter
        val sortOption = filter.sortOption
        val selectedPartyId = filter.selectedPartyId
        // Calculate balance for each party
        val partiesWithBalance = parties.map { party ->
            val partyEntries = entries.filter { it.partyId == party.id }
            var totalGave = 0.0
            var totalGot = 0.0
            var lastDate: Long? = null

            for (entry in partyEntries) {
                if (entry.type == KhataEntryType.YOU_GAVE) {
                    totalGave += entry.amount
                } else {
                    totalGot += entry.amount
                }
                if (lastDate == null || entry.dateMillis > lastDate) {
                    lastDate = entry.dateMillis
                }
            }

            // Customer: gave (debit) - got (credit) -> positive = receivable
            // Supplier: gave (paid) - got (purchased) -> positive = advance (receivable), negative = payable
            val net = if (party.type == PartyType.CUSTOMER) {
                totalGave - totalGot
            } else {
                totalGave - totalGot // If negative, you owe supplier (Payable)
            }

            PartyWithBalance(
                party = party,
                totalGave = totalGave,
                totalGot = totalGot,
                netBalance = net,
                lastEntryDateMillis = lastDate
            )
        }

        // Totals
        var receivable = 0.0
        var payable = 0.0
        for (pb in partiesWithBalance) {
            if (pb.party.type == PartyType.CUSTOMER) {
                if (pb.netBalance > 0.009) receivable += pb.netBalance
                else if (pb.netBalance < -0.009) payable += kotlin.math.abs(pb.netBalance)
            } else {
                if (pb.netBalance < -0.009) payable += kotlin.math.abs(pb.netBalance)
                else if (pb.netBalance > 0.009) receivable += pb.netBalance
            }
        }

        // Filtering
        val filtered = partiesWithBalance.filter { item ->
            val matchesType = when (typeFilter) {
                null -> true
                PartyType.CUSTOMER -> item.party.type == PartyType.CUSTOMER || item.party.type == PartyType.BOTH
                PartyType.SUPPLIER -> item.party.type == PartyType.SUPPLIER || item.party.type == PartyType.BOTH
                PartyType.BOTH -> item.party.type == PartyType.BOTH
            }
            val matchesTag = tagFilter == null || item.party.tag == tagFilter
            val matchesQuery = if (query.isBlank()) true else {
                item.party.name.contains(query, ignoreCase = true) ||
                        item.party.phone.contains(query, ignoreCase = true) ||
                        item.party.gstin.contains(query, ignoreCase = true) ||
                        item.party.address.contains(query, ignoreCase = true)
            }
            matchesType && matchesTag && matchesQuery
        }

        // Sorting
        val sorted = when (sortOption) {
            PartySortOption.MOST_DUE -> filtered.sortedByDescending { it.dueAmount }
            PartySortOption.RECENT -> filtered.sortedByDescending { it.lastEntryDateMillis ?: 0L }
            PartySortOption.NAME_ASC -> filtered.sortedBy { it.party.name.lowercase() }
            PartySortOption.SETTLED -> filtered.sortedBy { it.dueAmount }
        }

        val activeSelected = selectedPartyId?.let { id ->
            partiesWithBalance.find { it.party.id == id }
        }

        val selectedEntries = if (selectedPartyId != null) {
            entries.filter { it.partyId == selectedPartyId }.sortedByDescending { it.dateMillis }
        } else {
            emptyList()
        }

        PartyUiState(
            parties = partiesWithBalance,
            filteredParties = sorted,
            totalReceivable = receivable,
            totalPayable = payable,
            searchQuery = query,
            typeFilter = typeFilter,
            tagFilter = tagFilter,
            sortOption = sortOption,
            selectedParty = activeSelected,
            selectedPartyEntries = selectedEntries
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PartyUiState()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTypeFilter(type: PartyType?) {
        _typeFilter.value = type
    }

    fun setTagFilter(tag: PartyTag?) {
        _tagFilter.value = tag
    }

    fun setSortOption(sort: PartySortOption) {
        _sortOption.value = sort
    }

    fun selectParty(partyId: String?) {
        savedStateHandle[KEY_SELECTED_PARTY_ID] = partyId
    }

    fun addParty(
        name: String,
        phone: String,
        address: String,
        gstin: String,
        type: PartyType,
        tag: PartyTag
    ) {
        val created = repository.addParty(
            name = name,
            phone = phone,
            address = address,
            gstin = gstin,
            type = type,
            tag = tag
        )
        savedStateHandle[KEY_SELECTED_PARTY_ID] = created.id
    }

    fun updateParty(party: Party) {
        repository.updateParty(party)
        invoiceRepository.updateCustomerDetails(
            customerId = party.id,
            name = party.name,
            phone = party.phone,
            address = party.address,
            gstin = party.gstin
        )
        purchaseRepository.updateSupplierDetails(
            supplierId = party.id,
            name = party.name,
            phone = party.phone,
            address = party.address,
            gstin = party.gstin
        )
    }

    fun deleteParty(partyId: String) {
        if (savedStateHandle.get<String?>(KEY_SELECTED_PARTY_ID) == partyId) {
            savedStateHandle[KEY_SELECTED_PARTY_ID] = null
        }
        repository.deleteParty(partyId)
    }

    fun addKhataEntry(
        partyId: String,
        amount: Double,
        type: KhataEntryType,
        dateMillis: Long,
        billNumber: String,
        note: String
    ) {
        repository.addKhataEntry(
            partyId = partyId,
            amount = amount,
            type = type,
            dateMillis = dateMillis,
            billNumber = billNumber,
            note = note
        )
    }

    fun recordPartyPayment(
        partyId: String,
        amount: Double,
        direction: PaymentDirection,
        paymentMode: PaymentMode,
        referenceNo: String,
        notes: String,
        linkedInvoiceId: String? = null
    ) {
        recordPaymentUseCase.execute(
            partyId = partyId,
            amount = amount,
            direction = direction,
            paymentMode = paymentMode,
            referenceNo = referenceNo,
            notes = notes,
            linkedInvoiceId = linkedInvoiceId
        )
    }

    fun shareStatementPdf(
        context: Context,
        partyWithBalance: PartyWithBalance,
        entries: List<KhataEntry>,
        targetWhatsApp: Boolean = false
    ) {
        PartyStatementPdfGenerator.sharePdf(
            context = context,
            partyWithBalance = partyWithBalance,
            entries = entries,
            targetWhatsApp = targetWhatsApp
        )
    }

    fun exportLedgerCsv(
        context: Context,
        partyWithBalance: PartyWithBalance,
        entries: List<KhataEntry>
    ) {
        PartyStatementPdfGenerator.exportCsv(
            context = context,
            partyWithBalance = partyWithBalance,
            entries = entries
        )
    }

    fun exportAllPartiesCsv(context: Context) {
        try {
            val exportDir = File(context.cacheDir, "csv_exports").apply { mkdirs() }
            val file = File(exportDir, "Parties_Khata_Summary_${System.currentTimeMillis()}.csv")
            val currentParties = uiState.value.parties

            file.bufferedWriter().use { writer ->
                writer.write("Party Name,Phone,Type,Tag,GSTIN,Total Given (Debit),Total Received (Credit),Net Balance,Status\n")
                for (item in currentParties) {
                    val p = item.party
                    val name = "\"${p.name.replace("\"", "\"\"")}\""
                    val phone = "\"${p.phone.replace("\"", "\"\"")}\""
                    val type = p.type.label
                    val tag = p.tag.label
                    val gstin = "\"${p.gstin.replace("\"", "\"\"")}\""
                    val gave = item.totalGave
                    val got = item.totalGot
                    val net = item.dueAmount
                    val status = item.getStatusLabel()
                    writer.write("$name,$phone,$type,$tag,$gstin,$gave,$got,$net,$status\n")
                }
            }

            val uri = FileProvider.getUriForFile(
                context,
                "com.hisabpro.app.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Customer & Supplier Khata Summary")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export Parties Khata CSV"))
        } catch (_: Exception) {}
    }

    fun deleteKhataEntry(entryId: String) {
        repository.deleteKhataEntry(entryId)
    }

    fun resetToDemo() {
        repository.resetToDemo()
    }
}
