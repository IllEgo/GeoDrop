package com.kitheapp.util

import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class R5QrCodeTest {
    @Test
    fun `generated QR round trips without a hosted QR service`() {
        val payload = "https://join.kitheapp.com/e/DEMO2026?entry_session_id=0123456789abcdef&channel=QR"
        val bitmap = R5QrCode.createBitmap(payload, 512)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val decoded = QRCodeReader().decode(
            BinaryBitmap(HybridBinarizer(RGBLuminanceSource(bitmap.width, bitmap.height, pixels)))
        )

        assertEquals(payload, decoded.text)
    }

    @Test
    fun `empty payload cannot create a decorative QR`() {
        assertThrows(IllegalArgumentException::class.java) {
            R5QrCode.createBitmap("", 512)
        }
    }

    @Test
    fun `share card keeps a readable portrait print shape`() {
        val qr = R5QrCode.createBitmap("https://join.kitheapp.com/e/DEMO2026", 840)
        val card = R5QrCode.createShareCard(qr, "Hilo Garden Walk", "DEMO-2026")

        assertEquals(1200, card.width)
        assertEquals(1600, card.height)
    }
}
