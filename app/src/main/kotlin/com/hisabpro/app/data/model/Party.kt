package com.hisabpro.app.data.model

enum class PartyType(val label: String) {
    CUSTOMER("Customer"),
    SUPPLIER("Supplier"),
    BOTH("Both (Customer & Supplier)");

    companion object {
        fun fromString(value: String): PartyType {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: CUSTOMER
        }
    }
}

enum class PartyTag(val label: String) {
    REGULAR("Regular"),
    OCCASIONAL("Occasional"),
    BLOCKED("Blocked");

    companion object {
        fun fromString(value: String): PartyTag {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: REGULAR
        }
    }
}

data class Party(
    val id: String,
    val name: String,
    val phone: String,
    val address: String = "",
    val gstin: String = "",
    val type: PartyType = PartyType.CUSTOMER,
    val tag: PartyTag = PartyTag.REGULAR,
    val createdAt: Long = System.currentTimeMillis()
)

enum class KhataEntryType(val label: String) {
    YOU_GAVE("You Gave"),
    YOU_GOT("You Got");

    companion object {
        fun fromString(value: String): KhataEntryType {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: YOU_GAVE
        }
    }
}

data class KhataEntry(
    val id: String,
    val partyId: String,
    val amount: Double,
    val type: KhataEntryType,
    val dateMillis: Long,
    val billNumber: String = "",
    val note: String = ""
)

data class PartyWithBalance(
    val party: Party,
    val totalGave: Double,
    val totalGot: Double,
    val netBalance: Double,
    val lastEntryDateMillis: Long? = null
) {
    val dueAmount: Double
        get() = kotlin.math.abs(netBalance)

    val isSettled: Boolean
        get() = dueAmount < 0.01

    val isReceivable: Boolean
        get() = when (party.type) {
            PartyType.CUSTOMER -> netBalance > 0.009
            PartyType.SUPPLIER -> netBalance > 0.009
            PartyType.BOTH -> netBalance > 0.009
        }

    val isPayable: Boolean
        get() = when (party.type) {
            PartyType.CUSTOMER -> netBalance < -0.009
            PartyType.SUPPLIER -> netBalance < -0.009
            PartyType.BOTH -> netBalance < -0.009
        }

    fun getStatusLabel(): String {
        return when {
            isSettled -> "Settled"
            party.type == PartyType.CUSTOMER || party.type == PartyType.BOTH -> {
                if (netBalance > 0) "You'll Get" else "Advance (You'll Give)"
            }
            else -> { // SUPPLIER
                if (netBalance < 0) "You'll Give" else "Advance (You'll Get)"
            }
        }
    }
}
