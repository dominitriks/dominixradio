package com.bgradio.app.data.model

/** Display order of genres; genres missing from stations.json are simply not shown. */
object Categories {
    val ORDER = listOf(
        "Поп", "Хитове", "Рок", "Денс", "Българска музика", "Чалга",
        "Новини", "Спорт", "Джаз", "Класическа музика", "Други",
    )

    /** Short chip labels for long genre names. */
    fun shortLabel(genre: String): String = when (genre) {
        "Българска музика" -> "Българска"
        "Класическа музика" -> "Класическа"
        else -> genre
    }
}
