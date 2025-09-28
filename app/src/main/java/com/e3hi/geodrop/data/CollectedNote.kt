package com.e3hi.geodrop.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Represents a note the user has collected while near a drop.
 */
data class CollectedNote(
    val id: String,
    val text: String,
    val contentType: DropContentType,
    val mediaUrl: String?,
    val mediaMimeType: String?,
    val mediaData: String?,
    val lat: Double?,
    val lng: Double?,
    val groupCode: String?,
    val dropCreatedAt: Long?,
    val dropType: DropType = DropType.COMMUNITY,
    val businessId: String? = null,
    val businessName: String? = null,
    val redemptionLimit: Int? = null,
    val redemptionCount: Int = 0,
    val isRedeemed: Boolean = false,
    val redeemedAt: Long? = null,
    val collectedAt: Long,
    val isNsfw: Boolean = false,
    val nsfwLabels: List<String> = emptyList(),
    val nsfwConfidence: Double? = null
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put(KEY_ID, id)
            put(KEY_TEXT, text)
            put(KEY_CONTENT_TYPE, contentType.name)
            putOpt(KEY_MEDIA_URL, mediaUrl)
            putOpt(KEY_MEDIA_MIME, mediaMimeType)
            putOpt(KEY_MEDIA_DATA, mediaData)
            putOpt(KEY_LAT, lat)
            putOpt(KEY_LNG, lng)
            putOpt(KEY_GROUP_CODE, groupCode)
            putOpt(KEY_DROP_CREATED_AT, dropCreatedAt)
            put(KEY_DROP_TYPE, dropType.name)
            putOpt(KEY_BUSINESS_ID, businessId)
            putOpt(KEY_BUSINESS_NAME, businessName)
            putOpt(KEY_REDEMPTION_LIMIT, redemptionLimit)
            put(KEY_REDEMPTION_COUNT, redemptionCount)
            put(KEY_IS_REDEEMED, isRedeemed)
            putOpt(KEY_REDEEMED_AT, redeemedAt)
            put(KEY_COLLECTED_AT, collectedAt)
            put(KEY_IS_NSFW, isNsfw)
            putOpt(KEY_NSFW_CONFIDENCE, nsfwConfidence)
            put(KEY_NSFW_LABELS, JSONArray().apply { nsfwLabels.forEach { put(it) } })
        }
    }

    companion object {
        private const val KEY_ID = "id"
        private const val KEY_TEXT = "text"
        private const val KEY_CONTENT_TYPE = "contentType"
        private const val KEY_MEDIA_URL = "mediaUrl"
        private const val KEY_MEDIA_MIME = "mediaMimeType"
        private const val KEY_MEDIA_DATA = "mediaData"
        private const val KEY_LAT = "lat"
        private const val KEY_LNG = "lng"
        private const val KEY_GROUP_CODE = "groupCode"
        private const val KEY_DROP_CREATED_AT = "dropCreatedAt"
        private const val KEY_DROP_TYPE = "dropType"
        private const val KEY_BUSINESS_ID = "businessId"
        private const val KEY_BUSINESS_NAME = "businessName"
        private const val KEY_REDEMPTION_LIMIT = "redemptionLimit"
        private const val KEY_REDEMPTION_COUNT = "redemptionCount"
        private const val KEY_IS_REDEEMED = "isRedeemed"
        private const val KEY_REDEEMED_AT = "redeemedAt"
        private const val KEY_COLLECTED_AT = "collectedAt"
        private const val KEY_IS_NSFW = "isNsfw"
        private const val KEY_NSFW_LABELS = "nsfwLabels"
        private const val KEY_NSFW_CONFIDENCE = "nsfwConfidence"

        fun fromJson(json: JSONObject): CollectedNote {
            val id = json.optString(KEY_ID)
            val text = json.optString(KEY_TEXT)
            val contentType = DropContentType.fromRaw(json.optString(KEY_CONTENT_TYPE))
            val mediaUrl = json.optString(KEY_MEDIA_URL).takeIf { it.isNotBlank() }
            val mediaMimeType = json.optString(KEY_MEDIA_MIME).takeIf { it.isNotBlank() }
            val mediaData = json.optString(KEY_MEDIA_DATA).takeIf { it.isNotBlank() }
            val lat = json.optDouble(KEY_LAT).takeIf { json.has(KEY_LAT) }
            val lng = json.optDouble(KEY_LNG).takeIf { json.has(KEY_LNG) }
            val groupCode = json.optString(KEY_GROUP_CODE).takeIf { it.isNotBlank() }
            val dropCreatedAt = json.optLong(KEY_DROP_CREATED_AT).takeIf { json.has(KEY_DROP_CREATED_AT) }
            val dropType = DropType.fromRaw(json.optString(KEY_DROP_TYPE))
            val businessId = json.optString(KEY_BUSINESS_ID).takeIf { it.isNotBlank() }
            val businessName = json.optString(KEY_BUSINESS_NAME).takeIf { it.isNotBlank() }
            val redemptionLimit = json.optInt(KEY_REDEMPTION_LIMIT).takeIf { json.has(KEY_REDEMPTION_LIMIT) }
            val redemptionCount = json.optInt(KEY_REDEMPTION_COUNT)
            val isRedeemed = json.optBoolean(KEY_IS_REDEEMED)
            val redeemedAt = json.optLong(KEY_REDEEMED_AT).takeIf { json.has(KEY_REDEEMED_AT) }
            val collectedAt = json.optLong(KEY_COLLECTED_AT)
            val isNsfw = json.optBoolean(KEY_IS_NSFW)
            val nsfwConfidence = json.optDouble(KEY_NSFW_CONFIDENCE).takeIf { json.has(KEY_NSFW_CONFIDENCE) }
            val nsfwLabels = json.optJSONArray(KEY_NSFW_LABELS)
                ?.let { array ->
                    buildList {
                        for (i in 0 until array.length()) {
                            val value = array.optString(i)
                            if (value.isNotBlank()) add(value)
                        }
                    }
                }
                ?: emptyList()

            return CollectedNote(
                id = id,
                text = text,
                contentType = contentType,
                mediaUrl = mediaUrl,
                mediaMimeType = mediaMimeType,
                mediaData = mediaData,
                lat = lat,
                lng = lng,
                groupCode = groupCode,
                dropCreatedAt = dropCreatedAt,
                dropType = dropType,
                businessId = businessId,
                businessName = businessName,
                redemptionLimit = redemptionLimit,
                redemptionCount = redemptionCount,
                isRedeemed = isRedeemed,
                redeemedAt = redeemedAt,
                collectedAt = collectedAt,
                isNsfw = isNsfw,
                nsfwLabels = nsfwLabels,
                nsfwConfidence = nsfwConfidence
            )
        }
    }
}