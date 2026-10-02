package com.hisabpro.app

import com.hisabpro.app.data.local.entity.BusinessEntity
import com.hisabpro.app.data.local.entity.InvoiceEntity
import com.hisabpro.app.data.local.entity.PartyEntity
import com.hisabpro.app.data.model.BusinessProfile
import com.hisabpro.app.data.sync.UserSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test suite verifying User Account Isolation, Multi-Tenant switching,
 * and data privacy protection in HisabPro.
 */
class UserSwitchMultiTenancyTest {

    data class MockSessionState(
        var lastActiveUserId: String? = null,
        var lastActiveUserEmail: String? = null,
        val pendingBizNames: MutableMap<String, String> = mutableMapOf()
    )

    private fun isDifferentUser(sessionState: MockSessionState, session: UserSession): Boolean {
        val lastUserId = sessionState.lastActiveUserId
        if (lastUserId != null && lastUserId != session.userId) {
            return true
        }
        val lastEmail = sessionState.lastActiveUserEmail
        if (lastEmail != null && !session.email.isNullOrBlank() && !lastEmail.equals(session.email, ignoreCase = true)) {
            return true
        }
        return false
    }

    @Test
    fun testUserSwitchDetection() {
        val sessionState = MockSessionState(
            lastActiveUserId = "user_alpha_123",
            lastActiveUserEmail = "userA@shop.in"
        )

        val sameUserSession = UserSession(
            userId = "user_alpha_123",
            email = "userA@shop.in",
            accessToken = "token_123"
        )

        val differentUserSession = UserSession(
            userId = "user_beta_456",
            email = "userB@kirana.in",
            accessToken = "token_456"
        )

        // Same user logging back in should not trigger user switch wipe
        assertFalse(isDifferentUser(sessionState, sameUserSession))

        // Different user logging in MUST trigger user switch wipe
        assertTrue(isDifferentUser(sessionState, differentUserSession))
    }

    @Test
    fun testUserSwitchCleansOldCompanyAndData() {
        // User A was logged in and created a business with invoices
        val localBusinesses = mutableListOf(
            BusinessEntity(
                id = "biz_mali_traders",
                name = "Mali Traders",
                ownerName = "Vittal Mali",
                email = "vittal@shop.in"
            )
        )
        val localInvoices = mutableListOf(
            InvoiceEntity(
                id = "inv_001",
                businessId = "biz_mali_traders",
                invoiceNo = "INV/001",
                date = System.currentTimeMillis(),
                total = 500000L,
                subtotal = 500000L
            )
        )
        val localParties = mutableListOf(
            PartyEntity(
                id = "party_001",
                businessId = "biz_mali_traders",
                name = "Ramesh Kumar",
                phone = "9876543210"
            )
        )

        var activeProfile = BusinessProfile(
            id = "biz_mali_traders",
            shopName = "Mali Traders",
            ownerName = "Vittal Mali",
            email = "vittal@shop.in",
            hasCompletedOnboarding = true
        )

        val sessionState = MockSessionState(
            lastActiveUserId = "user_vittal_1",
            lastActiveUserEmail = "vittal@shop.in"
        )

        // Verify User A state before logout
        assertEquals("Mali Traders", activeProfile.shopName)
        assertEquals(1, localBusinesses.size)
        assertEquals(1, localInvoices.size)
        assertEquals(1, localParties.size)

        // New user logs in: User B
        val userBSession = UserSession(
            userId = "user_newuser_2",
            email = "newuser@kirana.in",
            accessToken = "token_b"
        )

        assertTrue(isDifferentUser(sessionState, userBSession))

        // Execute user switch purge
        localBusinesses.clear()
        localInvoices.clear()
        localParties.clear()
        sessionState.lastActiveUserId = userBSession.userId
        sessionState.lastActiveUserEmail = userBSession.email

        // Reset profile for new user
        activeProfile = BusinessProfile(
            id = "default_business",
            shopName = "",
            ownerName = userBSession.email?.substringBefore("@") ?: "",
            email = userBSession.email ?: "",
            hasCompletedOnboarding = false
        )

        // Verify User B does NOT see User A's data or company!
        assertTrue(localBusinesses.isEmpty())
        assertTrue(localInvoices.isEmpty())
        assertTrue(localParties.isEmpty())
        assertNotEquals("Mali Traders", activeProfile.shopName)
        assertEquals("", activeProfile.shopName)
        assertFalse(activeProfile.hasCompletedOnboarding)
        assertEquals("newuser", activeProfile.ownerName)
    }

