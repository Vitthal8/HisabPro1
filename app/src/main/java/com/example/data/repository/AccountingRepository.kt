package com.example.data.repository

import com.example.data.database.AppDatabase
import com.example.data.model.Account
import com.example.data.model.AccountType
import com.example.data.model.Business
import com.example.data.model.Expense
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.data.model.InvoiceType
import com.example.data.model.Item
import com.example.data.model.JournalEntry
import com.example.data.model.JournalLine
import com.example.data.model.Party
import com.example.data.model.PartyType
import com.example.data.model.Payment
import com.example.data.model.PaymentMode
import com.example.util.IndianAccountingUtils
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class AccountingRepository(private val db: AppDatabase) {

    val primaryBusiness: Flow<Business?> = db.businessDao().getPrimaryBusiness()
    val allParties: Flow<List<Party>> = db.partyDao().getAllParties()
    val allInvoices: Flow<List<Invoice>> = db.invoiceDao().getAllInvoices()
    val allPayments: Flow<List<Payment>> = db.paymentDao().getAllPayments()
    val allItems: Flow<List<Item>> = db.itemDao().getAllItems()
    val allExpenses: Flow<List<Expense>> = db.expenseDao().getAllExpenses()
    val allAccounts: Flow<List<Account>> = db.accountDao().getAllAccounts()
    val allJournalEntries: Flow<List<JournalEntry>> = db.journalEntryDao().getAllJournalEntries()

    suspend fun getPrimaryBusinessSync(): Business? = db.businessDao().getPrimaryBusinessSync()

    suspend fun saveBusiness(business: Business): Long {
        return if (business.id == 0L) {
            db.businessDao().insertBusiness(business)
        } else {
            db.businessDao().updateBusiness(business)
            business.id
        }
    }

    suspend fun addParty(party: Party): Long {
        // Initial current balance = opening balance
        val partyWithBalance = party.copy(currentBalance = party.openingBalance)
        return db.partyDao().insertParty(partyWithBalance)
    }

    suspend fun updateParty(party: Party) {
        db.partyDao().updateParty(party)
    }

    suspend fun deleteParty(party: Party) {
        db.partyDao().deleteParty(party)
    }

    fun getPartyFlow(partyId: Long): Flow<Party?> = db.partyDao().getPartyFlow(partyId)
    suspend fun getPartyById(partyId: Long): Party? = db.partyDao().getPartyById(partyId)

    fun getInvoicesForParty(partyId: Long): Flow<List<Invoice>> = db.invoiceDao().getInvoicesForParty(partyId)
    fun getPaymentsForParty(partyId: Long): Flow<List<Payment>> = db.paymentDao().getPaymentsForParty(partyId)
    fun getItemsForInvoice(invoiceId: Long): Flow<List<InvoiceItem>> = db.invoiceItemDao().getItemsForInvoice(invoiceId)
    suspend fun getItemsForInvoiceSync(invoiceId: Long): List<InvoiceItem> = db.invoiceItemDao().getItemsForInvoiceSync(invoiceId)

    suspend fun addItem(item: Item): Long = db.itemDao().insertItem(item)

    suspend fun getNextInvoiceSequence(): Int {
        val count = db.invoiceDao().getInvoiceCount()
        return count + 1
    }

    suspend fun saveInvoice(invoice: Invoice, items: List<InvoiceItem>): Long {
        val invoiceId = db.invoiceDao().insertInvoice(invoice)
        val itemsWithId = items.map { it.copy(invoiceId = invoiceId) }
        db.invoiceItemDao().insertInvoiceItems(itemsWithId)

        // Adjust Party Balance for Sales Invoice:
        // Due amount = total - paidAmount
        val dueAmount = invoice.total - invoice.paidAmount
        if (invoice.type == InvoiceType.SALE && dueAmount > 0) {
            // Customer owes us more -> positive delta (Dr)
            db.partyDao().updatePartyBalance(invoice.partyId, dueAmount)
        } else if (invoice.type == InvoiceType.PURCHASE && dueAmount > 0) {
            // We owe supplier more -> negative delta (Cr)
            db.partyDao().updatePartyBalance(invoice.partyId, -dueAmount)
        }

        // If paid amount > 0, record payment and cash/bank inflow
        if (invoice.paidAmount > 0) {
            db.paymentDao().insertPayment(
                Payment(
                    businessId = invoice.businessId,
                    partyId = invoice.partyId,
                    partyName = invoice.partyName,
                    date = invoice.date,
                    amount = invoice.paidAmount,
                    mode = invoice.paymentMode,
                    referenceNo = "Bill #${invoice.invoiceNo}",
                    notes = "Payment for invoice ${invoice.invoiceNo}",
                    linkedInvoiceId = invoiceId,
                    isReceived = (invoice.type == InvoiceType.SALE)
                )
            )
        }

        // Journal Entry (Double-entry principle)
        val journalLines = mutableListOf<JournalLine>()
        if (invoice.type == InvoiceType.SALE) {
            if (invoice.paidAmount > 0) {
                journalLines.add(JournalLine(accountName = invoice.paymentMode.name, debitAmount = invoice.paidAmount, creditAmount = 0.0))
            }
            if (dueAmount > 0) {
                journalLines.add(JournalLine(accountName = "${invoice.partyName} (Debtor)", debitAmount = dueAmount, creditAmount = 0.0))
            }
            journalLines.add(JournalLine(accountName = "Sales Account", debitAmount = 0.0, creditAmount = invoice.subtotal))
            if (invoice.cgst + invoice.sgst + invoice.igst > 0) {
                journalLines.add(JournalLine(accountName = "GST Output Liability", debitAmount = 0.0, creditAmount = invoice.cgst + invoice.sgst + invoice.igst))
            }
        }
        if (journalLines.isNotEmpty()) {
            db.journalEntryDao().insertJournalEntry(
                JournalEntry(
                    businessId = invoice.businessId,
                    date = invoice.date,
                    narration = "Sales bill ${invoice.invoiceNo} to ${invoice.partyName}",
                    entries = journalLines
                )
            )
        }

        return invoiceId
    }

    suspend fun recordPaymentReceived(
        partyId: Long,
        partyName: String,
        amount: Double,
        mode: PaymentMode,
        referenceNo: String,
        notes: String
    ): Long {
        val payment = Payment(
            businessId = 1,
            partyId = partyId,
            partyName = partyName,
            date = System.currentTimeMillis(),
            amount = amount,
            mode = mode,
            referenceNo = referenceNo,
            notes = notes,
            isReceived = true
        )
        val paymentId = db.paymentDao().insertPayment(payment)

        // Customer pays money -> reduces receivable (negative delta)
        db.partyDao().updatePartyBalance(partyId, -amount)

        // Journal Entry
        db.journalEntryDao().insertJournalEntry(
            JournalEntry(
                businessId = 1,
                date = payment.date,
                narration = "Payment received from $partyName by ${mode.name}",
                entries = listOf(
                    JournalLine(accountName = mode.name, debitAmount = amount, creditAmount = 0.0),
                    JournalLine(accountName = "$partyName (Debtor)", debitAmount = 0.0, creditAmount = amount)
                )
            )
        )

        return paymentId
    }

    suspend fun recordExpense(
        category: String,
        amount: Double,
        description: String,
        mode: PaymentMode
    ): Long {
        val expense = Expense(
            businessId = 1,
            date = System.currentTimeMillis(),
            category = category,
            amount = amount,
            description = description,
            mode = mode
        )
        val id = db.expenseDao().insertExpense(expense)

        db.journalEntryDao().insertJournalEntry(
            JournalEntry(
                businessId = 1,
                date = expense.date,
                narration = "Expense for $category: $description",
                entries = listOf(
                    JournalLine(accountName = "Expense - $category", debitAmount = amount, creditAmount = 0.0),
                    JournalLine(accountName = mode.name, debitAmount = 0.0, creditAmount = amount)
                )
            )
        )
        return id
    }

    fun getTodayInvoices(): Flow<List<Invoice>> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val endOfDay = cal.timeInMillis

        return db.invoiceDao().getTodayInvoices(1, startOfDay, endOfDay)
    }

    suspend fun seedInitialDataIfEmpty() {
        val existingBiz = db.businessDao().getPrimaryBusinessSync()
        if (existingBiz == null) {
            val bizId = db.businessDao().insertBusiness(
                Business(
                    name = "Mali General Stores",
                    address = "Shop No. 4, Shivaji Chowk, Pune",
                    phone = "9876543210",
                    gstEnabled = false, // Defaults to NON-GST mode as emphasized by user!
                    state = "Maharashtra",
                    language = "en"
                )
            )

            // Seed common accounts
            db.accountDao().insertAccount(Account(businessId = bizId, name = "Cash in Hand", type = AccountType.CASH, openingBalance = 15000.0, currentBalance = 15000.0))
            db.accountDao().insertAccount(Account(businessId = bizId, name = "State Bank of India", type = AccountType.BANK, openingBalance = 45000.0, currentBalance = 45000.0))

            // Seed common parties
            val p1 = db.partyDao().insertParty(
                Party(
                    businessId = bizId,
                    name = "Ramesh Patil",
                    phone = "9822100001",
                    address = "MG Road, Pune",
                    type = PartyType.CUSTOMER,
                    openingBalance = 3500.0,
                    currentBalance = 3500.0
                )
            )
            val p2 = db.partyDao().insertParty(
                Party(
                    businessId = bizId,
                    name = "Anil Kirana Mart",
                    phone = "9822100002",
                    address = "Market Yard, Pune",
                    type = PartyType.CUSTOMER,
                    openingBalance = 12400.0,
                    currentBalance = 12400.0
                )
            )
            val p3 = db.partyDao().insertParty(
                Party(
                    businessId = bizId,
                    name = "Maharashtra Wholesale Traders",
                    phone = "9822100003",
                    address = "Navi Peth, Pune",
                    type = PartyType.SUPPLIER,
                    openingBalance = -8500.0,
                    currentBalance = -8500.0
                )
            )

            // Seed common items
            db.itemDao().insertItem(Item(businessId = bizId, name = "Basmati Rice 10kg", unit = "BAG", purchasePrice = 650.0, sellPrice = 850.0, gstRate = 0.0, category = "Grains", stockQty = 25.0))
            db.itemDao().insertItem(Item(businessId = bizId, name = "Sunflower Cooking Oil 1L", unit = "LTR", purchasePrice = 110.0, sellPrice = 135.0, gstRate = 5.0, category = "Oils", stockQty = 40.0))
            db.itemDao().insertItem(Item(businessId = bizId, name = "Tata Salt 1kg", unit = "PKT", purchasePrice = 22.0, sellPrice = 28.0, gstRate = 0.0, category = "Spices", stockQty = 50.0))
            db.itemDao().insertItem(Item(businessId = bizId, name = "Wheat Flour (Atta) 5kg", unit = "BAG", purchasePrice = 180.0, sellPrice = 220.0, gstRate = 0.0, category = "Grains", stockQty = 30.0))

            // Seed sample initial invoice
            val invNo = IndianAccountingUtils.generateInvoiceNumber(1)
            val invId = db.invoiceDao().insertInvoice(
                Invoice(
                    businessId = bizId,
                    invoiceNo = invNo,
                    date = System.currentTimeMillis() - 3600000,
                    partyId = p1,
                    partyName = "Ramesh Patil",
                    type = InvoiceType.SALE,
                    subtotal = 1700.0,
                    total = 1700.0,
                    paidAmount = 1000.0,
                    paymentMode = PaymentMode.UPI,
                    notes = "Regular customer monthly purchase",
                    isGst = false
                )
            )
            db.invoiceItemDao().insertInvoiceItems(
                listOf(
                    InvoiceItem(invoiceId = invId, itemName = "Basmati Rice 10kg", qty = 2.0, unit = "BAG", rate = 850.0, amount = 1700.0)
                )
            )
        }
    }
}
