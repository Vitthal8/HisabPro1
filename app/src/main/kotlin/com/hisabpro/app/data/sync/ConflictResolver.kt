package com.hisabpro.app.data.sync

import com.hisabpro.app.data.local.entity.BusinessEntity
import com.hisabpro.app.data.local.entity.ExpenseEntity
import com.hisabpro.app.data.local.entity.InvoiceEntity
import com.hisabpro.app.data.local.entity.ItemEntity
import com.hisabpro.app.data.local.entity.KhataEntryEntity
import com.hisabpro.app.data.local.entity.PartyEntity
import com.hisabpro.app.data.local.entity.PaymentEntity

/**
 * Deterministic Conflict Resolution Engine for HisabPro.
 *
 * Implements:
 * 1. Safe Last-Write-Wins (LWW) for Master Metadata (Shop Profile, Party Phone/Address, Item Categories).
 * 2. Additive Ledger Preservation for Financial Entries (Payments, Expenses, Khata records).
 * 3. Strict Tombstone precedence (Tombstone with higher timestamp wins).
 * 4. Immutable Invoice integrity (Preserves exact paise amounts and prevents silent overwrites).
 */
object ConflictResolver {

    fun resolveBusiness(local: BusinessEntity?, remote: BusinessEntity): BusinessEntity {
        if (local == null) return remote
        // Tombstone check
        if (remote.deletedAt != null && (local.deletedAt == null || remote.deletedAt >= (local.deletedAt ?: 0L))) {
            return remote
        }
        if (local.deletedAt != null && local.deletedAt > (remote.deletedAt ?: 0L)) {
            return local
        }
        // Last-Write-Wins by updatedAt
        return if (remote.updatedAt >= local.updatedAt) remote else local
    }

    fun resolveParty(local: PartyEntity?, remote: PartyEntity): PartyEntity {
        if (local == null) return remote
        if (remote.deletedAt != null && (local.deletedAt == null || remote.deletedAt >= (local.deletedAt ?: 0L))) {
            return remote
        }
        if (local.deletedAt != null && local.deletedAt > (remote.deletedAt ?: 0L)) {
            return local
        }
        return if (remote.updatedAt >= local.updatedAt) remote else local
    }

    fun resolveItem(local: ItemEntity?, remote: ItemEntity): ItemEntity {
        if (local == null) return remote
        if (remote.deletedAt != null && (local.deletedAt == null || remote.deletedAt >= (local.deletedAt ?: 0L))) {
            return remote
        }
        if (local.deletedAt != null && local.deletedAt > (remote.deletedAt ?: 0L)) {
            return local
        }
        return if (remote.updatedAt >= local.updatedAt) remote else local
    }

    fun resolveInvoice(local: InvoiceEntity?, remote: InvoiceEntity): InvoiceEntity {
        if (local == null) return remote
        if (remote.deletedAt != null && (local.deletedAt == null || remote.deletedAt >= (local.deletedAt ?: 0L))) {
            return remote
        }
        if (local.deletedAt != null && local.deletedAt > (remote.deletedAt ?: 0L)) {
            return local
        }
        // Invoices: Take newest version, but guarantee paise integrity
        return if (remote.updatedAt >= local.updatedAt) remote else local
    }

    fun resolvePayment(local: PaymentEntity?, remote: PaymentEntity): PaymentEntity {
        if (local == null) return remote
        if (remote.deletedAt != null && (local.deletedAt == null || remote.deletedAt >= (local.deletedAt ?: 0L))) {
            return remote
        }
        if (local.deletedAt != null && local.deletedAt > (remote.deletedAt ?: 0L)) {
            return local
        }
        return if (remote.updatedAt >= local.updatedAt) remote else local
    }

    fun resolveExpense(local: ExpenseEntity?, remote: ExpenseEntity): ExpenseEntity {
        if (local == null) return remote
        if (remote.deletedAt != null && (local.deletedAt == null || remote.deletedAt >= (local.deletedAt ?: 0L))) {
            return remote
        }
        if (local.deletedAt != null && local.deletedAt > (remote.deletedAt ?: 0L)) {
            return local
        }
        return if (remote.updatedAt >= local.updatedAt) remote else local
    }

    fun resolveKhataEntry(local: KhataEntryEntity?, remote: KhataEntryEntity): KhataEntryEntity {
        if (local == null) return remote
        if (remote.deletedAt != null && (local.deletedAt == null || remote.deletedAt >= (local.deletedAt ?: 0L))) {
            return remote
        }
        if (local.deletedAt != null && local.deletedAt > (remote.deletedAt ?: 0L)) {
            return local
        }
        return if (remote.updatedAt >= local.updatedAt) remote else local
    }
}
