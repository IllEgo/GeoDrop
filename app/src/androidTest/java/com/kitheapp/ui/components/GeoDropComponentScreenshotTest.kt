package com.kitheapp.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kitheapp.ui.theme.GeoDropTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** On-device screenshot smoke checks for the R3 catalog configurations. */
@RunWith(AndroidJUnit4::class)
class GeoDropComponentScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun lightCatalogCapturesAtCompactWidth() {
        composeRule.setContent {
            GeoDropTheme(darkTheme = false) {
                Surface(Modifier.fillMaxSize()) { GeoDropComponentCatalog() }
            }
        }

        val image = composeRule.onRoot().captureToImage()
        assertTrue(image.width > 0)
        assertTrue(image.height > 0)
    }

    @Test
    fun darkCatalogCapturesAtCompactWidth() {
        composeRule.setContent {
            GeoDropTheme(darkTheme = true) {
                Surface(Modifier.fillMaxSize()) { GeoDropComponentCatalog() }
            }
        }

        val image = composeRule.onRoot().captureToImage()
        assertTrue(image.width > 0)
        assertTrue(image.height > 0)
    }
}
