package com.hisabpro.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.hisabpro.app.data.local.entity.BusinessEntity
import com.hisabpro.app.data.local.entity.SyncOutbox
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface BusinessDao {
    @Query("SELECT * FROM businesses WHERE id = :id LIMIT 1")
    fun getBusiness(id: String): Flow<BusinessEntity?>

    @Query("SELECT * FROM businesses WHERE id = :id LIMIT 1")
    suspend fun getBusinessSync(id: String): BusinessEntity?

    @Query("SELECT * FROM businesses WHERE deleted_at IS NULL ORDER BY name ASC")
    fun getAllBusinesses(): Flow<List<BusinessEntity>>

    @Query("SELECT * FROM businesses WHERE deleted_at IS NULL ORDER BY name ASC")
    suspend fun getAllBusinessesSync(): List<BusinessEntity>

    @Upsert
    suspend fun insertOrUpdate(business: BusinessEntity)

    @Upsert
    suspend fun insertAllBusinesses(businesses: List<BusinessEntity>)

    @Query("DELETE FROM businesses WHERE id = :id")
    suspend fun deleteBusiness(id: String)

    @Query("DELETE FROM businesses")
    suspend fun deleteAllBusinesses()

    @Query("DELETE FROM invoice_items WHERE invoice_id IN (SELECT id FROM invoices WHERE business_id = :businessId)")
    suspend fun deleteInvoiceItemsForBusiness(businessId: String)

    @Query("DELETE FROM invoices WHERE business_id = :businessId")
    suspend fun deleteInvoicesForBusiness(businessId: String)

    @Query("DELETE FROM expenses WHERE business_id = :businessId")
    suspend fun deleteExpensesForBusiness(businessId: String)

    @Query("DELETE FROM payments WHERE business_id = :businessId")
    suspend fun deletePaymentsForBusiness(businessId: String)

    @Query("DELETE FROM khata_entries WHERE business_id = :businessId")
    suspend fun deleteKhataEntriesForBusiness(businessId: String)

    @Query("DELETE FROM journal_entry_lines WHERE journal_entry_id IN (SELECT id FROM journal_entries WHERE business_id = :businessId)")
    suspend fun deleteJournalLinesForBusiness(businessId: String)

    @Query("DELETE FROM journal_entries WHERE business_id = :businessId")
    suspend fun deleteJournalEntriesForBusiness(businessId: String)

    @Query("DELETE FROM items WHERE business_id = :businessId")
    suspend fun deleteItemsForBusiness(businessId: String)

    @Query("DELETE FROM parties WHERE business_id = :businessId")
    suspend fun deletePartiesForBusiness(businessId: String)

    @Query("DELETE FROM accounts WHERE business_id = :businessId")
    suspend fun deleteAccountsForBusiness(businessId: String)

    @Query("DELETE FROM businesses WHERE id = :businessId")
    suspend fun deleteBusinessRecord(businessId: String)

    @Query("UPDATE businesses SET deleted_at = :now, updated_at = :now WHERE id = :businessId")
    suspend fun markBusinessDeletedTombstone(businessId: String, now: Long)

    /**
     * Atomically wipes all dependent local business database records (invoices, items, expenses, etc.)
     * and sets an explicit Tombstone (deleted_at = now, updated_at = now) on the business record
     * to enforce Last-Write-Wins and prevent cloud resurrection.
     */
    @Transaction
    suspend fun deleteBusinessCascadeLocally(
        businessId: String,
        outboxDao: SyncOutboxDao,
        eventIdGenerator: () -> String = { UUID.randomUUID().toString() }
    ) {
        deleteInvoiceItemsForBusiness(businessId)
        deleteInvoicesForBusiness(businessId)
        deleteExpensesForBusiness(businessId)
        deletePaymentsForBusiness(businessId)
        deleteKhataEntriesForBusiness(businessId)
        deleteJournalLinesForBusiness(businessId)
        deleteJournalEntriesForBusiness(businessId)
        deleteItemsForBusiness(businessId)
        deletePartiesForBusiness(businessId)
        deleteAccountsForBusiness(businessId)

        val now = System.currentTimeMillis()
        val existing = getBusinessSync(businessId)
        if (existing != null) {
            markBusinessDeletedTombstone(businessId, now)
        } else {
            insertOrUpdate(BusinessEntity(id = businessId, name = businessId, deletedAt = now, updatedAt = now))
        }

        val idempotencyKey = "del_biz_${businessId}_$now"
        val payloadJson = "{\"id\":\"$businessId\",\"idempotency_key\":\"$idempotencyKey\"}"

        val outboxEvent = SyncOutbox(
            eventId = eventIdGenerator(),
            entityId = businessId,
            tableName = "businesses",
            operationType = "DELETE",
            payload = payloadJson,
            createdAt = now
        )

        outboxDao.insert(outboxEvent)
    }
}
