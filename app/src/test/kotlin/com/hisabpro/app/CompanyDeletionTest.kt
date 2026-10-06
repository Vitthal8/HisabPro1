package com.hisabpro.app

import com.hisabpro.app.data.local.entity.SyncOutbox
import com.hisabpro.app.data.model.BusinessProfile
import com.hisabpro.app.ui.company.DeleteCompanyUiState
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class CompanyDeletionTest {

    @Test
    fun testDeleteCompanyUiStateTransitions() {
        val stateFlow = MutableStateFlow<DeleteCompanyUiState>(DeleteCompanyUiState.Idle)
        assertEquals(DeleteCompanyUiState.Idle, stateFlow.value)

        // Deleting state
        stateFlow.value = DeleteCompanyUiState.Deleting
        assertEquals(DeleteCompanyUiState.Deleting, stateFlow.value)

        // Success state
        stateFlow.value = DeleteCompanyUiState.Success("biz_shop2")
        assertTrue(stateFlow.value is DeleteCompanyUiState.Success)
        assertEquals("biz_shop2", (stateFlow.value as DeleteCompanyUiState.Success).deletedBusinessId)

        // Error state
        stateFlow.value = DeleteCompanyUiState.Error("Cannot delete the only company profile.")
        assertTrue(stateFlow.value is DeleteCompanyUiState.Error)
        assertEquals("Cannot delete the only company profile.", (stateFlow.value as DeleteCompanyUiState.Error).message)
    }

    @Test
    fun testDeleteBusinessDialogConfirmationValidation() {
        val businessName = "Vittal Supermarket & Traders"

        fun isConfirmAllowed(typedName: String, isAcknowledged: Boolean): Boolean {
            return isAcknowledged && typedName.trim() == businessName.trim()
        }

        // Case 1: Checkbox checked but wrong typed name -> disabled
        assertFalse(isConfirmAllowed("Vittal", isAcknowledged = true))

        // Case 2: Correct typed name but checkbox not acknowledged -> disabled
        assertFalse(isConfirmAllowed(businessName, isAcknowledged = false))

        // Case 3: Empty inputs -> disabled
        assertFalse(isConfirmAllowed("", isAcknowledged = false))

        // Case 4: Correct typed name AND checkbox acknowledged -> ENABLED!
        assertTrue(isConfirmAllowed(businessName, isAcknowledged = true))
    }

    @Test
    fun testCascadingDeleteOutboxEventPayload() {
        val businessId = "biz_vittal_supermarket"
        val idempotencyKey = "del_biz_${businessId}_1760000000000"
        val eventId = UUID.randomUUID().toString()

        val payloadJson = "{\"id\":\"$businessId\",\"idempotency_key\":\"$idempotencyKey\"}"

        val outboxEvent = SyncOutbox(
            eventId = eventId,
            entityId = businessId,
            tableName = "businesses",
            operationType = "DELETE",
            payload = payloadJson,
            createdAt = 1760000000000L
        )

        assertEquals("businesses", outboxEvent.tableName)
        assertEquals("DELETE", outboxEvent.operationType)
        assertEquals(businessId, outboxEvent.entityId)
        assertTrue(outboxEvent.payload.contains(businessId))
        assertTrue(outboxEvent.payload.contains(idempotencyKey))
    }

    @Test
    fun testCannotDeleteSingleRemainingCompanyProfile() {
        val currentBusinesses = listOf(
            BusinessProfile(id = "biz_vittal_supermarket", shopName = "Vittal Supermarket & Traders")
        )

        fun canDeleteCompany(bizList: List<BusinessProfile>): Boolean {
            return bizList.size > 1
        }

        assertFalse("Deletion must be prevented if only one company remains", canDeleteCompany(currentBusinesses))

        val multiBusinesses = listOf(
            BusinessProfile(id = "biz_vittal_supermarket", shopName = "Vittal Supermarket"),
            BusinessProfile(id = "biz_branch_2", shopName = "Vittal Electronics")
        )

        assertTrue("Deletion allowed when multiple companies exist", canDeleteCompany(multiBusinesses))
    }

    @Test
    fun testDeletedBusinessFilteringPreventsCloudResurrection() {
        val deletedIds = mutableSetOf<String>()
        val deletedBizId = "biz_branch_2"
        deletedIds.add(deletedBizId)
        deletedIds.add("Vittal Electronics")

        val incomingCloudProfiles = listOf(
            BusinessProfile(id = "biz_vittal_supermarket", shopName = "Vittal Supermarket"),
            BusinessProfile(id = "biz_branch_2", shopName = "Vittal Electronics")
        )

        // Filter incoming cloud profiles using the deleted tombstone set
        val filtered = incomingCloudProfiles.filterNot { 
            deletedIds.contains(it.id) || deletedIds.contains(it.shopName) 
        }

        assertEquals(1, filtered.size)
        assertEquals("biz_vittal_supermarket", filtered.first().id)
        assertFalse("Deleted company must not be resurrected by cloud pull", filtered.any { it.id == deletedBizId })
    }

    @Test
    fun testSyncQueueDeletePayloadForBusiness() {
        val bizId = "biz_closed_shop"
        val payload = org.json.JSONObject().apply {
            put("id", bizId)
            put("user_id", "user_12345")
            put("deleted_at", 1760000000000L)
        }

        assertEquals(bizId, payload.getString("id"))
        assertEquals("user_12345", payload.getString("user_id"))
        assertEquals(1760000000000L, payload.getLong("deleted_at"))
    }

    @Test
    fun testLastWriteWins_LocalDeletionWinsAgainstOlderRemoteUpdate() {
        val localTombstone = com.hisabpro.app.data.local.entity.BusinessEntity(
            id = "biz_branch_pune",
            name = "Pune Branch",
            deletedAt = 2000L,
            updatedAt = 2000L
        )

        val olderRemoteGhost = com.hisabpro.app.data.local.entity.BusinessEntity(
            id = "biz_branch_pune",
            name = "Pune Branch",
            deletedAt = null,
            updatedAt = 1500L
        )

        val resolved = com.hisabpro.app.data.sync.ConflictResolver.resolveBusiness(localTombstone, olderRemoteGhost)
        assertTrue("Local deletion with newer timestamp must WIN (LWW)", resolved.deletedAt != null)
        assertEquals(2000L, resolved.deletedAt)
    }

    @Test
    fun testLastWriteWins_NewerRemoteUpdateWinsAgainstOlderLocalDeletion() {
        val localTombstone = com.hisabpro.app.data.local.entity.BusinessEntity(
            id = "biz_branch_pune",
            name = "Pune Branch",
            deletedAt = 1000L,
            updatedAt = 1000L
        )

        val newerRemoteUpdate = com.hisabpro.app.data.local.entity.BusinessEntity(
            id = "biz_branch_pune",
            name = "Pune Branch Reopened",
            deletedAt = null,
            updatedAt = 2500L
        )

        val resolved = com.hisabpro.app.data.sync.ConflictResolver.resolveBusiness(localTombstone, newerRemoteUpdate)
        assertTrue("Remote record updated AFTER deletion must win by LWW", resolved.deletedAt == null)
        assertEquals("Pune Branch Reopened", resolved.name)
    }

    @Test
    fun testLastWriteWins_SynthesizedTombstoneTimestampWins() {
        val remoteGhost = com.hisabpro.app.data.local.entity.BusinessEntity(
            id = "biz_deleted_shop",
            name = "Deleted Shop",
            deletedAt = null,
            updatedAt = 3000L
        )

        // Local row in Room is null, but persistent tombstone timestamp is 4000L
        val resolved = com.hisabpro.app.data.sync.ConflictResolver.resolveBusiness(
            local = null,
            remote = remoteGhost,
            localTombstoneTimestamp = 4000L
        )

        assertTrue("Synthesized tombstone timestamp >= remote updatedAt must WIN and remain deleted", resolved.deletedAt != null)
        assertEquals(4000L, resolved.deletedAt)
    }
}
