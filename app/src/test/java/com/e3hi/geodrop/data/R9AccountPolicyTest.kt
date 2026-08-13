package com.e3hi.geodrop.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class R9AccountPolicyTest {
    @Test
    fun localSafetyActivityTracksReportsBlocksAndUnblocks() = runBlocking {
        val store = DebugDemoExperienceStore()
        val participant = DebugDemoR6ParticipantGateway(store)
        val account = DebugDemoR9AccountGateway(store)

        participant.submitReport(DebugDemoR6ParticipantGateway.WELCOME_DROP_ID, "OTHER")
        participant.blockHost(DebugDemoR6ParticipantGateway.WELCOME_DROP_ID)

        assertEquals(R9ReportState.RECEIVED, account.loadReportStatuses("user").single().state)
        val blocked = account.loadBlockedHosts("user").single()
        assertEquals("Local demo", blocked.hostLabel)
        assertEquals(true, account.unblockHost(blocked.hostId))
        assertEquals(emptyList<R9BlockedHost>(), account.loadBlockedHosts("user"))
    }

    @Test
    fun availabilityHonorsCancellationAndEventWindow() {
        val now = 1_000L
        assertEquals(
            R9ExperienceAvailability.CANCELLED,
            R9AccountPolicy.availability(R7ExperienceState.CANCELLED, 0L, 2_000L, now)
        )
        assertEquals(
            R9ExperienceAvailability.UPCOMING,
            R9AccountPolicy.availability(R7ExperienceState.PUBLISHED, 1_001L, 2_000L, now)
        )
        assertEquals(
            R9ExperienceAvailability.ACTIVE,
            R9AccountPolicy.availability(R7ExperienceState.PUBLISHED, 900L, 2_000L, now)
        )
        assertEquals(
            R9ExperienceAvailability.ENDED,
            R9AccountPolicy.availability(R7ExperienceState.PUBLISHED, 0L, 1_000L, now)
        )
    }

    @Test
    fun historyPutsCurrentWorkBeforePastAndCancelledExperiences() {
        val history = listOf(
            item("Ended", R9ExperienceAvailability.ENDED, 50L),
            item("Upcoming", R9ExperienceAvailability.UPCOMING, 300L),
            item("Cancelled", R9ExperienceAvailability.CANCELLED, 400L),
            item("Active", R9ExperienceAvailability.ACTIVE, 100L)
        )

        assertEquals(
            listOf("Active", "Upcoming", "Ended", "Cancelled"),
            R9AccountPolicy.sortHistory(history).map(R9JoinedExperience::name)
        )
    }

    @Test
    fun reportStatesUsePlainPublicLabels() {
        assertEquals(R9ReportState.RECEIVED, R9ReportState.fromRaw("queued"))
        assertEquals("Action taken", R9AccountPolicy.reportStatusLabel(R9ReportState.ACTION_TAKEN))
        assertEquals("Review complete", R9AccountPolicy.reportStatusLabel(R9ReportState.CLOSED))
    }

    private fun item(
        name: String,
        availability: R9ExperienceAvailability,
        startsAtMillis: Long
    ) = R9JoinedExperience(
        code = name.uppercase(),
        name = name,
        hostLabel = "Host",
        startsAtMillis = startsAtMillis,
        endsAtMillis = startsAtMillis + 100L,
        timeZone = "UTC",
        availability = availability,
        isOwned = false
    )
}
