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

    val decayDays = when {
        contains("decayDays") -> {
            when (val raw = get("decayDays")) {
                is Number -> raw.toInt().takeIf { it > 0 }
                is String -> raw.toIntOrNull()?.takeIf { it > 0 }
                else -> null
            }
        }
        else -> null
    } ?: base.decayDays

    return base.copy(
        isNsfw = nsfwFlag,
        nsfwLabels = nsfwLabels,
        decayDays = decayDays
    )
}