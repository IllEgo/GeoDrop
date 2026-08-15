package com.kitheapp.ui.components

import com.kitheapp.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [34],
    application = R3TestApplication::class,
    shadows = [NoOpFirebaseInitProviderShadow::class]
)
class R3StringResourcesTest {

    @Test
    fun appNameUsesKitheBrand() {
        val resources = RuntimeEnvironment.getApplication().resources

        assertEquals("Kithe", resources.getString(R.string.app_name))
    }

    @Test
    fun catalogSamplesPreserveOkinaAndKahako() {
        val resources = RuntimeEnvironment.getApplication().resources
        val samples = listOf(
            resources.getString(R.string.r3_catalog_drop_title),
            resources.getString(R.string.r3_catalog_host),
            resources.getString(R.string.r3_catalog_next_step),
            resources.getString(R.string.r3_catalog_code_business)
        ).joinToString()

        assertTrue(samples.contains('ʻ'))
        assertTrue(samples.contains('ī'))
    }

    @Test
    fun componentCopyUsesApprovedAudienceVocabulary() {
        val resources = RuntimeEnvironment.getApplication().resources
        val copy = listOf(
            resources.getString(R.string.r3_catalog_empty_body),
            resources.getString(R.string.r3_catalog_empty_action),
            resources.getString(R.string.r3_catalog_trail_title),
            resources.getString(R.string.r3_state_found),
            resources.getString(R.string.r3_unlock)
        ).joinToString(separator = " ")

        assertTrue(copy.contains("Experience"))
        assertTrue(copy.contains("Trail"))
        assertTrue(copy.contains("Found"))
        assertTrue(copy.contains("Unlock"))
        assertFalse(copy.contains("Explorer", ignoreCase = true))
        assertFalse(copy.contains("collect", ignoreCase = true))
        assertFalse(copy.contains("coupon", ignoreCase = true))
    }
}
