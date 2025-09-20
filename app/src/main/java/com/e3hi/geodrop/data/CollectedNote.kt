package com.e3hi.geodrop.data

import com.e3hi.geodrop.data.DropContentType
import org.json.JSONObject

/**
 * Represents a note the user has collected while near a drop.
 */
data class CollectedNote(
    val id: String,
    val text: String,
    val contentType: DropContentType,
    val mediaUrl: String?,
    val lat: Double?,
    val lng: Double?,
    val groupCode: String?,
    val dropCreatedAt: Long?,
    val collectedAt: Long
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put(KEY_ID, id)
            put(KEY_TEXT, text)
            put(KEY_CONTENT_TYPE, contentType.name)
            putOpt(KEY_MEDIA_URL, mediaUrl)
            putOpt(KEY_LAT, lat)
            putOpt(KEY_LNG, lng)
            putOpt(KEY_GROUP_CODE, groupCode)
            putOpt(KEY_DROP_CREATED_AT, dropCreatedAt)
            put(KEY_COLLECTED_AT, collectedAt)
        }
    }

    companion object {
        private const val KEY_ID = "id"
        private const val KEY_TEXT = "text"
        private const val KEY_CONTENT_TYPE = "contentType"
        private const val KEY_MEDIA_URL = "mediaUrl"
        private const val KEY_LAT = "lat"
        private const val KEY_LNG = "lng"
        private const val KEY_GROUP_CODE = "groupCode"
        private const val KEY_DROP_CREATED_AT = "dropCreatedAt"
        private const val KEY_COLLECTED_AT = "collectedAt"

        fun fromJson(json: JSONObject): CollectedNote {
            val id = json.optString(KEY_ID)
            val text = json.optString(KEY_TEXT)
            val contentType = DropContentType.fromRaw(json.optString(KEY_CONTENT_TYPE))
            val mediaUrl = json.optString(KEY_MEDIA_URL).takeIf { it.isNotBlank() }
            val lat = json.optDouble(KEY_LAT).takeIf { json.has(KEY_LAT) }
            val lng = json.optDouble(KEY_LNG).takeIf { json.has(KEY_LNG) }
            val groupCode = json.optString(KEY_GROUP_CODE).takeIf { it.isNotBlank() }
            val dropCreatedAt = json.optLong(KEY_DROP_CREATED_AT).takeIf { json.has(KEY_DROP_CREATED_AT) }
            val collectedAt = json.optLong(KEY_COLLECTED_AT)

            return CollectedNote(
                id = id,
                text = text,
                contentType = contentType,
                mediaUrl = mediaUrl,
                lat = lat,
                lng = lng,
                groupCode = groupCode,
                dropCreatedAt = dropCreatedAt,
                collectedAt = collectedAt
            )
        }
    }
}