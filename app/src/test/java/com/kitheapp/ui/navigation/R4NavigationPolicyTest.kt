package com.kitheapp.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class R4NavigationPolicyTest {

    @Test
    fun unknownOrMissingDestinationFallsBackToNearby() {
        assertEquals(
            ParticipantDestination.NEARBY,
            R4NavigationPolicy.resolveDestination(null)
        )
        assertEquals(
            ParticipantDestination.NEARBY,
            R4NavigationPolicy.resolveDestination("legacy-fourth-tab")
        )
    }

    @Test
    fun activeExperienceIsPreservedOrDeterministicallyFallsBack() {
        assertEquals(
            "BETA",
            R4NavigationPolicy.resolveActiveExperience("BETA", listOf("ALPHA", "BETA"))
        )
        assertEquals(
            "ALPHA",
            R4NavigationPolicy.resolveActiveExperience("REMOVED", listOf("ALPHA", "BETA"))
        )
        assertNull(R4NavigationPolicy.resolveActiveExperience("ALPHA", emptyList()))
    }

    @Test
    fun nearbyStateIsScopedPerExperienceWhileOtherTabsAreShared() {
        assertEquals(
            "nearby:ALPHA",
            R4NavigationPolicy.stateKey(ParticipantDestination.NEARBY, "ALPHA")
        )
        assertEquals(
            "nearby:BETA",
            R4NavigationPolicy.stateKey(ParticipantDestination.NEARBY, "BETA")
        )
        assertEquals(
            "collection",
            R4NavigationPolicy.stateKey(ParticipantDestination.COLLECTION, "ALPHA")
        )
        assertEquals(
            "account",
            R4NavigationPolicy.stateKey(ParticipantDestination.ACCOUNT, "BETA")
        )
    }
}
