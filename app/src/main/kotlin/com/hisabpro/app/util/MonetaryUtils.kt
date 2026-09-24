package com.hisabpro.app.util

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Utility functions for precise monetary representation in Room DB (storing values as Long paise).
 */
fun Double.toPaise(): Long = try {
    BigDecimal.valueOf(this)
        .setScale(2, RoundingMode.HALF_UP)
        .multiply(BigDecimal(100))
        .longValueExact()
} catch (e: Exception) {
    Math.round(this * 100.0)
}

fun Long.toRupees(): Double = BigDecimal.valueOf(this)
    .divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
    .toDouble()

fun BigDecimal.toPaise(): Long = this
    .setScale(2, RoundingMode.HALF_UP)
    .multiply(BigDecimal(100))
    .longValueExact()

fun Long.toBigDecimalRupees(): BigDecimal = BigDecimal.valueOf(this)
    .divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
