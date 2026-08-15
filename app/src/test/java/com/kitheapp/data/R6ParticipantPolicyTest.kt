package com.kitheapp.data

import org.junit.Assert.assertEquals
import org.junit.Test

class R6ParticipantPolicyTest {
    private val drop = R6DropDiscovery(
        id = "drop-1",
        experienceCode = "ABCD1234",
        ownerId = "host-1",
        hostLabel = "Museum",
        lat = 19.7,
        lng = -155.1,
        radiusM = 25,
        contentKind = R6ContentKind.TEXT,
        dropKind = R6DropKind.STANDARD,
        payloadVersion = 1,
        trailId = null,
        trailStepIndex = null,
        trailTotalSteps = null,
        likeCount = 0,
        publishedAtMillis = 1_000,
        editedAtMillis = null,
        expiryMode = R6ExpiryMode.NONE,
        expiresAtMillis = null
    )

    @Test
    fun `found and expired states outrank proximity`() {
        assertEquals(
            R6DiscoveryState.FOUND,
            state(drop, unlocked = setOf(drop.id), distance = 5.0, accuracy = 5.0)
        )
        assertEquals(
            R6DiscoveryState.EXPIRED,
            state(
                drop.copy(expiryMode = R6ExpiryMode.CUSTOM, expiresAtMillis = 500),
                unlocked = setOf(drop.id),
                distance = 5.0,
                accuracy = 5.0
            )
        )
    }

    @Test
    fun `future Trail step is visibly locked`() {
        val trailDrop = drop.copy(trailId = "trail", trailStepIndex = 2, trailTotalSteps = 4)
        assertEquals(
            R6DiscoveryState.TRAIL_LOCKED,
            R6ParticipantPolicy.discoveryState(
                drop = trailDrop,
                unlockedDropIds = emptySet(),
                trailProgress = R6TrailProgress("ABCD1234", "trail", 1, emptyList(), null),
                approximateDistanceM = 5.0,
                approximateAccuracyM = 5.0,
                nowMillis = 1_000,
                experienceEndsAtMillis = null
            )
        )
    }

    @Test
    fun `near requires a finite location estimate`() {
        assertEquals(R6DiscoveryState.NEAR, state(drop, emptySet(), 30.0, 10.0))
        assertEquals(R6DiscoveryState.LOCKED, state(drop, emptySet(), 60.0, 10.0))
        assertEquals(R6DiscoveryState.LOCKED, state(drop, emptySet(), Double.NaN, 10.0))
    }

    @Test
    fun `distance failures use server bucket without claiming exact tracking`() {
        val message = R6ParticipantPolicy.failureMessage(
            R6ParticipantException(
                R6UnlockFailureReason.TOO_FAR,
                retryable = true,
                distanceBucket = R6DistanceBucket.WITHIN_50_M
            )
        )
        assertEquals("Not there yet — you're within about 50 m.", message)
    }

    private fun state(
        candidate: R6DropDiscovery,
        unlocked: Set<String>,
        distance: Double?,
        accuracy: Double?
    ) = R6ParticipantPolicy.discoveryState(
        drop = candidate,
        unlockedDropIds = unlocked,
        trailProgress = null,
        approximateDistanceM = distance,
        approximateAccuracyM = accuracy,
        nowMillis = 1_000,
        experienceEndsAtMillis = null
    )
}
