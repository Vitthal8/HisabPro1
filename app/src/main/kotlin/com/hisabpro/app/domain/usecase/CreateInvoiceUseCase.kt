package com.hisabpro.app.domain.usecase

import com.hisabpro.app.data.model.Category
import com.hisabpro.app.data.model.Invoice
import com.hisabpro.app.data.model.KhataEntryType
import com.hisabpro.app.data.model.PaymentMode
import com.hisabpro.app.data.model.TransactionType
import com.hisabpro.app.data.repository.InvoiceRepository
import com.hisabpro.app.data.repository.ItemRepository
import com.hisabpro.app.data.repository.PartyRepository
import com.hisabpro.app.data.repository.TransactionRepository
import com.hisabpro.app.domain.subscription.EntitlementCheck
import com.hisabpro.app.domain.subscription.SubscriptionManager
import com.hisabpro.app.domain.validation.InputValidator

/**
 * Use Case: Atomic Invoice Creation
 * Coordinates:
 * 1. Form validation & Subscription entitlement check
 * 2. Persisting the invoice
 * 3. Automatic stock deduction from Inventory
 * 4. Automatic cashbook entry if paid (Cash/UPI/Bank)
 * 5. Automatic party khata ledger entry if credit due
 */
class CreateInvoiceUseCase(
    private val invoiceRepository: InvoiceRepository,
    private val itemRepository: ItemRepository,
    private val partyRepository: PartyRepository,
    private val transactionRepository: TransactionRepository,
    private val settingsRepository: com.hisabpro.app.data.repository.SettingsRepository? = null
) {

    fun execute(invoice: Invoice): Result<Invoice> {
        // 1. Validation
        val validation = InputValidator.validateInvoice(invoice.customerName, invoice.items.size)
        if (!validation.isSuccess) {
            return Result.failure(IllegalArgumentException(validation.errorMessage ?: "Validation error"))
        }

        // 2. Subscription entitlement check for current calendar month
        val currentMonthCount = countCurrentMonthInvoices(invoiceRepository.invoices.value, invoice.dateMillis)
        val check = SubscriptionManager.checkInvoiceCreationAllowed(currentMonthCount)
        if (check is EntitlementCheck.LimitExceeded) {
            return Result.failure(IllegalStateException(check.message))
        }

        // 3. Sanitize invoice according to GstPolicy single source of truth
        val profile = settingsRepository?.profile?.value ?: com.hisabpro.app.data.model.BusinessProfile()
        val sanitizedInvoice = com.hisabpro.app.domain.accounting.GstPolicy.sanitizeInvoiceForStorage(invoice, profile)

        val taxValidation = InputValidator.validateGstInvoiceTaxRates(sanitizedInvoice, profile.isGstRegistered)
        if (!taxValidation.isSuccess) {
            return Result.failure(IllegalArgumentException(taxValidation.errorMessage ?: "Invalid tax calculation"))
        }

        // 4. Save Invoice
        val saved = invoiceRepository.addInvoice(sanitizedInvoice)

        // 4. Atomic Inventory Stock Deduction
        invoice.items.forEach { lineItem ->
            itemRepository.deductStockForInvoiceItem(
                itemNameOrId = lineItem.description,
                quantity = lineItem.quantity,
                invoiceNumber = saved.invoiceNumber,
                sourceTransactionId = saved.id
            )
        }

        // 5. Cashbook / Daybook Income Entry for Paid Sales
        if (saved.paidAmount > 0) {
            val paymentMode = PaymentMode.fromString(saved.paymentMode)
            transactionRepository.addTransaction(
                title = "Sale #${saved.invoiceNumber} - ${saved.customerName}",
                amount = saved.paidAmount,
                type = TransactionType.INCOME,
                category = Category.BUSINESS,
                dateMillis = saved.dateMillis,
                paymentMode = paymentMode,
                note = "Bill #${saved.invoiceNumber} payment received",
                explicitBusinessId = saved.businessId,
                partyId = saved.customerId,
                linkedInvoiceId = saved.id
            )
        }

        // 6. Party Ledger Entry if credit outstanding on a registered party
        if (!saved.customerId.isNullOrBlank() && saved.dueAmount > 0) {
            val partyBiz = partyRepository.getPartyBusinessId(saved.customerId)
            require(saved.businessId == partyBiz) {
                "Multi-business violation: Invoice belongs to business '${saved.businessId}', but customer belongs to business '$partyBiz'!"
            }
            partyRepository.addKhataEntry(
                partyId = saved.customerId,
                amount = saved.dueAmount,
                type = KhataEntryType.YOU_GAVE,
                dateMillis = saved.dateMillis,
                billNumber = saved.invoiceNumber,
                note = "Bill #${saved.invoiceNumber} credit balance",
                explicitBusinessId = saved.businessId
            )
        }

        return Result.success(saved)
    }

    companion object {
        fun countCurrentMonthInvoices(invoices: List<Invoice>, targetDateMillis: Long = System.currentTimeMillis()): Int {
            val cal = java.util.Calendar.getInstance().apply {
                timeInMillis = targetDateMillis
            }
            val targetYear = cal.get(java.util.Calendar.YEAR)
            val targetMonth = cal.get(java.util.Calendar.MONTH)

            return invoices.count { inv ->
                val invCal = java.util.Calendar.getInstance().apply {
                    timeInMillis = inv.dateMillis
                }
                invCal.get(java.util.Calendar.YEAR) == targetYear &&
                invCal.get(java.util.Calendar.MONTH) == targetMonth
            }
        }
    }
}
