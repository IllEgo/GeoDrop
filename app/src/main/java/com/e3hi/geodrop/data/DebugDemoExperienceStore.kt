package com.e3hi.geodrop.data

import java.time.Instant
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Shared in-memory source for the debug R5, R6, and R7 gateways. Keeping one store is
 * important: an Experience authored through R7 must resolve through R5 and its published
 * drops must be discoverable through R6 without falling through to Firebase.
 */
class DebugDemoExperienceStore {
    private val lock = Any()
    private val experiences = linkedMapOf<String, R7Experience>()
    private val drops = linkedMapOf<String, R7OrganizerDrop>()
    private val rewardCodes = linkedMapOf<String, LinkedHashMap<String, R8RewardCode>>()
    private val joinedSessions = linkedMapOf<String, MutableSet<String>>()
    private val unlockReceipts = linkedMapOf<String, DebugUnlockRecord>()
    private val blockedHosts = linkedSetOf<String>()
    private val reportStatuses = linkedMapOf<String, R9ReportStatus>()

    init {
        experiences[DebugDemoR5EntryGateway.DEVICE_DEMO_CODE] = R7Experience(
            code = DebugDemoR5EntryGateway.DEVICE_DEMO_CODE,
            name = "GeoDrop device review",
            description = "Local R7 venue walkthrough fixture",
            startsAtMillis = 1_786_339_200_000L,
            endsAtMillis = 1_789_017_600_000L,
            timeZone = "Pacific/Honolulu",
            defaultRadiusM = 25
        )
        addFixtureDrop(
            id = "demo-welcome",
            title = "Welcome to the venue",
            body = "This text drop is ready for the R7 organizer walkthrough.",
            kind = R7DropContentKind.TEXT,
            lat = DebugDemoR6ParticipantGateway.DEMO_TEST_LATITUDE,
            lng = DebugDemoR6ParticipantGateway.DEMO_TEST_LONGITUDE,
            altText = null
        )
        addFixtureDrop(
            id = "demo-photo",
            title = "Garden colors",
            body = "Photo authoring remains local in this debug review.",
            kind = R7DropContentKind.PHOTO,
            lat = DebugDemoR6ParticipantGateway.DEMO_TEST_LATITUDE + 0.00035,
            lng = DebugDemoR6ParticipantGateway.DEMO_TEST_LONGITUDE + 0.00025,
            altText = "Bright tropical flowers beside a garden path."
        )
        addFixtureDrop(
            id = DebugDemoR6ParticipantGateway.REWARD_DROP_ID,
            title = "Island welcome reward",
            body = "Find this reward and show its unique code during the R8 walkthrough.",
            kind = R7DropContentKind.TEXT,
            lat = DebugDemoR6ParticipantGateway.DEMO_TEST_LATITUDE - 0.00035,
            lng = DebugDemoR6ParticipantGateway.DEMO_TEST_LONGITUDE + 0.00045,
            altText = null,
            dropKind = R6DropKind.REWARD,
            rewardPresentation = mapOf(
                "rewardLabel" to "Demo welcome reward",
                "businessLabel" to "Local demo counter",
                "instructions" to "Show this code at the review counter.",
                "terms" to "One local demo use."
            ),
            inventoryLimit = 3,
            fixtureCodes = listOf("DEMO-7K4P", "DEMO-8M2Q", "DEMO-9R6T")
        )
    }

    fun containsExperience(code: String): Boolean = synchronized(lock) {
        experiences.containsKey(code.trim().uppercase())
    }

    fun recordJoin(code: String, sessionId: String) = synchronized(lock) {
        if (experiences.containsKey(code)) {
            joinedSessions.getOrPut(code) { linkedSetOf() }.add(sessionId)
        }
    }

