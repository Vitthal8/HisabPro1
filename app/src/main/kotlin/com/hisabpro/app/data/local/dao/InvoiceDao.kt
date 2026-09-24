package com.hisabpro.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.hisabpro.app.data.local.entity.InvoiceEntity
import com.hisabpro.app.data.local.entity.InvoiceItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices WHERE business_id = :businessId ORDER BY date DESC")
    fun getAllInvoices(businessId: String = "default_business"): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE business_id = :businessId ORDER BY date DESC")
    suspend fun getAllInvoicesSync(businessId: String = "default_business"): List<InvoiceEntity>

    @Query("SELECT * FROM invoices WHERE business_id = :businessId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getInvoicesByDateRange(businessId: String = "default_business", startDate: Long, endDate: Long): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE business_id = :businessId AND party_id = :partyId ORDER BY date DESC")
    fun getInvoicesForParty(businessId: String = "default_business", partyId: String): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    fun getInvoiceById(id: String): Flow<InvoiceEntity?>

    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    suspend fun getInvoiceByIdSync(id: String): InvoiceEntity?

    @Query("SELECT * FROM invoice_items WHERE invoice_id = :invoiceId")
    fun getItemsForInvoice(invoiceId: String): Flow<List<InvoiceItemEntity>>

    @Query("SELECT * FROM invoice_items WHERE invoice_id = :invoiceId")
    suspend fun getItemsForInvoiceSync(invoiceId: String): List<InvoiceItemEntity>

    @Query("SELECT COALESCE(SUM(total), 0) FROM invoices WHERE business_id = :businessId AND date BETWEEN :startDate AND :endDate")
    fun getTotalSalesByDateRange(businessId: String = "default_business", startDate: Long, endDate: Long): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceItems(items: List<InvoiceItemEntity>)

    @Update
    suspend fun updateInvoice(invoice: InvoiceEntity)

    @Query("DELETE FROM invoice_items WHERE invoice_id = :invoiceId")
    suspend fun deleteItemsForInvoice(invoiceId: String)

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun deleteInvoice(id: String)

    @Transaction
    suspend fun insertInvoiceWithItems(invoice: InvoiceEntity, items: List<InvoiceItemEntity>) {
        insertInvoice(invoice)
        deleteItemsForInvoice(invoice.id)
        insertInvoiceItems(items)
    }

    @Transaction
    suspend fun deleteInvoiceWithItems(invoiceId: String) {
        deleteItemsForInvoice(invoiceId)
        deleteInvoice(invoiceId)
    }
}
