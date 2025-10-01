package com.e3hi.geodrop.data

import com.google.firebase.firestore.DocumentSnapshot

internal fun DocumentSnapshot.toDrop(): Drop {
    val base = toObject(Drop::class.java)?.copy(id = id) ?: Drop(id = id)

    val nsfwFlag = when {
        contains("isNsfw") -> getBoolean("isNsfw") == true
        contains("nsfw") -> getBoolean("nsfw") == true
        else -> base.isNsfw
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
    } ?: base.nsfwLabels

    return base.copy(
        isNsfw = nsfwFlag,
        nsfwLabels = nsfwLabels
    )
}