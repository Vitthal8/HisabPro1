package com.hisabpro.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.hisabpro.app.data.local.dao.AccountDao
import com.hisabpro.app.data.local.dao.BusinessDao
import com.hisabpro.app.data.local.dao.ExpenseDao
import com.hisabpro.app.data.local.dao.InvoiceDao
import com.hisabpro.app.data.local.dao.ItemDao
import com.hisabpro.app.data.local.dao.JournalDao
import com.hisabpro.app.data.local.dao.KhataDao
import com.hisabpro.app.data.local.dao.PartyDao
import com.hisabpro.app.data.local.dao.PaymentDao
import com.hisabpro.app.data.local.entity.AccountEntity
import com.hisabpro.app.data.local.entity.BusinessEntity
import com.hisabpro.app.data.local.entity.ExpenseEntity
import com.hisabpro.app.data.local.entity.InvoiceEntity
import com.hisabpro.app.data.local.entity.InvoiceItemEntity
import com.hisabpro.app.data.local.entity.ItemEntity
import com.hisabpro.app.data.local.entity.JournalEntryEntity
import com.hisabpro.app.data.local.entity.JournalEntryLineEntity
import com.hisabpro.app.data.local.entity.KhataEntryEntity
import com.hisabpro.app.data.local.entity.PartyEntity
import com.hisabpro.app.data.local.entity.PaymentEntity

@Database(
    entities = [
        BusinessEntity::class,
        PartyEntity::class,
        KhataEntryEntity::class,
        ItemEntity::class,
        InvoiceEntity::class,
        InvoiceItemEntity::class,
        PaymentEntity::class,
        ExpenseEntity::class,
        AccountEntity::class,
        JournalEntryEntity::class,
        JournalEntryLineEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun businessDao(): BusinessDao
    abstract fun partyDao(): PartyDao
    abstract fun khataDao(): KhataDao
    abstract fun itemDao(): ItemDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun paymentDao(): PaymentDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun accountDao(): AccountDao
    abstract fun journalDao(): JournalDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hisabpro_database"
                )
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                DatabaseMigrationHelper.migrateIfNecessary(context.applicationContext, instance)
                instance
            }
        }
    }
}
