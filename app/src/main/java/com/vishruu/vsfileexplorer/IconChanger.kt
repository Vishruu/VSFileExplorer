package com.vishruu.vsfileexplorer

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object IconChanger {
    private const val PREFS = "vs_icon_prefs"
    private const val KEY = "current_icon"

    data class IconVariant(
        val id: String,
        val label: String,
        val aliasName: String,
        val colorHex: Long
    )

    val ICONS = listOf(
        IconVariant("default", "Default", "com.vishruu.vsfileexplorer.MainActivityDefault", 0xFF9C27B0),
        IconVariant("blue", "Blue", "com.vishruu.vsfileexplorer.MainActivityBlue", 0xFF2196F3),
        IconVariant("green", "Green", "com.vishruu.vsfileexplorer.MainActivityGreen", 0xFF4CAF50),
        IconVariant("orange", "Orange", "com.vishruu.vsfileexplorer.MainActivityOrange", 0xFFFF9800),
        IconVariant("red", "Red", "com.vishruu.vsfileexplorer.MainActivityRed", 0xFFE53935)
    )

    fun getCurrentId(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, "default") ?: "default"

    fun applyIcon(context: Context, variant: IconVariant): Boolean {
        return try {
            val pm = context.packageManager

            // Disable all first
            ICONS.forEach { icon ->
                try {
                    pm.setComponentEnabledSetting(
                        ComponentName(context.packageName, icon.aliasName),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                } catch (_: Exception) {}
            }

            // Enable selected
            pm.setComponentEnabledSetting(
                ComponentName(context.packageName, variant.aliasName),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY, variant.id).apply()

            true
        } catch (e: Exception) {
            false
        }
    }
}