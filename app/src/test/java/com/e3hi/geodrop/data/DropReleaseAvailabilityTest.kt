package com.e3hi.geodrop.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The owner dashboard lists every drop the organiser created and labels the ones
 * attendees cannot reach, so the list reconciles against the server rollup above
 * it. These pin down which reason wins and that every non-available state can
 * explain itself.
 */
class DropReleaseAvailabilityTest {

    private fun availability(
        drop: Drop,
        coupons: Boolean = true,
        media: Boolean = true,
        hunts: Boolean = true
    ) = drop.releaseAvailability(
        couponsEnabled = coupons,
        mediaEnabled = media,
        huntsEnabled = hunts
    )

    @Test
    fun `a plain text drop is available when nothing gates it`() {
        val result = availability(Drop(id = "a", text = "hi"))

        assertEquals(DropReleaseAvailability.AVAILABLE, result)
        assertTrue(result.isAvailable)
        assertNull(result.ownerExplanation())
    }

    @Test
    fun `a coupon is unreachable while offers are off`() {
        val coupon = Drop(id = "a", dropType = DropType.RESTAURANT_COUPON)

        assertEquals(DropReleaseAvailability.COUPONS_DISABLED, availability(coupon, coupons = false))
        assertEquals(DropReleaseAvailability.AVAILABLE, availability(coupon, coupons = true))
    }

    @Test
    fun `media and hunt drops are gated by their own flags`() {
        val photo = Drop(id = "a", contentType = DropContentType.PHOTO)
        val huntStep = Drop(id = "b", huntId = "hunt-1", huntStepIndex = 0, huntTotalSteps = 3)

        assertEquals(DropReleaseAvailability.MEDIA_DISABLED, availability(photo, media = false))
        assertEquals(DropReleaseAvailability.HUNTS_DISABLED, availability(huntStep, hunts = false))
        assertEquals(DropReleaseAvailability.AVAILABLE, availability(photo))
        assertEquals(DropReleaseAvailability.AVAILABLE, availability(huntStep))
    }

    @Test
    fun `moderation outranks every feature flag`() {
        val flaggedCoupon = Drop(
            id = "a",
            dropType = DropType.RESTAURANT_COUPON,
            contentType = DropContentType.PHOTO,
            isNsfw = true
        )

        val result = availability(flaggedCoupon, coupons = false, media = false)

        assertEquals(DropReleaseAvailability.FLAGGED, result)
        assertFalse(result.isAvailable)
    }

    @Test
    fun `every unavailable state tells the owner why, and none of them read as available`() {
        DropReleaseAvailability.entries
            .filter { it != DropReleaseAvailability.AVAILABLE }
            .forEach { state ->
                val explanation = state.ownerExplanation()
                assertNotNull("$state must explain itself", explanation)
                assertTrue(explanation!!.isNotBlank())
                assertFalse(state.isAvailable)
            }
    }
}
