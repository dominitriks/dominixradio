package com.bgradio.app.data.repository

import android.app.DownloadManager
import com.bgradio.app.data.model.AppVersion
import com.bgradio.app.data.model.ReleaseInfo
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class UpdateFlowTest {

    private val realJson = javaClass.classLoader!!.getResource("github_release_latest.json")!!.readText() // v1.3.0

    private fun repo(installed: String, online: Boolean = true, fetch: () -> HttpAnswer) =
        UpdateRepository(isOnline = { online }, fetcher = { fetch() }, installedVersion = AppVersion.parse(installed)!!)

    // ---- Version check ---------------------------------------------------------------------

    @Test
    fun newerVersionIsAvailable() = runBlocking {
        val r = repo("1.2.0") { HttpAnswer(200, realJson) }.check()
        assertTrue(r is UpdateResult.Available)
        assertEquals("1.3.0", (r as UpdateResult.Available).release.version.toString())
    }

    @Test
    fun sameOrOlderInstalledIsUpToDate() = runBlocking {
        assertTrue(repo("1.3.0") { HttpAnswer(200, realJson) }.check() is UpdateResult.UpToDate)
        assertTrue(repo("1.3.3") { HttpAnswer(200, realJson) }.check() is UpdateResult.UpToDate)
        // numeric, not text comparison: 1.10.0 is newer than 1.3.0
        assertTrue(repo("1.10.0") { HttpAnswer(200, realJson) }.check() is UpdateResult.UpToDate)
    }

    @Test
    fun invalidApiAnswerIsAFailureNotAnUpdate() = runBlocking {
        assertTrue(repo("1.0.0") { HttpAnswer(200, "<html>oops</html>") }.check() is UpdateResult.Failed)
        assertTrue(repo("1.0.0") { HttpAnswer(200, "{}") }.check() is UpdateResult.Failed)
        assertTrue(repo("1.0.0") { HttpAnswer(500, "") }.check() is UpdateResult.Failed)
        assertTrue(repo("1.0.0") { HttpAnswer(403, "") }.check() is UpdateResult.Failed)
        assertTrue(repo("1.0.0") { HttpAnswer(404, "") }.check() is UpdateResult.Failed)
    }

    @Test
    fun noInternetDoesNotEvenCallGitHub() = runBlocking {
        var called = false
        val r = repo("1.0.0", online = false) { called = true; HttpAnswer(200, realJson) }.check()
        assertTrue(r is UpdateResult.Failed)
        assertFalse(called)
    }

    @Test
    fun timeoutAndBrokenConnectionAreHandled() = runBlocking {
        val t = repo("1.0.0") { throw SocketTimeoutException() }.check()
        assertTrue((t as UpdateResult.Failed).message.contains("навреме"))
        assertTrue(repo("1.0.0") { throw IOException("reset") }.check() is UpdateResult.Failed)
        assertTrue(repo("1.0.0") { throw IllegalStateException() }.check() is UpdateResult.Failed)
    }

    @Test
    fun onlyOneCheckRunsAtATime() = runBlocking {
        val inside = CountDownLatch(1)
        val release = CountDownLatch(1)
        val r = repo("1.2.0") {
            inside.countDown()
            release.await(5, TimeUnit.SECONDS)
            HttpAnswer(200, realJson)
        }
        val first = async(kotlinx.coroutines.Dispatchers.IO) { r.check() }
        assertTrue(inside.await(5, TimeUnit.SECONDS))
        assertNull("second parallel check must be skipped", r.check())
        release.countDown()
        assertTrue(first.await() is UpdateResult.Available)
        // after it finished, a new check is allowed again
        assertNotNull(r.check())
    }

    // ---- Release asset ---------------------------------------------------------------------

    @Test
    fun onlyOfficialApkUrlsAreTrusted() {
        assertTrue(ReleaseInfo.isTrustedApkUrl("https://github.com/dominitriks/dominixradio/releases/download/v1.3.3/Dominix-v1.3.3.apk"))
        assertFalse(ReleaseInfo.isTrustedApkUrl("https://evil.example/Dominix.apk"))
        assertFalse(ReleaseInfo.isTrustedApkUrl("https://github.com/someone/else/releases/download/v1/Dominix.apk"))
        assertFalse(ReleaseInfo.isTrustedApkUrl("http://github.com/dominitriks/dominixradio/releases/download/v1/D.apk"))
        assertFalse(ReleaseInfo.isTrustedApkUrl("https://github.com/dominitriks/dominixradio/releases/download/v1/../../x/D.apk"))
        assertFalse(ReleaseInfo.isTrustedApkUrl("https://github.com/dominitriks/dominixradio/releases/download/v1/D.zip"))
    }

    @Test
    fun releaseWithForeignApkLinkHasNoApk() {
        val r = ReleaseInfo.parse(
            """{"tag_name":"v9.0.0","assets":[{"name":"Dominix.apk","browser_download_url":"https://evil.example/Dominix.apk"}]}"""
        )!!
        assertNull(r.apkUrl)
    }

    // ---- Downloaded file -------------------------------------------------------------------

    private fun tmp(bytes: Int): File = File.createTempFile("dominix", ".apk").apply {
        writeBytes(ByteArray(bytes) { 7 })
        deleteOnExit()
    }

    private val good = ApkFacts("com.bgradio.app", versionCode = 10, sameSigner = true)

    private fun verify(file: File, size: Long = file.length(), sha: String? = null, facts: ApkFacts? = good, installed: Long = 9) =
        UpdateVerifier.check(file, size, sha, facts, "com.bgradio.app", installed, sha256 = { "abc" })

    @Test
    fun validUpdatePasses() {
        assertNull(verify(tmp(100), sha = "abc"))
        assertNull(verify(tmp(100), facts = good.copy(sameSigner = null))) // signer unknown → installer decides
    }

    @Test
    fun emptyIncompleteOrCorruptFilesAreRejected() {
        assertTrue(verify(tmp(0))!!.contains("празен"))
        assertTrue(verify(tmp(100), size = 3_000_000)!!.contains("непълен"))
        assertTrue(verify(tmp(100), sha = "different")!!.contains("контролната сума"))
        assertTrue(verify(File("does-not-exist.apk"))!!.contains("празен"))
    }

    @Test
    fun wrongAppOldVersionOrOtherSignerIsRejected() {
        assertTrue(verify(tmp(10), facts = null)!!.contains("не е валидно"))
        assertTrue(verify(tmp(10), facts = good.copy(packageName = "com.other"))!!.contains("не е обновление"))
        assertTrue(verify(tmp(10), facts = good.copy(versionCode = 9))!!.contains("не е по-нова"))
        assertTrue(verify(tmp(10), facts = good.copy(versionCode = 3))!!.contains("не е по-нова"))
        assertTrue(verify(tmp(10), facts = good.copy(sameSigner = false))!!.contains("различен ключ"))
    }

    @Test
    fun failedDownloadNeverReplacesAGoodFile() {
        val dir = kotlin.io.path.createTempDirectory("dominix").toFile()
        val target = File(dir, "Dominix-v1.4.0.apk").apply { writeText("GOOD") }
        val part = File(dir, "Dominix-v1.4.0-partial.apk").apply { writeText("BROKEN") }
        assertNull(UpdateVerifier.finalize(part, target, verified = false))
        assertEquals("GOOD", target.readText())
        assertFalse(part.exists())

        val part2 = File(dir, "Dominix-v1.4.0-partial.apk").apply { writeText("NEW") }
        val result = UpdateVerifier.finalize(part2, target, verified = true)
        assertEquals(target, result)
        assertEquals("NEW", target.readText())
        dir.deleteRecursively()
    }

    // ---- Download progress / interruption ---------------------------------------------------

    @Test
    fun interruptedDownloadIsReportedForRetry() {
        val part = tmp(10)
        val failed = UpdateDownloader.mapStatus(DownloadManager.STATUS_FAILED, 50, 100, 1008, part)
        assertTrue(failed is DownloadState.Failed && failed.message.contains("опитайте отново"))

        val running = UpdateDownloader.mapStatus(DownloadManager.STATUS_RUNNING, 50, 200, 0, part)
        assertEquals(DownloadState.Running(25), running)
        assertEquals(DownloadState.Running(null), UpdateDownloader.mapStatus(DownloadManager.STATUS_PENDING, 0, -1, 0, part))

        assertEquals(DownloadState.Done(part), UpdateDownloader.mapStatus(DownloadManager.STATUS_SUCCESSFUL, 10, 10, 0, part))
        // "successful" but the file is missing or empty → not offered for installation
        val missing = UpdateDownloader.mapStatus(DownloadManager.STATUS_SUCCESSFUL, 10, 10, 0, File("nope.apk"))
        assertTrue(missing is DownloadState.Failed)
    }
}
