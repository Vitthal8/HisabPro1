package com.hisabpro.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hisabpro.app.data.local.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE business_id = :businessId ORDER BY date DESC")
    fun getAllPayments(businessId: String = "default_business"): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE business_id = :businessId AND party_id = :partyId ORDER BY date DESC")
    fun getPaymentsForParty(businessId: String = "default_business", partyId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE business_id = :businessId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getPaymentsByDateRange(businessId: String = "default_business", startDate: Long, endDate: Long): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Query("DELETE FROM payments WHERE id = :id")
    suspend fun deletePayment(id: String)

    @Query("SELECT * FROM payments WHERE business_id = :businessId ORDER BY date DESC")
    suspend fun getAllPaymentsSync(businessId: String = "default_business"): List<PaymentEntity>

    @Query("SELECT * FROM payments ORDER BY date DESC")
    suspend fun getAllPaymentsGlobalSync(): List<PaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPayments(payments: List<PaymentEntity>)

    @Query("DELETE FROM payments")
    suspend fun deleteAllPayments()
}
