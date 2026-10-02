package com.hisabpro.app.data.local

import android.content.Context
import androidx.room.withTransaction
import com.hisabpro.app.data.local.entity.AccountEntity
import com.hisabpro.app.data.local.entity.BusinessEntity
import com.hisabpro.app.data.local.entity.ExpenseEntity
import com.hisabpro.app.data.local.entity.InvoiceEntity
import com.hisabpro.app.data.local.entity.InvoiceItemEntity
import com.hisabpro.app.data.local.entity.ItemEntity
import com.hisabpro.app.data.local.entity.JournalEntryEntity
import com.hisabpro.app.data.local.entity.JournalEntryLineEntity
import com.hisabpro.app.data.local.entity.KhataEntryEntity
import com.hisabpro.app.data.local.entity.PartyEntity
import com.hisabpro.app.data.local.entity.PaymentEntity
import com.hisabpro.app.data.model.BusinessProfile
import com.hisabpro.app.data.repository.BusinessManager
import com.hisabpro.app.data.repository.InvoiceRepository
import com.hisabpro.app.data.repository.ItemRepository
import com.hisabpro.app.data.repository.PartyRepository
import com.hisabpro.app.data.repository.SettingsRepository
import com.hisabpro.app.data.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utility responsible for performing a fresh database wipe and populating rich,
 * realistic sample business data for user vittalmali3@gmail.com ("Vittal Supermarket & Traders").
 */
object DatabaseSeeder {

    suspend fun seedFreshStartForVittalMali(context: Context): Result<String> = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val db = AppDatabase.getInstance(appContext)

