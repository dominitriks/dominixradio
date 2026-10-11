package com.bgradio.app.data.partners

import com.bgradio.app.data.catalog.CatalogAnswer
import com.bgradio.app.data.catalog.CatalogCache
import com.bgradio.app.data.catalog.CatalogFetcher
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException

class PartnersTest {

    private val json = """[
        {"id":"shop","name":"Магазин Слънце","url":"https://www.slance.bg/oferti","tier":"basic","until":"2026-12-31"},
        {"id":"cafe","name":"Кафе Радио","url":"https://caferadio.bg","logoUrl":"https://caferadio.bg/logo.png","tier":"premium","until":"2027-10-31"},
        {"id":"old","name":"Изтекъл","url":"https://old.bg","tier":"premium","until":"2026-01-31"},
        {"id":"forever","name":"Без срок","url":"https://forever.bg"},
        {"id":"http","name":"Без https","url":"http://plain.bg"},
        {"id":"evil","name":"Схема","url":"intent://open#Intent;end"},
        {"id":"js","name":"JS","url":"javascript:alert(1)"},
        {"id":"Bad Id","name":"Лош id","url":"https://x.bg"},
        {"id":"nologo","name":"Лого по http","url":"https://n.bg","logoUrl":"http://n.bg/l.png","tier":"PREMIUM","until":"31.12.2027"}
    ]"""

    @Test
    fun onlySafeWellFormedEntriesAreKept() {
        val all = PartnersParser.parse(json)!!
        assertEquals(listOf("shop", "cafe", "old", "forever", "nologo"), all.map { it.id })
        val nologo = all.first { it.id == "nologo" }
        assertNull("http logo is not loaded", nologo.logoUrl)
        assertNull("invalid date means no end date", nologo.until)
        assertEquals(PartnerTier.PREMIUM, nologo.tier) // case-insensitive
        assertEquals(PartnerTier.BASIC, all.first { it.id == "forever" }.tier) // default
        assertEquals("slance.bg", all.first { it.id == "shop" }.domain)
    }

    @Test
    fun expiredPartnersDisappearPremiumComesFirst() {
        val active = PartnersParser.active(PartnersParser.parse(json)!!, today = "2026-10-12")
        assertEquals(listOf("cafe", "nologo", "forever", "shop"), active.map { it.id })
        // last day is still shown, the day after it is not
        assertTrue(PartnersParser.active(PartnersParser.parse(json)!!, "2026-12-31").any { it.id == "shop" })
        assertFalse(PartnersParser.active(PartnersParser.parse(json)!!, "2027-01-01").any { it.id == "shop" })
    }

    @Test
    fun unusableFilesAreRejectedButEmptyListIsValid() {
        assertNull(PartnersParser.parse("<html>"))
        assertNull(PartnersParser.parse("{}"))
        assertNull(PartnersParser.parse(" ".repeat(PartnersParser.MAX_BYTES + 1)))
        assertEquals(emptyList<Partner>(), PartnersParser.parse("[]"))
        assertEquals("[]", File("src/main/assets/partners.json").readText().trim()) // ships empty
    }

    // ---- Repository ---------------------------------------------------------------------

    private var clock = 1_000_000_000L
    private val dir = kotlin.io.path.createTempDirectory("partners").toFile()

    private fun repo(online: Boolean = true, fetch: CatalogFetcher) = PartnersRepository(
        cache = CatalogCache(dir, "partners.json"),
        bundled = { "[]" },
        isOnline = { online },
        fetcher = fetch,
        now = { clock },
        versionCode = 13,
    )

    @Test
    fun startsEmptyThenAppliesDownloadedList() = runBlocking {
        val r = repo { CatalogAnswer(200, json, "\"p1\"") }
        r.ensureLoaded()
        assertTrue(r.all.value.isEmpty())
        assertEquals(PartnersUpdateResult.Updated(5), r.refresh(force = false))
        assertEquals(5, r.all.value.size)
        // a new process reads the cached copy without network
        val again = repo(online = false) { throw IOException() }
        again.ensureLoaded()
        assertEquals(5, again.all.value.size)
    }

    @Test
    fun removingAllPartnersOnGitHubIsApplied() = runBlocking {
        repo { CatalogAnswer(200, json, "\"p1\"") }.refresh(force = true)
        val r = repo { CatalogAnswer(200, "[]", "\"p2\"") }
        assertEquals(PartnersUpdateResult.Updated(0), r.refresh(force = true))
        assertTrue(r.all.value.isEmpty())
    }

    @Test
    fun badDownloadKeepsCurrentListAndChecksOncePerDay() = runBlocking {
        repo { CatalogAnswer(200, json, "\"p1\"") }.refresh(force = true)
        val broken = repo { CatalogAnswer(200, "not json", null) }
        assertTrue(broken.refresh(force = true) is PartnersUpdateResult.Failed)
        assertEquals(5, broken.all.value.size)

        var calls = 0
        val counting = repo { calls++; CatalogAnswer(304, "", "\"p1\"") }
        assertEquals(PartnersUpdateResult.NotDue, counting.refresh(force = false))
        assertEquals(0, calls)
        assertEquals(PartnersUpdateResult.NotModified, counting.refresh(force = true))
        assertEquals(1, calls)
    }

    @Test
    fun offlineChangesNothing() = runBlocking {
        val r = repo(online = false) { CatalogAnswer(200, json, null) }
        assertTrue(r.refresh(force = true) is PartnersUpdateResult.Failed)
        assertTrue(r.all.value.isEmpty())
    }
}
