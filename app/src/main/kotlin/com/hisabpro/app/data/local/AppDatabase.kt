package com.hisabpro.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hisabpro.app.data.local.dao.AccountDao
import com.hisabpro.app.data.local.dao.BusinessDao
import com.hisabpro.app.data.local.dao.ExpenseDao
import com.hisabpro.app.data.local.dao.InvoiceDao
import com.hisabpro.app.data.local.dao.ItemDao
import com.hisabpro.app.data.local.dao.JournalDao
import com.hisabpro.app.data.local.dao.KhataDao
import com.hisabpro.app.data.local.dao.PartyDao
import com.hisabpro.app.data.local.dao.PaymentDao
import com.hisabpro.app.data.local.dao.SyncMetadataDao
import com.hisabpro.app.data.local.dao.SyncQueueDao
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
import com.hisabpro.app.data.local.entity.SyncMetadataEntity
import com.hisabpro.app.data.local.entity.SyncQueueEntity

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
        JournalEntryLineEntity::class,
        SyncQueueEntity::class,
        SyncMetadataEntity::class
    ],
    version = 4,
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
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun syncMetadataDao(): SyncMetadataDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("PRAGMA foreign_keys = OFF;")

                // 1. businesses
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `businesses_v2` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `owner_name` TEXT NOT NULL DEFAULT '',
                        `address` TEXT NOT NULL DEFAULT '',
                        `phone` TEXT NOT NULL DEFAULT '',
                        `email` TEXT NOT NULL DEFAULT '',
                        `gstin` TEXT NOT NULL DEFAULT '',
                        `pan` TEXT NOT NULL DEFAULT '',
                        `logo_path` TEXT NOT NULL DEFAULT '',
                        `gst_enabled` INTEGER NOT NULL DEFAULT 0,
                        `financial_year_start` TEXT NOT NULL DEFAULT '01-04',
                        `upi_id` TEXT NOT NULL DEFAULT '',
                        `bank_name` TEXT NOT NULL DEFAULT '',
                        `account_number` TEXT NOT NULL DEFAULT '',
                        `ifsc_code` TEXT NOT NULL DEFAULT '',
                        `terms_and_conditions` TEXT NOT NULL DEFAULT '',
                        `created_at` INTEGER NOT NULL DEFAULT 0,
                        `updated_at` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`)
                    );
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `businesses_v2` (
                        `id`, `name`, `owner_name`, `address`, `phone`, `email`, `gstin`, `pan`,
                        `logo_path`, `gst_enabled`, `financial_year_start`, `upi_id`, `bank_name`,
                        `account_number`, `ifsc_code`, `terms_and_conditions`
                    )
                    SELECT
                        `id`, `name`, `ownerName`, `address`, `phone`, `email`, `gstin`, `pan`,
                        `logoPath`, `gstEnabled`, `fyStart`, `upiId`, `bankName`,
                        `accountNumber`, `ifscCode`, `termsAndConditions`
                    FROM `businesses`;
                """.trimIndent())
                db.execSQL("DROP TABLE IF EXISTS `businesses`;")
                db.execSQL("ALTER TABLE `businesses_v2` RENAME TO `businesses`;")

                // 2. parties
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `parties_v2` (
                        `id` TEXT NOT NULL,
                        `business_id` TEXT NOT NULL DEFAULT 'default_business',
                        `name` TEXT NOT NULL,
                        `phone` TEXT NOT NULL,
                        `email` TEXT NOT NULL DEFAULT '',
                        `address` TEXT NOT NULL DEFAULT '',
                        `gstin` TEXT NOT NULL DEFAULT '',
                        `type` TEXT NOT NULL DEFAULT 'CUSTOMER',
                        `tag` TEXT NOT NULL DEFAULT 'REGULAR',
                        `opening_balance` INTEGER NOT NULL DEFAULT 0,
                        `created_at` INTEGER NOT NULL DEFAULT 0,
                        `updated_at` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`business_id`) REFERENCES `businesses`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    );
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `parties_v2` (
                        `id`, `business_id`, `name`, `phone`, `email`, `address`, `gstin`, `type`, `tag`, `opening_balance`, `created_at`, `updated_at`
                    )
                    SELECT
                        `id`, `businessId`, `name`, `phone`, `email`, `address`, `gstin`, `type`, `tag`,
                        CAST(ROUND(`openingBalance` * 100) AS INTEGER), `createdAt`, `createdAt`
                    FROM `parties`;
                """.trimIndent())
                db.execSQL("DROP TABLE IF EXISTS `parties`;")
                db.execSQL("ALTER TABLE `parties_v2` RENAME TO `parties`;")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_parties_business_id` ON `parties` (`business_id`);")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_parties_phone` ON `parties` (`phone`);")

                // 3. items
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `items_v2` (
                        `id` TEXT NOT NULL,
                        `business_id` TEXT NOT NULL DEFAULT 'default_business',
                        `name` TEXT NOT NULL,
                        `item_code` TEXT NOT NULL DEFAULT '',
                        `unit` TEXT NOT NULL DEFAULT 'Pcs',
                        `hsn_code` TEXT NOT NULL DEFAULT '',
                        `purchase_price` INTEGER NOT NULL DEFAULT 0,
                        `sell_price` INTEGER NOT NULL DEFAULT 0,
                        `gst_rate` REAL NOT NULL DEFAULT 0.0,
                        `category` TEXT NOT NULL DEFAULT 'General',
                        `stock_qty` REAL NOT NULL DEFAULT 0.0,
                        `low_stock_threshold` REAL NOT NULL DEFAULT 5.0,
                        `created_at` INTEGER NOT NULL DEFAULT 0,
                        `updated_at` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`business_id`) REFERENCES `businesses`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    );
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `items_v2` (
                        `id`, `business_id`, `name`, `item_code`, `unit`, `hsn_code`, `purchase_price`, `sell_price`,
                        `gst_rate`, `category`, `stock_qty`, `low_stock_threshold`, `created_at`, `updated_at`
                    )
                    SELECT
                        `id`, `businessId`, `name`, `itemCode`, `unit`, `hsnCode`,
                        CAST(ROUND(`purchasePrice` * 100) AS INTEGER),
                        CAST(ROUND(`sellPrice` * 100) AS INTEGER),
                        `gstRate`, `category`, `stockQty`, `minStockAlert`, `createdAt`, `createdAt`
                    FROM `items`;
                """.trimIndent())
                db.execSQL("DROP TABLE IF EXISTS `items`;")
                db.execSQL("ALTER TABLE `items_v2` RENAME TO `items`;")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_items_business_id` ON `items` (`business_id`);")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_items_category` ON `items` (`category`);")

                // 4. invoices
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `invoices_v2` (
                        `id` TEXT NOT NULL,
                        `business_id` TEXT NOT NULL DEFAULT 'default_business',
                        `invoice_no` TEXT NOT NULL,
                        `date` INTEGER NOT NULL,
                        `party_id` TEXT,
                        `customer_name` TEXT NOT NULL DEFAULT '',
                        `customer_phone` TEXT NOT NULL DEFAULT '',
                        `customer_address` TEXT NOT NULL DEFAULT '',
                        `customer_gstin` TEXT NOT NULL DEFAULT '',
                        `type` TEXT NOT NULL DEFAULT 'NON_GST_BILL',
                        `gst_mode` TEXT NOT NULL DEFAULT 'EXEMPT',
                        `subtotal` INTEGER NOT NULL DEFAULT 0,
                        `discount` INTEGER NOT NULL DEFAULT 0,
                        `taxable_amount` INTEGER NOT NULL DEFAULT 0,
                        `cgst` INTEGER NOT NULL DEFAULT 0,
                        `sgst` INTEGER NOT NULL DEFAULT 0,
                        `igst` INTEGER NOT NULL DEFAULT 0,
                        `total` INTEGER NOT NULL DEFAULT 0,
                        `paid_amount` INTEGER NOT NULL DEFAULT 0,
                        `payment_status` TEXT NOT NULL DEFAULT 'PAID',
                        `payment_mode` TEXT NOT NULL DEFAULT 'Cash',
                        `notes` TEXT NOT NULL DEFAULT '',
                        `is_gst` INTEGER NOT NULL DEFAULT 0,
                        `created_at` INTEGER NOT NULL DEFAULT 0,
                        `updated_at` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`business_id`) REFERENCES `businesses`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`party_id`) REFERENCES `parties`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    );
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `invoices_v2` (
                        `id`, `business_id`, `invoice_no`, `date`, `party_id`, `customer_name`, `customer_phone`,
                        `customer_address`, `customer_gstin`, `type`, `gst_mode`, `subtotal`, `discount`,
                        `taxable_amount`, `cgst`, `sgst`, `igst`, `total`, `paid_amount`, `payment_status`,
                        `payment_mode`, `notes`, `is_gst`, `created_at`, `updated_at`
                    )
                    SELECT
                        `id`, `businessId`, `invoiceNo`, `dateMillis`, `partyId`, `customerName`, `customerPhone`,
                        `customerAddress`, `customerGstin`, `type`, `gstMode`,
                        CAST(ROUND(`subtotal` * 100) AS INTEGER),
                        CAST(ROUND(`discountAmount` * 100) AS INTEGER),
                        CAST(ROUND((`total` - `cgst` - `sgst` - `igst`) * 100) AS INTEGER),
                        CAST(ROUND(`cgst` * 100) AS INTEGER),
                        CAST(ROUND(`sgst` * 100) AS INTEGER),
                        CAST(ROUND(`igst` * 100) AS INTEGER),
                        CAST(ROUND(`total` * 100) AS INTEGER),
                        CAST(ROUND(`paidAmount` * 100) AS INTEGER),
                        `paymentStatus`, `paymentMode`, `notes`, `isGst`, `createdAt`, `createdAt`
                    FROM `invoices`;
                """.trimIndent())
                db.execSQL("DROP TABLE IF EXISTS `invoices`;")
                db.execSQL("ALTER TABLE `invoices_v2` RENAME TO `invoices`;")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_invoices_business_id` ON `invoices` (`business_id`);")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_invoices_invoice_no` ON `invoices` (`invoice_no`);")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_invoices_party_id` ON `invoices` (`party_id`);")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_invoices_date` ON `invoices` (`date`);")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_invoices_business_id_invoice_no` ON `invoices` (`business_id`, `invoice_no`);")

                // 5. invoice_items
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `invoice_items_v2` (
                        `id` TEXT NOT NULL,
                        `invoice_id` TEXT NOT NULL,
                        `item_id` TEXT,
                        `item_name` TEXT NOT NULL,
                        `hsn_code` TEXT NOT NULL DEFAULT '',
                        `qty` REAL NOT NULL DEFAULT 1.0,
                        `unit` TEXT NOT NULL DEFAULT 'Pcs',
                        `rate` INTEGER NOT NULL DEFAULT 0,
                        `discount` INTEGER NOT NULL DEFAULT 0,
                        `cgst_rate` REAL NOT NULL DEFAULT 0.0,
                        `sgst_rate` REAL NOT NULL DEFAULT 0.0,
                        `igst_rate` REAL NOT NULL DEFAULT 0.0,
                        `amount` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`invoice_id`) REFERENCES `invoices`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`item_id`) REFERENCES `items`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    );
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `invoice_items_v2` (
                        `id`, `invoice_id`, `item_id`, `item_name`, `hsn_code`, `qty`, `unit`,
                        `rate`, `discount`, `cgst_rate`, `sgst_rate`, `igst_rate`, `amount`
                    )
                    SELECT
                        `id`, `invoiceId`, `itemId`, `itemName`, `hsnCode`, `qty`, `unit`,
                        CAST(ROUND(`rate` * 100) AS INTEGER),
                        CAST(ROUND(`discount` * 100) AS INTEGER),
                        `cgstRate`, `sgstRate`, 0.0,
                        CAST(ROUND(`amount` * 100) AS INTEGER)
                    FROM `invoice_items`;
                """.trimIndent())
                db.execSQL("DROP TABLE IF EXISTS `invoice_items`;")
                db.execSQL("ALTER TABLE `invoice_items_v2` RENAME TO `invoice_items`;")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_invoice_items_invoice_id` ON `invoice_items` (`invoice_id`);")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_invoice_items_item_id` ON `invoice_items` (`item_id`);")

                // 6. payments
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `payments_v2` (
                        `id` TEXT NOT NULL,
                        `business_id` TEXT NOT NULL DEFAULT 'default_business',
                        `party_id` TEXT,
                        `date` INTEGER NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `mode` TEXT NOT NULL DEFAULT 'Cash',
                        `reference_no` TEXT NOT NULL DEFAULT '',
                        `notes` TEXT NOT NULL DEFAULT '',
                        `linked_invoice_id` TEXT,
                        `created_at` INTEGER NOT NULL DEFAULT 0,
                        `updated_at` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`business_id`) REFERENCES `businesses`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`party_id`) REFERENCES `parties`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(`linked_invoice_id`) REFERENCES `invoices`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    );
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `payments_v2` (
                        `id`, `business_id`, `party_id`, `date`, `amount`, `mode`, `reference_no`, `notes`, `linked_invoice_id`
                    )
                    SELECT
                        `id`, `businessId`, NULLIF(`partyId`, ''), `dateMillis`,
                        CAST(ROUND(`amount` * 100) AS INTEGER),
                        `mode`, `referenceNo`, `notes`, `linkedInvoiceId`
                    FROM `payments`;
                """.trimIndent())
                db.execSQL("DROP TABLE IF EXISTS `payments`;")
                db.execSQL("ALTER TABLE `payments_v2` RENAME TO `payments`;")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_payments_business_id` ON `payments` (`business_id`);")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_payments_party_id` ON `payments` (`party_id`);")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_payments_date` ON `payments` (`date`);")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_payments_linked_invoice_id` ON `payments` (`linked_invoice_id`);")

                // 7. expenses
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `expenses_v2` (
                        `id` TEXT NOT NULL,
                        `business_id` TEXT NOT NULL DEFAULT 'default_business',
                        `date` INTEGER NOT NULL,
                        `category` TEXT NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `description` TEXT NOT NULL DEFAULT '',
                        `mode` TEXT NOT NULL DEFAULT 'Cash',
                        `receipt_path` TEXT NOT NULL DEFAULT '',
                        `created_at` INTEGER NOT NULL DEFAULT 0,
                        `updated_at` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`business_id`) REFERENCES `businesses`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    );
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `expenses_v2` (
                        `id`, `business_id`, `date`, `category`, `amount`, `description`, `mode`, `receipt_path`
                    )
                    SELECT
                        `id`, `businessId`, `dateMillis`, `category`,
                        CAST(ROUND(`amount` * 100) AS INTEGER),
                        `description`, `mode`, `receiptPath`
                    FROM `expenses`;
                """.trimIndent())
                db.execSQL("DROP TABLE IF EXISTS `expenses`;")
                db.execSQL("ALTER TABLE `expenses_v2` RENAME TO `expenses`;")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_business_id` ON `expenses` (`business_id`);")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_date` ON `expenses` (`date`);")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_category` ON `expenses` (`category`);")

                // 8. accounts
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `accounts_v2` (
                        `id` TEXT NOT NULL,
                        `business_id` TEXT NOT NULL DEFAULT 'default_business',
                        `name` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `opening_balance` INTEGER NOT NULL DEFAULT 0,
                        `account_number` TEXT NOT NULL DEFAULT '',
                        `ifsc_code` TEXT NOT NULL DEFAULT '',
                        `created_at` INTEGER NOT NULL DEFAULT 0,
                        `updated_at` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`business_id`) REFERENCES `businesses`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    );
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `accounts_v2` (
                        `id`, `business_id`, `name`, `type`, `opening_balance`, `account_number`, `ifsc_code`
                    )
                    SELECT
                        `id`, `businessId`, `name`, `type`,
                        CAST(ROUND(`openingBalance` * 100) AS INTEGER),
                        `accountNumber`, `ifscCode`
                    FROM `accounts`;
                """.trimIndent())
                db.execSQL("DROP TABLE IF EXISTS `accounts`;")
                db.execSQL("ALTER TABLE `accounts_v2` RENAME TO `accounts`;")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_accounts_business_id` ON `accounts` (`business_id`);")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_accounts_type` ON `accounts` (`type`);")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_accounts_business_id_name` ON `accounts` (`business_id`, `name`);")

                // 9. journal_entries
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `journal_entries_v2` (
                        `id` TEXT NOT NULL,
                        `business_id` TEXT NOT NULL DEFAULT 'default_business',
                        `date` INTEGER NOT NULL,
                        `voucher_no` TEXT NOT NULL DEFAULT '',
                        `narration` TEXT NOT NULL DEFAULT '',
                        `created_at` INTEGER NOT NULL DEFAULT 0,
                        `updated_at` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`business_id`) REFERENCES `businesses`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    );
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `journal_entries_v2` (
                        `id`, `business_id`, `date`, `voucher_no`, `narration`
                    )
                    SELECT
                        `id`, `businessId`, `dateMillis`, `voucherNo`, `narration`
                    FROM `journal_entries`;
                """.trimIndent())
                db.execSQL("DROP TABLE IF EXISTS `journal_entries`;")
                db.execSQL("ALTER TABLE `journal_entries_v2` RENAME TO `journal_entries`;")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_journal_entries_business_id` ON `journal_entries` (`business_id`);")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_journal_entries_date` ON `journal_entries` (`date`);")

                // 10. journal_entry_lines
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `journal_entry_lines_v2` (
                        `id` TEXT NOT NULL,
                        `journal_entry_id` TEXT NOT NULL,
                        `account_id` TEXT NOT NULL,
                        `account_name` TEXT NOT NULL DEFAULT '',
                        `is_debit` INTEGER NOT NULL DEFAULT 1,
                        `debit` INTEGER NOT NULL DEFAULT 0,
                        `credit` INTEGER NOT NULL DEFAULT 0,
                        `created_at` INTEGER NOT NULL DEFAULT 0,
                        `updated_at` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`journal_entry_id`) REFERENCES `journal_entries`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`account_id`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    );
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `journal_entry_lines_v2` (
                        `id`, `journal_entry_id`, `account_id`, `account_name`, `is_debit`, `debit`, `credit`
                    )
                    SELECT
                        `id`, `journalEntryId`, `accountId`, `accountName`, `isDebit`,
                        CASE WHEN `isDebit` = 1 THEN CAST(ROUND(`amount` * 100) AS INTEGER) ELSE 0 END,
                        CASE WHEN `isDebit` = 0 THEN CAST(ROUND(`amount` * 100) AS INTEGER) ELSE 0 END
                    FROM `journal_entry_lines`;
                """.trimIndent())
                db.execSQL("DROP TABLE IF EXISTS `journal_entry_lines`;")
                db.execSQL("ALTER TABLE `journal_entry_lines_v2` RENAME TO `journal_entry_lines`;")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_journal_entry_lines_journal_entry_id` ON `journal_entry_lines` (`journal_entry_id`);")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_journal_entry_lines_account_id` ON `journal_entry_lines` (`account_id`);")

                // 11. khata_entries
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `khata_entries_v2` (
                        `id` TEXT NOT NULL,
                        `party_id` TEXT NOT NULL,
                        `amount` INTEGER NOT NULL DEFAULT 0,
                        `type` TEXT NOT NULL,
                        `date` INTEGER NOT NULL,
                        `bill_number` TEXT NOT NULL DEFAULT '',
                        `note` TEXT NOT NULL DEFAULT '',
                        `created_at` INTEGER NOT NULL DEFAULT 0,
                        `updated_at` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`party_id`) REFERENCES `parties`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    );
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `khata_entries_v2` (
                        `id`, `party_id`, `amount`, `type`, `date`, `bill_number`, `note`
                    )
                    SELECT
                        `id`, `partyId`,
                        CAST(ROUND(`amount` * 100) AS INTEGER),
                        `type`, `dateMillis`, `billNumber`, `note`
                    FROM `khata_entries`;
                """.trimIndent())
                db.execSQL("DROP TABLE IF EXISTS `khata_entries`;")
                db.execSQL("ALTER TABLE `khata_entries_v2` RENAME TO `khata_entries`;")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_khata_entries_party_id` ON `khata_entries` (`party_id`);")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_khata_entries_date` ON `khata_entries` (`date`);")

                db.execSQL("PRAGMA foreign_keys = ON;")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Alter existing tables to add deleted_at and synced_at nullable columns
                val tables = listOf(
                    "businesses", "parties", "items", "invoices", "invoice_items",
                    "payments", "expenses", "accounts", "journal_entries", "journal_entry_lines", "khata_entries"
                )
                for (table in tables) {
                    db.execSQL("ALTER TABLE `$table` ADD COLUMN `deleted_at` INTEGER DEFAULT NULL;")
                    db.execSQL("ALTER TABLE `$table` ADD COLUMN `synced_at` INTEGER DEFAULT NULL;")
                }

                // 2. Create sync_queue table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sync_queue` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `entity_type` TEXT NOT NULL,
                        `entity_id` TEXT NOT NULL,
                        `operation` TEXT NOT NULL,
                        `payload_json` TEXT NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        `status` TEXT NOT NULL DEFAULT 'PENDING',
                        `retry_count` INTEGER NOT NULL DEFAULT 0,
                        `last_error` TEXT,
                        `next_retry_at` INTEGER NOT NULL DEFAULT 0
                    );
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_queue_status` ON `sync_queue` (`status`);")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_queue_entity_type_entity_id` ON `sync_queue` (`entity_type`, `entity_id`);")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_queue_created_at` ON `sync_queue` (`created_at`);")

                // 3. Create sync_metadata table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sync_metadata` (
                        `table_name` TEXT PRIMARY KEY NOT NULL,
                        `last_pulled_at` INTEGER NOT NULL DEFAULT 0,
                        `last_pushed_at` INTEGER NOT NULL DEFAULT 0,
                        `last_sync_status` TEXT NOT NULL DEFAULT 'IDLE',
                        `error_message` TEXT
                    );
                """.trimIndent())
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("PRAGMA foreign_keys = OFF;")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `khata_entries_v3` (
                        `id` TEXT NOT NULL,
                        `business_id` TEXT NOT NULL DEFAULT 'default_business',
                        `party_id` TEXT NOT NULL,
                        `amount` INTEGER NOT NULL DEFAULT 0,
                        `type` TEXT NOT NULL,
                        `date` INTEGER NOT NULL,
                        `bill_number` TEXT NOT NULL DEFAULT '',
                        `note` TEXT NOT NULL DEFAULT '',
                        `created_at` INTEGER NOT NULL DEFAULT 0,
                        `updated_at` INTEGER NOT NULL DEFAULT 0,
                        `deleted_at` INTEGER DEFAULT NULL,
                        `synced_at` INTEGER DEFAULT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`business_id`) REFERENCES `businesses`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`party_id`) REFERENCES `parties`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    );
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `khata_entries_v3` (
                        `id`, `business_id`, `party_id`, `amount`, `type`, `date`, `bill_number`, `note`, `created_at`, `updated_at`, `deleted_at`, `synced_at`
                    )
                    SELECT
                        k.`id`, COALESCE(p.`business_id`, 'default_business'), k.`party_id`, k.`amount`, k.`type`, k.`date`, k.`bill_number`, k.`note`, k.`created_at`, k.`updated_at`, k.`deleted_at`, k.`synced_at`
                    FROM `khata_entries` k
                    LEFT JOIN `parties` p ON k.`party_id` = p.`id`;
                """.trimIndent())
                db.execSQL("DROP TABLE IF EXISTS `khata_entries`;")
                db.execSQL("ALTER TABLE `khata_entries_v3` RENAME TO `khata_entries`;")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_khata_entries_business_id` ON `khata_entries` (`business_id`);")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_khata_entries_party_id` ON `khata_entries` (`party_id`);")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_khata_entries_date` ON `khata_entries` (`date`);")
                db.execSQL("PRAGMA foreign_keys = ON;")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hisabpro_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
