package com.e3hi.geodrop.util

import java.text.DateFormat
import java.util.Date

/**
 * Formats an epoch timestamp (milliseconds) into a localized date & time string.
 *
 * Returns null when the timestamp is not set (<= 0).
 */
fun formatTimestamp(epochMillis: Long?): String? {
    val value = epochMillis ?: return null
    if (value <= 0L) return null

    val formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
    return formatter.format(Date(value))
}