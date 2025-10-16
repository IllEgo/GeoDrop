package com.e3hi.geodrop.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class FirestoreRepoTest {

    @Test
    fun applyUserLike_addsEntryAndIncrementsCount() {
        val drop = Drop(likeCount = 0, likedBy = emptyMap())

        val updated = drop.applyUserLike("userA", DropLikeStatus.LIKED)

        assertEquals(1, updated.likeCount)
        assertEquals(true, updated.likedBy["userA"])
    }

    @Test
    fun applyUserLike_removingLikeDecrementsCount() {
        val drop = Drop(likeCount = 2, likedBy = mapOf("userA" to true, "userB" to true))

        val updated = drop.applyUserLike("userA", DropLikeStatus.NONE)

        assertEquals(1, updated.likeCount)
        assertEquals(false, updated.likedBy.containsKey("userA"))
        assertEquals(true, updated.likedBy["userB"])
    }

    @Test
    fun applyUserLike_noChangeReturnsSameInstance() {
        val drop = Drop(likeCount = 1, likedBy = mapOf("userA" to true))

        val updated = drop.applyUserLike("userA", DropLikeStatus.LIKED)

        assertSame(drop, updated)
    }
}