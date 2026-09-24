package com.hisabpro.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hisabpro.app.data.local.entity.KhataEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KhataDao {
    @Query("SELECT * FROM khata_entries ORDER BY date DESC")
    fun getAllEntries(): Flow<List<KhataEntryEntity>>

    @Query("SELECT * FROM khata_entries ORDER BY date DESC")
    suspend fun getAllEntriesSync(): List<KhataEntryEntity>

    @Query("SELECT * FROM khata_entries WHERE party_id = :partyId ORDER BY date DESC")
    fun getEntriesForParty(partyId: String): Flow<List<KhataEntryEntity>>

    @Query("SELECT * FROM khata_entries WHERE party_id = :partyId ORDER BY date DESC")
    suspend fun getEntriesForPartySync(partyId: String): List<KhataEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: KhataEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllEntries(entries: List<KhataEntryEntity>)

    @Query("DELETE FROM khata_entries WHERE id = :id")
    suspend fun deleteEntry(id: String)

    @Query("DELETE FROM khata_entries WHERE party_id = :partyId")
    suspend fun deleteEntriesForParty(partyId: String)

    @Query("DELETE FROM khata_entries")
    suspend fun deleteAllEntries()
}
