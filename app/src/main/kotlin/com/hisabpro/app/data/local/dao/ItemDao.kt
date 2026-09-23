package com.hisabpro.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hisabpro.app.data.local.entity.ItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM items WHERE businessId = :businessId ORDER BY name ASC")
    fun getAllItems(businessId: String = "default_business"): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE businessId = :businessId ORDER BY name ASC")
    suspend fun getAllItemsSync(businessId: String = "default_business"): List<ItemEntity>

    @Query("SELECT * FROM items WHERE id = :id LIMIT 1")
    fun getItemById(id: String): Flow<ItemEntity?>

    @Query("SELECT * FROM items WHERE id = :id LIMIT 1")
    suspend fun getItemByIdSync(id: String): ItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllItems(items: List<ItemEntity>)

    @Update
    suspend fun updateItem(item: ItemEntity)

    @Query("UPDATE items SET stockQty = :newStock WHERE id = :itemId")
    suspend fun updateStock(itemId: String, newStock: Double)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteItem(id: String)
}
