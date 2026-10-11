package com.bgradio.app.share

import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.bgradio.app.data.model.AppInfo
import com.bgradio.app.data.model.Station
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File

/** Text and QR content for sharing a station. Pure functions — unit tested. */
object ShareContent {

    fun message(station: Station, link: String = AppInfo.DOWNLOAD_URL): String =
        "Слушам ${station.name} с DOMINIX – Bulgarian Radio. Опитай и ти! $link"

    /** "Сподели DOMINIX": recommends the app itself with the official download link. */
    fun appMessage(link: String = AppInfo.DOWNLOAD_URL): String =
        "Слушай любимите си български радиостанции с DOMINIX – Bulgarian Radio! Изтегли приложението оттук: $link"

    const val APP_SUBJECT = "DOMINIX – Bulgarian Radio"

    /**
     * What the QR code contains. There is no per-station deep link, so it is the official
     * download page; the station name is printed next to the code instead.
     */
    fun qrContent(link: String = AppInfo.DOWNLOAD_URL): String = link

    /** QR modules (true = dark) with a 2-module quiet zone, medium error correction. */
    fun qrMatrix(content: String, size: Int = 512): BitMatrix = QRCodeWriter().encode(
        content,
        BarcodeFormat.QR_CODE,
        size,
        size,
        mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 2,
            EncodeHintType.CHARACTER_SET to "UTF-8",
        ),
    )
}

/** Android side: share sheet, QR bitmap, saving to the gallery. */
object StationShare {

    /** Opens the Android share sheet for a station. Returns false if no app can share text. */
    fun shareText(context: Context, station: Station): Boolean =
        sharePlainText(context, "${station.name} в DOMINIX", ShareContent.message(station), "Сподели радиостанция")

    /** "Сподели DOMINIX" — shares the download link of the app. Returns false if no app can share text. */
    fun shareApp(context: Context): Boolean =
        sharePlainText(context, ShareContent.APP_SUBJECT, ShareContent.appMessage(), "Сподели DOMINIX")

    private fun sharePlainText(context: Context, subject: String, text: String, chooserTitle: String): Boolean {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        return start(context, Intent.createChooser(send, chooserTitle))
    }

    fun qrBitmap(content: String, size: Int = 720): Bitmap {
        val m = ShareContent.qrMatrix(content, size)
        val pixels = IntArray(m.width * m.height)
        for (y in 0 until m.height) {
            val row = y * m.width
            for (x in 0 until m.width) pixels[row + x] = if (m[x, y]) Color.BLACK else Color.WHITE
        }
        return Bitmap.createBitmap(pixels, m.width, m.height, Bitmap.Config.ARGB_8888)
    }

    /** White card with the QR code, station name and app name — for saving / sharing as image. */
    fun qrCard(station: Station, content: String): Bitmap {
        val qr = qrBitmap(content, 720)
        val w = 800
        val h = 1000
        val card = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(card)
        c.drawColor(Color.WHITE)
        c.drawBitmap(qr, ((w - qr.width) / 2).toFloat(), 40f, null)
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 44f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val sub = Paint(title).apply {
            textSize = 30f
            typeface = Typeface.DEFAULT
            color = Color.rgb(70, 70, 80)
        }
        c.drawText(ellipsize(station.name, title, w - 80f), w / 2f, 830f, title)
        c.drawText("Слушайте с DOMINIX – Bulgarian Radio", w / 2f, 885f, sub)
        c.drawText("Сканирайте, за да изтеглите приложението", w / 2f, 930f, sub)
        qr.recycle()
        return card
    }

    private fun ellipsize(text: String, paint: Paint, max: Float): String {
        if (paint.measureText(text) <= max) return text
        var t = text
        while (t.isNotEmpty() && paint.measureText("$t…") > max) t = t.dropLast(1)
        return "$t…"
    }

    /** Shares the QR card as PNG (through FileProvider) together with the text. */
    fun shareQrImage(context: Context, station: Station, card: Bitmap): Boolean {
        val uri = try {
            val dir = File(context.cacheDir, "qr").apply { mkdirs() }
            val file = File(dir, "dominix-${station.id}.png")
            file.outputStream().use { card.compress(Bitmap.CompressFormat.PNG, 100, it) }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (_: Exception) {
            return false
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, ShareContent.message(station))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return start(context, Intent.createChooser(send, "Сподели QR кода"))
    }

    /** Saving without any storage permission is possible from Android 10. */
    val canSaveToGallery: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    /** Saves to Pictures/Dominix. Returns true only if the image was actually written. */
    fun saveToGallery(context: Context, station: Station, card: Bitmap): Boolean {
        if (!canSaveToGallery) return false
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "Dominix-QR-${station.id}-${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Dominix")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        return try {
            val ok = resolver.openOutputStream(uri)?.use { card.compress(Bitmap.CompressFormat.PNG, 100, it) } == true
            if (!ok) {
                resolver.delete(uri, null, null)
                return false
            }
            resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
            true
        } catch (_: Exception) {
            resolver.delete(uri, null, null)
            false
        }
    }

    internal fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
