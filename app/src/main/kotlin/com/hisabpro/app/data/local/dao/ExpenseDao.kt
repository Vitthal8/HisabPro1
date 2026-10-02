package com.hisabpro.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hisabpro.app.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE business_id = :businessId AND (deleted_at IS NULL OR deleted_at = 0) ORDER BY date DESC")
    fun getAllExpenses(businessId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE business_id = :businessId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getExpensesByDateRange(businessId: String, startDate: Long, endDate: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE business_id = :businessId AND date BETWEEN :startDate AND :endDate")
    fun getTotalExpensesByDateRange(businessId: String, startDate: Long, endDate: Long): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id AND business_id = :businessId")
    suspend fun deleteExpense(id: String, businessId: String): Int

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseLegacy(id: String)

    @Query("SELECT * FROM expenses WHERE business_id = :businessId AND (deleted_at IS NULL OR deleted_at = 0) ORDER BY date DESC")
    suspend fun getAllExpensesSync(businessId: String): List<ExpenseEntity>

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    suspend fun getAllExpensesGlobalSync(): List<ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllExpenses(expenses: List<ExpenseEntity>)

    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()
}
