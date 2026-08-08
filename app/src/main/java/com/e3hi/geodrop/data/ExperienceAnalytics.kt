package com.e3hi.geodrop.data

/**
 * Owner-only aggregate for one invite-only experience.
 *
 * The backend writes this to `groups/{groupCode}/analytics/summary`. Counts are
 * deliberately aggregate-only: no attendee identity is copied into the rollup.
 */
data class ExperienceAnalytics(
    val groupCode: String,
    val drops: Long = 0,
    val collects: Long = 0,
    val redemptions: Long = 0,
    val updatedAtMillis: Long? = null,
    val reconciledAtMillis: Long? = null
) {
    companion object {
        fun fromMap(groupCode: String, data: Map<String, Any?>?): ExperienceAnalytics {
            fun count(field: String): Long =
                (data?.get(field) as? Number)?.toLong()?.coerceAtLeast(0) ?: 0

            fun timestamp(field: String): Long? =
                (data?.get(field) as? Number)?.toLong()?.takeIf { it > 0 }

            return ExperienceAnalytics(
                groupCode = groupCode,
                drops = count("drops"),
                collects = count("collects"),
                redemptions = count("redemptions"),
                updatedAtMillis = timestamp("updatedAt"),
                reconciledAtMillis = timestamp("reconciledAt")
            )
        }
    }
}
