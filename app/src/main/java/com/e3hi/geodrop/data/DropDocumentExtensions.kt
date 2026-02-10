package com.e3hi.geodrop.data

import com.e3hi.geodrop.util.GroupPreferences
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

internal fun DocumentSnapshot.toDrop(): Drop {
    val base = runCatching {
        toObject(Drop::class.java)
    }.getOrNull()

    val baseDrop = base?.copy(id = id) ?: Drop(id = id)

    val createdAt = getLongCompat("createdAt") ?: baseDrop.createdAt
    val deletedAt = getLongCompat("deletedAt") ?: baseDrop.deletedAt
    val likeCount = getLongCompat("likeCount") ?: baseDrop.likeCount
    val dislikeCount = getLongCompat("dislikeCount") ?: baseDrop.dislikeCount
    val reportCount = getLongCompat("reportCount") ?: baseDrop.reportCount
    val redemptionLimit = getIntCompat("redemptionLimit") ?: baseDrop.redemptionLimit
    val redemptionCount = getIntCompat("redemptionCount") ?: baseDrop.redemptionCount

    val text = getString("text") ?: baseDrop.text
    val descriptionFromSnapshot = getString("description")
    val lat = getDouble("lat") ?: baseDrop.lat
    val lng = getDouble("lng") ?: baseDrop.lng
    val createdBy = getString("createdBy") ?: baseDrop.createdBy
    val isAnonymous = getBooleanCompat("isAnonymous") ?: baseDrop.isAnonymous
    val isDeleted = getBooleanCompat("isDeleted") ?: baseDrop.isDeleted
    val decayDays = getIntCompat("decayDays") ?: baseDrop.decayDays
    val groupCode = GroupPreferences.normalizeGroupCode(getString("groupCode")) ?: baseDrop.groupCode
    val dropType = DropType.fromRaw(getString("dropType"))
    val experienceType = DropExperienceType.fromRaw(getString("experienceType"))
    val businessId = getString("businessId")?.takeIf { it.isNotBlank() } ?: baseDrop.businessId
    val businessName = getString("businessName")?.takeIf { it.isNotBlank() } ?: baseDrop.businessName
    val contentType = DropContentType.fromRaw(getString("contentType"))
    val mediaUrl = getString("mediaUrl")?.takeIf { it.isNotBlank() } ?: baseDrop.mediaUrl
    val mediaMimeType = getString("mediaMimeType")?.takeIf { it.isNotBlank() } ?: baseDrop.mediaMimeType
    val mediaData = getString("mediaData")?.takeIf { it.isNotBlank() } ?: baseDrop.mediaData
    val mediaStoragePath = getString("mediaStoragePath")?.takeIf { it.isNotBlank() } ?: baseDrop.mediaStoragePath
    val redemptionCode = getString("redemptionCode")?.takeIf { it.isNotBlank() } ?: baseDrop.redemptionCode

    val nsfwFlag = when {
        contains("isNsfw") -> getBooleanCompat("isNsfw") ?: baseDrop.isNsfw
        contains("nsfw") -> getBooleanCompat("nsfw") ?: baseDrop.isNsfw
        else -> baseDrop.isNsfw
    }

    val nsfwLabels = when {
        contains("nsfwLabels") -> {
            val raw = get("nsfwLabels")
            if (raw is List<*>) {
                raw.mapNotNull { value ->
                    value?.toString()?.takeIf { it.isNotBlank() }
                }.takeIf { it.isNotEmpty() }
            } else {
                null
            }
        }
        else -> null
    } ?: baseDrop.nsfwLabels

    val likedBy = getBooleanMap("likedBy").takeIf { it.isNotEmpty() } ?: baseDrop.likedBy
    val dislikedBy = getBooleanMap("dislikedBy").takeIf { it.isNotEmpty() } ?: baseDrop.dislikedBy
    val reportedBy = getLongMap("reportedBy").takeIf { it.isNotEmpty() } ?: baseDrop.reportedBy
    val redeemedBy = getLongMap("redeemedBy").takeIf { it.isNotEmpty() } ?: baseDrop.redeemedBy

    val dropperUsername = when {
        contains("dropperUsername") -> getString("dropperUsername")
        contains("createdByUsername") -> getString("createdByUsername")
        else -> baseDrop.dropperUsername
    }?.trim()?.takeIf { it.isNotEmpty() }

    val description = (descriptionFromSnapshot ?: baseDrop.description)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

    return baseDrop.copy(
        text = text,
        description = description,
        lat = lat,
        lng = lng,
        createdBy = createdBy,
        createdAt = createdAt,
        dropperUsername = dropperUsername,
        isAnonymous = isAnonymous,
        isDeleted = isDeleted,
        deletedAt = deletedAt,
        isNsfw = nsfwFlag,
        nsfwLabels = nsfwLabels,
        decayDays = decayDays,
        groupCode = groupCode,
        dropType = dropType,
        experienceType = experienceType,
        businessId = businessId,
        businessName = businessName,
        contentType = contentType,
        mediaUrl = mediaUrl,
        mediaMimeType = mediaMimeType,
        mediaData = mediaData,
        mediaStoragePath = mediaStoragePath,
        likeCount = likeCount,
        likedBy = likedBy,
        dislikeCount = dislikeCount,
        dislikedBy = dislikedBy,
        reportCount = reportCount,
        reportedBy = reportedBy,
        redemptionCode = redemptionCode,
        redemptionLimit = redemptionLimit,
        redemptionCount = redemptionCount,
        redeemedBy = redeemedBy
    )
}

private fun DocumentSnapshot.getLongCompat(field: String): Long? {
    return when (val raw = get(field)) {
        is Number -> raw.toLong()
        is Timestamp -> raw.toDate().time
        is java.util.Date -> raw.time
        is String -> raw.toLongOrNull()
        else -> null
    }
}

private fun DocumentSnapshot.getIntCompat(field: String): Int? {
    return when (val raw = get(field)) {
        is Number -> raw.toInt()
        is String -> raw.toIntOrNull()
        else -> null
    }
}

private fun DocumentSnapshot.getBooleanCompat(field: String): Boolean? {
    return when (val raw = get(field)) {
        is Boolean -> raw
        is Number -> raw.toInt() != 0
        is String -> raw.equals("true", ignoreCase = true) || raw == "1"
        else -> null
    }
}

private fun DocumentSnapshot.getBooleanMap(field: String): Map<String, Boolean> {
    val raw = get(field) as? Map<*, *> ?: return emptyMap()
    return raw.mapNotNull { (key, value) ->
        val keyString = key as? String ?: return@mapNotNull null
        val booleanValue = when (value) {
            is Boolean -> value
            is Number -> value.toInt() != 0
            is String -> value.equals("true", ignoreCase = true) || value == "1"
            else -> null
        } ?: return@mapNotNull null
        keyString to booleanValue
    }.toMap()
}

private fun DocumentSnapshot.getLongMap(field: String): Map<String, Long> {
    val raw = get(field) as? Map<*, *> ?: return emptyMap()
    return raw.mapNotNull { (key, value) ->
        val keyString = key as? String ?: return@mapNotNull null
        val longValue = when (value) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull()
            is Timestamp -> value.toDate().time
            is java.util.Date -> value.time
            else -> null
        } ?: return@mapNotNull null
        keyString to longValue
    }.toMap()
}