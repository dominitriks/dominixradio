package com.bgradio.app.utils

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * What the system allows this app to do in the background. Media playback with a foreground
 * service normally works without exemptions, but:
 *  - "Background restricted" (Android 9+, set by the user or the OEM) blocks it completely;
 *  - some vendors (Xiaomi/HyperOS, Huawei, ...) also need Autostart / "No restrictions".
 */
data class BackgroundStatus(
    val backgroundRestricted: Boolean,
    val ignoringBatteryOptimizations: Boolean,
    val isXiaomi: Boolean,
) {
    companion object {
        fun read(context: Context): BackgroundStatus {
            val am = context.getSystemService(ActivityManager::class.java)
            val pm = context.getSystemService(PowerManager::class.java)
            return BackgroundStatus(
                backgroundRestricted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && am?.isBackgroundRestricted == true,
                ignoringBatteryOptimizations = pm?.isIgnoringBatteryOptimizations(context.packageName) == true,
                isXiaomi = Build.MANUFACTURER.lowercase() in setOf("xiaomi", "redmi", "poco"),
            )
        }

        /** App info page (battery / background settings live there on all vendors). */
        fun openAppSettings(context: Context) {
            launchFirst(
                context,
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")),
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
            )
        }

        /** Xiaomi "Autostart" screen; falls back to the app page on other ROM versions. */
        fun openXiaomiAutostart(context: Context) {
            launchFirst(
                context,
                Intent().setComponent(
                    ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
                ),
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")),
            )
        }

        private fun launchFirst(context: Context, vararg intents: Intent) {
            for (intent in intents) {
                try {
                    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    return
                } catch (_: Exception) {
                    // not available on this device, try the next one
                }
            }
        }
    }
}
