package com.hisabpro.app.domain.usecase

import com.hisabpro.app.data.model.Invoice
import com.hisabpro.app.data.repository.InvoiceRepository
import com.hisabpro.app.data.repository.ItemRepository
import com.hisabpro.app.domain.validation.InputValidator

/**
 * Use Case: Safe Invoice Update & Inventory Reconciliation
 * Coordinates:
 * 1. Form validation
 * 2. Calculating differences between previous and updated invoice items
 * 3. Adjusting inventory stock movements with source tracking
 * 4. Persisting updated invoice to Room & local storage
 */
class UpdateInvoiceUseCase(
    private val invoiceRepository: InvoiceRepository,
    private val itemRepository: ItemRepository,
    private val settingsRepository: com.hisabpro.app.data.repository.SettingsRepository? = null
) {

    fun execute(updatedInvoice: Invoice): Result<Invoice> {
        val validation = InputValidator.validateInvoice(updatedInvoice.customerName, updatedInvoice.items.size)
        if (!validation.isSuccess) {
            return Result.failure(IllegalArgumentException(validation.errorMessage ?: "Validation error"))
        }

        val profile = settingsRepository?.profile?.value ?: com.hisabpro.app.data.model.BusinessProfile()
        val sanitizedInvoice = com.hisabpro.app.domain.accounting.GstPolicy.sanitizeInvoiceForStorage(updatedInvoice, profile)

        val taxValidation = InputValidator.validateGstInvoiceTaxRates(sanitizedInvoice, profile.isGstRegistered)
        if (!taxValidation.isSuccess) {
            return Result.failure(IllegalArgumentException(taxValidation.errorMessage ?: "Invalid tax calculation"))
        }

        val oldInvoice = invoiceRepository.invoices.value.find { it.id == sanitizedInvoice.id }
        if (oldInvoice != null) {
            // Reconcile stock for items added, removed, or quantity altered, or status changed
            itemRepository.adjustStockForInvoiceUpdate(oldInvoice, sanitizedInvoice)
        }

        invoiceRepository.updateInvoice(sanitizedInvoice)
        return Result.success(sanitizedInvoice)
    }
}
