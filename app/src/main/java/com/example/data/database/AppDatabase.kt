package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.dao.AccountDao
import com.example.data.dao.BusinessDao
import com.example.data.dao.ExpenseDao
import com.example.data.dao.InvoiceDao
import com.example.data.dao.InvoiceItemDao
import com.example.data.dao.ItemDao
import com.example.data.dao.JournalEntryDao
import com.example.data.dao.PartyDao
import com.example.data.dao.PaymentDao
import com.example.data.model.Account
import com.example.data.model.Business
import com.example.data.model.Converters
import com.example.data.model.Expense
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.data.model.Item
import com.example.data.model.JournalEntry
import com.example.data.model.Party
import com.example.data.model.Payment

@Database(
    entities = [
        Business::class,
        Party::class,
        Item::class,
        Invoice::class,
        InvoiceItem::class,
        Payment::class,
        Expense::class,
        Account::class,
        JournalEntry::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun businessDao(): BusinessDao
    abstract fun partyDao(): PartyDao
    abstract fun itemDao(): ItemDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun invoiceItemDao(): InvoiceItemDao
    abstract fun paymentDao(): PaymentDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun accountDao(): AccountDao
    abstract fun journalEntryDao(): JournalEntryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hisabpro_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
