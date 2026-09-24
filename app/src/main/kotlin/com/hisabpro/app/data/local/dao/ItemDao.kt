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
    @Query("SELECT * FROM items WHERE business_id = :businessId ORDER BY name ASC")
    fun getAllItems(businessId: String = "default_business"): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE business_id = :businessId ORDER BY name ASC")
    suspend fun getAllItemsSync(businessId: String = "default_business"): List<ItemEntity>

    @Query("SELECT * FROM items WHERE business_id = :businessId AND category = :category ORDER BY name ASC")
    fun getItemsByCategory(businessId: String = "default_business", category: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE business_id = :businessId AND stock_qty <= low_stock_threshold ORDER BY name ASC")
    fun getLowStockItems(businessId: String = "default_business"): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id LIMIT 1")
    fun getItemById(id: String): Flow<ItemEntity?>

    @Query("SELECT * FROM items WHERE id = :id LIMIT 1")
    suspend fun getItemByIdSync(id: String): ItemEntity?

    @Query("SELECT * FROM items WHERE business_id = :businessId AND (name LIKE '%' || :query || '%' OR item_code LIKE '%' || :query || '%') ORDER BY name ASC")
    fun searchItems(businessId: String = "default_business", query: String): Flow<List<ItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllItems(items: List<ItemEntity>)

    @Update
    suspend fun updateItem(item: ItemEntity)

    @Query("UPDATE items SET stock_qty = :newStock, updated_at = :updatedAt WHERE id = :itemId")
    suspend fun updateStock(itemId: String, newStock: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteItem(id: String)
}
