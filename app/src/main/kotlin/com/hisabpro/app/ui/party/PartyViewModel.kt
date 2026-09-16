package com.hisabpro.app.ui.party

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hisabpro.app.data.model.KhataEntry
import com.hisabpro.app.data.model.KhataEntryType
import com.hisabpro.app.data.model.Party
import com.hisabpro.app.data.model.PartyTag
import com.hisabpro.app.data.model.PartyType
import com.hisabpro.app.data.model.PartyWithBalance
import com.hisabpro.app.data.repository.InvoiceRepository
import com.hisabpro.app.data.repository.PartyRepository
import com.hisabpro.app.data.repository.PurchaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class PartyUiState(
    val parties: List<PartyWithBalance> = emptyList(),
    val filteredParties: List<PartyWithBalance> = emptyList(),
    val totalReceivable: Double = 0.0, // You'll Get
    val totalPayable: Double = 0.0,    // You'll Give
    val searchQuery: String = "",
    val typeFilter: PartyType? = null,
    val tagFilter: PartyTag? = null,
    val selectedParty: PartyWithBalance? = null,
    val selectedPartyEntries: List<KhataEntry> = emptyList()
)

class PartyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PartyRepository.getInstance(application.applicationContext)
    private val invoiceRepository = InvoiceRepository.getInstance(application.applicationContext)
    private val purchaseRepository = PurchaseRepository.getInstance(application.applicationContext)

    private val _searchQuery = MutableStateFlow("")
    private val _typeFilter = MutableStateFlow<PartyType?>(null)
    private val _tagFilter = MutableStateFlow<PartyTag?>(null)
    private val _selectedPartyId = MutableStateFlow<String?>(null)

    private data class FilterParams(
        val query: String,
        val typeFilter: PartyType?,
        val tagFilter: PartyTag?,
        val selectedPartyId: String?
    )

    private val _filterParams = combine(
        _searchQuery,
        _typeFilter,
        _tagFilter,
        _selectedPartyId
    ) { query, typeFilter, tagFilter, selectedPartyId ->
        FilterParams(query, typeFilter, tagFilter, selectedPartyId)
    }

    val uiState: StateFlow<PartyUiState> = combine(
        repository.parties,
        repository.entries,
        _filterParams
    ) { parties, entries, filter ->
        val query = filter.query
        val typeFilter = filter.typeFilter
        val tagFilter = filter.tagFilter
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
            val matchesType = typeFilter == null || item.party.type == typeFilter
            val matchesTag = tagFilter == null || item.party.tag == tagFilter
            val matchesQuery = if (query.isBlank()) true else {
                item.party.name.contains(query, ignoreCase = true) ||
                        item.party.phone.contains(query, ignoreCase = true) ||
                        item.party.gstin.contains(query, ignoreCase = true) ||
                        item.party.address.contains(query, ignoreCase = true)
            }
            matchesType && matchesTag && matchesQuery
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
            filteredParties = filtered,
            totalReceivable = receivable,
            totalPayable = payable,
            searchQuery = query,
            typeFilter = typeFilter,
            tagFilter = tagFilter,
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

    fun selectParty(partyId: String?) {
        _selectedPartyId.value = partyId
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
        _selectedPartyId.value = created.id
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
        if (_selectedPartyId.value == partyId) {
            _selectedPartyId.value = null
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

    fun deleteKhataEntry(entryId: String) {
        repository.deleteKhataEntry(entryId)
    }

    fun resetToDemo() {
        repository.resetToDemo()
    }
}