    fun preview(code: String): R5ExperiencePreview? = synchronized(lock) {
        val experience = experiences[code.trim().uppercase()] ?: return@synchronized null
        val availability = when {
            experience.state == R7ExperienceState.CANCELLED -> R5ExperienceAvailability.CANCELLED
            System.currentTimeMillis() < experience.startsAtMillis -> R5ExperienceAvailability.UPCOMING
            System.currentTimeMillis() >= experience.endsAtMillis -> R5ExperienceAvailability.ENDED
            else -> R5ExperienceAvailability.ACTIVE
        }
        R5ExperiencePreview(
            code = experience.code,
            name = experience.name,
            description = experience.description,
            hostLabel = "Local demo",
            startsAt = Instant.ofEpochMilli(experience.startsAtMillis).toString(),
            endsAt = Instant.ofEpochMilli(experience.endsAtMillis).toString(),
            timeZone = experience.timeZone,
            availability = availability,
            availableDropCount = if (experience.code == DebugDemoR5EntryGateway.DEVICE_DEMO_CODE) {
                4
            } else {
                drops.values.count {
                    it.summary.experienceCode == experience.code &&
                        it.summary.moderationState == "SAFE"
                }
            },
            membership = R5ExperienceMembership.MEMBER
        )
    }

    fun listExperiences(): List<R7Experience> = synchronized(lock) {
        experiences.values.map { experience ->
            experience.copy(
                dropCount = drops.values.count { it.summary.experienceCode == experience.code }
            )
        }.sortedByDescending { it.startsAtMillis }
    }

    fun blockHost(hostId: String): Boolean = synchronized(lock) {
        blockedHosts.add(hostId)
    }

    fun unblockHost(hostId: String): Boolean = synchronized(lock) {
        blockedHosts.remove(hostId)
    }

    fun listBlockedHosts(): Set<String> = synchronized(lock) { blockedHosts.toSet() }

    fun recordReport(dropId: String): R9ReportStatus = synchronized(lock) {
        val reportId = "demo-report-$dropId"
        reportStatuses.getOrPut(reportId) {
            R9ReportStatus(
                reportId = reportId,
                dropId = dropId,
                state = R9ReportState.RECEIVED,
                updatedAtMillis = System.currentTimeMillis()
            )
        }
    }

    fun listReportStatuses(): List<R9ReportStatus> = synchronized(lock) {
        reportStatuses.values.sortedByDescending { it.updatedAtMillis ?: 0L }
    }

    fun createExperience(draft: R7ExperienceDraft): R7Experience = synchronized(lock) {
        var code: String
        do {
            code = generateExperienceCode()
        } while (experiences.containsKey(code) || !issuedExperienceCodes.add(code))
        R7Experience(
            code = code,
            name = draft.name.trim(),
            description = draft.description?.trim()?.takeIf(String::isNotEmpty),
            startsAtMillis = draft.startsAtMillis,
            endsAtMillis = draft.endsAtMillis,
            timeZone = draft.timeZone.trim(),
            state = draft.state,
            defaultRadiusM = draft.defaultRadiusM
        ).also { experiences[code] = it }
    }

    fun updateExperience(code: String, draft: R7ExperienceDraft): R7Experience =
        synchronized(lock) {
            val current = experiences[code]
                ?: throw R7OrganizerException("This Experience could not be found.", false)
            current.copy(
                name = draft.name.trim(),
                description = draft.description?.trim()?.takeIf(String::isNotEmpty),
                startsAtMillis = draft.startsAtMillis,
                endsAtMillis = draft.endsAtMillis,
                timeZone = draft.timeZone.trim(),
                state = draft.state,
                defaultRadiusM = draft.defaultRadiusM
            ).also { experiences[code] = it }
        }

    fun listOrganizerDrops(experienceCode: String): List<R7OrganizerDropSummary> =
        synchronized(lock) {
            drops.values
                .filter { it.summary.experienceCode == experienceCode }
                .map { it.summary }
                .sortedByDescending { it.publishedAtMillis }
        }

    fun organizerDrop(dropId: String): R7OrganizerDrop? = synchronized(lock) { drops[dropId] }

