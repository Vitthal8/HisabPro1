package com.hisabpro.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.hisabpro.app.data.local.entity.PartyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PartyDao {
    @Query("SELECT * FROM parties WHERE business_id = :businessId ORDER BY name ASC")
    fun getAllParties(businessId: String): Flow<List<PartyEntity>>

    @Query("SELECT * FROM parties WHERE business_id = :businessId ORDER BY name ASC")
    suspend fun getAllPartiesSync(businessId: String): List<PartyEntity>

    @Query("SELECT * FROM parties WHERE business_id = :businessId AND type = :type ORDER BY name ASC")
    fun getPartiesByType(businessId: String, type: String): Flow<List<PartyEntity>>

    @Query("SELECT * FROM parties WHERE id = :id LIMIT 1")
    fun getPartyById(id: String): Flow<PartyEntity?>

    @Query("SELECT * FROM parties WHERE id = :id LIMIT 1")
    suspend fun getPartyByIdSync(id: String): PartyEntity?

    @Query("SELECT * FROM parties WHERE id = :id AND business_id = :businessId LIMIT 1")
    suspend fun getPartyByIdAndBusinessSync(id: String, businessId: String): PartyEntity?

    @Query("SELECT * FROM parties WHERE business_id = :businessId AND (name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%') ORDER BY name ASC")
    fun searchParties(businessId: String, query: String): Flow<List<PartyEntity>>

    @Upsert
    suspend fun insertParty(party: PartyEntity)

    @Upsert
    suspend fun insertAllParties(parties: List<PartyEntity>)

    @Update
    suspend fun updateParty(party: PartyEntity)

    @Query("UPDATE parties SET name = :name, phone = :phone, email = :email, address = :address, gstin = :gstin, type = :type, tag = :tag, updated_at = :updatedAt WHERE id = :id AND business_id = :businessId")
    suspend fun updatePartyScoped(
        id: String,
        businessId: String,
        name: String,
        phone: String,
        email: String,
        address: String,
        gstin: String,
        type: String,
        tag: String,
        updatedAt: Long = System.currentTimeMillis()
    ): Int

    @Query("DELETE FROM parties WHERE id = :id AND business_id = :businessId")
    suspend fun deleteParty(id: String, businessId: String): Int

    @Query("DELETE FROM parties WHERE id = :id")
    suspend fun deletePartyLegacy(id: String)

    @Query("SELECT * FROM parties ORDER BY name ASC")
    suspend fun getAllPartiesGlobalSync(): List<PartyEntity>

    @Query("DELETE FROM parties")
    suspend fun deleteAllParties()
}

