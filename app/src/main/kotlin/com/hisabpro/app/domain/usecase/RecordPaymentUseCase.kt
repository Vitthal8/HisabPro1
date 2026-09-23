package com.hisabpro.app.domain.usecase

import com.hisabpro.app.data.model.Category
import com.hisabpro.app.data.model.KhataEntryType
import com.hisabpro.app.data.model.PaymentMode
import com.hisabpro.app.data.model.TransactionType
import com.hisabpro.app.data.repository.InvoiceRepository
import com.hisabpro.app.data.repository.PartyRepository
import com.hisabpro.app.data.repository.TransactionRepository
import com.hisabpro.app.domain.validation.InputValidator
import com.hisabpro.app.ui.payments.PaymentDirection

/**
 * Use Case: Atomic Payment Processing
 * Coordinates:
 * 1. Party ledger Khata update (Receivable reduction or Payable settlement)
 * 2. Cashbook / Bank Book transaction posting
 * 3. Optional invoice paid status update
 */
class RecordPaymentUseCase(
    private val partyRepository: PartyRepository,
    private val transactionRepository: TransactionRepository,
    private val invoiceRepository: InvoiceRepository
) {

    fun execute(
        partyId: String,
        amount: Double,
        direction: PaymentDirection,
        paymentMode: PaymentMode,
        referenceNo: String,
        notes: String,
        linkedInvoiceId: String? = null
    ): Result<Unit> {
        val amountCheck = InputValidator.validatePaymentAmount(amount)
        if (!amountCheck.isSuccess) {
            return Result.failure(IllegalArgumentException(amountCheck.errorMessage ?: "Invalid amount"))
        }

        val partyName = partyRepository.parties.value.find { it.id == partyId }?.name ?: "Party"
        val isReceipt = direction == PaymentDirection.RECEIPT_IN

        val entryType = if (isReceipt) KhataEntryType.YOU_GOT else KhataEntryType.YOU_GAVE
        val actionLabel = if (isReceipt) "Payment Received" else "Payment Made"

        val noteCombined = buildString {
            append("$actionLabel via ${paymentMode.label}")
            if (referenceNo.isNotBlank()) append(" (Ref: $referenceNo)")
            if (notes.isNotBlank()) append(" - $notes")
        }

        // 1. Post to Party Khata Ledger
        partyRepository.addKhataEntry(
            partyId = partyId,
            amount = amount,
            type = entryType,
            dateMillis = System.currentTimeMillis(),
            billNumber = referenceNo,
            note = noteCombined
        )

        // 2. Post to Cashbook / Bank Book
        val transType = if (isReceipt) TransactionType.INCOME else TransactionType.EXPENSE
        transactionRepository.addTransaction(
            title = "$actionLabel - $partyName",
            amount = amount,
            type = transType,
            category = Category.BUSINESS,
            dateMillis = System.currentTimeMillis(),
            paymentMode = paymentMode,
            note = noteCombined
        )

        // 3. Update linked invoice if specified
        if (!linkedInvoiceId.isNullOrBlank()) {
            val invoice = invoiceRepository.invoices.value.find { it.id == linkedInvoiceId }
            if (invoice != null) {
                val newPaid = (invoice.paidAmount + amount).coerceAtMost(invoice.grandTotal)
                val newStatus = if (newPaid >= invoice.grandTotal) {
                    com.hisabpro.app.data.model.InvoiceStatus.PAID
                } else {
                    com.hisabpro.app.data.model.InvoiceStatus.PARTIAL
                }
                invoiceRepository.updateInvoice(
                    invoice.copy(
                        paidAmount = newPaid,
                        paymentStatus = newStatus
                    )
                )
            }
        }

        return Result.success(Unit)
    }
}