    fun saveDrop(draft: R7DropDraft): R7SaveDropResult = synchronized(lock) {
        if (!experiences.containsKey(draft.experienceCode)) {
            throw R7OrganizerException("This Experience could not be found.", false)
        }
        val now = System.currentTimeMillis()
        val id = draft.dropId ?: "local-${UUID.randomUUID()}"
        val existing = drops[id]
        if (existing != null && existing.summary.dropKind != draft.dropKind) {
            throw R7OrganizerException("Drop type cannot change after publishing.", false)
        }
        val summary = R7OrganizerDropSummary(
            id = id,
            experienceCode = draft.experienceCode,
            title = draft.title.trim(),
            contentKind = draft.contentKind,
            dropKind = draft.dropKind,
            // Production photo drops remain PENDING until moderation runs. This local
            // fixture has no moderation service, so simulate approval for end-to-end
            // organizer-to-participant device review.
            moderationState = "SAFE",
            lat = draft.lat,
            lng = draft.lng,
            radiusM = draft.radiusM,
            expiryMode = draft.expiryMode,
            expiresAtMillis = draft.expiresAtMillis,
            publishedAtMillis = existing?.summary?.publishedAtMillis ?: now,
            editedAtMillis = existing?.let { now }
        )
        drops[id] = R7OrganizerDrop(
            summary = summary,
            body = draft.body?.trim()?.takeIf(String::isNotEmpty),
            mediaAltText = draft.mediaAltText?.trim()?.takeIf(String::isNotEmpty),
            rewardPresentation = if (draft.dropKind == R6DropKind.REWARD) {
                mapOfNotNull(
                    "rewardLabel" to draft.rewardLabel,
                    "businessLabel" to draft.businessLabel,
                    "instructions" to draft.rewardInstructions,
                    "terms" to draft.rewardTerms
                )
            } else emptyMap(),
            inventoryLimit = draft.inventoryLimit
        )
        if (draft.dropKind == R6DropKind.REWARD && existing == null) {
            rewardCodes[id] = generateRewardCodes(id, draft.inventoryLimit ?: 1)
        }
        R7SaveDropResult(
            dropId = id,
            payloadVersion = if (existing == null) 1 else 2
        )
    }

    fun deleteDrop(dropId: String): Boolean = synchronized(lock) { drops.remove(dropId) != null }

    fun issueReward(dropId: String, receiptId: String, issuedAtMillis: Long): DebugRewardIssue =
        synchronized(lock) {
            val drop = drops[dropId]
            if (drop?.summary?.dropKind != R6DropKind.REWARD) return@synchronized DebugRewardIssue()
            val existing = rewardCodes[dropId]?.values?.firstOrNull { code ->
                code.history.any { it.reason == receiptId }
            }
            if (existing != null) {
                return@synchronized DebugRewardIssue(existing.toReceipt(drop, receiptId))
            }
            val available = rewardCodes[dropId]?.entries
                ?.firstOrNull { it.value.state == R8RewardCodeState.AVAILABLE }
                ?: return@synchronized DebugRewardIssue(unavailable = true)
            val issued = available.value.copy(
                state = R8RewardCodeState.ISSUED,
                issuedAtMillis = issuedAtMillis,
                version = available.value.version + 1,
                history = listOf(
                    R8RewardCodeEvent("AVAILABLE_TO_ISSUED", issuedAtMillis, receiptId)
                ) + available.value.history
            )
            rewardCodes[dropId]!![available.key] = issued
            DebugRewardIssue(issued.toReceipt(drop, receiptId))
        }

    fun recordUnlock(
        dropId: String,
        receiptId: String,
        actorKey: String,
        unlockedAtMillis: Long
    ) = synchronized(lock) {
        val code = drops[dropId]?.summary?.experienceCode ?: return@synchronized
        unlockReceipts.putIfAbsent(
            receiptId,
            DebugUnlockRecord(code, dropId, actorKey, unlockedAtMillis)
        )
    }

    fun currentRewardReceipt(dropId: String, receiptId: String): R6RewardReceipt? =
        synchronized(lock) {
            val drop = drops[dropId] ?: return@synchronized null
            rewardCodes[dropId]?.values?.firstOrNull { code ->
                code.history.any { it.reason == receiptId }
            }?.toReceipt(drop, receiptId)
        }

    fun listRewardCodes(
        dropId: String,
        state: R8RewardCodeState?,
        searchCode: String?
    ): List<R8RewardCode> = synchronized(lock) {
        val normalized = searchCode?.let(R8RewardPolicy::normalizeCode)?.takeIf(String::isNotEmpty)
        rewardCodes[dropId]?.values.orEmpty()
            .filter { state == null || it.state == state }
            .filter { normalized == null || it.code == normalized }
            .sortedByDescending { it.issuedAtMillis ?: 0L }
            .take(100)
    }

