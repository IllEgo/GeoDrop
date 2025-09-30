package com.e3hi.geodrop.data

/**
 * High-level status reported by the Google Vision SafeSearch integration
 * whenever a drop is evaluated for adult content.
 */
enum class VisionApiStatus {
    /** The Vision API could not run because an API key was missing or disabled. */
    NOT_CONFIGURED,

    /** The drop was not eligible for Vision (for example, it wasn't a photo). */
    NOT_ELIGIBLE,

    /** A network or API error prevented Vision from completing the request. */
    ERROR,

    /** Vision executed and cleared the drop without any adult-content warnings. */
    CLEARED,

    /** Vision executed and flagged the drop for adult content. */
    FLAGGED
}