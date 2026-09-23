package com.hisabpro.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "parties",
    indices = [
        Index(value = ["businessId"]),
        Index(value = ["phone"])
    ]
)
data class PartyEntity(
    @PrimaryKey val id: String,
    val businessId: String = "default_business",
    val name: String,
    val phone: String,
    val email: String = "",
    val address: String = "",
    val gstin: String = "",
    val type: String = "CUSTOMER", // CUSTOMER, SUPPLIER
    val tag: String = "REGULAR",   // REGULAR, OCCASIONAL, BLOCKED
    val openingBalance: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)
