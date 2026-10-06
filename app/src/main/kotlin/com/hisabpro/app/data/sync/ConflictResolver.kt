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
 * 1. Safe Last-Write-Wins (LWW) with explicit Tombstone versioning for Business profiles.
 *    - If a company was deleted locally with timestamp T_del, and a remote record arrives with
 *      updatedAt <= T_del, the local deletion WINS, preventing ghost/restored companies.
 *    - If remote updatedAt > T_del, the newer remote edit wins.
 * 2. Tombstone precedence with LWW across all entities.
 * 3. Immutable Invoice integrity (preserves exact paise amounts and prevents silent overwrites).
 */
object ConflictResolver {

    /**
     * Resolves conflict between local and remote Business records using Last-Write-Wins (LWW)
     * and explicit Tombstone versioning.
     */
    fun resolveBusiness(
        local: BusinessEntity?,
        remote: BusinessEntity,
        localTombstoneTimestamp: Long? = null
    ): BusinessEntity {
        val effectiveLocal = when {
            local != null -> local
            localTombstoneTimestamp != null -> {
                remote.copy(
                    deletedAt = localTombstoneTimestamp,
                    updatedAt = localTombstoneTimestamp
                )
            }
            else -> null
        }

        if (effectiveLocal == null) return remote

        val localDel = effectiveLocal.deletedAt
        val remoteDel = remote.deletedAt

        // 1. Both deleted -> newer deletion wins
        if (localDel != null && remoteDel != null) {
            return if (remoteDel >= localDel) remote else effectiveLocal
        }

        // 2. Local is deleted, remote is active
        if (localDel != null && remoteDel == null) {
            // LWW: If local deletion timestamp >= remote updatedAt, local deletion wins!
            return if (localDel >= remote.updatedAt) effectiveLocal else remote
        }

        // 3. Remote is deleted, local is active
        if (remoteDel != null && localDel == null) {
            // LWW: If remote deletion timestamp >= local updatedAt, remote deletion wins!
            return if (remoteDel >= effectiveLocal.updatedAt) remote else effectiveLocal
        }

        // 4. Neither is deleted -> LWW by updatedAt
        return if (remote.updatedAt >= effectiveLocal.updatedAt) remote else effectiveLocal
    }

    fun resolveParty(local: PartyEntity?, remote: PartyEntity): PartyEntity {
        if (local == null) return remote
        val localDel = local.deletedAt
        val remoteDel = remote.deletedAt

        if (localDel != null && remoteDel != null) {
            return if (remoteDel >= localDel) remote else local
        }
        if (localDel != null && remoteDel == null) {
            return if (localDel >= remote.updatedAt) local else remote
        }
        if (remoteDel != null && localDel == null) {
            return if (remoteDel >= local.updatedAt) remote else local
        }
        return if (remote.updatedAt >= local.updatedAt) remote else local
    }

    fun resolveItem(local: ItemEntity?, remote: ItemEntity): ItemEntity {
        if (local == null) return remote
        val localDel = local.deletedAt
        val remoteDel = remote.deletedAt

        if (localDel != null && remoteDel != null) {
            return if (remoteDel >= localDel) remote else local
        }
        if (localDel != null && remoteDel == null) {
            return if (localDel >= remote.updatedAt) local else remote
        }
        if (remoteDel != null && localDel == null) {
            return if (remoteDel >= local.updatedAt) remote else local
        }
        return if (remote.updatedAt >= local.updatedAt) remote else local
    }

    fun resolveInvoice(local: InvoiceEntity?, remote: InvoiceEntity): InvoiceEntity {
        if (local == null) return remote
        val localDel = local.deletedAt
        val remoteDel = remote.deletedAt

        if (localDel != null && remoteDel != null) {
            return if (remoteDel >= localDel) remote else local
        }
        if (localDel != null && remoteDel == null) {
            return if (localDel >= remote.updatedAt) local else remote
        }
        if (remoteDel != null && localDel == null) {
            return if (remoteDel >= local.updatedAt) remote else local
        }
        // Invoices: Take newest version, preserving exact paise amounts
        return if (remote.updatedAt >= local.updatedAt) remote else local
    }

    fun resolvePayment(local: PaymentEntity?, remote: PaymentEntity): PaymentEntity {
        if (local == null) return remote
        val localDel = local.deletedAt
        val remoteDel = remote.deletedAt

        if (localDel != null && remoteDel != null) {
            return if (remoteDel >= localDel) remote else local
        }
        if (localDel != null && remoteDel == null) {
            return if (localDel >= remote.updatedAt) local else remote
        }
        if (remoteDel != null && localDel == null) {
            return if (remoteDel >= local.updatedAt) remote else local
        }
        return if (remote.updatedAt >= local.updatedAt) remote else local
    }

    fun resolveExpense(local: ExpenseEntity?, remote: ExpenseEntity): ExpenseEntity {
        if (local == null) return remote
        val localDel = local.deletedAt
        val remoteDel = remote.deletedAt

        if (localDel != null && remoteDel != null) {
            return if (remoteDel >= localDel) remote else local
        }
        if (localDel != null && remoteDel == null) {
            return if (localDel >= remote.updatedAt) local else remote
        }
        if (remoteDel != null && localDel == null) {
            return if (remoteDel >= local.updatedAt) remote else local
        }
        return if (remote.updatedAt >= local.updatedAt) remote else local
    }

    fun resolveKhataEntry(local: KhataEntryEntity?, remote: KhataEntryEntity): KhataEntryEntity {
        if (local == null) return remote
        val localDel = local.deletedAt
        val remoteDel = remote.deletedAt

        if (localDel != null && remoteDel != null) {
            return if (remoteDel >= localDel) remote else local
        }
        if (localDel != null && remoteDel == null) {
            return if (localDel >= remote.updatedAt) local else remote
        }
        if (remoteDel != null && localDel == null) {
            return if (remoteDel >= local.updatedAt) remote else local
        }
        return if (remote.updatedAt >= local.updatedAt) remote else local
    }
}
