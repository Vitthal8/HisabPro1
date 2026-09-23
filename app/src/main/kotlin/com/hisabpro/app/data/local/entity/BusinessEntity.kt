package com.hisabpro.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "businesses")
data class BusinessEntity(
    @PrimaryKey val id: String = "default_business",
    val name: String,
    val ownerName: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val gstin: String = "",
    val pan: String = "",
    val logoPath: String = "",
    val gstEnabled: Boolean = false,
    val fyStart: String = "01-04",
    val upiId: String = "",
    val bankName: String = "",
    val accountNumber: String = "",
    val ifscCode: String = "",
    val termsAndConditions: String = ""
)
