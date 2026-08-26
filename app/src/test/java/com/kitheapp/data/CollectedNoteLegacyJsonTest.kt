package com.kitheapp.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Locally stored collected notes are the one place pre-2.6 dislike data survives:
 * the wipe cleared Firestore, but a device still holds whatever JSON it persisted
 * before dislikes were removed. These notes must keep loading, with the dislike
 * keys ignored rather than treated as a reaction.
 *
 * Runs under Robolectric because `CollectedNote` persists through `org.json`,
 * which is stubbed out in plain JVM unit tests.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CollectedNoteLegacyJsonTest {

    private fun legacyJson(isLiked: Boolean, isDisliked: Boolean): JSONObject =
        JSONObject()
            .put("id", "legacy-drop")
            .put("text", "Stored before task 2.6")
            .put("contentType", "TEXT")
            .put("collectedAt", 1_700_000_000_000L)
            .put("likeCount", 4L)
            .put("isLiked", isLiked)
            .put("dislikeCount", 7L)
            .put("isDisliked", isDisliked)

    @Test
    fun fromJson_ignoresLegacyDislikeKeys() {
        val note = CollectedNote.fromJson(legacyJson(isLiked = false, isDisliked = true))

        assertEquals("legacy-drop", note.id)
        assertEquals(4L, note.likeCount)
        assertEquals(false, note.isLiked)
        assertEquals(DropLikeStatus.NONE, note.likeStatus())
    }

    @Test
    fun fromJson_keepsLikeAlongsideLegacyDislikeKeys() {
        val note = CollectedNote.fromJson(legacyJson(isLiked = true, isDisliked = false))

        assertEquals(DropLikeStatus.LIKED, note.likeStatus())
        assertEquals(4L, note.likeCount)
    }

    @Test
    fun toJson_noLongerWritesDislikeKeys() {
        val note = CollectedNote.fromJson(legacyJson(isLiked = true, isDisliked = true))

        val rewritten = note.toJson()

        assertEquals(false, rewritten.has("dislikeCount"))
        assertEquals(false, rewritten.has("isDisliked"))
        assertEquals(true, rewritten.getBoolean("isLiked"))
    }

    @Test
    fun likeStatusFromRaw_treatsLegacyDownvoteEncodingAsNone() {
        assertEquals(DropLikeStatus.NONE, DropLikeStatus.fromRaw("disliked"))
        assertEquals(DropLikeStatus.NONE, DropLikeStatus.fromRaw("-1"))
        assertEquals(DropLikeStatus.NONE, DropLikeStatus.fromRaw(-1))
        assertEquals(DropLikeStatus.LIKED, DropLikeStatus.fromRaw("liked"))
    }
}
