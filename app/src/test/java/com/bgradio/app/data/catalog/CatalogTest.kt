package com.bgradio.app.data.catalog

import com.bgradio.app.data.model.Station
import kotlinx.coroutines.Dispatchers
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class CatalogTest {

    /** The real catalog built into the APK (and published on GitHub). */
    private val bundledJson = File("src/main/assets/stations.json").readText()
    private val bundled = CatalogParser.parse(bundledJson)
    private val bundledIds = bundled.map { it.id }.toSet()

    // ---- Parser / validation -----------------------------------------------------------------

    @Test
    fun builtInCatalogIsValidAndComplete() {
        assertEquals(106, bundled.size)
        val check = CatalogParser.validateRemote(bundledJson, bundledIds)
        assertTrue(check is CatalogCheck.Accepted)
        assertEquals(106, (check as CatalogCheck.Accepted).stations.size)
    }

    @Test
    fun brokenOrForeignFilesAreRejected() {
        assertTrue(CatalogParser.validateRemote("<html>404</html>", bundledIds) is CatalogCheck.Rejected)
        assertTrue(CatalogParser.validateRemote("[]", bundledIds) is CatalogCheck.Rejected)
        // truncated: only a few stations left
        val few = "[" + bundledJson.trim().removePrefix("[").split("},").take(5).joinToString("},") + "}]"
        assertTrue(CatalogParser.validateRemote(few, bundledIds) is CatalogCheck.Rejected)
        // a completely different list of stations
        val foreign = (1..60).joinToString(",", "[", "]") { """{"id":"x$it","name":"X $it","streamUrl":"https://x.example/$it"}""" }
        assertTrue(CatalogParser.validateRemote(foreign, bundledIds) is CatalogCheck.Rejected)
        // oversized
        assertTrue(CatalogParser.validateRemote(" ".repeat(CatalogParser.MAX_BYTES + 1), bundledIds) is CatalogCheck.Rejected)
    }

    @Test
    fun invalidEntriesAreSkippedOthersKept() {
        val json = """[
            {"id":"ok","name":"Радио ОК","streamUrl":"https://ok.example/stream","fallbackUrls":["ftp://bad","http://fb.example/s"],"logoUrl":"javascript:alert(1)"},
            {"id":"Bad Id","name":"Bad","streamUrl":"https://x.example/s"},
            {"id":"noname","name":"","streamUrl":"https://x.example/s"},
            {"id":"badurl","name":"Bad URL","streamUrl":"file:///etc/passwd"},
            {"id":"ok","name":"Duplicate","streamUrl":"https://dup.example/s"}
        ]"""
        val list = CatalogParser.parse(json)
        assertEquals(listOf("ok"), list.map { it.id })
        assertEquals(listOf("http://fb.example/s"), list.single().fallbackUrls)
        assertEquals("", list.single().logoUrl)
    }

    @Test
    fun editedCatalogWithChangedUrlAndNewStationIsAccepted() {
        val edited = bundledJson.replace("http://stream.metacast.eu/radio1128", "https://new.example/radio1") // fixed URL
            .trimEnd().removeSuffix("]") + """,{"id":"novo_radio","name":"Ново радио","streamUrl":"https://novo.example/live","genre":"Поп"}]"""
        val check = CatalogParser.validateRemote(edited, bundledIds) as CatalogCheck.Accepted
        assertEquals(107, check.stations.size)
        assertEquals("https://new.example/radio1", check.stations.first { it.id == "radio1" }.streamUrl)
    }

    // ---- Cache + updater ---------------------------------------------------------------------

    private class FakeFetcher(var answer: () -> CatalogAnswer) : CatalogFetcher {
        val etags = mutableListOf<String?>()
        override fun fetch(etag: String?): CatalogAnswer {
            etags += etag
            return answer()
        }
    }

    private var clock = 1_000_000_000L
    private val dir = kotlin.io.path.createTempDirectory("catalog").toFile()
    private val cache = CatalogCache(dir)
    private var applied: List<Station>? = null

    private fun updater(fetcher: CatalogFetcher, online: Boolean = true, version: Int = 12) = CatalogUpdater(
        cache = cache,
        apply = { list, _ -> applied = list },
        bundledIds = { bundledIds },
        isOnline = { online },
        fetcher = fetcher,
        versionCode = version,
        now = { clock },
    )

    @Test
    fun downloadStoreAndApply() = runBlocking {
        val f = FakeFetcher { CatalogAnswer(200, bundledJson, "\"etag-1\"") }
        assertEquals(CatalogUpdateResult.Updated(106), updater(f).refresh(force = false))
        assertEquals(106, applied?.size)
        assertEquals(listOf<String?>(null), f.etags) // nothing cached yet → no ETag sent
        val cached = cache.readValid(bundledIds, 12)
        assertNotNull(cached)
        assertEquals("\"etag-1\"", cached!!.second.etag)
    }

    @Test
    fun checksAtMostOncePerDayUnlessForced() = runBlocking {
        val f = FakeFetcher { CatalogAnswer(200, bundledJson, "\"e1\"") }
        updater(f).refresh(force = false)
        clock += 60 * 60 * 1000L // one hour later
        assertEquals(CatalogUpdateResult.NotDue, updater(f).refresh(force = false))
        assertEquals(1, f.etags.size)

        f.answer = { CatalogAnswer(304, "", "\"e1\"") }
        assertEquals(CatalogUpdateResult.NotModified, updater(f).refresh(force = true))
        assertEquals("\"e1\"", f.etags.last()) // conditional request

        clock += CatalogUpdater.INTERVAL_MS + 1
        assertEquals(CatalogUpdateResult.NotModified, updater(f).refresh(force = false))
    }

    @Test
    fun badDownloadKeepsTheCurrentCatalog() = runBlocking {
        updater(FakeFetcher { CatalogAnswer(200, bundledJson, "\"good\"") }).refresh(force = true)
        applied = null
        val r = updater(FakeFetcher { CatalogAnswer(200, "[]", "\"bad\"") }).refresh(force = true)
        assertTrue(r is CatalogUpdateResult.Failed)
        assertNull("rejected catalog must not be applied", applied)
        assertEquals("\"good\"", cache.readValid(bundledIds, 12)!!.second.etag) // previous copy still there
    }

    @Test
    fun networkProblemsChangeNothing() = runBlocking {
        val offline = FakeFetcher { CatalogAnswer(200, bundledJson, null) }
        assertTrue(updater(offline, online = false).refresh(force = true) is CatalogUpdateResult.Failed)
        assertTrue(offline.etags.isEmpty())

        assertTrue(updater(FakeFetcher { throw IOException("reset") }).refresh(force = true) is CatalogUpdateResult.Failed)
        assertTrue(updater(FakeFetcher { CatalogAnswer(500, "", null) }).refresh(force = true) is CatalogUpdateResult.Failed)
        assertNull(applied)
        assertNull(cache.readValid(bundledIds, 12))
    }

    @Test
    fun newAppVersionIgnoresOldDownloadedCatalog() = runBlocking {
        updater(FakeFetcher { CatalogAnswer(200, bundledJson, "\"old\"") }, version = 12).refresh(force = true)
        assertNull("a new APK must start from its own built-in catalog", cache.readValid(bundledIds, 13))
        val f = FakeFetcher { CatalogAnswer(200, bundledJson, "\"new\"") }
        updater(f, version = 13).refresh(force = false) // not "not due": cache belongs to another version
        assertEquals(listOf<String?>(null), f.etags) // and no stale ETag is sent
    }

    @Test
    fun corruptedCacheFallsBackToBuiltIn() = runBlocking {
        updater(FakeFetcher { CatalogAnswer(200, bundledJson, "\"e\"") }).refresh(force = true)
        File(dir, "stations.json").writeText("{ garbage")
        assertNull(cache.readValid(bundledIds, 12))
    }

    @Test
    fun onlyOneRefreshAtATime() = runBlocking {
        val inside = CountDownLatch(1)
        val release = CountDownLatch(1)
        val u = updater(FakeFetcher {
            inside.countDown()
            release.await(5, TimeUnit.SECONDS)
            CatalogAnswer(200, bundledJson, null)
        })
        val first = async(Dispatchers.IO) { u.refresh(force = true) }
        assertTrue(inside.await(5, TimeUnit.SECONDS))
        assertNull(u.refresh(force = true))
        release.countDown()
        assertTrue(first.await() is CatalogUpdateResult.Updated)
        assertFalse(dir.listFiles()!!.any { it.name.endsWith(".tmp") }) // atomic writes leave no temp files
    }
}
