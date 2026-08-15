package com.kitheapp.geo

import com.kitheapp.data.Drop
import com.kitheapp.data.DropContentType
import com.kitheapp.data.DropType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DropCollectionTest {

    @Test
    fun `a fix without accuracy can never unlock a drop`() {
        assertFalse(isWithinPickupRadius(distanceMeters = 1f, accuracyMeters = null))
        assertFalse(isWithinPickupRadius(distanceMeters = 1f, accuracyMeters = 0f))
    }

    @Test
    fun `a fix coarser than the pickup radius can never unlock a drop`() {
        assertFalse(isWithinPickupRadius(distanceMeters = 0f, accuracyMeters = 30.1f))
        assertFalse(isWithinPickupRadius(distanceMeters = 0f, accuracyMeters = 500f))
    }

    @Test
    fun `distance is judged against the radius plus the fix's own error`() {
        assertTrue(isWithinPickupRadius(distanceMeters = 40f, accuracyMeters = 10f))
        assertTrue(isWithinPickupRadius(distanceMeters = 30f, accuracyMeters = 1f))
        assertFalse(isWithinPickupRadius(distanceMeters = 40.1f, accuracyMeters = 10f))
    }

    @Test
    fun `expiration uses the drop's own creation time and decay window`() {
        val created = 1_000_000L
        val request = DropCollectionRequest(
            dropId = "a",
            createdAt = created,
            decayDays = 1
        )

        assertFalse(request.isExpired(created + 86_399_999L))
        assertTrue(request.isExpired(created + 86_400_000L))
    }

    @Test
    fun `a drop without a decay window never expires`() {
        val request = DropCollectionRequest(dropId = "a", createdAt = 1_000L, decayDays = null)

        assertNull(request.expiresAtMillis())
        assertFalse(request.isExpired(Long.MAX_VALUE))
    }

    @Test
    fun `a collected note carries the whole drop, not just its text`() {
        val drop = Drop(
            id = "drop-1",
            text = "Find me",
            description = "Behind the mural",
            lat = 19.703995,
            lng = -155.0768,
            createdAt = 42L,
            dropperUsername = "GeoDropTest",
            decayDays = 3,
            groupCode = "HILO",
            dropType = DropType.RESTAURANT_COUPON,
            businessId = "biz-1",
            businessName = "Cafe",
            contentType = DropContentType.PHOTO,
            mediaUrl = "https://example.test/p.jpg",
            mediaMimeType = "image/jpeg",
            redemptionLimit = 5,
            redemptionCount = 2,
            huntId = "hunt-1",
            huntStepIndex = 1,
            huntTotalSteps = 4
        )

        val note = drop.toCollectionRequest().toCollectedNote(
            resolvedUsername = "GeoDropTest",
            collectedAt = 99L
        )

        assertEquals("drop-1", note.id)
        assertEquals("Find me", note.text)
        assertEquals("Behind the mural", note.description)
        assertEquals(19.703995, note.lat!!, 0.0)
        assertEquals(-155.0768, note.lng!!, 0.0)
        assertEquals(42L, note.dropCreatedAt)
        assertEquals(99L, note.collectedAt)
        assertEquals("GeoDropTest", note.dropperUsername)
        assertEquals(3, note.decayDays)
        assertEquals("HILO", note.groupCode)
        assertEquals(DropType.RESTAURANT_COUPON, note.dropType)
        assertEquals("biz-1", note.businessId)
        assertEquals("Cafe", note.businessName)
        assertEquals(DropContentType.PHOTO, note.contentType)
        assertEquals("https://example.test/p.jpg", note.mediaUrl)
        assertEquals("image/jpeg", note.mediaMimeType)
        assertEquals(5, note.redemptionLimit)
        assertEquals(2, note.redemptionCount)
        assertEquals("hunt-1", note.huntId)
        assertEquals(1, note.huntStepIndex)
        assertEquals(4, note.huntTotalSteps)
    }

    @Test
    fun `an unset created time does not masquerade as an epoch timestamp`() {
        val note = Drop(id = "drop-2", createdAt = 0L).toCollectionRequest()

        assertNull(note.createdAt)
    }

    @Test
    fun `every failure has a message and none of them claim success`() {
        val failures = listOf(
            DropCollectionResult.Expired,
            DropCollectionResult.OutOfRange,
            DropCollectionResult.LocationUnavailable,
            DropCollectionResult.Failed(IllegalStateException("boom"))
        )

        failures.forEach { result ->
            val message = pickupFailureMessage(result)
            assertTrue(message.isNotBlank())
            assertFalse(message.contains("added to your collection"))
        }
    }
}
