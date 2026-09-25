package com.hisabpro.app.domain.validation

import android.content.Context
import androidx.annotation.StringRes
import com.hisabpro.app.R

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(
        val message: String? = null,
        @param:StringRes @get:StringRes val resId: Int? = null
    ) : ValidationResult() {
        fun getLocalizedMessage(context: Context): String {
            return resId?.let { context.getString(it) } ?: message ?: ""
        }
    }

    val isSuccess: Boolean get() = this is Success
    val errorMessage: String? get() = (this as? Error)?.let { err ->
        err.message ?: err.resId?.let { id -> "Validation error code $id" }
    }
}

/**
 * Centralized Input Validation Service for HisabPro.
 * Complies with Indian business, tax, and accounting standards.
 */
object InputValidator {

    // 15-character Indian GSTIN regex: 2 digits (state code), 5 chars (PAN part 1), 4 digits (PAN part 2), 1 char (PAN part 3), 1 char (entity code), 1 char (Z), 1 check char
    private val GSTIN_REGEX = Regex("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$")

    // 10-character Indian PAN regex: 5 chars, 4 digits, 1 char
    private val PAN_REGEX = Regex("^[A-Z]{5}[0-9]{4}[A-Z]{1}$")

    // Standard 10-digit Indian mobile starting with 6, 7, 8, or 9
    private val PHONE_REGEX = Regex("^[6-9]\\d{9}$")

    fun validateGstin(gstin: String): ValidationResult {
        val trimmed = gstin.trim().uppercase()
        if (trimmed.isBlank()) return ValidationResult.Success // GSTIN is optional unless in GST mode
        if (!GSTIN_REGEX.matches(trimmed)) {
            return ValidationResult.Error(
                message = "Invalid GSTIN format. Expected 15 characters (e.g., 27ABCDE1234F1Z5)",
                resId = R.string.err_invalid_gstin
            )
        }
        return ValidationResult.Success
    }

    fun validatePan(pan: String): ValidationResult {
        val trimmed = pan.trim().uppercase()
        if (trimmed.isBlank()) return ValidationResult.Success
        if (!PAN_REGEX.matches(trimmed)) {
            return ValidationResult.Error(
                message = "Invalid PAN format. Expected 10 alphanumeric characters (e.g., ABCDE1234F)",
                resId = R.string.err_invalid_pan
            )
        }
        return ValidationResult.Success
    }

    fun validatePhone(phone: String): ValidationResult {
        val digitsOnly = phone.replace(Regex("[^0-9]"), "")
        val normalized = if (digitsOnly.startsWith("91") && digitsOnly.length == 12) {
            digitsOnly.substring(2)
        } else {
            digitsOnly
        }
        if (normalized.isBlank()) return ValidationResult.Success
        if (!PHONE_REGEX.matches(normalized)) {
            return ValidationResult.Error(
                message = "Invalid phone number. Enter a valid 10-digit Indian mobile number.",
                resId = R.string.err_invalid_phone
            )
        }
        return ValidationResult.Success
    }

    fun validateParty(name: String, phone: String, gstin: String, isGst: Boolean): ValidationResult {
        if (name.trim().isBlank()) {
            return ValidationResult.Error(
                message = "Party name cannot be empty.",
                resId = R.string.err_party_name_empty
            )
        }
        val phoneResult = validatePhone(phone)
        if (!phoneResult.isSuccess) return phoneResult

        if (isGst && gstin.isNotBlank()) {
            val gstResult = validateGstin(gstin)
            if (!gstResult.isSuccess) return gstResult
        }
        return ValidationResult.Success
    }

    fun validateItem(name: String, salePrice: Double, purchasePrice: Double): ValidationResult {
        if (name.trim().isBlank()) {
            return ValidationResult.Error(
                message = "Product name cannot be empty.",
                resId = R.string.err_product_name_empty
            )
        }
        if (salePrice < 0.0) {
            return ValidationResult.Error(
                message = "Selling price cannot be negative.",
                resId = R.string.err_selling_price_negative
            )
        }
        if (purchasePrice < 0.0) {
            return ValidationResult.Error(
                message = "Purchase price cannot be negative.",
                resId = R.string.err_purchase_price_negative
            )
        }
        return ValidationResult.Success
    }

    fun validateInvoice(customerName: String, itemCount: Int): ValidationResult {
        if (customerName.trim().isBlank()) {
            return ValidationResult.Error(
                message = "Customer name is required.",
                resId = R.string.err_customer_name_required
            )
        }
        if (itemCount <= 0) {
            return ValidationResult.Error(
                message = "Please add at least one line item to the bill.",
                resId = R.string.err_add_at_least_one_item
            )
        }
        return ValidationResult.Success
    }

    fun validatePaymentAmount(amount: Double): ValidationResult {
        if (amount <= 0.0) {
            return ValidationResult.Error(
                message = "Payment amount must be greater than ₹0.",
                resId = R.string.err_payment_amount_positive
            )
        }
        return ValidationResult.Success
    }
}