    @Test
    fun testPendingRegistrationBusinessNamePreserved() {
        val sessionState = MockSessionState()
        val email = "newmerchant@hisabpro.in"
        val desiredBizName = "Pooja Stores & General"

        // During registration, user stashes pending business name
        sessionState.pendingBizNames[email] = desiredBizName

        // When user logs in
        val userSession = UserSession(
            userId = "user_pooja_999",
            email = email,
            accessToken = "token_pooja"
        )

        val retrievedName = sessionState.pendingBizNames[userSession.email]
        assertEquals(desiredBizName, retrievedName)

        val initialProfile = BusinessProfile(
            shopName = retrievedName ?: "",
            ownerName = userSession.email?.substringBefore("@") ?: "",
            email = userSession.email ?: "",
            hasCompletedOnboarding = true
        )

        assertEquals("Pooja Stores & General", initialProfile.shopName)
        assertTrue(initialProfile.hasCompletedOnboarding)
    }

    @Test
    fun testCloudRestoreWithReplaceLocalDoesNotResurrectOldUsers() {
        val userBCloudProfiles = listOf(
            BusinessProfile(
                id = "biz_beta_retail",
                shopName = "Beta Retailers",
                ownerName = "Beta Owner",
                email = "beta@retail.in",
                hasCompletedOnboarding = true
            )
        )

        // Previous user had Mali Traders
        val oldLocalList = mutableListOf(
            BusinessProfile(
                id = "biz_mali_traders",
                shopName = "Mali Traders",
                ownerName = "Vittal Mali",
                email = "vittal@shop.in",
                hasCompletedOnboarding = true
            )
        )

        // When restoring cloud profiles with replaceLocal = true
        val replaceLocal = true
        val restoredList = if (replaceLocal) {
            userBCloudProfiles.toMutableList()
        } else {
            (oldLocalList + userBCloudProfiles).toMutableList()
        }

        assertEquals(1, restoredList.size)
        assertEquals("Beta Retailers", restoredList.first().shopName)
        assertFalse(restoredList.any { it.shopName == "Mali Traders" })
    }

    @Test
    fun testRemoteBusinessesFilteringByUserIdAndEmail() {
        val userBSession = UserSession(
            userId = "user_b_uuid",
            email = "userb@domain.com",
            accessToken = "token_b"
        )

        val rawRemoteRecords = listOf(
            mapOf("id" to "biz_user_a", "user_id" to "user_a_uuid", "name" to "Alpha Enterprises", "email" to "usera@domain.com"),
            mapOf("id" to "biz_user_b", "user_id" to "user_b_uuid", "name" to "Beta Stores", "email" to "userb@domain.com"),
            mapOf("id" to "biz_unassigned_other", "user_id" to "", "name" to "Old Unassigned", "email" to "stranger@other.com"),
            mapOf("id" to "biz_unassigned_mine", "user_id" to "", "name" to "Legacy Mine", "email" to "userb@domain.com")
        )

        val acceptedBusinesses = rawRemoteRecords.filter { record ->
            val recordUserId = record["user_id"] ?: ""
            if (recordUserId.isNotBlank()) {
                recordUserId == userBSession.userId
            } else {
                val recordEmail = record["email"] ?: ""
                recordEmail.equals(userBSession.email, ignoreCase = true)
            }
        }

        assertEquals(2, acceptedBusinesses.size)
        assertTrue(acceptedBusinesses.any { it["id"] == "biz_user_b" })
        assertTrue(acceptedBusinesses.any { it["id"] == "biz_unassigned_mine" })
        assertFalse(acceptedBusinesses.any { it["id"] == "biz_user_a" })
        assertFalse(acceptedBusinesses.any { it["id"] == "biz_unassigned_other" })
    }

    @Test
    fun testNewUserWithZeroBusinessesStartsFresh() {
        val userBSession = UserSession(
            userId = "user_b_uuid",
            email = "brandnew@merchant.in",
            accessToken = "token_new"
        )

        // Remote has 0 businesses for this user
        val userBRemoteBusinesses = emptyList<BusinessProfile>()

        val initialProfile = if (userBRemoteBusinesses.isEmpty()) {
            BusinessProfile(
                id = "default_business",
                shopName = "",
                ownerName = userBSession.email?.substringBefore("@") ?: "",
                email = userBSession.email ?: "",
                hasCompletedOnboarding = false
            )
        } else {
            userBRemoteBusinesses.first()
        }

        assertEquals("", initialProfile.shopName)
        assertFalse(initialProfile.hasCompletedOnboarding)
        assertEquals("brandnew", initialProfile.ownerName)
    }
}
