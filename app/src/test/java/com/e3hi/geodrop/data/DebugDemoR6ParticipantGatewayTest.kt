package com.e3hi.geodrop.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.hypot

class DebugDemoR6ParticipantGatewayTest {
    @Test
    fun demoProvidesDiscoveryAndPersistentCollectionFixture() = runBlocking {
        val gateway = DebugDemoR6ParticipantGateway()

        val discoveries = gateway.loadDiscoveries(DebugDemoR5EntryGateway.DEVICE_DEMO_CODE)
        val collection = gateway.loadCollection("debug-user")

        assertEquals(4, discoveries.size)
        assertTrue(discoveries.all { it.experienceCode == "DEMO2026" })
        assertEquals(1, collection.size)
        assertEquals(DebugDemoR6ParticipantGateway.WELCOME_DROP_ID, collection.single().dropId)
    }

    @Test
    fun demoDiscoveriesStayWithinShortWalkOfOutdoorTestPoint() = runBlocking {
        val discoveries = DebugDemoR6ParticipantGateway().loadDiscoveries("DEMO2026")
        val longitudeMetersPerDegree = 111_320.0 * cos(
            Math.toRadians(DebugDemoR6ParticipantGateway.DEMO_TEST_LATITUDE)
        )

        val distancesMeters = discoveries.map { discovery ->
            hypot(
                (discovery.lat - DebugDemoR6ParticipantGateway.DEMO_TEST_LATITUDE) * 111_320.0,
                (discovery.lng - DebugDemoR6ParticipantGateway.DEMO_TEST_LONGITUDE) *
                    longitudeMetersPerDegree
            )
        }

        assertEquals(0.0, distancesMeters.first(), 0.1)
        assertTrue(distancesMeters.all { it <= 75.0 })
    }

    @Test
    fun demoUnlockIssuesRewardAndBlockStaysLocal() = runBlocking {
        val gateway = DebugDemoR6ParticipantGateway()
        val result = gateway.unlock(
            R6UnlockRequest(
                dropId = DebugDemoR6ParticipantGateway.REWARD_DROP_ID,
                entrySessionId = "0123456789abcdef",
                latitude = 0.0,
                longitude = 0.0,
                accuracyM = 10.0,
                capturedAtMillis = 1_786_425_600_000L
            )
        )

        assertFalse(result.alreadyUnlocked)
        assertEquals("DEMO-7K4P", result.receipt.reward?.code)
        assertEquals(2, gateway.loadCollection("debug-user").size)

        gateway.blockHost(DebugDemoR6ParticipantGateway.REWARD_DROP_ID)
        assertTrue("debug-demo-host" in gateway.loadBlockedHostIds("debug-user"))
    }
}
