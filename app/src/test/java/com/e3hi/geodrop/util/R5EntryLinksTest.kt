package com.e3hi.geodrop.util

import com.e3hi.geodrop.data.R5EntryChannel
import com.e3hi.geodrop.data.R5EntryRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class R5EntryLinksTest {
    private val request = R5EntryRequest(
        code = "ABCDEFGH",
        entrySessionId = "0123456789abcdef",
        channel = R5EntryChannel.QR
    )

    @Test
    fun `qr payload round trips through strict app link parser`() {
        val uri = R5EntryLinks.appLink("go.example.org", request)

        assertEquals(request, R5EntryParser.fromAppLink(uri, "go.example.org"))
    }

    @Test
    fun `play url carries encoded install referrer`() {
        val play = R5EntryLinks.playListingUrl("com.e3hi.geodrop", request)
        val recovered = R5EntryParser.fromInstallReferrer(play.getQueryParameter("referrer"))

        assertEquals(request, recovered)
    }

    @Test
    fun `known unowned domain cannot become a share artifact`() {
        assertThrows(IllegalArgumentException::class.java) {
            R5EntryLinks.appLink("geodrop.app", request)
        }
    }
}
