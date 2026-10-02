package com.hisabpro.app.util

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * HisabPro Precision Monetary Utility
 *
 * RULES:
 * 1. Storage: All monetary amounts across entities and models are stored as Long paise (1 Rupee = 100 Paise).
 * 2. Calculation: All intermediate financial calculations (line totals, tax amounts, running balances, daybook totals)
 *    use BigDecimal with scale 2 and RoundingMode.HALF_UP to eliminate floating-point drift (e.g. 0.1 + 0.2).
 * 3. Rounding Boundaries: Rounding is performed ONLY at defined boundaries:
 *    - Per-line GST tax calculation.
 *    - Invoice grand total & balance due calculation.
 */
fun Double.toPaise(): Long = try {
    BigDecimal.valueOf(this)
        .setScale(2, RoundingMode.HALF_UP)
        .multiply(BigDecimal("100"))
        .longValueExact()
} catch (e: Exception) {
    Math.round(this * 100.0)
}

fun Long.toRupees(): Double = BigDecimal.valueOf(this)
    .divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)
    .toDouble()

fun BigDecimal.toPaise(): Long = this
    .setScale(2, RoundingMode.HALF_UP)
    .multiply(BigDecimal("100"))
    .longValueExact()

fun Long.toBigDecimalRupees(): BigDecimal = BigDecimal.valueOf(this)
    .divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)

fun Long.toBigDecimalPaise(): BigDecimal = BigDecimal.valueOf(this)

fun Double.toBigDecimalMoney(): BigDecimal = BigDecimal.valueOf(this)
    .setScale(2, RoundingMode.HALF_UP)

fun BigDecimal.roundMoney(): BigDecimal = this.setScale(2, RoundingMode.HALF_UP)

fun BigDecimal.paiseToBigDecimalRupees(): BigDecimal = this
    .divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)

fun BigDecimal.rupeesToPaiseLong(): Long = this
    .multiply(BigDecimal("100"))
    .setScale(0, RoundingMode.HALF_UP)
    .longValueExact()
