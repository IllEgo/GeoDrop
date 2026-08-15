package com.kitheapp.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class R8RewardResultsTest {
    @Test
    fun rewardAuthoringRequiresPresentationAndBoundedInventory() {
        val standard = rewardDraft("DEMO2026")

        assertNull(R7OrganizerPolicy.validateDrop(standard))
        assertNotNull(R7OrganizerPolicy.validateDrop(standard.copy(rewardLabel = "")))
        assertNotNull(R7OrganizerPolicy.validateDrop(standard.copy(inventoryLimit = 0)))
        assertNotNull(R7OrganizerPolicy.validateDrop(standard.copy(inventoryLimit = 10_001)))
    }

    @Test
    fun issuanceIsIdempotentAndSoldOutStillReturnsContentReceipt() = runBlocking {
        val fixture = activeRewardFixture(inventory = 1)
        val firstParticipant = DebugDemoR6ParticipantGateway(fixture.store)
        val secondParticipant = DebugDemoR6ParticipantGateway(fixture.store)

        val first = firstParticipant.unlock(unlockRequest(fixture.dropId))
        val retry = firstParticipant.unlock(unlockRequest(fixture.dropId))
        val soldOut = secondParticipant.unlock(unlockRequest(fixture.dropId))

        assertNotNull(first.receipt.reward)
        assertEquals(first.receipt.reward?.code, retry.receipt.reward?.code)
        assertTrue(retry.alreadyUnlocked)
        assertTrue(soldOut.rewardUnavailable)
        assertNotNull(soldOut.receipt)
        assertNull(soldOut.receipt.reward)
        assertFalse(soldOut.receipt.hasRewardReceipt)
    }

    @Test
    fun ownerCanMarkUsedThenCorrectWithoutErasingHistory() = runBlocking {
        val fixture = activeRewardFixture(inventory = 2)
        val participant = DebugDemoR6ParticipantGateway(fixture.store)
        val gateway = DebugDemoR7OrganizerGateway(fixture.store)
        val issued = participant.unlock(unlockRequest(fixture.dropId)).receipt.reward!!

        assertTrue(gateway.markRewardCodeUsed(fixture.dropId, issued.code))
        assertFalse(gateway.markRewardCodeUsed(fixture.dropId, issued.code))
        val used = gateway.loadRewardCodes(
            fixture.dropId,
            R8RewardCodeState.USED,
            null
        ).single()
        assertNotNull(used.usedAtMillis)
        assertEquals("ISSUED_TO_USED", used.history.first().transition)

        assertTrue(
            gateway.correctRewardCodeUse(
                fixture.dropId,
                issued.code,
                R8CorrectionReason.MARKED_BY_MISTAKE
            )
        )
        val corrected = gateway.loadRewardCodes(
            fixture.dropId,
            R8RewardCodeState.ISSUED,
            issued.code
        ).single()
        assertNull(corrected.usedAtMillis)
        assertEquals(3, corrected.history.size)
        assertEquals("USED_TO_ISSUED", corrected.history.first().transition)
    }

    @Test
    fun collectionKeepsIssuedCodeAndReflectsOnlineStatusUpdate() = runBlocking {
        val fixture = activeRewardFixture(inventory = 1)
        val participant = DebugDemoR6ParticipantGateway(fixture.store)
        val organizer = DebugDemoR7OrganizerGateway(fixture.store)
        val issued = participant.unlock(unlockRequest(fixture.dropId)).receipt.reward!!

        assertEquals(issued.code, participant.loadCollection("participant").first {
            it.dropId == fixture.dropId
        }.reward?.code)
        organizer.markRewardCodeUsed(fixture.dropId, issued.code)
        val refreshed = participant.loadCollection("participant").first {
            it.dropId == fixture.dropId
        }.reward

        assertEquals(R8RewardCodeState.USED.name, refreshed?.state)
        assertNotNull(refreshed?.usedAtMillis)
    }

    @Test
    fun resultsUseReceiptAndRewardStateDefinitions() = runBlocking {
        val fixture = activeRewardFixture(inventory = 2)
        val participant = DebugDemoR6ParticipantGateway(fixture.store)
        val organizer = DebugDemoR7OrganizerGateway(fixture.store)
        val issued = participant.unlock(unlockRequest(fixture.dropId)).receipt.reward!!
        organizer.markRewardCodeUsed(fixture.dropId, issued.code)

        val results = organizer.loadResults(fixture.experienceCode)
        val drop = results.drops.single { it.dropId == fixture.dropId }

        assertEquals(1L, results.uniqueUnlockers)
        assertEquals(1L, results.unlocks)
        assertEquals(1L, results.codesIssued)
        assertEquals(1L, results.codesUsed)
        assertEquals(1L, drop.unlocks)
        assertEquals(1L, drop.codesIssued)
        assertEquals(1L, drop.codesUsed)
    }

    private suspend fun activeRewardFixture(inventory: Int): Fixture {
        val store = DebugDemoExperienceStore()
        val organizer = DebugDemoR7OrganizerGateway(store)
        val now = System.currentTimeMillis()
        val experience = organizer.createExperience(
            R7ExperienceDraft(
                name = "R8 gate",
                description = null,
                startsAtMillis = now - 60_000L,
                endsAtMillis = now + 3_600_000L,
                timeZone = "Pacific/Honolulu"
            )
        )
        val drop = organizer.saveDrop("owner", rewardDraft(experience.code, inventory))
        return Fixture(store, experience.code, drop.dropId)
    }

    private fun rewardDraft(code: String, inventory: Int = 25) = R7DropDraft(
        experienceCode = code,
        lat = DebugDemoR6ParticipantGateway.DEMO_TEST_LATITUDE,
        lng = DebugDemoR6ParticipantGateway.DEMO_TEST_LONGITUDE,
        title = "Welcome reward",
        body = "Find a unique welcome code.",
        mediaAltText = null,
        dropKind = R6DropKind.REWARD,
        rewardLabel = "Free small coffee",
        businessLabel = "R8 review counter",
        rewardInstructions = "Show the code before ordering.",
        rewardTerms = "One use.",
        inventoryLimit = inventory
    )

    private fun unlockRequest(dropId: String) = R6UnlockRequest(
        dropId = dropId,
        entrySessionId = "r8gate123456789",
        latitude = DebugDemoR6ParticipantGateway.DEMO_TEST_LATITUDE,
        longitude = DebugDemoR6ParticipantGateway.DEMO_TEST_LONGITUDE,
        accuracyM = 5.0,
        capturedAtMillis = System.currentTimeMillis()
    )

    private data class Fixture(
        val store: DebugDemoExperienceStore,
        val experienceCode: String,
        val dropId: String
    )
}
