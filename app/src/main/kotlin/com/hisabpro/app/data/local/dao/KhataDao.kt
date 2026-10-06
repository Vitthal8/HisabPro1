package com.hisabpro.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.hisabpro.app.data.local.entity.KhataEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KhataDao {
    @Query("SELECT * FROM khata_entries WHERE business_id = :businessId AND (deleted_at IS NULL OR deleted_at = 0) ORDER BY date DESC")
    fun getAllEntries(businessId: String): Flow<List<KhataEntryEntity>>

    @Query("SELECT * FROM khata_entries WHERE business_id = :businessId AND (deleted_at IS NULL OR deleted_at = 0) ORDER BY date DESC")
    suspend fun getAllEntriesSync(businessId: String): List<KhataEntryEntity>

    @Query("SELECT * FROM khata_entries WHERE business_id = :businessId AND party_id = :partyId ORDER BY date DESC")
    fun getEntriesForParty(businessId: String, partyId: String): Flow<List<KhataEntryEntity>>

    @Query("SELECT * FROM khata_entries WHERE business_id = :businessId AND party_id = :partyId ORDER BY date DESC")
    suspend fun getEntriesForPartySync(businessId: String, partyId: String): List<KhataEntryEntity>

    @Query("SELECT * FROM khata_entries WHERE id = :id")
    suspend fun getEntryByIdSync(id: String): KhataEntryEntity?

    @Upsert
    suspend fun insertEntry(entry: KhataEntryEntity)

    @Upsert
    suspend fun insertAllEntries(entries: List<KhataEntryEntity>)

    @Query("DELETE FROM khata_entries WHERE id = :id AND business_id = :businessId")
    suspend fun deleteEntry(id: String, businessId: String): Int

    @Query("DELETE FROM khata_entries WHERE id = :id")
    suspend fun deleteEntryLegacy(id: String)

    @Query("DELETE FROM khata_entries WHERE party_id = :partyId AND business_id = :businessId")
    suspend fun deleteEntriesForParty(partyId: String, businessId: String): Int

    @Query("DELETE FROM khata_entries WHERE party_id = :partyId")
    suspend fun deleteEntriesForPartyLegacy(partyId: String)

    @Query("DELETE FROM khata_entries")
    suspend fun deleteAllEntries()

    @Query("SELECT * FROM khata_entries ORDER BY date DESC")
    suspend fun getAllEntriesGlobalSync(): List<KhataEntryEntity>

    @Query("""
        UPDATE khata_entries 
        SET business_id = (SELECT p.business_id FROM parties p WHERE p.id = khata_entries.party_id)
        WHERE EXISTS (
            SELECT 1 FROM parties p 
            WHERE p.id = khata_entries.party_id 
            AND p.business_id != khata_entries.business_id
        )
    """)
    suspend fun repairMisroutedKhataEntries(): Int

    @Query("""
        SELECT COUNT(*) FROM khata_entries k
        INNER JOIN parties p ON k.party_id = p.id
        WHERE k.business_id != p.business_id
    """)
    suspend fun countMisroutedKhataEntries(): Int
}
