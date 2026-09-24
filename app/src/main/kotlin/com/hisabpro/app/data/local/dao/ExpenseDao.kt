package com.hisabpro.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hisabpro.app.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE business_id = :businessId ORDER BY date DESC")
    fun getAllExpenses(businessId: String = "default_business"): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE business_id = :businessId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getExpensesByDateRange(businessId: String = "default_business", startDate: Long, endDate: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE business_id = :businessId AND date BETWEEN :startDate AND :endDate")
    fun getTotalExpensesByDateRange(businessId: String = "default_business", startDate: Long, endDate: Long): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpense(id: String)

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    suspend fun getAllExpensesGlobalSync(): List<ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllExpenses(expenses: List<ExpenseEntity>)

    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()
}
