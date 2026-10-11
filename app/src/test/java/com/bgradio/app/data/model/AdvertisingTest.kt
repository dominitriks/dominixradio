package com.bgradio.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLDecoder

class AdvertisingTest {

    @Test
    fun packagesNamesPricesAndFeaturesAreExact() {
        assertEquals(
            listOf("Основен партньор" to "20 € / месец", "Премиум партньор" to "30 € / месец", "Годишен партньор" to "300 € / година"),
            Advertising.PACKAGES.map { it.name to it.price },
        )
        assertEquals(
            listOf("Име на бизнеса.", "Линк към сайта или бизнес страницата.", "Представяне в секцията за партньори."),
            Advertising.PACKAGES[0].features,
        )
        assertEquals(
            listOf("Лого на бизнеса.", "Име и линк към сайта или бизнес страницата.", "По-видимо представяне в секцията за партньори."),
            Advertising.PACKAGES[1].features,
        )
        assertEquals(
            listOf("Премиум представяне за 12 месеца.", "Лого, име и линк към бизнеса.", "Посочена крайна цена за целия период."),
            Advertising.PACKAGES[2].features,
        )
    }

    @Test
    fun noReachOrResultsArePromised() {
        val all = (Advertising.PACKAGES.flatMap { it.features } + Advertising.INTRO).joinToString(" ").lowercase()
        listOf("гарант", "показвани", "потребители", "продажби", "iban").forEach { assertFalse(it, all.contains(it)) }
    }

    @Test
    fun titleAndIntroAreExact() {
        assertEquals("Рекламирай в DOMINIX", Advertising.TITLE)
        assertEquals(
            "Достигнете до аудиторията на DOMINIX и подкрепете развитието на независимо българско радио приложение. " +
                "Предлагаме ясни условия и фиксирани цени без скрити такси.",
            Advertising.INTRO,
        )
    }

    @Test
    fun mailtoLinkCarriesAddressSubjectAndBody() {
        val uri = Advertising.mailtoUri()
        assertTrue(uri.startsWith("mailto:alwaysh685@gmail.com?subject="))
        assertFalse("spaces must be %20, not +", uri.contains('+'))
        val query = uri.substringAfter('?').split('&').associate { it.substringBefore('=') to URLDecoder.decode(it.substringAfter('='), "UTF-8") }
        assertEquals("Запитване за реклама в DOMINIX", query["subject"])
        assertEquals(
            "Здравейте,\n\nИнтересувам се от реклама в DOMINIX.\n\nЖелан пакет:\nИме на бизнеса:\nУебсайт:\nДопълнителна информация:\n\nПоздрави",
            query["body"],
        )
    }
}
