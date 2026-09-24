package com.hisabpro.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hisabpro.app.data.local.entity.BusinessEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessDao {
    @Query("SELECT * FROM businesses WHERE id = :id LIMIT 1")
    fun getBusiness(id: String = "default_business"): Flow<BusinessEntity?>

    @Query("SELECT * FROM businesses WHERE id = :id LIMIT 1")
    suspend fun getBusinessSync(id: String = "default_business"): BusinessEntity?

    @Query("SELECT * FROM businesses ORDER BY name ASC")
    fun getAllBusinesses(): Flow<List<BusinessEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(business: BusinessEntity)

    @Query("DELETE FROM businesses WHERE id = :id")
    suspend fun deleteBusiness(id: String)
}
