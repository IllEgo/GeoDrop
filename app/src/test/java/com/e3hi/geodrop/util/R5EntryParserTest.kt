package com.e3hi.geodrop.util

import android.net.Uri
import com.e3hi.geodrop.data.R5EntryChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class R5EntryParserTest {
    @Test
    fun `owned https app link preserves session and qr channel`() {
        val parsed = R5EntryParser.fromAppLink(
            Uri.parse(
                "https://entry.example/e/abcd-efgh" +
                    "?entry_session_id=0123456789abcdef&channel=QR"
            ),
            "entry.example"
        )

        assertEquals("ABCDEFGH", parsed?.code)
        assertEquals("0123456789abcdef", parsed?.entrySessionId)
        assertEquals(R5EntryChannel.QR, parsed?.channel)
    }

    @Test
    fun `app link rejects wrong host scheme and extra path`() {
        assertNull(
            R5EntryParser.fromAppLink(
                Uri.parse("https://wrong.example/e/ABCDEFGH"),
                "entry.example"
            )
        )
        assertNull(
            R5EntryParser.fromAppLink(
                Uri.parse("http://entry.example/e/ABCDEFGH"),
                "entry.example"
            )
        )
        assertNull(
            R5EntryParser.fromAppLink(
                Uri.parse("https://entry.example/e/ABCDEFGH/more"),
                "entry.example"
            )
        )
    }

    @Test
    fun `stripped install referrer still recovers code and creates session`() {
        val parsed = R5EntryParser.fromInstallReferrer("code=ABCD-EFGH")

        assertEquals("ABCDEFGH", parsed?.code)
        assertEquals(R5EntryChannel.LINK, parsed?.channel)
        assertNotNull(parsed?.entrySessionId)
        assertEquals(32, parsed?.entrySessionId?.length)
    }

    @Test
    fun `manual code tolerates spaces and presentation dashes`() {
        val parsed = R5EntryParser.manual(" abcd — efgh ", "0123456789abcdef")

        assertEquals("ABCDEFGH", parsed?.code)
        assertEquals("ABCD-EFGH", R5EntryParser.displayCode(parsed!!.code))
        assertEquals(R5EntryChannel.MANUAL, parsed.channel)
    }
}
