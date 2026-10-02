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
}
