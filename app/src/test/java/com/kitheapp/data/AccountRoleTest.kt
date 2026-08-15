package com.kitheapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 2.7 — pins the two-type account model on the client side.
 *
 * The launch scope has exactly EXPLORER and BUSINESS. firestore.rules compares the
 * stored `role` string exactly, so this test guards the two ways the client used to
 * disagree with the server about who is a business: case-folding the stored value, and
 * inferring BUSINESS from business metadata. The rules-layer half (role immutability,
 * server-authored business metadata) is covered by
 * firestore-tests/accountRoleRules.test.js.
 */
class AccountRoleTest {

    @Test
    fun onlyTwoAccountTypesExist() {
        assertEquals(
            listOf("EXPLORER", "BUSINESS"),
            UserRole.entries.map { it.name }
        )
    }

    @Test
    fun canonicalStoredValuesParse() {
        assertEquals(UserRole.EXPLORER, UserRole.fromRaw("EXPLORER"))
        assertEquals(UserRole.BUSINESS, UserRole.fromRaw("BUSINESS"))
        // The clients write the enum name, so a round trip must hold.
        UserRole.entries.forEach { role ->
            assertEquals(role, UserRole.fromRaw(role.name))
        }
    }

    @Test
    fun offModelValuesResolveToTheLeastPrivilegedType() {
        // Anything the rules would not accept as BUSINESS must not read as BUSINESS
        // here either — including a lower-cased spelling of a real type, which the
        // rules' exact string comparison rejects.
        listOf("business", "Business", "BUSINESS_PRO", "ADMIN", "MODERATOR", "ORGANIZER", "", null)
            .forEach { raw ->
                assertEquals("role=$raw must not grant BUSINESS", UserRole.EXPLORER, UserRole.fromRaw(raw))
            }
    }

    @Test
    fun surroundingWhitespaceIsToleratedOnCanonicalValues() {
        assertEquals(UserRole.BUSINESS, UserRole.fromRaw("BUSINESS "))
        assertEquals(UserRole.BUSINESS, UserRole.fromRaw("\tBUSINESS\n"))
    }

    @Test
    fun businessMetadataDoesNotMakeAnExplorerABusiness() {
        // The removed inference treated a stored businessName as proof of BUSINESS. The
        // server never agreed: firestore.rules gates the business drop axis on the
        // stored role, so a profile like this one gets explorer access.
        val profile = UserProfile(
            id = "uid",
            role = UserRole.EXPLORER,
            businessName = "Fake Co",
            businessCategories = listOf(BusinessCategory.FOOD_RESTAURANTS_CAFES)
        )

        assertFalse(profile.isBusiness())
    }

    @Test
    fun theStoredRoleIsWhatDecidesTheBusinessSurface() {
        val business = UserProfile(id = "uid", role = UserRole.BUSINESS)

        assertTrue(business.isBusiness())
    }
}