    fun markRewardCodeUsed(dropId: String, rawCode: String): Boolean = synchronized(lock) {
        val code = R8RewardPolicy.normalizeCode(rawCode)
        val pool = rewardCodes[dropId]
            ?: throw R7OrganizerException("This reward could not be found.", false)
        val current = pool[code]
            ?: throw R7OrganizerException("That code was not found for this reward.", false)
        if (current.state == R8RewardCodeState.USED) return@synchronized false
        if (current.state != R8RewardCodeState.ISSUED) {
            throw R7OrganizerException("That code has not been issued to a guest.", false)
        }
        val now = System.currentTimeMillis()
        pool[code] = current.copy(
            state = R8RewardCodeState.USED,
            usedAtMillis = now,
            version = current.version + 1,
            history = listOf(R8RewardCodeEvent("ISSUED_TO_USED", now, null)) + current.history
        )
        true
    }

    fun correctRewardCodeUse(
        dropId: String,
        rawCode: String,
        reason: R8CorrectionReason
    ): Boolean = synchronized(lock) {
        val code = R8RewardPolicy.normalizeCode(rawCode)
        val pool = rewardCodes[dropId]
            ?: throw R7OrganizerException("This reward could not be found.", false)
        val current = pool[code]
            ?: throw R7OrganizerException("That code was not found for this reward.", false)
        if (current.state == R8RewardCodeState.ISSUED) return@synchronized false
        if (current.state != R8RewardCodeState.USED) {
            throw R7OrganizerException("That code is not marked used.", false)
        }
        val now = System.currentTimeMillis()
        pool[code] = current.copy(
            state = R8RewardCodeState.ISSUED,
            usedAtMillis = null,
            version = current.version + 1,
            history = listOf(
                R8RewardCodeEvent("USED_TO_ISSUED", now, reason.name)
            ) + current.history
        )
        true
    }

    fun results(experienceCode: String): R8ExperienceResults = synchronized(lock) {
        val experienceDrops = drops.values.filter { it.summary.experienceCode == experienceCode }
        val experienceUnlocks = unlockReceipts.values.filter { it.experienceCode == experienceCode }
        val dropResults = experienceDrops.map { drop ->
            val codes = rewardCodes[drop.summary.id]?.values.orEmpty()
            R8DropResult(
                dropId = drop.summary.id,
                unlocks = experienceUnlocks.count { it.dropId == drop.summary.id }.toLong(),
                codesIssued = codes.count { it.state != R8RewardCodeState.AVAILABLE }.toLong(),
                codesUsed = codes.count { it.state == R8RewardCodeState.USED }.toLong(),
                updatedAtMillis = (
                    experienceUnlocks.filter { it.dropId == drop.summary.id }.map { it.unlockedAtMillis } +
                        codes.mapNotNull { it.usedAtMillis ?: it.issuedAtMillis }
                    ).maxOrNull()
            )
        }
        R8ExperienceResults(
            experienceCode = experienceCode,
            joinedParticipants = joinedSessions[experienceCode]?.size?.toLong() ?: 0L,
            publishedDrops = experienceDrops.size.toLong(),
            uniqueUnlockers = experienceUnlocks.map(DebugUnlockRecord::actorKey).distinct().size.toLong(),
            unlocks = experienceUnlocks.size.toLong(),
            mainTrailCompletions = 0,
            codesIssued = dropResults.sumOf(R8DropResult::codesIssued),
            codesUsed = dropResults.sumOf(R8DropResult::codesUsed),
            updatedAtMillis = experienceUnlocks.maxOfOrNull { it.unlockedAtMillis },
            reconciledAtMillis = System.currentTimeMillis(),
            drops = dropResults.sortedByDescending(R8DropResult::unlocks)
        )
    }

    fun participantDiscoveries(experienceCode: String): List<R6DropDiscovery> = synchronized(lock) {
        val experience = experiences[experienceCode] ?: return@synchronized emptyList()
        val now = System.currentTimeMillis()
        if (
            experience.state != R7ExperienceState.PUBLISHED ||
            now < experience.startsAtMillis ||
            now >= experience.endsAtMillis
        ) return@synchronized emptyList()
        drops.values.mapNotNull { drop ->
            val summary = drop.summary
            if (
                summary.experienceCode != experienceCode ||
                summary.moderationState != "SAFE"
            ) return@mapNotNull null
            R6DropDiscovery(
                id = summary.id,
                experienceCode = summary.experienceCode,
                ownerId = "debug-demo-host",
                hostLabel = "Local demo",
                lat = summary.lat,
                lng = summary.lng,
                radiusM = summary.radiusM,
                contentKind = when (summary.contentKind) {
                    R7DropContentKind.TEXT -> R6ContentKind.TEXT
                    R7DropContentKind.PHOTO -> R6ContentKind.PHOTO
                },
                dropKind = summary.dropKind,
                payloadVersion = if (summary.editedAtMillis == null) 1 else 2,
                trailId = null,
                trailStepIndex = null,
                trailTotalSteps = null,
                likeCount = 0,
                publishedAtMillis = summary.publishedAtMillis,
                editedAtMillis = summary.editedAtMillis,
                expiryMode = when (summary.expiryMode) {
                    R7ExpiryMode.NONE -> R6ExpiryMode.NONE
                    R7ExpiryMode.EXPERIENCE_END -> R6ExpiryMode.EXPERIENCE_END
                    R7ExpiryMode.CUSTOM -> R6ExpiryMode.CUSTOM
                },
                expiresAtMillis = summary.expiresAtMillis
            )
        }
    }

