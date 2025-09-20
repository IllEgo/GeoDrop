package com.e3hi.geodrop.data

data class Drop(
    val id: String = "",
    val text: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val createdBy: String = "",
    val createdAt: Long = 0L,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val groupCode: String? = null,
    val contentType: DropContentType = DropContentType.TEXT,
    val mediaUrl: String? = null
)

enum class DropContentType {
    TEXT,
    PHOTO,
    AUDIO;

    companion object {
        fun fromRaw(value: String?): DropContentType {
            if (value.isNullOrBlank()) return TEXT
            return values().firstOrNull { it.name.equals(value, ignoreCase = true) } ?: TEXT
        }
    }
}

fun Drop.displayTitle(): String = when (contentType) {
    DropContentType.TEXT -> text.ifBlank { "(No message)" }
    DropContentType.PHOTO -> text.ifBlank { "Photo drop" }
    DropContentType.AUDIO -> text.ifBlank { "Audio drop" }
}

fun Drop.mediaLabel(): String? = mediaUrl?.takeIf { it.isNotBlank() }
