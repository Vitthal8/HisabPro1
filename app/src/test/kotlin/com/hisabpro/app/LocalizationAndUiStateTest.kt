package com.hisabpro.app

import com.hisabpro.app.domain.validation.InputValidator
import com.hisabpro.app.domain.validation.ValidationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalizationAndUiStateTest {

    @Test
    fun testValidationResultLocalizationKeys() {
        val gstinError = InputValidator.validateGstin("INVALID_GSTIN")
        assertTrue(gstinError is ValidationResult.Error)
        val err = gstinError as ValidationResult.Error

        assertNotNull(err.resId)
        assertEquals(R.string.err_invalid_gstin, err.resId)
        assertTrue(err.errorMessage!!.contains("Invalid GSTIN"))
    }

    @Test
    fun testInputValidatorPhoneKeys() {
        val phoneError = InputValidator.validatePhone("123")
        assertTrue(phoneError is ValidationResult.Error)
        val err = phoneError as ValidationResult.Error

        assertNotNull(err.resId)
        assertEquals(R.string.err_invalid_phone, err.resId)
    }

    @Test
    fun testInputValidatorEmptyFieldsKeys() {
        val partyError = InputValidator.validateParty("", "9876543210", "", isGst = false)
        assertTrue(partyError is ValidationResult.Error)
        val pErr = partyError as ValidationResult.Error
        assertEquals(R.string.err_party_name_empty, pErr.resId)

        val itemError = InputValidator.validateItem("", 100.0, 80.0)
        assertTrue(itemError is ValidationResult.Error)
        val iErr = itemError as ValidationResult.Error
        assertEquals(R.string.err_product_name_empty, iErr.resId)

        val invError = InputValidator.validateInvoice("", 2)
        assertTrue(invError is ValidationResult.Error)
        val invErr = invError as ValidationResult.Error
        assertEquals(R.string.err_customer_name_required, invErr.resId)
    }

    @Test
    fun testLanguageSelectionCodes() {
        val supportedCodes = listOf("en", "hi", "mr")

        assertTrue(supportedCodes.contains("en"))
        assertTrue(supportedCodes.contains("hi"))
        assertTrue(supportedCodes.contains("mr"))
    }
}
