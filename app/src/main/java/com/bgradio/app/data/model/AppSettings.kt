package com.bgradio.app.data.model

enum class ThemeMode { DARK, LIGHT, SYSTEM }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val wifiOnly: Boolean = false,
    val autoReconnect: Boolean = true,
)
