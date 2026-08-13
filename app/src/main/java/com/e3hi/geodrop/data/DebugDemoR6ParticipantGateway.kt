package com.e3hi.geodrop.data

import java.util.UUID

/** Complete local R6 fixture used only by MainActivity in a debug APK. */
class DebugDemoR6ParticipantGateway(
    private val store: DebugDemoExperienceStore = DebugDemoExperienceStore()
) : R6ParticipantGateway {
    private val lock = Any()
    private val participantKey = UUID.randomUUID().toString().take(8)
    private val receipts = linkedMapOf<String, R6CollectionReceipt>()

    init {
        val welcome = DISCOVERIES.first { it.id == WELCOME_DROP_ID }
        receipts[welcome.id] = receiptFor(welcome, unlockedAtMillis = DEMO_TIME_MILLIS)
    }

    override suspend fun loadDiscoveries(experienceCode: String): List<R6DropDiscovery> =
        if (experienceCode == DebugDemoR5EntryGateway.DEVICE_DEMO_CODE) {
            DISCOVERIES
        } else {
            store.participantDiscoveries(experienceCode)
        }

    override suspend fun loadCollection(userId: String): List<R6CollectionReceipt> =
        synchronized(lock) {
            receipts.values.map { receipt ->
                receipt.copy(
                    reward = receipt.reward?.let { reward ->
                        store.currentRewardReceipt(receipt.dropId, reward.receiptId) ?: reward
                    }
                )
            }.sortedByDescending { it.unlockedAtMillis }
        }

    override suspend fun loadTrailProgress(
        userId: String,
        experienceCode: String
    ): List<R6TrailProgress> = if (
        experienceCode == DebugDemoR5EntryGateway.DEVICE_DEMO_CODE
    ) {
        listOf(
            R6TrailProgress(
                experienceCode = experienceCode,
                trailId = DEMO_TRAIL_ID,
                currentStepIndex = 0,
                completedDropIds = emptyList(),
                completedAtMillis = null
            )
        )
    } else {
        emptyList()
    }

    override suspend fun loadBlockedHostIds(userId: String): Set<String> =
        store.listBlockedHosts()

    override suspend fun unlock(request: R6UnlockRequest): R6UnlockResult {
        val drop = DISCOVERIES.firstOrNull { it.id == request.dropId }
            ?: store.listExperiences().asSequence()
                .flatMap { store.participantDiscoveries(it.code).asSequence() }
                .firstOrNull { it.id == request.dropId }
            ?: throw R6ParticipantException(
                reason = R6UnlockFailureReason.DROP_NOT_AVAILABLE,
                retryable = false
            )
        synchronized(lock) {
            val existing = receipts[drop.id]
            if (existing != null) {
                return R6UnlockResult(
                    alreadyUnlocked = true,
                    receipt = existing,
                    rewardUnavailable = drop.dropKind == R6DropKind.REWARD && existing.reward == null
                )
            }
            val issue = if (drop.dropKind == R6DropKind.REWARD) {
                store.issueReward(
                    drop.id,
                    "demo-reward-$participantKey-${drop.id}",
                    System.currentTimeMillis()
                )
            } else null
            val receipt = receiptFor(
                drop,
                unlockedAtMillis = System.currentTimeMillis(),
                rewardIssue = issue
            )
            receipts[drop.id] = receipt
            return R6UnlockResult(
                alreadyUnlocked = false,
                receipt = receipt,
                rewardUnavailable = issue?.unavailable == true
            )
        }
    }

    override suspend fun submitReport(dropId: String, reason: String, narrative: String?) {
        val exists = DISCOVERIES.any { it.id == dropId } ||
            store.listExperiences().any { experience ->
                store.participantDiscoveries(experience.code).any { it.id == dropId }
            }
        if (!exists) {
            throw R6ParticipantException(R6UnlockFailureReason.DROP_NOT_AVAILABLE, false)
        }
        store.recordReport(dropId)
    }

    override suspend fun blockHost(dropId: String) {
        val drop = DISCOVERIES.firstOrNull { it.id == dropId }
            ?: store.listExperiences().asSequence()
                .flatMap { store.participantDiscoveries(it.code).asSequence() }
                .firstOrNull { it.id == dropId }
        drop?.ownerId?.let { ownerId ->
            store.blockHost(ownerId)
        }
    }

    private fun receiptFor(
        drop: R6DropDiscovery,
        unlockedAtMillis: Long,
        rewardIssue: DebugRewardIssue? = null
    ): R6CollectionReceipt {
        val isReward = drop.dropKind == R6DropKind.REWARD
        val localPayload = store.organizerDrop(drop.id)
        val trail = drop.trailId?.let { trailId ->
            R6ReceiptTrail(
                trailId = trailId,
                stepIndex = drop.trailStepIndex ?: 0,
                totalSteps = drop.trailTotalSteps ?: 1,
                completedAtUnlock = drop.trailStepIndex == (drop.trailTotalSteps ?: 1) - 1
            )
        }
        val issuedReward = if (isReward) {
            rewardIssue?.receipt ?: if (drop.id == REWARD_DROP_ID && rewardIssue == null) {
                R6RewardReceipt(
                    receiptId = "demo-reward-$participantKey-${drop.id}",
                    dropId = drop.id,
                    experienceCode = drop.experienceCode,
                    code = "DEMO-7K4P",
                    state = "ISSUED",
                    issuedAtMillis = unlockedAtMillis,
                    usedAtMillis = null
                )
            } else null
        } else null
        return R6CollectionReceipt(
            receiptId = "demo-receipt-$participantKey-${drop.id}",
            dropId = drop.id,
            experienceCode = drop.experienceCode,
            unlockedAtMillis = unlockedAtMillis,
            payloadVersion = drop.payloadVersion,
            snapshot = R6PayloadSnapshot(
                title = localPayload?.summary?.title ?: when (drop.id) {
                    WELCOME_DROP_ID -> "Welcome to the device demo"
                    PHOTO_DROP_ID -> "Garden colors"
                    REWARD_DROP_ID -> "Demo reward"
                    TRAIL_DROP_ID -> "First Trail clue"
                    else -> "Demo find"
                },
                body = localPayload?.body ?: when (drop.id) {
                    WELCOME_DROP_ID -> "This locally saved find confirms that Collection works without a production fixture."
                    PHOTO_DROP_ID -> "A photo payload will use the private media flow when the R6 server is deployed."
                    REWARD_DROP_ID -> "Show the demo code below while reviewing reward presentation."
                    TRAIL_DROP_ID -> "Follow the path toward the next stop."
                    else -> null
                },
                contentKind = drop.contentKind,
                hostLabel = drop.hostLabel,
                mediaAssetId = if (drop.contentKind == R6ContentKind.PHOTO) "debug-photo" else null,
                mediaMimeType = if (drop.contentKind == R6ContentKind.PHOTO) "image/jpeg" else null,
                mediaAltText = localPayload?.mediaAltText ?: if (drop.id == PHOTO_DROP_ID) {
                    "Bright tropical flowers beside a garden path."
                } else null,
                rewardPresentation = localPayload?.rewardPresentation ?: if (isReward) {
                    mapOf("instructions" to "Show this demo code at the review counter.")
                } else emptyMap(),
                editedAtMillis = null
            ),
            trail = trail,
            hasRewardReceipt = issuedReward != null,
            reward = issuedReward
        ).also {
            store.recordUnlock(drop.id, it.receiptId, participantKey, unlockedAtMillis)
        }
    }

    companion object {
        const val WELCOME_DROP_ID = "demo-welcome"
        const val PHOTO_DROP_ID = "demo-photo"
        const val REWARD_DROP_ID = "demo-reward"
        const val TRAIL_DROP_ID = "demo-trail-1"
        // 19°42'14.4"N, 155°04'36.4"W — owner-provided outdoor test point.
        const val DEMO_TEST_LATITUDE = 19.704
        const val DEMO_TEST_LONGITUDE = -155.0767777778
        private const val DEMO_TRAIL_ID = "demo-main-trail"
        private const val DEMO_TIME_MILLIS = 1_786_425_600_000L

        private val DISCOVERIES = listOf(
            discovery(
                id = WELCOME_DROP_ID,
                lat = DEMO_TEST_LATITUDE,
                lng = DEMO_TEST_LONGITUDE,
                contentKind = R6ContentKind.TEXT
            ),
            discovery(
                id = PHOTO_DROP_ID,
                lat = DEMO_TEST_LATITUDE + 0.00035,
                lng = DEMO_TEST_LONGITUDE + 0.00025,
                contentKind = R6ContentKind.PHOTO
            ),
            discovery(
                id = REWARD_DROP_ID,
                lat = DEMO_TEST_LATITUDE - 0.00035,
                lng = DEMO_TEST_LONGITUDE + 0.00045,
                contentKind = R6ContentKind.TEXT,
                dropKind = R6DropKind.REWARD
            ),
            discovery(
                id = TRAIL_DROP_ID,
                lat = DEMO_TEST_LATITUDE + 0.00015,
                lng = DEMO_TEST_LONGITUDE - 0.00060,
                contentKind = R6ContentKind.TEXT,
                trailId = DEMO_TRAIL_ID,
                trailStepIndex = 0,
                trailTotalSteps = 1
            )
        )

        private fun discovery(
            id: String,
            lat: Double,
            lng: Double,
            contentKind: R6ContentKind,
            dropKind: R6DropKind = R6DropKind.STANDARD,
            trailId: String? = null,
            trailStepIndex: Int? = null,
            trailTotalSteps: Int? = null
        ) = R6DropDiscovery(
            id = id,
            experienceCode = DebugDemoR5EntryGateway.DEVICE_DEMO_CODE,
            ownerId = "debug-demo-host",
            hostLabel = "Local demo",
            lat = lat,
            lng = lng,
            radiusM = 30,
            contentKind = contentKind,
            dropKind = dropKind,
            payloadVersion = 1,
            trailId = trailId,
            trailStepIndex = trailStepIndex,
            trailTotalSteps = trailTotalSteps,
            likeCount = 0,
            publishedAtMillis = DEMO_TIME_MILLIS,
            editedAtMillis = null,
            expiryMode = R6ExpiryMode.NONE,
            expiresAtMillis = null
        )
    }
}
