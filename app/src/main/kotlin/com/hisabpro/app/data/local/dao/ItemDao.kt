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
    fun getAllItems(businessId: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE business_id = :businessId ORDER BY name ASC")
    suspend fun getAllItemsSync(businessId: String): List<ItemEntity>

    @Query("SELECT * FROM items WHERE business_id = :businessId AND category = :category ORDER BY name ASC")
    fun getItemsByCategory(businessId: String, category: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE business_id = :businessId AND stock_qty <= low_stock_threshold ORDER BY name ASC")
    fun getLowStockItems(businessId: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id LIMIT 1")
    fun getItemById(id: String): Flow<ItemEntity?>

    @Query("SELECT * FROM items WHERE id = :id LIMIT 1")
    suspend fun getItemByIdSync(id: String): ItemEntity?

    @Query("SELECT * FROM items WHERE id = :id AND business_id = :businessId LIMIT 1")
    suspend fun getItemByIdAndBusinessSync(id: String, businessId: String): ItemEntity?

    @Query("SELECT * FROM items WHERE business_id = :businessId AND (name LIKE '%' || :query || '%' OR item_code LIKE '%' || :query || '%') ORDER BY name ASC")
    fun searchItems(businessId: String, query: String): Flow<List<ItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllItems(items: List<ItemEntity>)

    @Update
    suspend fun updateItem(item: ItemEntity)

    @Query("UPDATE items SET stock_qty = :newStock, updated_at = :updatedAt WHERE id = :itemId AND business_id = :businessId")
    suspend fun updateStock(itemId: String, businessId: String, newStock: Double, updatedAt: Long = System.currentTimeMillis()): Int

    @Query("UPDATE items SET stock_qty = :newStock, updated_at = :updatedAt WHERE id = :itemId")
    suspend fun updateStockLegacy(itemId: String, newStock: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM items WHERE id = :id AND business_id = :businessId")
    suspend fun deleteItem(id: String, businessId: String): Int

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteItemLegacy(id: String)

    @Query("SELECT * FROM items ORDER BY name ASC")
    suspend fun getAllItemsGlobalSync(): List<ItemEntity>

    @Query("DELETE FROM items")
    suspend fun deleteAllItems()
}
