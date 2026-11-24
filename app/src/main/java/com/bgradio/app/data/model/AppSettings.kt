package com.bgradio.app.data.model

enum class ThemeMode { DARK, LIGHT, SYSTEM, CHRISTMAS }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    /** Falling snow — only shown with the Christmas theme. */
    val snow: Boolean = true,
    val wifiOnly: Boolean = false,
    val autoReconnect: Boolean = true,
)
