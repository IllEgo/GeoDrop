package com.e3hi.geodrop.ui.organizer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class R7PlacementMapGestureTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun oneFingerDragReservesGestureUntilFingerIsLifted() {
        val touchActivity = mutableListOf<Boolean>()
        composeRule.setContent {
            Box(
                Modifier
                    .size(240.dp)
                    .testTag("placement-map-touch-target")
                    .reportMapTouchActivity(touchActivity::add)
            )
        }

        composeRule.onNodeWithTag("placement-map-touch-target").performTouchInput {
            down(center)
            moveBy(Offset(x = 0f, y = 80f))
            up()
        }

        composeRule.runOnIdle {
            assertEquals(listOf(true, false), touchActivity)
        }
    }
}