    private fun addFixtureDrop(
        id: String,
        title: String,
        body: String,
        kind: R7DropContentKind,
        lat: Double,
        lng: Double,
        altText: String?,
        dropKind: R6DropKind = R6DropKind.STANDARD,
        rewardPresentation: Map<String, String> = emptyMap(),
        inventoryLimit: Int? = null,
        fixtureCodes: List<String> = emptyList()
    ) {
        val summary = R7OrganizerDropSummary(
            id = id,
            experienceCode = DebugDemoR5EntryGateway.DEVICE_DEMO_CODE,
            title = title,
            contentKind = kind,
            dropKind = dropKind,
            moderationState = "SAFE",
            lat = lat,
            lng = lng,
            radiusM = 25,
            expiryMode = R7ExpiryMode.NONE,
            expiresAtMillis = null,
            publishedAtMillis = 1_786_425_600_000L,
            editedAtMillis = null
        )
        drops[id] = R7OrganizerDrop(
            summary,
            body,
            altText,
            rewardPresentation = rewardPresentation,
            inventoryLimit = inventoryLimit
        )
        if (dropKind == R6DropKind.REWARD) {
            rewardCodes[id] = fixtureCodes.associateWithTo(linkedMapOf()) { code ->
                R8RewardCode(code, R8RewardCodeState.AVAILABLE, null, null, 1)
            }
        }
    }

    private fun generateRewardCodes(
        dropId: String,
        inventoryLimit: Int
    ): LinkedHashMap<String, R8RewardCode> {
        val prefix = dropId.filter(Char::isLetterOrDigit).uppercase().takeLast(4).padStart(4, 'X')
        return (1..inventoryLimit).associateTo(linkedMapOf()) { index ->
            val code = "LOCAL-$prefix-${index.toString().padStart(4, '0')}"
            code to R8RewardCode(code, R8RewardCodeState.AVAILABLE, null, null, 1)
        }
    }

    private fun mapOfNotNull(vararg pairs: Pair<String, String?>): Map<String, String> =
        pairs.mapNotNull { (key, value) ->
            value?.trim()?.takeIf(String::isNotEmpty)?.let { key to it }
        }.toMap()

    private fun R8RewardCode.toReceipt(
        drop: R7OrganizerDrop,
        receiptId: String
    ) = R6RewardReceipt(
        receiptId = receiptId,
        dropId = drop.summary.id,
        experienceCode = drop.summary.experienceCode,
        code = code,
        state = state.name,
        issuedAtMillis = issuedAtMillis ?: System.currentTimeMillis(),
        usedAtMillis = usedAtMillis
    )

    private fun generateExperienceCode(): String = buildString(EXPERIENCE_CODE_LENGTH) {
        repeat(EXPERIENCE_CODE_LENGTH) {
            append(EXPERIENCE_CODE_ALPHABET[secureRandom.nextInt(EXPERIENCE_CODE_ALPHABET.length)])
        }
    }

    private companion object {
        const val EXPERIENCE_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        const val EXPERIENCE_CODE_LENGTH = 8
        val secureRandom = SecureRandom()
        val issuedExperienceCodes = ConcurrentHashMap.newKeySet<String>()
    }
}

data class DebugRewardIssue(
    val receipt: R6RewardReceipt? = null,
    val unavailable: Boolean = false
)

private data class DebugUnlockRecord(
    val experienceCode: String,
    val dropId: String,
    val actorKey: String,
    val unlockedAtMillis: Long
)
