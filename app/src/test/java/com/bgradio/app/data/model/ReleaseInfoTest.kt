package com.bgradio.app.data.model

import com.bgradio.app.data.repository.UpdateRepository
import com.bgradio.app.data.repository.UpdateResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseInfoTest {

    private val OFFICIAL = "https://github.com/dominitriks/dominixradio/releases/download"

    /** Real response of GET /repos/dominitriks/dominixradio/releases/latest (v1.3.0, with asset digests). */
    private val realJson = javaClass.classLoader!!.getResource("github_release_latest.json")!!.readText()

    @Test
    fun versionsCompareNumerically() {
        assertTrue(AppVersion.parse("v1.10.0")!! > AppVersion.parse("1.9.9")!!)
        assertTrue(AppVersion.parse("1.2.0")!! > AppVersion.parse("1.1.0")!!)
        assertEquals(0, AppVersion.parse("1.1")!!.compareTo(AppVersion.parse("1.1.0")!!))
        assertEquals(AppVersion(listOf(1, 1, 0)), AppVersion.parse("1.1.0-debug"))
        assertNull(AppVersion.parse("latest"))
        assertNull(AppVersion.parse(""))
    }

    @Test
    fun parsesRealGitHubRelease() {
        val r = ReleaseInfo.parse(realJson)
        assertNotNull(r)
        assertEquals("1.3.0", r!!.version.toString())
        assertEquals("Dominix v1.3.0", r.title)
        assertEquals("https://github.com/dominitriks/dominixradio/releases/download/v1.3.0/Dominix-v1.3.0.apk", r.apkUrl)
        assertTrue(r.apkSizeBytes > 1_000_000)
        // Markdown cleaned, checksum line dropped
        assertTrue(r.notes.contains("Listening statistics"))
        // GitHub's published checksum of exactly this APK
        assertEquals("49c5eba433def7160f71a80925601a5d6eccc0cbe5a6d140a157a9416232a002", r.apkSha256)
        assertFalse(r.notes.contains("**"))
        assertFalse(r.notes.contains("SHA-256"))
    }

    @Test
    fun newerReleaseIsOfferedOlderOrSameIsNot() {
        val r = ReleaseInfo.parse(realJson)
        assertTrue(UpdateRepository.evaluate(r, AppVersion.parse("1.2.0")!!) is UpdateResult.Available)
        assertTrue(UpdateRepository.evaluate(r, AppVersion.parse("1.3.0")!!) is UpdateResult.UpToDate)
        assertTrue(UpdateRepository.evaluate(r, AppVersion.parse("1.3.1")!!) is UpdateResult.UpToDate)
    }

    @Test
    fun invalidResponsesAreHandled() {
        assertNull(ReleaseInfo.parse("<html>rate limited</html>"))
        assertNull(ReleaseInfo.parse("{}"))
        assertNull(ReleaseInfo.parse("""{"tag_name":"v9.0.0","draft":true}"""))
        assertTrue(UpdateRepository.evaluate(null, AppVersion.parse("1.0.0")!!) is UpdateResult.Failed)
    }

    @Test
    fun releaseWithoutApkFallsBackToPageAndPrefersReleaseOverDebug() {
        val noApk = ReleaseInfo.parse(
            """{"tag_name":"v2.0.0","html_url":"https://github.com/x/y/releases/tag/v2.0.0",
               "assets":[{"name":"notes.txt","browser_download_url":"https://x/notes.txt"}]}"""
        )!!
        assertNull(noApk.apkUrl)
        assertEquals("https://github.com/x/y/releases/tag/v2.0.0", noApk.pageUrl)

        val both = ReleaseInfo.parse(
            """{"tag_name":"v2.0.0","assets":[
                {"name":"Dominix-v2.0.0-debug.apk","browser_download_url":"$OFFICIAL/v2.0.0/Dominix-v2.0.0-debug.apk","size":20},
                {"name":"Dominix-v2.0.0.apk","browser_download_url":"$OFFICIAL/v2.0.0/Dominix-v2.0.0.apk","size":3}]}"""
        )!!
        assertEquals("$OFFICIAL/v2.0.0/Dominix-v2.0.0.apk", both.apkUrl)
    }

    @Test
    fun invalidDigestIsIgnored() {
        val r = ReleaseInfo.parse(
            """{"tag_name":"v2.0.0","assets":[{"name":"D.apk","browser_download_url":"https://x/D.apk","digest":"md5:abc"}]}"""
        )!!
        assertNull(r.apkSha256)
    }

    @Test
    fun sha256OfFileMatchesKnownValue() {
        val f = java.io.File.createTempFile("dominix", ".bin").apply { writeText("abc"); deleteOnExit() }
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", com.bgradio.app.data.repository.UpdateDownloader.sha256(f))
    }
}
