package com.kitheapp.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream

/** Generates the local, privacy-safe QR and its printable Kithe share card. */
object R5QrCode {
    private const val INK = 0xFF132322.toInt()
    private const val TEAL = 0xFF0B5D5D.toInt()
    private const val AMBER = 0xFFE07B24.toInt()
    private const val CREAM = 0xFFFFF8EC.toInt()
    private const val CARD_WIDTH = 1200
    private const val CARD_HEIGHT = 1600
    private const val CARD_QR_SIZE = 840

    fun createBitmap(payload: String, size: Int = CARD_QR_SIZE): Bitmap {
        require(payload.isNotBlank()) { "A QR payload is required." }
        require(size in 128..2048) { "QR size must be between 128 and 2048 pixels." }
        val matrix = QRCodeWriter().encode(
            payload,
            BarcodeFormat.QR_CODE,
            size,
            size,
            mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 4
            )
        )
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                pixels[y * size + x] = if (matrix[x, y]) TEAL else Color.WHITE
            }
        }
        return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    }

    fun createShareCard(
        qr: Bitmap,
        experienceName: String,
        displayCode: String
    ): Bitmap {
        val result = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(CREAM)

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TEAL
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 58f
            letterSpacing = 0.14f
        }
        canvas.drawText("KITHE", CARD_WIDTH / 2f, 82f, brandPaint)
        canvas.drawRoundRect(RectF(522f, 104f, 678f, 116f), 6f, 6f, Paint().apply {
            color = AMBER
        })

        drawCenteredText(
            canvas = canvas,
            text = experienceName.trim().ifEmpty { "Kithe Experience" },
            top = 150f,
            width = 1040,
            paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = INK
                textSize = 66f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            },
            maxLines = 2
        )

        canvas.drawRoundRect(RectF(140f, 350f, 1060f, 1270f), 44f, 44f, Paint().apply {
            color = Color.WHITE
        })
        val scaledQr = if (qr.width == CARD_QR_SIZE && qr.height == CARD_QR_SIZE) {
            qr
        } else {
            Bitmap.createScaledBitmap(qr, CARD_QR_SIZE, CARD_QR_SIZE, false)
        }
        canvas.drawBitmap(scaledQr, 180f, 390f, null)

        val instructionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = INK
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 44f
        }
        canvas.drawText("Scan to join", CARD_WIDTH / 2f, 1350f, instructionPaint)

        val codePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TEAL
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = 74f
            letterSpacing = 0.08f
        }
        canvas.drawText(displayCode, CARD_WIDTH / 2f, 1460f, codePaint)

        val fallbackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = INK
            textAlign = Paint.Align.CENTER
            textSize = 30f
        }
        canvas.drawText("Scan the QR or enter the code in Kithe", CARD_WIDTH / 2f, 1530f, fallbackPaint)
        return result
    }

    fun writePng(context: Context, destination: Uri, bitmap: Bitmap) {
        val wrote = context.contentResolver.openOutputStream(destination, "w")?.use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        } ?: false
        check(wrote) { "The QR image could not be saved." }
    }

    fun share(
        context: Context,
        bitmap: Bitmap,
        experienceName: String,
        displayCode: String,
        message: String
    ) {
        val directory = File(context.cacheDir, "qr-shares").apply { mkdirs() }
        val safeCode = displayCode.filter { it.isLetterOrDigit() || it == '-' }.ifEmpty { "experience" }
        val file = File(directory, "kithe-$safeCode.png")
        FileOutputStream(file).use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                "The QR image could not be prepared."
            }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            clipData = ClipData.newUri(context.contentResolver, "Kithe Experience QR", uri)
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, experienceName)
            putExtra(Intent.EXTRA_TEXT, message)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Share Experience QR"))
    }

    private fun drawCenteredText(
        canvas: Canvas,
        text: String,
        top: Float,
        width: Int,
        paint: TextPaint,
        maxLines: Int
    ) {
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setMaxLines(maxLines)
            .setIncludePad(false)
            .build()
        canvas.save()
        canvas.translate((CARD_WIDTH - width) / 2f, top)
        layout.draw(canvas)
        canvas.restore()
    }
}
