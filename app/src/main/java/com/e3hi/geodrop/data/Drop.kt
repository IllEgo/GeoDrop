package com.e3hi.geodrop.data

data class Drop(
    val id: String = "",
    val text: String = "",
    val description: String? = null,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val createdBy: String = "",
    val createdAt: Long = 0L,
    val dropperUsername: String? = null,
    val isAnonymous: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val decayDays: Int? = null,
    val groupCode: String? = null,
    val dropType: DropType = DropType.COMMUNITY,
    val businessId: String? = null,
    val businessName: String? = null,
    val contentType: DropContentType = DropContentType.TEXT,
    val mediaUrl: String? = null,
    val mediaMimeType: String? = null,
    val mediaData: String? = null,
    val mediaStoragePath: String? = null,
    val isNsfw: Boolean = false,
    val nsfwLabels: List<String> = emptyList(),
    val upvoteCount: Long = 0,
    val downvoteCount: Long = 0,
    val voteMap: Map<String, Long> = emptyMap(),
    val reportCount: Long = 0,
    val reportedBy: Map<String, Long> = emptyMap(),
    val redemptionCode: String? = null,
    val redemptionLimit: Int? = null,
    val redemptionCount: Int = 0,
    val redeemedBy: Map<String, Long> = emptyMap()
)

enum class DropType {
    COMMUNITY,
    RESTAURANT_COUPON,
    TOUR_STOP;

    companion object {
        fun fromRaw(raw: String?): DropType {
            if (raw.isNullOrBlank()) return COMMUNITY
            return entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: COMMUNITY
        }
    }
}

fun Drop.requiresRedemption(): Boolean {
    if (dropType != DropType.RESTAURANT_COUPON) return false
    return !redemptionCode.isNullOrBlank()
}

fun Drop.remainingRedemptions(): Int? {
    val limit = redemptionLimit ?: return null
    return (limit - redemptionCount).coerceAtLeast(0)
}

fun Drop.isBusinessDrop(): Boolean = dropType != DropType.COMMUNITY

fun Drop.isRedeemedBy(userId: String?): Boolean {
    if (userId.isNullOrBlank()) return false
    return redeemedBy.containsKey(userId)
}

private const val MILLIS_PER_DAY = 86_400_000L

fun Drop.decayAtMillis(): Long? {
    val days = decayDays?.takeIf { it > 0 } ?: return null
    val created = createdAt.takeIf { it > 0L } ?: return null
    return created + days * MILLIS_PER_DAY
}

fun Drop.isExpired(nowMillis: Long = System.currentTimeMillis()): Boolean {
    val expireAt = decayAtMillis() ?: return false
    return expireAt <= nowMillis
}

fun Drop.remainingDecayMillis(nowMillis: Long = System.currentTimeMillis()): Long? {
    val expireAt = decayAtMillis() ?: return null
    val remaining = expireAt - nowMillis
    return if (remaining > 0) remaining else 0L
}

enum class DropVoteType(val value: Int) {
    NONE(0),
    UPVOTE(1),
    DOWNVOTE(-1);

    companion object {
        fun fromRaw(raw: Long?): DropVoteType = when (raw?.toInt()) {
            1 -> UPVOTE
            -1 -> DOWNVOTE
            else -> NONE
        }
    }
}

enum class DropContentType {
    TEXT,
    PHOTO,
    AUDIO,
    VIDEO;

    companion object {
        fun fromRaw(value: String?): DropContentType {
            if (value.isNullOrBlank()) return TEXT
            return values().firstOrNull { it.name.equals(value, ignoreCase = true) } ?: TEXT
        }
    }
}

fun Drop.displayTitle(): String {
    val descriptionText = description.orEmpty()
    val baseTitle = when (dropType) {
        DropType.RESTAURANT_COUPON -> text.ifBlank { descriptionText.ifBlank { "Special offer" } }
        DropType.TOUR_STOP -> text.ifBlank { descriptionText.ifBlank { "Tour stop" } }
        DropType.COMMUNITY -> when (contentType) {
            DropContentType.TEXT -> text.ifBlank { descriptionText.ifBlank { "(No message)" } }
            DropContentType.PHOTO -> text.ifBlank { descriptionText }.ifBlank { "Photo drop" }
            DropContentType.AUDIO -> text.ifBlank { descriptionText }.ifBlank { "Audio drop" }
            DropContentType.VIDEO -> text.ifBlank { descriptionText }.ifBlank { "Video drop" }
        }
    }

    val username = dropperUsername?.trim()?.takeIf { it.isNotEmpty() }
    return if (!isAnonymous && !username.isNullOrEmpty()) {
        val handle = if (username.startsWith("@")) username else "@$username"
        "$handle dropped $baseTitle"
    } else {
        baseTitle
    }
}

fun Drop.mediaLabel(): String? = mediaUrl?.takeIf { it.isNotBlank() }

fun Drop.discoveryTitle(): String = when (dropType) {
    DropType.RESTAURANT_COUPON -> "Local business offer"
    DropType.TOUR_STOP -> "Guided tour stop"
    DropType.COMMUNITY -> when (contentType) {
        DropContentType.TEXT -> "Hidden note"
        DropContentType.PHOTO -> "Hidden photo drop"
        DropContentType.AUDIO -> "Hidden audio drop"
        DropContentType.VIDEO -> "Hidden video drop"
    }
}

fun Drop.discoveryDescription(): String {
    val descriptionText = description.orEmpty()
    return when (dropType) {
        DropType.RESTAURANT_COUPON -> descriptionText.ifBlank {
            "Unlock the details to redeem this business offer nearby."
        }
        DropType.TOUR_STOP -> descriptionText.ifBlank {
            "Pick up this stop to access the guided story or directions."
        }
        DropType.COMMUNITY -> when (contentType) {
            DropContentType.TEXT -> descriptionText.ifBlank { "Collect this drop to read the message inside." }
            DropContentType.PHOTO -> descriptionText.ifBlank { "Pick up this drop to reveal the photo." }
            DropContentType.AUDIO -> descriptionText.ifBlank { "Collect this drop to listen to the recording." }
            DropContentType.VIDEO -> descriptionText.ifBlank { "Collect this drop to watch the clip." }
        }
    }
}

fun Drop.voteScore(): Long = upvoteCount - downvoteCount

fun Drop.userVote(userId: String?): DropVoteType {
    if (userId.isNullOrBlank()) return DropVoteType.NONE
    return DropVoteType.fromRaw(voteMap[userId])
}

fun Drop.applyUserVote(userId: String, vote: DropVoteType): Drop {
    val previousVote = voteMap[userId]?.toInt() ?: 0
    val targetVote = vote.value
    if (previousVote == targetVote) return this

    var updatedUpvotes = upvoteCount
    var updatedDownvotes = downvoteCount
    val updatedMap = voteMap.toMutableMap()

    when (previousVote) {
        1 -> updatedUpvotes = (updatedUpvotes - 1).coerceAtLeast(0)
        -1 -> updatedDownvotes = (updatedDownvotes - 1).coerceAtLeast(0)
    }

    when (vote) {
        DropVoteType.UPVOTE -> {
            updatedUpvotes += 1
            updatedMap[userId] = 1L
        }
        DropVoteType.DOWNVOTE -> {
            updatedDownvotes += 1
            updatedMap[userId] = -1L
        }
        DropVoteType.NONE -> updatedMap.remove(userId)
    }

    return copy(
        upvoteCount = updatedUpvotes,
        downvoteCount = updatedDownvotes,
        voteMap = updatedMap
    )
}
