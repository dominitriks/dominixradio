package com.bgradio.app.data.model

import java.net.URLEncoder

/** One advertising package. Prices are starting offers; nothing about reach is promised. */
data class AdPackage(
    val name: String,
    val price: String,
    val features: List<String>,
)

/** Content of the "Рекламирай в DOMINIX" page. Pure — unit tested. */
object Advertising {

    const val TITLE = "Рекламирай в DOMINIX"

    const val INTRO = "Достигнете до аудиторията на DOMINIX и подкрепете развитието на независимо българско " +
        "радио приложение. Предлагаме ясни условия и фиксирани цени без скрити такси."

    /** Shown under the packages so no reach or results are implied. */
    const val NOTE = "Цените са начални предложения. Не обещаваме конкретен брой показвания, потребители или продажби."

    val PACKAGES = listOf(
        AdPackage(
            name = "Основен партньор",
            price = "20 € / месец",
            features = listOf(
                "Име на бизнеса.",
                "Линк към сайта или бизнес страницата.",
                "Представяне в секцията за партньори.",
            ),
        ),
        AdPackage(
            name = "Премиум партньор",
            price = "30 € / месец",
            features = listOf(
                "Лого на бизнеса.",
                "Име и линк към сайта или бизнес страницата.",
                "По-видимо представяне в секцията за партньори.",
            ),
        ),
        AdPackage(
            name = "Годишен партньор",
            price = "300 € / година",
            features = listOf(
                "Премиум представяне за 12 месеца.",
                "Лого, име и линк към бизнеса.",
                "Посочена крайна цена за целия период.",
            ),
        ),
    )

    const val EMAIL = "alwaysh685@gmail.com"
    const val SUBJECT = "Запитване за реклама в DOMINIX"
    const val BODY = "Здравейте,\n\n" +
        "Интересувам се от реклама в DOMINIX.\n\n" +
        "Желан пакет:\n" +
        "Име на бизнеса:\n" +
        "Уебсайт:\n" +
        "Допълнителна информация:\n\n" +
        "Поздрави"

    /**
     * mailto: link with subject and body (RFC 6068: spaces as %20, not '+'), so mail apps that
     * ignore Intent extras still get the pre-filled text.
     */
    fun mailtoUri(): String =
        "mailto:$EMAIL?subject=${encode(SUBJECT)}&body=${encode(BODY)}"

    private fun encode(s: String): String = URLEncoder.encode(s, "UTF-8").replace("+", "%20")
}
