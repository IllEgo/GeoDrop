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

fun Drop.discoveryTitle(): String = when (contentType) {
    DropContentType.TEXT -> "Hidden note"
    DropContentType.PHOTO -> "Hidden photo drop"
    DropContentType.AUDIO -> "Hidden audio drop"
}

fun Drop.discoveryDescription(): String = when (contentType) {
    DropContentType.TEXT -> "Collect this drop to read the message inside."
    DropContentType.PHOTO -> "Pick up this drop to reveal the photo."
    DropContentType.AUDIO -> "Collect this drop to listen to the recording."
}
