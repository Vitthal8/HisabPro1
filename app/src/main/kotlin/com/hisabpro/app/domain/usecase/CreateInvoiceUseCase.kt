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
    private val transactionRepository: TransactionRepository
) {

    fun execute(invoice: Invoice): Result<Invoice> {
        // 1. Validation
        val validation = InputValidator.validateInvoice(invoice.customerName, invoice.items.size)
        if (!validation.isSuccess) {
            return Result.failure(IllegalArgumentException(validation.errorMessage ?: "Validation error"))
        }

        // 2. Subscription check
        val currentMonthCount = invoiceRepository.invoices.value.size
        val check = SubscriptionManager.checkInvoiceCreationAllowed(currentMonthCount)
        if (check is EntitlementCheck.LimitExceeded) {
            return Result.failure(IllegalStateException(check.message))
        }

        // 3. Save Invoice
        val saved = invoiceRepository.addInvoice(invoice)

        // 4. Atomic Inventory Stock Deduction
        invoice.items.forEach { lineItem ->
            itemRepository.deductStockForInvoiceItem(
                itemNameOrId = lineItem.description,
                quantity = lineItem.quantity,
                invoiceNumber = saved.invoiceNumber
            )
        }

        // 5. Cashbook / Daybook Income Entry for Paid Sales
        if (saved.paidAmount > 0) {
            val paymentMode = PaymentMode.CASH // Default payment mode on invoice
            transactionRepository.addTransaction(
                title = "Sale #${saved.invoiceNumber} - ${saved.customerName}",
                amount = saved.paidAmount,
                type = TransactionType.INCOME,
                category = Category.BUSINESS,
                dateMillis = saved.dateMillis,
                paymentMode = paymentMode,
                note = "Bill #${saved.invoiceNumber} payment received"
            )
        }

        // 6. Party Ledger Entry if credit outstanding on a registered party
        if (!saved.customerId.isNullOrBlank() && saved.dueAmount > 0) {
            partyRepository.addKhataEntry(
                partyId = saved.customerId,
                amount = saved.dueAmount,
                type = KhataEntryType.YOU_GAVE,
                dateMillis = saved.dateMillis,
                billNumber = saved.invoiceNumber,
                note = "Bill #${saved.invoiceNumber} credit balance"
            )
        }

        return Result.success(saved)
    }
}
