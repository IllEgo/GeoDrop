package com.e3hi.geodrop.ui.organizer

import android.graphics.Bitmap
import android.media.ExifInterface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class R7PhotoOrientationTest {
    @Test
    fun portraitExifRotationsSwapBitmapDimensions() {
        val clockwise = R7PhotoOrientation.apply(
            Bitmap.createBitmap(4, 7, Bitmap.Config.ARGB_8888),
            ExifInterface.ORIENTATION_ROTATE_90
        )
        val counterClockwise = R7PhotoOrientation.apply(
            Bitmap.createBitmap(4, 7, Bitmap.Config.ARGB_8888),
            ExifInterface.ORIENTATION_ROTATE_270
        )

        assertEquals(7, clockwise.width)
        assertEquals(4, clockwise.height)
        assertEquals(7, counterClockwise.width)
        assertEquals(4, counterClockwise.height)
    }

    @Test
    fun mirroredExifVariantsKeepTheirRequiredFlip() {
        val horizontal = R7PhotoOrientation.transformFor(
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL
        )
        val transpose = R7PhotoOrientation.transformFor(ExifInterface.ORIENTATION_TRANSPOSE)
        val normal = R7PhotoOrientation.transformFor(ExifInterface.ORIENTATION_NORMAL)

        assertTrue(horizontal.flipHorizontal)
        assertEquals(90f, transpose.rotationDegrees)
        assertTrue(transpose.flipHorizontal)
        assertEquals(0f, normal.rotationDegrees)
        assertFalse(normal.flipHorizontal)
    }
}
