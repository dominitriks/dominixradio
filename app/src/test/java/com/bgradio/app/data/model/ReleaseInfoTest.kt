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

    /** Real response of GET /repos/dominitriks/dominixradio/releases/latest (v1.1.0). */
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
        assertEquals("1.1.0", r!!.version.toString())
        assertEquals("Dominix v1.1.0", r.title)
        assertEquals("https://github.com/dominitriks/dominixradio/releases/download/v1.1.0/Dominix-v1.1.0.apk", r.apkUrl)
        assertTrue(r.apkSizeBytes > 1_000_000)
        // Markdown cleaned, checksum line dropped
        assertTrue(r.notes.contains("Sleep timer"))
        assertFalse(r.notes.contains("**"))
        assertFalse(r.notes.contains("SHA-256"))
    }

    @Test
    fun newerReleaseIsOfferedOlderOrSameIsNot() {
        val r = ReleaseInfo.parse(realJson)
        assertTrue(UpdateRepository.evaluate(r, AppVersion.parse("1.0.1")!!) is UpdateResult.Available)
        assertTrue(UpdateRepository.evaluate(r, AppVersion.parse("1.1.0")!!) is UpdateResult.UpToDate)
        assertTrue(UpdateRepository.evaluate(r, AppVersion.parse("1.2.0")!!) is UpdateResult.UpToDate)
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
                {"name":"Dominix-v2.0.0-debug.apk","browser_download_url":"https://x/debug.apk","size":20},
                {"name":"Dominix-v2.0.0.apk","browser_download_url":"https://x/release.apk","size":3}]}"""
        )!!
        assertEquals("https://x/release.apk", both.apkUrl)
    }
}
