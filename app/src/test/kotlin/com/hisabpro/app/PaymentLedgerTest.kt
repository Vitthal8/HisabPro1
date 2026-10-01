package com.hisabpro.app

import com.hisabpro.app.data.model.KhataEntry
import com.hisabpro.app.data.model.KhataEntryType
import com.hisabpro.app.data.model.Party
import com.hisabpro.app.data.model.PartyType
import com.hisabpro.app.data.model.PartyWithBalance
import com.hisabpro.app.util.IndianAccountingFormat
import com.hisabpro.app.util.toPaise
import com.hisabpro.app.util.toRupees
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentLedgerTest {

    private fun createParty(id: String, name: String, type: PartyType): Party {
        return Party(
            id = id,
            name = name,
            phone = "9876543210",
            type = type
        )
    }

    private fun calculateLedgerBalance(openingPaise: Long, entries: List<KhataEntry>): Long {
        var balance = openingPaise
        for (e in entries) {
            val amountPaise = e.amount.toPaise()
            if (e.type == KhataEntryType.YOU_GAVE) {
                balance += amountPaise
            } else {
                balance -= amountPaise
            }
        }
        return balance
    }

    // 1. Opening debit
    @Test
    fun testOpeningDebit() {
        val customer = createParty("p1", "Ramesh Kumar", PartyType.CUSTOMER)
        val openingDrPaise = 5000.0.toPaise() // ₹5,000 Dr
        val netBalance = calculateLedgerBalance(openingDrPaise, emptyList()).toRupees()

        val partyBalance = PartyWithBalance(
            party = customer,
            totalGave = 5000.0,
            totalGot = 0.0,
            netBalance = netBalance
        )

        assertEquals(5000.0, partyBalance.dueAmount, 0.01)
        assertTrue(partyBalance.isReceivable)
        assertFalse(partyBalance.isSettled)
        assertEquals("Dr", IndianAccountingFormat.getDrCrIndicator(partyBalance.netBalance, isCustomer = true))
    }

    // 2. Opening credit
    @Test
    fun testOpeningCredit() {
        val supplier = createParty("p2", "Shree Balaji Traders", PartyType.SUPPLIER)
        val openingCrPaise = (-10000.0).toPaise() // ₹10,000 Cr
        val netBalance = calculateLedgerBalance(openingCrPaise, emptyList()).toRupees()

        val partyBalance = PartyWithBalance(
            party = supplier,
            totalGave = 0.0,
            totalGot = 10000.0,
            netBalance = netBalance
        )

        assertEquals(10000.0, partyBalance.dueAmount, 0.01)
        assertTrue(partyBalance.isPayable)
        assertFalse(partyBalance.isSettled)
        assertEquals("Cr", IndianAccountingFormat.getDrCrIndicator(partyBalance.dueAmount, isCustomer = false))
    }

    // 3. Sale
    @Test
    fun testSale() {
        val customer = createParty("p1", "Anjali Verma", PartyType.CUSTOMER)
        val entries = listOf(
            KhataEntry("e1", "p1", 10000.0, KhataEntryType.YOU_GAVE, System.currentTimeMillis(), "INV-001", "Sale of goods")
        )
        val netBalance = calculateLedgerBalance(0L, entries).toRupees()

        val partyBalance = PartyWithBalance(
            party = customer,
            totalGave = 10000.0,
            totalGot = 0.0,
            netBalance = netBalance
        )

        assertEquals(10000.0, partyBalance.dueAmount, 0.01)
        assertTrue(partyBalance.isReceivable)
    }

    // 4. Payment (Customer owes ₹10,000, pays ₹4,000 -> Outstanding becomes ₹6,000)
    @Test
    fun testPayment() {
        val customer = createParty("p1", "Ramesh Sharma", PartyType.CUSTOMER)
        val entries = listOf(
            KhataEntry("e1", "p1", 10000.0, KhataEntryType.YOU_GAVE, System.currentTimeMillis() - 1000, "INV-001", "Sale"),
            KhataEntry("e2", "p1", 4000.0, KhataEntryType.YOU_GOT, System.currentTimeMillis(), "REC-001", "Payment received")
        )
        val netBalance = calculateLedgerBalance(0L, entries).toRupees()

        val partyBalance = PartyWithBalance(
            party = customer,
            totalGave = 10000.0,
            totalGot = 4000.0,
            netBalance = netBalance
        )

        // Customer owes ₹10,000, paid ₹4,000 -> Outstanding becomes ₹6,000 Dr
        assertEquals(6000.0, partyBalance.dueAmount, 0.01)
        assertTrue(partyBalance.isReceivable)
        assertEquals("Dr", IndianAccountingFormat.getDrCrIndicator(partyBalance.netBalance, isCustomer = true))
    }

    // 5. Multiple payments
    @Test
    fun testMultiplePayments() {
        val customer = createParty("p1", "Vikram Patel", PartyType.CUSTOMER)
        val entries = listOf(
            KhataEntry("e1", "p1", 10000.0, KhataEntryType.YOU_GAVE, 100L, "INV-001", "Sale"),
            KhataEntry("e2", "p1", 2000.0, KhataEntryType.YOU_GOT, 200L, "REC-001", "Payment 1"),
            KhataEntry("e3", "p1", 3000.0, KhataEntryType.YOU_GOT, 300L, "REC-002", "Payment 2"),
            KhataEntry("e4", "p1", 1000.0, KhataEntryType.YOU_GOT, 400L, "REC-003", "Payment 3")
        )
        val netBalance = calculateLedgerBalance(0L, entries).toRupees()

        val partyBalance = PartyWithBalance(
            party = customer,
            totalGave = 10000.0,
            totalGot = 6000.0,
            netBalance = netBalance
        )

        // 10000 - (2000 + 3000 + 1000) = 4000 Dr
        assertEquals(4000.0, partyBalance.dueAmount, 0.01)
        assertTrue(partyBalance.isReceivable)
    }

    // 6. Supplier purchase
    @Test
    fun testSupplierPurchase() {
        val supplier = createParty("p2", "Gupta Hardware", PartyType.SUPPLIER)
        val entries = listOf(
            KhataEntry("e1", "p2", 25000.0, KhataEntryType.YOU_GOT, System.currentTimeMillis(), "PUR-001", "Raw materials purchase")
        )
        val netBalance = calculateLedgerBalance(0L, entries).toRupees()

        val partyBalance = PartyWithBalance(
            party = supplier,
            totalGave = 0.0,
            totalGot = 25000.0,
            netBalance = netBalance
        )

        assertEquals(25000.0, partyBalance.dueAmount, 0.01)
        assertTrue(partyBalance.isPayable)
        assertEquals("Cr", IndianAccountingFormat.getDrCrIndicator(partyBalance.dueAmount, isCustomer = false))
    }

    // 7. Supplier payment
    @Test
    fun testSupplierPayment() {
        val supplier = createParty("p2", "Apex Wholesale", PartyType.SUPPLIER)
        val entries = listOf(
            KhataEntry("e1", "p2", 25000.0, KhataEntryType.YOU_GOT, 100L, "PUR-001", "Purchase bill"),
            KhataEntry("e2", "p2", 15000.0, KhataEntryType.YOU_GAVE, 200L, "PAY-001", "NEFT Payment to vendor")
        )
        val netBalance = calculateLedgerBalance(0L, entries).toRupees()

        val partyBalance = PartyWithBalance(
            party = supplier,
            totalGave = 15000.0,
            totalGot = 25000.0,
            netBalance = netBalance
        )

        // We owed ₹25,000, paid ₹15,000 -> Outstanding payable becomes ₹10,000 Cr
        assertEquals(10000.0, partyBalance.dueAmount, 0.01)
        assertTrue(partyBalance.isPayable)
        assertEquals("Cr", IndianAccountingFormat.getDrCrIndicator(partyBalance.dueAmount, isCustomer = false))
    }

    // 8. Zero balance
    @Test
    fun testZeroBalance() {
        val customer = createParty("p1", "Anjali Verma", PartyType.CUSTOMER)
        val entries = listOf(
            KhataEntry("e1", "p1", 5000.0, KhataEntryType.YOU_GAVE, 100L, "INV-001", "Sale"),
            KhataEntry("e2", "p1", 5000.0, KhataEntryType.YOU_GOT, 200L, "REC-001", "Full Cash Payment")
        )
        val netBalance = calculateLedgerBalance(0L, entries).toRupees()

        val partyBalance = PartyWithBalance(
            party = customer,
            totalGave = 5000.0,
            totalGot = 5000.0,
            netBalance = netBalance
        )

        assertEquals(0.0, partyBalance.dueAmount, 0.01)
        assertTrue(partyBalance.isSettled)
    }

    // 9. Overpayment
    @Test
    fun testOverpayment() {
        val customer = createParty("p1", "Client Overpay", PartyType.CUSTOMER)
        val entries = listOf(
            KhataEntry("e1", "p1", 2000.0, KhataEntryType.YOU_GAVE, 100L, "INV-001", "Bill"),
            KhataEntry("e2", "p1", 3000.0, KhataEntryType.YOU_GOT, 200L, "REC-001", "Advance payment")
        )
        val netBalance = calculateLedgerBalance(0L, entries).toRupees()

        val partyBalance = PartyWithBalance(
            party = customer,
            totalGave = 2000.0,
            totalGot = 3000.0,
            netBalance = netBalance
        )

        // Customer owed ₹2,000 and paid ₹3,000 -> Net balance = -₹1,000 (Advance / Credit)
        assertEquals(1000.0, partyBalance.dueAmount, 0.01)
        assertTrue(partyBalance.isPayable)
        assertEquals("Cr", IndianAccountingFormat.getDrCrIndicator(partyBalance.netBalance, isCustomer = true))
    }
}
