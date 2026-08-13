package com.e3hi.geodrop.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.e3hi.geodrop.data.R5EntryChannel
import com.e3hi.geodrop.data.R5EntryRequest
import com.e3hi.geodrop.data.R5PendingUnlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class R5EntryStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun clearState() {
        context.getSharedPreferences("geodrop_r5_entry", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun `pending entry expires after twenty four hours`() {
        val store = R5EntryStore(context)
        val request = R5EntryRequest("ABCDEFGH", "0123456789abcdef", R5EntryChannel.QR)
        store.savePendingEntry(request, nowMillis = 1_000L)

        assertEquals(request, store.pendingEntry(nowMillis = 2_000L))
        assertNull(
            store.pendingEntry(nowMillis = 1_000L + R5EntryStore.ENTRY_TTL_MILLIS + 1L)
        )
    }

    @Test
    fun `exact unlock target and notification primer survive recreation`() {
        val first = R5EntryStore(context)
        first.savePendingUnlock(R5PendingUnlock("ABCDEFGH", "drop-42"))
        first.markNotificationPrimerSeen("ABCDEFGH")

        val recreated = R5EntryStore(context)
        assertEquals(R5PendingUnlock("ABCDEFGH", "drop-42"), recreated.pendingUnlock())
        assertTrue(recreated.notificationPrimerSeen("ABCDEFGH"))
        assertFalse(recreated.notificationPrimerSeen("OTHER123"))

        recreated.clearPendingUnlock()
        assertNull(recreated.pendingUnlock())
    }
}
