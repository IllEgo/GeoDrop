package com.e3hi.geodrop.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FirestoreRepoTest {

    @Test
    fun computeVoteTransaction_normalizesComplexVoteMap() {
        val rawVoteMap = mapOf(
            "userA" to true,
            "userB" to "downvote",
            "userC" to mapOf("value" to "UP"),
            "userD" to mapOf("state" to mapOf("choice" to "-1")),
            "userE" to "  ThUmBs_Up  ",
            "userF" to false
        )

        val resultA = FirestoreRepo.computeVoteTransaction(
            currentUpvotes = 5,
            currentDownvotes = 2,
            currentVoteMap = rawVoteMap,
            userId = "userA",
            vote = DropVoteType.DOWNVOTE
        )

        assertEquals(1, resultA.previousVote)
        assertEquals(4, resultA.updatedUpvotes)
        assertEquals(3, resultA.updatedDownvotes)
        assertEquals(-1L, resultA.newVoteValue)

        val resultB = FirestoreRepo.computeVoteTransaction(
            currentUpvotes = 5,
            currentDownvotes = 2,
            currentVoteMap = rawVoteMap,
            userId = "userB",
            vote = DropVoteType.NONE
        )

        assertEquals(-1, resultB.previousVote)
        assertEquals(5, resultB.updatedUpvotes)
        assertEquals(1, resultB.updatedDownvotes)
        assertNull(resultB.newVoteValue)

        val resultD = FirestoreRepo.computeVoteTransaction(
            currentUpvotes = 5,
            currentDownvotes = 2,
            currentVoteMap = rawVoteMap,
            userId = "userD",
            vote = DropVoteType.UPVOTE
        )

        assertEquals(-1, resultD.previousVote)
        assertEquals(6, resultD.updatedUpvotes)
        assertEquals(1, resultD.updatedDownvotes)
        assertEquals(1L, resultD.newVoteValue)

        val resultE = FirestoreRepo.computeVoteTransaction(
            currentUpvotes = 5,
            currentDownvotes = 2,
            currentVoteMap = rawVoteMap,
            userId = "userE",
            vote = DropVoteType.UPVOTE
        )

        assertEquals(1, resultE.previousVote)
        assertEquals(5, resultE.updatedUpvotes)
        assertEquals(2, resultE.updatedDownvotes)
        assertEquals(1L, resultE.newVoteValue)

        val normalized = FirestoreRepo.normalizeVoteMap(rawVoteMap)
        assertEquals(1, normalized["userA"])
        assertEquals(-1, normalized["userB"])
        assertEquals(1, normalized["userC"])
        assertEquals(-1, normalized["userD"])
        assertEquals(1, normalized["userE"])
        assertEquals(0, normalized["userF"])
    }

    @Test
    fun computeVoteTransaction_noChangeSkipsUpdates() {
        val rawVoteMap = mapOf(
            "userC" to mapOf("direction" to "UP")
        )

        val result = FirestoreRepo.computeVoteTransaction(
            currentUpvotes = 5,
            currentDownvotes = 2,
            currentVoteMap = rawVoteMap,
            userId = "userC",
            vote = DropVoteType.UPVOTE
        )

        assertEquals(1, result.previousVote)
        assertEquals(5, result.updatedUpvotes)
        assertEquals(2, result.updatedDownvotes)
        assertEquals(1L, result.newVoteValue)
    }
}