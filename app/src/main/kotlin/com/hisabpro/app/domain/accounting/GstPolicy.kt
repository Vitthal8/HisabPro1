package com.hisabpro.app.domain.accounting

import com.hisabpro.app.data.model.BusinessProfile
import com.hisabpro.app.data.model.GstMode
import com.hisabpro.app.data.model.Invoice
import com.hisabpro.app.data.model.InvoiceType

/**
 * Single Source of Truth for GST Application Policy in HisabPro.
 *
 * RULES:
 * 1. Non-GST Business (!isGstRegistered):
 *    - A non-GST business can NEVER produce GST tax output under any circumstances.
 *    - Document type is forced to NON_GST_BILL (or PROFORMA for quotation).
 *    - cgst = sgst = igst = 0, gst_rate is not applied (0% tax), no GSTIN or HSN required.
 *
 * 2. GST Registered Business (isGstRegistered):
 *    - Composition Scheme (isCompositionScheme == true):
 *      // FLAG FOR VERIFICATION: Under Section 10 of CGST Act 2017, Composition Scheme dealers
 *      // issue a "Bill of Supply" and CANNOT collect tax from buyers or issue Tax Invoices.
 *      // Handled safely by disabling tax collection on composition documents (0 tax output).
 *    - Regular Scheme: TAX_INVOICE supports Intra-State (CGST + SGST) and Inter-State (IGST)
 *      based on business state and customer state.
 */
object GstPolicy {

    /**
     * Determines whether GST tax logic applies to a given document/invoice
     * for a given business profile.
     */
    fun isGstApplicable(
        profile: BusinessProfile,
        invoiceType: InvoiceType = InvoiceType.TAX_INVOICE,
        gstMode: GstMode = GstMode.INTRA_STATE
    ): Boolean {
        return isGstApplicable(
            isGstRegistered = profile.isGstRegistered,
            isCompositionScheme = profile.isCompositionScheme,
            invoiceType = invoiceType,
            gstMode = gstMode
        )
    }

    /**
     * Determines whether GST tax logic applies based on flags.
     */
    fun isGstApplicable(
        isGstRegistered: Boolean,
        isCompositionScheme: Boolean = false,
        invoiceType: InvoiceType = InvoiceType.TAX_INVOICE,
        gstMode: GstMode = GstMode.INTRA_STATE
    ): Boolean {
        if (!isGstRegistered) return false
        if (isCompositionScheme) return false // Composition scheme cannot collect GST tax
        if (invoiceType == InvoiceType.NON_GST_BILL) return false
        if (gstMode == GstMode.EXEMPT) return false
        return true
    }

    /**
     * Resolves the effective InvoiceType for a document.
     * Non-GST business always resolves to NON_GST_BILL (or PROFORMA for quotation).
     */
    fun resolveEffectiveInvoiceType(
        profile: BusinessProfile,
        requestedType: InvoiceType
    ): InvoiceType {
        if (!profile.isGstRegistered || profile.isCompositionScheme) {
            return if (requestedType == InvoiceType.PROFORMA) InvoiceType.PROFORMA else InvoiceType.NON_GST_BILL
        }
        return requestedType
    }

    /**
     * Resolves effective GstMode for an invoice document.
     */
    fun resolveEffectiveGstMode(
        profile: BusinessProfile,
        requestedType: InvoiceType,
        determinedMode: GstMode
    ): GstMode {
        if (!isGstApplicable(profile, requestedType, determinedMode)) {
            return GstMode.EXEMPT
        }
        return determinedMode
    }

    /**
     * Sanitizes an invoice before saving/persisting to ensure a non-GST business
     * stored invoice never holds misleading GST amounts or modes.
     */
    fun sanitizeInvoiceForStorage(
        invoice: Invoice,
        profile: BusinessProfile
    ): Invoice {
        val effectiveType = resolveEffectiveInvoiceType(profile, invoice.type)
        val effectiveMode = resolveEffectiveGstMode(profile, invoice.type, invoice.gstMode)

        return invoice.copy(
            type = effectiveType,
            gstMode = effectiveMode
        )
    }
}
