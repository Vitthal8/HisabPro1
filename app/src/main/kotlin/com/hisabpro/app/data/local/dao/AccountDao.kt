package com.hisabpro.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hisabpro.app.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE business_id = :businessId ORDER BY name ASC")
    fun getAllAccounts(businessId: String = "default_business"): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE business_id = :businessId AND type = :type ORDER BY name ASC")
    fun getAccountsByType(businessId: String = "default_business", type: String): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountByIdSync(id: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccount(id: String)

    @Query("SELECT * FROM accounts ORDER BY name ASC")
    suspend fun getAllAccountsGlobalSync(): List<AccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAccounts(accounts: List<AccountEntity>)

    @Query("DELETE FROM accounts")
    suspend fun deleteAllAccounts()
}
