package com.hisabpro.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hisabpro.app.data.local.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE businessId = :businessId ORDER BY dateMillis DESC")
    fun getAllPayments(businessId: String = "default_business"): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE partyId = :partyId ORDER BY dateMillis DESC")
    fun getPaymentsForParty(partyId: String): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Query("DELETE FROM payments WHERE id = :id")
    suspend fun deletePayment(id: String)
}
