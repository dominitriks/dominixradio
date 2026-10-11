package com.bgradio.app.share

import com.bgradio.app.data.model.AppInfo
import com.bgradio.app.data.model.Station
import com.google.zxing.BinaryBitmap
import com.google.zxing.LuminanceSource
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareContentTest {

    private val station = Station("darik", "Дарик Радио", "http://stream/darik", emptyList(), "Новини", "София", "", true, false)

    /** Turns the encoder's module matrix into something the ZXing reader can scan. */
    private class MatrixSource(private val m: BitMatrix) : LuminanceSource(m.width, m.height) {
        override fun getRow(y: Int, row: ByteArray?): ByteArray {
            val r = if (row != null && row.size >= width) row else ByteArray(width)
            for (x in 0 until width) r[x] = if (m[x, y]) 0 else 255.toByte()
            return r
        }
        override fun getMatrix(): ByteArray = ByteArray(width * height).also { out ->
            for (y in 0 until height) for (x in 0 until width) out[y * width + x] = if (m[x, y]) 0 else 255.toByte()
        }
    }

    private fun scan(m: BitMatrix): String = QRCodeReader().decode(BinaryBitmap(HybridBinarizer(MatrixSource(m)))).text

    @Test
    fun qrCodeScansBackToTheOfficialDownloadLink() {
        val content = ShareContent.qrContent()
        assertEquals("https://github.com/dominitriks/dominixradio/releases/latest", content)
        assertEquals(content, scan(ShareContent.qrMatrix(content, 400)))
    }

    @Test
    fun qrCodeHasQuietZone() {
        val m = ShareContent.qrMatrix(ShareContent.qrContent(), 400)
        // Corners must be white (quiet zone) for reliable scanning.
        assertFalse(m[0, 0])
        assertFalse(m[399, 399])
    }

    @Test
    fun shareAppMessageHasExactTextAndOfficialLink() {
        assertEquals(
            "Слушай любимите си български радиостанции с DOMINIX – Bulgarian Radio! " +
                "Изтегли приложението оттук: https://github.com/dominitriks/dominixradio/releases/latest",
            ShareContent.appMessage(),
        )
        assertEquals("https://github.com/dominitriks/dominixradio/releases/latest", AppInfo.DOWNLOAD_URL)
        assertTrue(ShareContent.appMessage().endsWith(AppInfo.DOWNLOAD_URL))
        assertEquals("DOMINIX – Bulgarian Radio", ShareContent.APP_SUBJECT)
        // A link to the page, never the APK file itself.
        assertFalse(ShareContent.appMessage().contains(".apk"))
    }

    @Test
    fun shareMessageNamesStationAppAndLinkButNotTheStream() {
        val msg = ShareContent.message(station)
        assertEquals("Слушам Дарик Радио с DOMINIX – Bulgarian Radio. Опитай и ти! ${AppInfo.DOWNLOAD_URL}", msg)
        assertFalse(msg.contains(station.streamUrl))
        assertTrue(msg.contains("https://"))
    }
}
