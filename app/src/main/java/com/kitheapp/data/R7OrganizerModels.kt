package com.kitheapp.data

import java.time.ZoneId

enum class R7OrganizerAccessStatus {
    NOT_APPLIED,
    PENDING,
    APPROVED,
    DENIED;

    companion object {
        fun fromRaw(raw: Any?): R7OrganizerAccessStatus = when (raw?.toString()) {
            "PENDING" -> PENDING
            "APPROVED" -> APPROVED
            "DENIED" -> DENIED
            else -> NOT_APPLIED
        }
    }
}

data class R7OrganizerAccessState(
    val status: R7OrganizerAccessStatus = R7OrganizerAccessStatus.NOT_APPLIED,
    val submittedAtMillis: Long? = null,
    val reviewedAtMillis: Long? = null
)

data class R7OrganizerApplicationLink(
    val url: String,
    val expiresAtMillis: Long
)

enum class R7ExperienceState { PUBLISHED, CANCELLED }

data class R7Experience(
    val code: String,
    val name: String,
    val description: String? = null,
    val startsAtMillis: Long,
    val endsAtMillis: Long,
    val timeZone: String,
    val state: R7ExperienceState = R7ExperienceState.PUBLISHED,
    val defaultRadiusM: Int = 25,
    val dropCount: Int = 0
)

data class R7ExperienceDraft(
    val name: String,
    val description: String?,
    val startsAtMillis: Long,
    val endsAtMillis: Long,
    val timeZone: String = ZoneId.systemDefault().id,
    val defaultRadiusM: Int = 25,
    val state: R7ExperienceState = R7ExperienceState.PUBLISHED
)

enum class R7DropContentKind { TEXT, PHOTO }
enum class R7ExpiryMode { NONE, EXPERIENCE_END, CUSTOM }

data class R7OrganizerDropSummary(
    val id: String,
    val experienceCode: String,
    val title: String,
    val contentKind: R7DropContentKind,
    val dropKind: R6DropKind = R6DropKind.STANDARD,
    val moderationState: String,
    val lat: Double,
    val lng: Double,
    val radiusM: Int,
    val expiryMode: R7ExpiryMode,
    val expiresAtMillis: Long?,
    val publishedAtMillis: Long,
    val editedAtMillis: Long?
)

data class R7OrganizerDrop(
    val summary: R7OrganizerDropSummary,
    val body: String?,
    val mediaAltText: String?,
    val rewardPresentation: Map<String, String> = emptyMap(),
    val inventoryLimit: Int? = null
)

data class R7DropDraft(
    val experienceCode: String,
    val dropId: String? = null,
    val lat: Double,
    val lng: Double,
    val radiusM: Int = 25,
    val expiryMode: R7ExpiryMode = R7ExpiryMode.NONE,
    val expiresAtMillis: Long? = null,
    val contentKind: R7DropContentKind = R7DropContentKind.TEXT,
    val dropKind: R6DropKind = R6DropKind.STANDARD,
    val title: String,
    val body: String?,
    val mediaAltText: String?,
    val rewardLabel: String? = null,
    val businessLabel: String? = null,
    val rewardInstructions: String? = null,
    val rewardTerms: String? = null,
    val inventoryLimit: Int? = null,
    val photoBytes: ByteArray? = null,
    val photoMimeType: String? = null
)

data class R7SaveDropResult(val dropId: String, val payloadVersion: Int)

object R7OrganizerPolicy {
    const val MIN_RADIUS_M = 15
    const val MAX_RADIUS_M = 100
    const val DEFAULT_RADIUS_M = 25

    fun validateExperience(draft: R7ExperienceDraft): String? = when {
        draft.name.trim().isEmpty() -> "Enter an Experience name."
        draft.name.trim().length > 100 -> "Keep the Experience name under 100 characters."
        (draft.description?.trim()?.length ?: 0) > 240 ->
            "Keep the description under 240 characters."
        draft.endsAtMillis <= draft.startsAtMillis ->
            "This needs to end after it starts — check the dates."
        runCatching { ZoneId.of(draft.timeZone.trim()) }.isFailure ->
            "Enter a valid time zone."
        draft.defaultRadiusM !in MIN_RADIUS_M..MAX_RADIUS_M ->
            "Choose a default distance from 15 to 100 meters."
        else -> null
    }

    fun validateDrop(draft: R7DropDraft): String? = when {
        draft.experienceCode.isBlank() -> "Choose an Experience."
        !draft.lat.isFinite() || draft.lat !in -90.0..90.0 ||
            !draft.lng.isFinite() || draft.lng !in -180.0..180.0 ->
            "Choose a valid drop location."
        draft.radiusM !in MIN_RADIUS_M..MAX_RADIUS_M ->
            "Choose how close guests need to be, from 15 to 100 meters."
        draft.title.trim().isEmpty() -> "Add a title before publishing."
        draft.title.trim().length > 80 -> "Keep the title under 80 characters."
        draft.contentKind == R7DropContentKind.TEXT && draft.body.isNullOrBlank() ->
            "Add a message for this text drop."
        (draft.body?.trim()?.length ?: 0) > 2_000 -> "Keep the message under 2,000 characters."
        draft.contentKind == R7DropContentKind.PHOTO && draft.photoBytes == null ->
            if (draft.dropId == null) "Choose or take a photo." else "Choose a replacement photo to save this edit."
        draft.contentKind == R7DropContentKind.PHOTO && draft.mediaAltText.isNullOrBlank() ->
            "Describe the photo for guests using screen readers."
        (draft.mediaAltText?.trim()?.length ?: 0) > 240 ->
            "Keep the photo description under 240 characters."
        draft.dropKind == R6DropKind.REWARD && draft.rewardLabel.isNullOrBlank() ->
            "Describe the reward guests will receive."
        (draft.rewardLabel?.trim()?.length ?: 0) > 240 ->
            "Keep the reward name under 240 characters."
        draft.dropKind == R6DropKind.REWARD && draft.businessLabel.isNullOrBlank() ->
            "Add the business or redemption location name."
        (draft.businessLabel?.trim()?.length ?: 0) > 240 ->
            "Keep the business name under 240 characters."
        draft.dropKind == R6DropKind.REWARD && draft.rewardInstructions.isNullOrBlank() ->
            "Explain how the guest should use the reward."
        (draft.rewardInstructions?.trim()?.length ?: 0) > 240 ->
            "Keep reward instructions under 240 characters."
        (draft.rewardTerms?.trim()?.length ?: 0) > 500 ->
            "Keep reward terms under 500 characters."
        draft.dropKind == R6DropKind.REWARD &&
            (draft.inventoryLimit == null || draft.inventoryLimit !in 1..10_000) ->
            "Choose a reward inventory from 1 to 10,000 codes."
        draft.dropKind == R6DropKind.STANDARD && draft.inventoryLimit != null ->
            "Standard drops do not use reward inventory."
        draft.expiryMode == R7ExpiryMode.CUSTOM && draft.expiresAtMillis == null ->
            "Choose when this drop expires."
        else -> null
    }
}
