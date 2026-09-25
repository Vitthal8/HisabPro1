package com.hisabpro.app.domain.usecase

import com.hisabpro.app.data.repository.InvoiceRepository
import com.hisabpro.app.data.repository.ItemRepository

/**
 * Use Case: Safe Invoice Cancellation & Deletion
 * Coordinates:
 * 1. Restoring inventory stock for all invoice line items
 * 2. Deleting the invoice from persistent storage
 */
class DeleteInvoiceUseCase(
    private val invoiceRepository: InvoiceRepository,
    private val itemRepository: ItemRepository
) {

    fun execute(invoiceId: String): Result<Unit> {
        val invoice = invoiceRepository.invoices.value.find { it.id == invoiceId }
            ?: return Result.failure(NoSuchElementException("Invoice not found"))

        // 1. Restore Stock for each item if invoice was not already cancelled
        if (invoice.paymentStatus != com.hisabpro.app.data.model.InvoiceStatus.CANCELLED) {
            invoice.items.forEach { lineItem ->
                itemRepository.restoreStockForInvoiceItem(
                    itemNameOrId = lineItem.description,
                    quantity = lineItem.quantity,
                    invoiceNumber = invoice.invoiceNumber,
                    sourceTransactionId = invoice.id,
                    reason = com.hisabpro.app.data.model.StockReason.SALES_RETURN,
                    notePrefix = "Restored: Deleted Invoice #"
                )
            }
        }

        // 2. Delete Invoice from Repository & Room
        invoiceRepository.deleteInvoice(invoiceId)

        return Result.success(Unit)
    }
}
