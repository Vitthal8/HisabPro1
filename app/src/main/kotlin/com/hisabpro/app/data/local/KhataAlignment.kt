package com.hisabpro.app.data.local

import android.util.Log
import com.hisabpro.app.data.local.dao.PartyDao
import com.hisabpro.app.data.local.entity.KhataEntryEntity

/**
 * Shared Architectural Service: Khata Ledger Business Alignment
 *
 * Enforces the strict invariant:
 * A khata entry MUST ALWAYS belong to the exact same business as its parent party.
 * Used identically on:
 * 1. Local entity creation / insert path
 * 2. Cloud sync delta pull / restore path
 */
object KhataAlignment {
    private const val TAG = "KhataAlignment"

    suspend fun alignBusinessId(
        entry: KhataEntryEntity,
        partyDao: PartyDao
    ): KhataEntryEntity {
        val party = partyDao.getPartyByIdSync(entry.partyId) ?: return entry
        val partyBizId = party.businessId
        if (partyBizId.isNotBlank() && entry.businessId != partyBizId) {
            Log.w(
                TAG,
                "ALIGNMENT DETECTED: KhataEntry ${entry.id} had businessId='${entry.businessId}', but parent party '${party.name}' belongs to '$partyBizId'. Re-aligning to '$partyBizId'."
            )
            return entry.copy(businessId = partyBizId)
        }
        return entry
    }
}
