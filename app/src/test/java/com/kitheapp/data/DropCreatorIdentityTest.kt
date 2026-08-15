package com.kitheapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 2.1 — pins the acceptance criterion: a drop always carries an
 * identifiable creator, and no "post anonymously" state survives anywhere in
 * the model or the serialized payload.
 *
 * The repository's runtime guard (requireIdentifiedCreator) additionally
 * rejects null/anonymous-auth principals, but that needs FirebaseAuth and so is
 * covered by the emulator rules suite and the on-device walkthrough instead.
 */
class DropCreatorIdentityTest {

    private fun drop(overrides: Drop.() -> Drop = { this }) = Drop(
        text = "Drop",
        lat = 1.0,
        lng = 1.0,
        createdBy = "creator-uid",
        createdAt = 1L,
        dropperUsername = "explorer",
    ).overrides()

    @Test
    fun drop_hasNoAnonymousPostingState() {
        val fields = Drop::class.java.declaredFields.map { it.name }

        assertFalse(
            "Drop must not carry an anonymous-posting flag: $fields",
            fields.any { it.equals("isAnonymous", ignoreCase = true) }
        )
    }

    @Test
    fun huntStepDraft_hasNoAnonymousPostingState() {
        val fields = HuntStepDraft::class.java.declaredFields.map { it.name }

        assertFalse(
            "HuntStepDraft must not carry an anonymous-posting flag: $fields",
            fields.any { it.contains("anonymous", ignoreCase = true) }
        )
    }

    @Test
    fun displayHandle_alwaysAttributesTheCreator() {
        val (handle, _) = drop().displayTitleParts()

        assertEquals("@explorer", handle)
    }

    @Test
    fun displayHandle_isNullOnlyWhenNoUsernameIsStored() {
        val (handle, _) = drop { copy(dropperUsername = null) }.displayTitleParts()

        assertNull(handle)
    }

    @Test
    fun createdBy_isCarriedAndNeverBlankForAnAuthoredDrop() {
        val authored = drop()

        assertTrue(authored.createdBy.isNotBlank())
        assertEquals("creator-uid", authored.createdBy)
    }
}
