package com.e3hi.geodrop.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugDemoR5EntryGatewayTest {
    @Test
    fun demoCodeResolvesAndJoinsWithoutCallingFirebaseDelegate() = runBlocking {
        val delegate = RecordingEntryGateway()
        val gateway = DebugDemoR5EntryGateway(delegate)
        val request = R5EntryRequest(
            code = DebugDemoR5EntryGateway.DEVICE_DEMO_CODE,
            entrySessionId = "0123456789abcdef",
            channel = R5EntryChannel.MANUAL
        )

        gateway.ensureGuestSession(request.entrySessionId)
        val resolved = gateway.resolve(request)
        val joined = gateway.join(request)
        gateway.recordClientEvent(
            eventName = "app_first_open",
            entrySessionId = request.entrySessionId,
            experienceCode = request.code
        )

        assertEquals("GeoDrop Device Demo", resolved.name)
        assertEquals(R5ExperienceMembership.MEMBER, joined.membership)
        assertFalse(delegate.called)
    }

    @Test
    fun anyOtherCodeKeepsTheRealGatewayPath() = runBlocking {
        val delegate = RecordingEntryGateway()
        val gateway = DebugDemoR5EntryGateway(delegate)
        val request = R5EntryRequest(
            code = "REAL2026",
            entrySessionId = "fedcba9876543210",
            channel = R5EntryChannel.MANUAL
        )

        gateway.resolve(request)

        assertTrue(delegate.called)
    }

    @Test
    fun locallyAuthoredExperienceJoinsAndPublishesWithoutFirebaseFallthrough() = runBlocking {
        val store = DebugDemoExperienceStore()
        val delegate = RecordingEntryGateway()
        val entryGateway = DebugDemoR5EntryGateway(delegate, store)
        val organizerGateway = DebugDemoR7OrganizerGateway(store)
        val participantGateway = DebugDemoR6ParticipantGateway(store)
        val now = System.currentTimeMillis()
        val experience = organizerGateway.createExperience(
            R7ExperienceDraft(
                name = "Shared local Experience",
                description = null,
                startsAtMillis = now - 60_000L,
                endsAtMillis = now + 3_600_000L,
                timeZone = "Pacific/Honolulu"
            )
        )
        organizerGateway.saveDrop(
            userId = "debug-user",
            draft = R7DropDraft(
                experienceCode = experience.code,
                lat = 19.704,
                lng = -155.0767777778,
                title = "Locally shared drop",
                body = "Visible through the participant fixture.",
                mediaAltText = null
            )
        )
        val request = R5EntryRequest(
            code = experience.code,
            entrySessionId = "sharedlocal12345",
            channel = R5EntryChannel.MANUAL
        )

        val joined = entryGateway.join(request)
        val discoveries = participantGateway.loadDiscoveries(experience.code)

        assertEquals(experience.code, joined.code)
        assertEquals("Shared local Experience", joined.name)
        assertEquals("Locally shared drop", store.organizerDrop(discoveries.single().id)?.summary?.title)
        assertFalse(delegate.called)
    }

    @Test
    fun locallyAuthoredTextAndPhotoDropsBothReachParticipantDiscovery() = runBlocking {
        val store = DebugDemoExperienceStore()
        val organizer = DebugDemoR7OrganizerGateway(store)
        val participant = DebugDemoR6ParticipantGateway(store)
        val now = System.currentTimeMillis()
        val experience = organizer.createExperience(
            R7ExperienceDraft(
                name = "Content bridge review",
                description = null,
                startsAtMillis = now - 1_000L,
                endsAtMillis = now + 3_600_000L,
                timeZone = "Pacific/Honolulu"
            )
        )
        organizer.saveDrop(
            "debug-organizer",
            R7DropDraft(
                experienceCode = experience.code,
                lat = 19.704,
                lng = -155.0767777778,
                title = "Visible text",
                body = "Participant text payload",
                mediaAltText = null
            )
        )
        organizer.saveDrop(
            "debug-organizer",
            R7DropDraft(
                experienceCode = experience.code,
                lat = 19.7042,
                lng = -155.0765,
                contentKind = R7DropContentKind.PHOTO,
                title = "Visible photo",
                body = null,
                mediaAltText = "A local portrait photo.",
                photoBytes = byteArrayOf(1, 2, 3),
                photoMimeType = "image/jpeg"
            )
        )

        val discoveries = participant.loadDiscoveries(experience.code)

        assertEquals(2, discoveries.size)
        assertTrue(discoveries.any { it.contentKind == R6ContentKind.TEXT })
        assertTrue(discoveries.any { it.contentKind == R6ContentKind.PHOTO })
    }

    @Test
    fun scheduledExperienceKeepsDropsHiddenUntilItsStart() = runBlocking {
        val store = DebugDemoExperienceStore()
        val organizer = DebugDemoR7OrganizerGateway(store)
        val now = System.currentTimeMillis()
        val experience = organizer.createExperience(
            R7ExperienceDraft(
                name = "Scheduled review",
                description = null,
                startsAtMillis = now + 3_600_000L,
                endsAtMillis = now + 7_200_000L,
                timeZone = "Pacific/Honolulu"
            )
        )
        organizer.saveDrop(
            "debug-organizer",
            R7DropDraft(
                experienceCode = experience.code,
                lat = 19.704,
                lng = -155.0767777778,
                title = "Not live yet",
                body = "Wait for the Experience start.",
                mediaAltText = null
            )
        )

        assertTrue(store.participantDiscoveries(experience.code).isEmpty())
    }
}

private class RecordingEntryGateway : R5EntryGateway {
    var called = false

    override suspend fun ensureGuestSession(entrySessionId: String) {
        called = true
    }

    override suspend fun resolve(request: R5EntryRequest): R5ExperiencePreview {
        called = true
        return DebugDemoR5EntryGateway.DEVICE_DEMO_PREVIEW
    }

    override suspend fun join(request: R5EntryRequest): R5ExperiencePreview {
        called = true
        return DebugDemoR5EntryGateway.DEVICE_DEMO_PREVIEW
    }

    override suspend fun recordAuthCompletion(
        entrySessionId: String,
        upgradePath: String?,
        pendingUnlockResumed: Boolean
    ) {
        called = true
    }

    override suspend fun recordClientEvent(
        eventName: String,
        entrySessionId: String?,
        experienceCode: String?,
        dropId: String?,
        installKey: String?,
        params: Map<String, Any>
    ) {
        called = true
    }
}
