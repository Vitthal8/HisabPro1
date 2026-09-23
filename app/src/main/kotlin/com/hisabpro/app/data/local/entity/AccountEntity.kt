package com.hisabpro.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "accounts",
    indices = [
        Index(value = ["businessId"]),
        Index(value = ["type"])
    ]
)
data class AccountEntity(
    @PrimaryKey val id: String,
    val businessId: String = "default_business",
    val name: String,
    val type: String, // CASH, BANK, ASSET, LIABILITY, EQUITY
    val openingBalance: Double = 0.0,
    val accountNumber: String = "",
    val ifscCode: String = ""
)