        try {
            val now = System.currentTimeMillis()
            val dayMillis = 86_400_000L

            val targetBusinessId = "biz_vittal_supermarket_traders"
            val targetOwnerName = "Vittal Mali"
            val targetEmail = "vittalmali3@gmail.com"
            val targetPhone = "+919822012345"

            // Step 1: Atomic Database Reset inside Room transaction
            db.withTransaction {
                db.journalDao().deleteAllJournalLines()
                db.journalDao().deleteAllJournalEntries()
                db.invoiceDao().deleteAllInvoiceItems()
                db.invoiceDao().deleteAllInvoices()
                db.paymentDao().deleteAllPayments()
                db.expenseDao().deleteAllExpenses()
                db.khataDao().deleteAllEntries()
                db.itemDao().deleteAllItems()
                db.accountDao().deleteAllAccounts()
                db.partyDao().deleteAllParties()
                db.businessDao().deleteAllBusinesses()
                db.syncQueueDao().clearAll()
                db.syncOutboxDao().clearAll()
            }

            // Step 2: Seed Business Profile
            val businessEntity = BusinessEntity(
                id = targetBusinessId,
                name = "Vittal Supermarket & Traders",
                ownerName = targetOwnerName,
                address = "Shop No 12, Main Market Yard, Pune, Maharashtra 411037",
                phone = targetPhone,
                email = targetEmail,
                gstin = "27ABCDE1234F1Z5",
                pan = "ABCDE1234F",
                logoPath = "",
                gstEnabled = true,
                financialYearStart = "01-04",
                upiId = "vittalmali@upi",
                bankName = "State Bank of India",
                accountNumber = "987654321012",
                ifscCode = "SBIN0001234",
                termsAndConditions = "Goods once sold will not be taken back. Payment due within 15 days.",
                createdAt = now - (30 * dayMillis),
                updatedAt = now
            )

            // Step 3: Seed Parties (Customers & Vendors)
            val parties = listOf(
                PartyEntity(
                    id = "party_ramesh",
                    businessId = targetBusinessId,
                    name = "Ramesh Kumar Traders",
                    phone = "+919822011111",
                    email = "ramesh@traders.com",
                    address = "Swargate, Pune, MH",
                    gstin = "27AAACR1234A1Z1",
                    type = "CUSTOMER",
                    tag = "REGULAR",
                    openingBalance = 0L,
                    createdAt = now - (20 * dayMillis),
                    updatedAt = now - (20 * dayMillis)
                ),
                PartyEntity(
                    id = "party_priya",
                    businessId = targetBusinessId,
                    name = "Priya Grocery Store",
                    phone = "+919822022222",
                    email = "priya@groceries.com",
                    address = "Kothrud, Pune, MH",
                    gstin = "27BBBCP5678B1Z2",
                    type = "CUSTOMER",
                    tag = "REGULAR",
                    openingBalance = 0L,
                    createdAt = now - (15 * dayMillis),
                    updatedAt = now - (15 * dayMillis)
                ),
                PartyEntity(
                    id = "party_amit",
                    businessId = targetBusinessId,
                    name = "Amit Wholesalers",
                    phone = "+919822033333",
                    email = "amit@wholesalers.com",
                    address = "Market Yard, Pune, MH",
                    gstin = "27CCCAW9012C1Z3",
                    type = "SUPPLIER",
                    tag = "VENDOR",
                    openingBalance = 0L,
                    createdAt = now - (25 * dayMillis),
                    updatedAt = now - (25 * dayMillis)
                ),
                PartyEntity(
                    id = "party_sunil",
                    businessId = targetBusinessId,
                    name = "Sunil Distributors",
                    phone = "+919822044444",
                    email = "sunil@distributors.com",
                    address = "Chinchwad, Pune, MH",
                    gstin = "27DDDSU3456D1Z4",
                    type = "SUPPLIER",
                    tag = "VENDOR",
                    openingBalance = 0L,
                    createdAt = now - (10 * dayMillis),
                    updatedAt = now - (10 * dayMillis)
                )
            )

            // Step 4: Seed Items / Inventory
            val items = listOf(
                ItemEntity(
                    id = "item_oil_5l",
                    businessId = targetBusinessId,
                    name = "Fortune Sunflower Oil 5L",
                    itemCode = "OIL-5L",
                    unit = "Pcs",
                    hsnCode = "1512",
                    purchasePrice = 60000L, // ₹600.00
                    sellPrice = 75000L,     // ₹750.00
                    gstRate = 5.0,
                    category = "Groceries",
                    stockQty = 40.0,
                    lowStockThreshold = 5.0,
                    createdAt = now - (25 * dayMillis),
                    updatedAt = now - (25 * dayMillis)
                ),
                ItemEntity(
                    id = "item_rice_25kg",
                    businessId = targetBusinessId,
                    name = "Basmati Rice 25kg",
                    itemCode = "RICE-25KG",
                    unit = "Bag",
                    hsnCode = "1006",
                    purchasePrice = 120000L, // ₹1,200.00
                    sellPrice = 150000L,     // ₹1,500.00
                    gstRate = 5.0,
                    category = "Groceries",
                    stockQty = 25.0,
                    lowStockThreshold = 5.0,
                    createdAt = now - (25 * dayMillis),
                    updatedAt = now - (25 * dayMillis)
                ),
                ItemEntity(
                    id = "item_salt_1kg",
                    businessId = targetBusinessId,
                    name = "Tata Salt 1kg",
                    itemCode = "SALT-1KG",
                    unit = "Pcs",
                    hsnCode = "2501",
                    purchasePrice = 2000L,  // ₹20.00
                    sellPrice = 2800L,      // ₹28.00
                    gstRate = 0.0,
                    category = "Groceries",
                    stockQty = 100.0,
                    lowStockThreshold = 10.0,
                    createdAt = now - (25 * dayMillis),
                    updatedAt = now - (25 * dayMillis)
                ),
                ItemEntity(
                    id = "item_silk_choco",
                    businessId = targetBusinessId,
                    name = "Cadbury Dairy Milk Silk",
                    itemCode = "CHOCO-SILK",
                    unit = "Pcs",
                    hsnCode = "1806",
                    purchasePrice = 14000L, // ₹140.00
                    sellPrice = 17500L,     // ₹175.00
                    gstRate = 18.0,
                    category = "Confectionery",
                    stockQty = 50.0,
                    lowStockThreshold = 10.0,
                    createdAt = now - (25 * dayMillis),
                    updatedAt = now - (25 * dayMillis)
                )
            )

            // Step 5: Seed Invoices and Line Items
            val invoice1 = InvoiceEntity(
                id = "inv_001",
                businessId = targetBusinessId,
                invoiceNo = "2026-27/INV/001",
                date = now - (2 * dayMillis),
                partyId = "party_ramesh",
                customerName = "Ramesh Kumar Traders",
                customerPhone = "+919822011111",
                customerAddress = "Swargate, Pune, MH",
                customerGstin = "27AAACR1234A1Z1",
                type = "TAX_INVOICE",
                gstMode = "INTRA_STATE",
                subtotal = 225000L,
                discount = 0L,
                taxableAmount = 225000L,
                cgst = 5625L,
                sgst = 5625L,
                igst = 0L,
                total = 236250L, // ₹2,362.50
                paidAmount = 236250L,
                paymentStatus = "PAID",
                paymentMode = "UPI",
                notes = "Thank you for your business!",
                isGst = true,
                createdAt = now - (2 * dayMillis),
                updatedAt = now - (2 * dayMillis)
            )

            val invoiceItems1 = listOf(
                InvoiceItemEntity(
                    id = "ii_001_1",
                    invoiceId = "inv_001",
                    itemId = "item_oil_5l",
                    itemName = "Fortune Sunflower Oil 5L",
                    hsnCode = "1512",
                    qty = 1.0,
                    unit = "Pcs",
                    rate = 75000L,
                    discount = 0L,
                    cgstRate = 2.5,
                    sgstRate = 2.5,
                    igstRate = 0.0,
                    amount = 75000L
                ),
                InvoiceItemEntity(
                    id = "ii_001_2",
                    invoiceId = "inv_001",
                    itemId = "item_rice_25kg",
                    itemName = "Basmati Rice 25kg",
                    hsnCode = "1006",
                    qty = 1.0,
                    unit = "Bag",
                    rate = 150000L,
                    discount = 0L,
                    cgstRate = 2.5,
                    sgstRate = 2.5,
                    igstRate = 0.0,
                    amount = 150000L
                )
            )

            val invoice2 = InvoiceEntity(
                id = "inv_002",
                businessId = targetBusinessId,
                invoiceNo = "2026-27/INV/002",
                date = now - (1 * dayMillis),
                partyId = "party_priya",
                customerName = "Priya Grocery Store",
                customerPhone = "+919822022222",
                customerAddress = "Kothrud, Pune, MH",
                customerGstin = "27BBBCP5678B1Z2",
                type = "TAX_INVOICE",
                gstMode = "INTRA_STATE",
                subtotal = 352500L,
                discount = 0L,
                taxableAmount = 352500L,
                cgst = 20625L,
                sgst = 20625L,
                igst = 0L,
                total = 393750L, // ₹3,937.50
                paidAmount = 200000L, // ₹2,000.00
                paymentStatus = "PARTIAL",
                paymentMode = "UPI",
                notes = "Balance ₹1,937.50 pending",
                isGst = true,
                createdAt = now - (1 * dayMillis),
                updatedAt = now - (1 * dayMillis)
            )

            val invoiceItems2 = listOf(
                InvoiceItemEntity(
                    id = "ii_002_1",
                    invoiceId = "inv_002",
                    itemId = "item_rice_25kg",
                    itemName = "Basmati Rice 25kg",
                    hsnCode = "1006",
                    qty = 2.0,
                    unit = "Bag",
                    rate = 150000L,
                    discount = 0L,
                    cgstRate = 2.5,
                    sgstRate = 2.5,
                    igstRate = 0.0,
                    amount = 300000L
                ),
                InvoiceItemEntity(
                    id = "ii_002_2",
                    invoiceId = "inv_002",
                    itemId = "item_silk_choco",
                    itemName = "Cadbury Dairy Milk Silk",
                    hsnCode = "1806",
                    qty = 3.0,
                    unit = "Pcs",
                    rate = 17500L,
                    discount = 0L,
                    cgstRate = 9.0,
                    sgstRate = 9.0,
                    igstRate = 0.0,
                    amount = 52500L
                )
            )

            val invoice3 = InvoiceEntity(
                id = "inv_003",
                businessId = targetBusinessId,
                invoiceNo = "2026-27/BILL/001",
                date = now,
                partyId = null,
                customerName = "Walk-in Retail Buyer",
                customerPhone = "+919988776655",
                customerAddress = "Pune",
                customerGstin = "",
                type = "NON_GST_BILL",
                gstMode = "EXEMPT",
                subtotal = 5600L,
                discount = 0L,
                taxableAmount = 0L,
                cgst = 0L,
                sgst = 0L,
                igst = 0L,
                total = 5600L, // ₹56.00
                paidAmount = 5600L,
                paymentStatus = "PAID",
                paymentMode = "Cash",
                notes = "Retail Cash Sale",
                isGst = false,
                createdAt = now,
                updatedAt = now
            )

            val invoiceItems3 = listOf(
                InvoiceItemEntity(
                    id = "ii_003_1",
                    invoiceId = "inv_003",
                    itemId = "item_salt_1kg",
                    itemName = "Tata Salt 1kg",
                    hsnCode = "2501",
                    qty = 2.0,
                    unit = "Pcs",
                    rate = 2800L,
                    discount = 0L,
                    cgstRate = 0.0,
                    sgstRate = 0.0,
                    igstRate = 0.0,
                    amount = 5600L
                )
            )

            // Step 6: Seed Payments
            val payments = listOf(
                PaymentEntity(
                    id = "pay_001",
                    businessId = targetBusinessId,
                    partyId = "party_priya",
                    date = now - (1 * dayMillis),
                    amount = 200000L, // ₹2,000.00
                    mode = "UPI",
                    referenceNo = "UPI-98765432101",
                    notes = "Part payment for Invoice #2026-27/INV/002",
                    linkedInvoiceId = "inv_002",
                    createdAt = now - (1 * dayMillis),
                    updatedAt = now - (1 * dayMillis)
                ),
                PaymentEntity(
                    id = "pay_002",
                    businessId = targetBusinessId,
                    partyId = "party_amit",
                    date = now - (3 * dayMillis),
                    amount = 1000000L, // ₹10,000.00
                    mode = "Bank Transfer",
                    referenceNo = "NEFT-SBI-112233",
                    notes = "Advance payment to supplier for bulk rice order",
                    linkedInvoiceId = null,
                    createdAt = now - (3 * dayMillis),
                    updatedAt = now - (3 * dayMillis)
                )
            )

            // Step 7: Seed Expenses
            val expenses = listOf(
                ExpenseEntity(
                    id = "exp_001",
                    businessId = targetBusinessId,
                    date = now - (5 * dayMillis),
                    category = "Rent",
                    amount = 1500000L, // ₹15,000.00
                    description = "Shop Monthly Rent for Market Yard Premises",
                    mode = "Bank Transfer",
                    receiptPath = "",
                    createdAt = now - (5 * dayMillis),
                    updatedAt = now - (5 * dayMillis)
                ),
                ExpenseEntity(
                    id = "exp_002",
                    businessId = targetBusinessId,
                    date = now - (3 * dayMillis),
                    category = "Utilities",
                    amount = 320000L, // ₹3,200.00
                    description = "Electricity Bill MSEDCL Commercial",
                    mode = "UPI",
                    receiptPath = "",
                    createdAt = now - (3 * dayMillis),
                    updatedAt = now - (3 * dayMillis)
                ),
                ExpenseEntity(
                    id = "exp_003",
                    businessId = targetBusinessId,
                    date = now - (1 * dayMillis),
                    category = "Salaries",
                    amount = 2500000L, // ₹25,000.00
                    description = "Staff Monthly Salary",
                    mode = "Bank Transfer",
                    receiptPath = "",
                    createdAt = now - (1 * dayMillis),
                    updatedAt = now - (1 * dayMillis)
                )
            )

            // Step 8: Seed Accounts & Journal Entries
            val accounts = listOf(
                AccountEntity(
                    id = "acc_cash",
                    businessId = targetBusinessId,
                    name = "Cash Account",
                    type = "ASSET",
                    openingBalance = 5000000L, // ₹50,000.00
                    accountNumber = "",
                    ifscCode = "",
                    createdAt = now - (30 * dayMillis),
                    updatedAt = now - (30 * dayMillis)
                ),
                AccountEntity(
                    id = "acc_bank",
                    businessId = targetBusinessId,
                    name = "SBI Bank Account",
                    type = "ASSET",
                    openingBalance = 25000000L, // ₹2,50,000.00
                    accountNumber = "987654321012",
                    ifscCode = "SBIN0001234",
                    createdAt = now - (30 * dayMillis),
                    updatedAt = now - (30 * dayMillis)
                ),
                AccountEntity(
                    id = "acc_sales",
                    businessId = targetBusinessId,
                    name = "Sales Account",
                    type = "INCOME",
                    openingBalance = 0L,
                    createdAt = now - (30 * dayMillis),
                    updatedAt = now - (30 * dayMillis)
                ),
                AccountEntity(
                    id = "acc_purchases",
                    businessId = targetBusinessId,
                    name = "Purchase Account",
                    type = "EXPENSE",
                    openingBalance = 0L,
                    createdAt = now - (30 * dayMillis),
                    updatedAt = now - (30 * dayMillis)
                )
            )

            val journalEntry = JournalEntryEntity(
                id = "je_001",
                businessId = targetBusinessId,
                date = now - (10 * dayMillis),
                voucherNo = "VOUCH-001",
                narration = "Initial Owner Capital Contribution into SBI Bank Account",
                createdAt = now - (10 * dayMillis),
                updatedAt = now - (10 * dayMillis)
            )

            val journalLines = listOf(
                JournalEntryLineEntity(
                    id = "jl_001_1",
                    journalEntryId = "je_001",
                    accountId = "acc_bank",
                    accountName = "SBI Bank Account",
                    isDebit = true,
                    debit = 30000000L, // ₹3,00,000.00
                    credit = 0L,
                    createdAt = now - (10 * dayMillis),
                    updatedAt = now - (10 * dayMillis)
                ),
                JournalEntryLineEntity(
                    id = "jl_001_2",
                    journalEntryId = "je_001",
                    accountId = "acc_cash",
                    accountName = "Cash Account",
                    isDebit = false,
                    debit = 0L,
                    credit = 30000000L,
                    createdAt = now - (10 * dayMillis),
                    updatedAt = now - (10 * dayMillis)
                )
            )

            // Step 9: Seed Khata Entries
            val khataEntries = listOf(
                KhataEntryEntity(
                    id = "ke_001",
                    businessId = targetBusinessId,
                    partyId = "party_ramesh",
                    amount = 236250L,
                    type = "YOU_GAVE",
                    date = now - (2 * dayMillis),
                    billNumber = "2026-27/INV/001",
                    note = "Goods delivered on credit under Invoice #2026-27/INV/001",
                    createdAt = now - (2 * dayMillis),
                    updatedAt = now - (2 * dayMillis)
                ),
                KhataEntryEntity(
                    id = "ke_002",
                    businessId = targetBusinessId,
                    partyId = "party_ramesh",
                    amount = 236250L,
                    type = "YOU_GOT",
                    date = now - (2 * dayMillis),
                    billNumber = "2026-27/INV/001",
                    note = "Full bill payment received via UPI",
                    createdAt = now - (2 * dayMillis),
                    updatedAt = now - (2 * dayMillis)
                ),
                KhataEntryEntity(
                    id = "ke_003",
                    businessId = targetBusinessId,
                    partyId = "party_priya",
                    amount = 393750L,
                    type = "YOU_GAVE",
                    date = now - (1 * dayMillis),
                    billNumber = "2026-27/INV/002",
                    note = "Goods delivered on credit under Invoice #2026-27/INV/002",
                    createdAt = now - (1 * dayMillis),
                    updatedAt = now - (1 * dayMillis)
                ),
                KhataEntryEntity(
                    id = "ke_004",
                    businessId = targetBusinessId,
                    partyId = "party_priya",
                    amount = 200000L,
                    type = "YOU_GOT",
                    date = now - (1 * dayMillis),
                    billNumber = "2026-27/INV/002",
                    note = "Part payment received via UPI. Pending: ₹1,937.50",
                    createdAt = now - (1 * dayMillis),
                    updatedAt = now - (1 * dayMillis)
                )
            )

            // Step 10: Persist all seeded entities into Room database
            db.withTransaction {
                db.businessDao().insertOrUpdate(businessEntity)
                db.partyDao().insertAllParties(parties)
                db.itemDao().insertAllItems(items)
                db.accountDao().insertAllAccounts(accounts)
                
                db.invoiceDao().insertInvoice(invoice1)
                db.invoiceDao().insertInvoiceItems(invoiceItems1)

                db.invoiceDao().insertInvoice(invoice2)
                db.invoiceDao().insertInvoiceItems(invoiceItems2)

                db.invoiceDao().insertInvoice(invoice3)
                db.invoiceDao().insertInvoiceItems(invoiceItems3)

                db.paymentDao().insertAllPayments(payments)
                db.expenseDao().insertAllExpenses(expenses)
                db.khataDao().insertAllEntries(khataEntries)
                db.journalDao().insertAllJournalEntries(listOf(journalEntry))
                db.journalDao().insertJournalLines(journalLines)
            }

            // Step 11: Save active business profile settings
            val profile = BusinessProfile(
                id = targetBusinessId,
                shopName = "Vittal Supermarket & Traders",
                ownerName = targetOwnerName,
                phone = targetPhone,
                email = targetEmail,
                isGstRegistered = true,
                gstin = "27ABCDE1234F1Z5",
                pan = "ABCDE1234F",
                isCompositionScheme = false,
                state = "Maharashtra",
                stateCode = "27",
                city = "Pune",
                pincode = "411037",
                address = "Shop No 12, Main Market Yard, Pune, Maharashtra 411037",
                upiId = "vittalmali@upi",
                bankName = "State Bank of India",
                accountNumber = "987654321012",
                ifscCode = "SBIN0001234",
                invoicePrefix = "2026-27/INV",
                purchasePrefix = "2026-27/PUR",
                termsAndConditions = "Goods once sold will not be taken back. Payment due within 15 days.",
                logoPath = "",
                isThermalPrinterMode = false,
                showUpiQrOnInvoice = true,
                appLanguage = "en",
                hasCompletedOnboarding = true
            )

            SettingsRepository.getInstance(appContext).saveProfile(profile)
            BusinessManager.getInstance(appContext).reloadFromRoomAndCloud()

            // Step 12: Force reload all active in-memory repositories
            InvoiceRepository.getInstance(appContext).reloadFromDatabase()
            PartyRepository.getInstance(appContext).reloadFromDatabase()
            ItemRepository.getInstance(appContext).reloadFromDatabase()
            TransactionRepository.getInstance(appContext).reloadFromDatabase()

            Result.success("Fresh start completed for Vittal Mali ($targetEmail). Sample data seeded successfully.")
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
