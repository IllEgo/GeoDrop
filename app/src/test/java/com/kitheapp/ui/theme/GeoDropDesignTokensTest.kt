package com.kitheapp.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class GeoDropDesignTokensTest {

    @Test
    fun approvedPaletteMatchesR3Specification() {
        assertEquals(Color(0xFF0B5D5D), BrandPrimaryLight)
        assertEquals(Color(0xFF4FD1C5), BrandPrimaryDark)
        assertEquals(Color(0xFF5B6470), StateLocked)
        assertEquals(Color(0xFFE07B24), StateNear)
        assertEquals(Color(0xFFB3261E), FeedbackErrorLight)
        assertEquals(Color(0xFFFDFCFA), SurfaceLight)
        assertEquals(Color(0xFF14171A), SurfaceDark)
    }

    @Test
    fun typographyNeverDropsBelowThirteenSp() {
        val styles = listOf(
            GeoDropTypography.displayLarge,
            GeoDropTypography.headlineLarge,
            GeoDropTypography.titleLarge,
            GeoDropTypography.bodyLarge,
            GeoDropTypography.bodyMedium,
            GeoDropTypography.bodySmall,
            GeoDropTypography.labelLarge,
            GeoDropTypography.labelMedium,
            GeoDropTypography.labelSmall,
            RewardCodeTextStyle
        )

        assertTrue(styles.all { it.fontSize >= 13.sp })
        assertEquals(34.sp, GeoDropTypography.displayLarge.fontSize)
        assertEquals(26.sp, GeoDropTypography.headlineLarge.fontSize)
        assertEquals(20.sp, GeoDropTypography.titleLarge.fontSize)
        assertEquals(17.sp, GeoDropTypography.bodyLarge.fontSize)
        assertEquals(15.sp, GeoDropTypography.bodyMedium.fontSize)
        assertEquals(13.sp, GeoDropTypography.labelLarge.fontSize)
        assertEquals(28.sp, RewardCodeTextStyle.fontSize)
    }

    @Test
    fun spacingTouchTargetAndReducedMotionMatchR3Specification() {
        assertEquals(4.dp, GeoDropSpacing.xxs)
        assertEquals(8.dp, GeoDropSpacing.xs)
        assertEquals(12.dp, GeoDropSpacing.sm)
        assertEquals(16.dp, GeoDropSpacing.md)
        assertEquals(20.dp, GeoDropSpacing.screenGutter)
        assertEquals(24.dp, GeoDropSpacing.lg)
        assertEquals(32.dp, GeoDropSpacing.xl)
        assertEquals(48.dp, GeoDropSpacing.xxl)
        assertEquals(48.dp, GeoDropSize.minimumTouchTarget)
        assertEquals(8.dp, GeoDropSize.adjacentTargetSpacing)

        val regular = motionTokens(reducedMotion = false)
        val reduced = motionTokens(reducedMotion = true)
        assertEquals(250, regular.unlockRevealMillis)
        assertEquals(200, regular.pinStateChangeMillis)
        assertEquals(0, reduced.unlockRevealMillis)
        assertEquals(0, reduced.pinStateChangeMillis)
        assertTrue(reduced.reducedMotion)
    }

    @Test
    fun specifiedTextPairsMeetWcagAaAndUnlockPairMeetsSevenToOne() {
        assertContrastAtLeast(InkPrimaryLight, SurfaceLight, 4.5)
        assertContrastAtLeast(InkSecondaryLight, SurfaceElevatedLight, 4.5)
        assertContrastAtLeast(InkPrimaryDark, SurfaceDark, 4.5)
        assertContrastAtLeast(InkSecondaryDark, SurfaceElevatedDark, 4.5)
        assertContrastAtLeast(Color.White, BrandPrimaryLight, 7.0)
        assertContrastAtLeast(Color(0xFF003735), BrandPrimaryDark, 7.0)
    }

    private fun assertContrastAtLeast(foreground: Color, background: Color, expected: Double) {
        val ratio = contrastRatio(foreground, background)
        assertTrue("Expected contrast >= $expected, was $ratio", ratio >= expected)
    }

    private fun contrastRatio(first: Color, second: Color): Double {
        val firstLuminance = luminance(first)
        val secondLuminance = luminance(second)
        return (max(firstLuminance, secondLuminance) + 0.05) /
            (min(firstLuminance, secondLuminance) + 0.05)
    }

    private fun luminance(color: Color): Double {
        fun channel(value: Float): Double {
            val normalized = value.toDouble()
            return if (normalized <= 0.04045) {
                normalized / 12.92
            } else {
                ((normalized + 0.055) / 1.055).pow(2.4)
            }
        }
        return 0.2126 * channel(color.red) +
            0.7152 * channel(color.green) +
            0.0722 * channel(color.blue)
    }
}
