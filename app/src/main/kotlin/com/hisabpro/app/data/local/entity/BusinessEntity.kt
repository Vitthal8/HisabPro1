package com.hisabpro.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "businesses")
data class BusinessEntity(
    @PrimaryKey
    val id: String = "default_business",
    val name: String,
    @ColumnInfo(name = "owner_name")
    val ownerName: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val gstin: String = "",
    val pan: String = "",
    @ColumnInfo(name = "logo_path")
    val logoPath: String = "",
    @ColumnInfo(name = "gst_enabled")
    val gstEnabled: Boolean = false,
    @ColumnInfo(name = "financial_year_start")
    val financialYearStart: String = "01-04",
    @ColumnInfo(name = "upi_id")
    val upiId: String = "",
    @ColumnInfo(name = "bank_name")
    val bankName: String = "",
    @ColumnInfo(name = "account_number")
    val accountNumber: String = "",
    @ColumnInfo(name = "ifsc_code")
    val ifscCode: String = "",
    @ColumnInfo(name = "terms_and_conditions")
    val termsAndConditions: String = "",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
