package com.hisabpro.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.hisabpro.app.data.local.entity.JournalEntryEntity
import com.hisabpro.app.data.local.entity.JournalEntryLineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries WHERE business_id = :businessId ORDER BY date DESC")
    fun getAllJournalEntries(businessId: String = "default_business"): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entry_lines WHERE journal_entry_id = :journalEntryId")
    fun getLinesForJournalEntry(journalEntryId: String): Flow<List<JournalEntryLineEntity>>

    @Query("SELECT * FROM journal_entry_lines WHERE journal_entry_id = :journalEntryId")
    suspend fun getLinesForJournalEntrySync(journalEntryId: String): List<JournalEntryLineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntry(entry: JournalEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalLines(lines: List<JournalEntryLineEntity>)

    @Query("DELETE FROM journal_entry_lines WHERE journal_entry_id = :journalEntryId")
    suspend fun deleteLinesForJournalEntry(journalEntryId: String)

    @Query("DELETE FROM journal_entries WHERE id = :id")
    suspend fun deleteJournalEntry(id: String)

    @Transaction
    suspend fun insertEntryWithLines(entry: JournalEntryEntity, lines: List<JournalEntryLineEntity>) {
        insertJournalEntry(entry)
        deleteLinesForJournalEntry(entry.id)
        insertJournalLines(lines)
    }

    @Query("SELECT * FROM journal_entries ORDER BY date DESC")
    suspend fun getAllJournalEntriesGlobalSync(): List<JournalEntryEntity>

    @Query("SELECT * FROM journal_entry_lines")
    suspend fun getAllJournalEntryLinesGlobalSync(): List<JournalEntryLineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllJournalEntries(entries: List<JournalEntryEntity>)

    @Query("DELETE FROM journal_entry_lines")
    suspend fun deleteAllJournalLines()

    @Query("DELETE FROM journal_entries")
    suspend fun deleteAllJournalEntries()
}
