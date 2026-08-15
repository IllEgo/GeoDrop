package com.kitheapp.ui

/**
 * Keeps transient drag-state values out of Compose transforms.
 *
 * [androidx.compose.foundation.gestures.AnchoredDraggableState.offset] is NaN until its
 * anchors are installed. Passing that value into a graphics layer creates a non-invertible
 * transform; a later pointer hit test can then surface as `Offset is unspecified`.
 */
internal fun finitePanelTranslation(offset: Float, collapsedOffset: Float): Float = when {
    offset.isFinite() -> offset
    collapsedOffset.isFinite() -> collapsedOffset
    else -> 0f
}
