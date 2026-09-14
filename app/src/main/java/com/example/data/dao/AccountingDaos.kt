package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Account
import com.example.data.model.Business
import com.example.data.model.Expense
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.data.model.Item
import com.example.data.model.JournalEntry
import com.example.data.model.Party
import com.example.data.model.PartyType
import com.example.data.model.Payment
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessDao {
    @Query("SELECT * FROM businesses LIMIT 1")
    fun getPrimaryBusiness(): Flow<Business?>

    @Query("SELECT * FROM businesses LIMIT 1")
    suspend fun getPrimaryBusinessSync(): Business?

    @Query("SELECT * FROM businesses WHERE id = :id")
    fun getBusinessById(id: Long): Flow<Business?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBusiness(business: Business): Long

    @Update
    suspend fun updateBusiness(business: Business)
}

@Dao
interface PartyDao {
    @Query("SELECT * FROM parties WHERE business_id = :businessId ORDER BY name ASC")
    fun getAllParties(businessId: Long = 1): Flow<List<Party>>

    @Query("SELECT * FROM parties WHERE business_id = :businessId AND (type = :type OR type = 'BOTH') ORDER BY name ASC")
    fun getPartiesByType(businessId: Long = 1, type: PartyType): Flow<List<Party>>

    @Query("SELECT * FROM parties WHERE id = :id")
    suspend fun getPartyById(id: Long): Party?

    @Query("SELECT * FROM parties WHERE id = :id")
    fun getPartyFlow(id: Long): Flow<Party?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParty(party: Party): Long

    @Update
    suspend fun updateParty(party: Party)

    @Delete
    suspend fun deleteParty(party: Party)

    @Query("UPDATE parties SET current_balance = current_balance + :delta WHERE id = :partyId")
    suspend fun updatePartyBalance(partyId: Long, delta: Double)
}

@Dao
interface ItemDao {
    @Query("SELECT * FROM items WHERE business_id = :businessId ORDER BY name ASC")
    fun getAllItems(businessId: Long = 1): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getItemById(id: Long): Item?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: Item): Long

    @Update
    suspend fun updateItem(item: Item)

    @Delete
    suspend fun deleteItem(item: Item)

    @Query("UPDATE items SET stock_qty = stock_qty + :delta WHERE id = :itemId")
    suspend fun updateStock(itemId: Long, delta: Double)
}

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices WHERE business_id = :businessId ORDER BY date DESC, id DESC")
    fun getAllInvoices(businessId: Long = 1): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getInvoiceById(id: Long): Invoice?

    @Query("SELECT * FROM invoices WHERE party_id = :partyId ORDER BY date DESC")
    fun getInvoicesForParty(partyId: Long): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE business_id = :businessId AND date >= :startOfDay AND date <= :endOfDay")
    fun getTodayInvoices(businessId: Long = 1, startOfDay: Long, endOfDay: Long): Flow<List<Invoice>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: Invoice): Long

    @Update
    suspend fun updateInvoice(invoice: Invoice)

    @Delete
    suspend fun deleteInvoice(invoice: Invoice)

    @Query("SELECT COUNT(*) FROM invoices WHERE business_id = :businessId")
    suspend fun getInvoiceCount(businessId: Long = 1): Int
}

@Dao
interface InvoiceItemDao {
    @Query("SELECT * FROM invoice_items WHERE invoice_id = :invoiceId")
    fun getItemsForInvoice(invoiceId: Long): Flow<List<InvoiceItem>>

    @Query("SELECT * FROM invoice_items WHERE invoice_id = :invoiceId")
    suspend fun getItemsForInvoiceSync(invoiceId: Long): List<InvoiceItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceItems(items: List<InvoiceItem>)

    @Query("DELETE FROM invoice_items WHERE invoice_id = :invoiceId")
    suspend fun deleteItemsForInvoice(invoiceId: Long)
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE business_id = :businessId ORDER BY date DESC, id DESC")
    fun getAllPayments(businessId: Long = 1): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE party_id = :partyId ORDER BY date DESC")
    fun getPaymentsForParty(partyId: Long): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE business_id = :businessId AND date >= :startOfDay AND date <= :endOfDay")
    fun getTodayPayments(businessId: Long = 1, startOfDay: Long, endOfDay: Long): Flow<List<Payment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment): Long

    @Delete
    suspend fun deletePayment(payment: Payment)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE business_id = :businessId ORDER BY date DESC")
    fun getAllExpenses(businessId: Long = 1): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Delete
    suspend fun deleteExpense(expense: Expense)
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE business_id = :businessId")
    fun getAllAccounts(businessId: Long = 1): Flow<List<Account>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account): Long

    @Query("UPDATE accounts SET current_balance = current_balance + :delta WHERE id = :accountId")
    suspend fun updateAccountBalance(accountId: Long, delta: Double)
}

@Dao
interface JournalEntryDao {
    @Query("SELECT * FROM journal_entries WHERE business_id = :businessId ORDER BY date DESC")
    fun getAllJournalEntries(businessId: Long = 1): Flow<List<JournalEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntry(entry: JournalEntry): Long
}
