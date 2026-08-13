package com.e3hi.geodrop.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotEquals
import org.junit.Test

class R7OrganizerPolicyTest {
    @Test
    fun experienceRequiresOrderedDatesAndValidRadius() {
        val valid = R7ExperienceDraft(
            name = "Garden walk",
            description = null,
            startsAtMillis = 1_000L,
            endsAtMillis = 2_000L,
            timeZone = "Pacific/Honolulu",
            defaultRadiusM = 25
        )

        assertNull(R7OrganizerPolicy.validateExperience(valid))
        assertNotNull(R7OrganizerPolicy.validateExperience(valid.copy(endsAtMillis = 1_000L)))
        assertNotNull(R7OrganizerPolicy.validateExperience(valid.copy(defaultRadiusM = 10)))
    }

    @Test
    fun textAndPhotoDropsValidateDifferentRequiredContent() {
        val text = R7DropDraft(
            experienceCode = "DEMO2026",
            lat = 19.704,
            lng = -155.0767777778,
            title = "Welcome",
            body = "A location note",
            mediaAltText = null
        )
        assertNull(R7OrganizerPolicy.validateDrop(text))
        assertNotNull(R7OrganizerPolicy.validateDrop(text.copy(body = "")))
        assertNull(
            R7OrganizerPolicy.validateDrop(
                text.copy(
                    contentKind = R7DropContentKind.PHOTO,
                    body = null,
                    mediaAltText = "A red flower beside the path",
                    photoBytes = byteArrayOf(1),
                    photoMimeType = "image/jpeg"
                )
            )
        )
    }

    @Test
    fun debugGatewaySupportsCreateEditAndDeleteWithoutFirebase() = runBlocking {
        val gateway = DebugDemoR7OrganizerGateway()
        assertEquals(
            R7OrganizerAccessStatus.APPROVED,
            gateway.loadAccessState("local-user").status
        )
        val draft = R7ExperienceDraft(
            name = "Venue review",
            description = "Local only",
            startsAtMillis = 1_800_000_000_000L,
            endsAtMillis = 1_800_014_400_000L,
            timeZone = "Pacific/Honolulu"
        )
        val experience = gateway.createExperience(draft)
        assertTrue(experience.code.matches(Regex("^[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8}$")))

        val saved = gateway.saveDrop(
            "local-user",
            R7DropDraft(
                experienceCode = experience.code,
                lat = 19.704,
                lng = -155.0767777778,
                title = "Test location",
                body = "Created in the local R7 fixture",
                mediaAltText = null
            )
        )
        assertEquals(1, gateway.loadDrops("local-user", experience.code).size)
        gateway.deleteDrop(saved.dropId)
        assertTrue(gateway.loadDrops("local-user", experience.code).isEmpty())
    }

    @Test
    fun debugExperienceCodesStayUniqueAcrossRecreatedStores() = runBlocking {
        val draft = R7ExperienceDraft(
            name = "Unique code review",
            description = null,
            startsAtMillis = 1_800_000_000_000L,
            endsAtMillis = 1_800_014_400_000L,
            timeZone = "Pacific/Honolulu"
        )

        val first = DebugDemoR7OrganizerGateway().createExperience(draft).code
        val second = DebugDemoR7OrganizerGateway().createExperience(draft).code

        assertNotEquals(first, second)
        assertTrue(first.matches(Regex("^[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8}$")))
        assertTrue(second.matches(Regex("^[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8}$")))
    }
}
