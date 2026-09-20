package com.vishruu.vsfileexplorer

import android.content.Context

object SettingsManager {
    private const val PREFS = "vs_settings_prefs"

    // Vault
    private const val KEY_VAULT_LOCK_DELAY = "vault_lock_delay"
    // Shredder
    private const val KEY_SHRED_PASSES = "shred_passes"
    // Network
    private const val KEY_NETWORK_DOWNLOAD = "network_download"
    // Cache
    private const val KEY_CACHE_THUMBS = "cache_thumbs"
    // Theme
    private const val KEY_AMOLED = "amoled"
    // Defaults
    private const val KEY_DEFAULT_VIEW_MODE = "default_view_mode"
    private const val KEY_DEFAULT_SORT_MODE = "default_sort_mode"
    private const val KEY_DEFAULT_SHOW_HIDDEN = "default_show_hidden"
    // Safety
    private const val KEY_CONFIRM_DELETE = "confirm_delete"
    private const val KEY_RECYCLE_AUTO_DELETE_DAYS = "recycle_auto_delete_days"
    // Performance
    private const val KEY_THUMB_QUALITY = "thumb_quality"
    // First launch
    private const val KEY_FIRST_LAUNCH_DONE = "first_launch_done"

    // Theme mode: "dark" or "light"
    private const val KEY_THEME_MODE = "theme_mode"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ========== VAULT ==========
    fun getVaultLockDelay(context: Context): Int =
        prefs(context).getInt(KEY_VAULT_LOCK_DELAY, 0)

    fun setVaultLockDelay(context: Context, seconds: Int) {
        prefs(context).edit().putInt(KEY_VAULT_LOCK_DELAY, seconds).apply()
    }

    fun vaultLockDelayLabel(seconds: Int): String = when (seconds) {
        0 -> "Instant"
        30 -> "30 seconds"
        60 -> "1 minute"
        300 -> "5 minutes"
        900 -> "15 minutes"
        -1 -> "Never"
        else -> "$seconds seconds"
    }

    // ========== SHREDDER ==========
    fun getShredPasses(context: Context): Int =
        prefs(context).getInt(KEY_SHRED_PASSES, 3)

    fun setShredPasses(context: Context, passes: Int) {
        prefs(context).edit().putInt(KEY_SHRED_PASSES, passes).apply()
    }

    fun shredPassesLabel(passes: Int): String = when (passes) {
        1 -> "1 pass (fast)"
        3 -> "3 passes (recommended)"
        7 -> "7 passes (maximum)"
        else -> "$passes passes"
    }

    // ========== NETWORK ==========
    fun getNetworkDownloadFolder(context: Context): String =
        prefs(context).getString(KEY_NETWORK_DOWNLOAD, "Download") ?: "Download"

    fun setNetworkDownloadFolder(context: Context, folder: String) {
        prefs(context).edit().putString(KEY_NETWORK_DOWNLOAD, folder).apply()
    }

    // ========== CACHE ==========
    fun isThumbCacheEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_CACHE_THUMBS, true)

    fun setThumbCacheEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_CACHE_THUMBS, enabled).apply()
    }

    // ========== THEME ==========
    fun isAmoled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AMOLED, true)

    fun setAmoled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AMOLED, enabled).apply()
    }

    // ========== DEFAULTS ==========
    fun getDefaultViewMode(context: Context): String =
        prefs(context).getString(KEY_DEFAULT_VIEW_MODE, "MEDIUM_ICON") ?: "MEDIUM_ICON"

    fun setDefaultViewMode(context: Context, mode: String) {
        prefs(context).edit().putString(KEY_DEFAULT_VIEW_MODE, mode).apply()
    }

    fun getDefaultSortMode(context: Context): String =
        prefs(context).getString(KEY_DEFAULT_SORT_MODE, "NAME_ASC") ?: "NAME_ASC"

    fun setDefaultSortMode(context: Context, mode: String) {
        prefs(context).edit().putString(KEY_DEFAULT_SORT_MODE, mode).apply()
    }

    fun getDefaultShowHidden(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DEFAULT_SHOW_HIDDEN, false)

    fun setDefaultShowHidden(context: Context, show: Boolean) {
        prefs(context).edit().putBoolean(KEY_DEFAULT_SHOW_HIDDEN, show).apply()
    }

    fun viewModeLabel(mode: String): String = when (mode) {
        "LARGE_ICON" -> "Large Icon"
        "MEDIUM_ICON" -> "Medium Icon"
        "SMALL_ICON" -> "Small Icon"
        "LARGE_LIST" -> "Large List"
        "MEDIUM_LIST" -> "Medium List"
        "SMALL_LIST" -> "Small List"
        "LARGE_DETAIL" -> "Large Detail"
        "MEDIUM_DETAIL" -> "Medium Detail"
        "SMALL_DETAIL" -> "Small Detail"
        else -> mode
    }

    fun sortModeLabel(mode: String): String = when (mode) {
        "NAME_ASC" -> "Name (A → Z)"
        "NAME_DESC" -> "Name (Z → A)"
        "SIZE_ASC" -> "Size (Small → Large)"
        "SIZE_DESC" -> "Size (Large → Small)"
        "DATE_ASC" -> "Date (Old → New)"
        "DATE_DESC" -> "Date (New → Old)"
        "TYPE" -> "Type"
        else -> mode
    }

    // ========== SAFETY ==========
    fun isConfirmDeleteEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_CONFIRM_DELETE, true)

    fun setConfirmDeleteEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_CONFIRM_DELETE, enabled).apply()
    }

    fun getRecycleAutoDeleteDays(context: Context): Int =
        prefs(context).getInt(KEY_RECYCLE_AUTO_DELETE_DAYS, 30)

    fun setRecycleAutoDeleteDays(context: Context, days: Int) {
        prefs(context).edit().putInt(KEY_RECYCLE_AUTO_DELETE_DAYS, days).apply()
    }

    fun recycleAutoDeleteLabel(days: Int): String = when (days) {
        0 -> "Never"
        7 -> "7 days"
        15 -> "15 days"
        30 -> "30 days"
        60 -> "60 days"
        else -> "$days days"
    }

    // ========== PERFORMANCE ==========
    fun getThumbQuality(context: Context): Int =
        prefs(context).getInt(KEY_THUMB_QUALITY, 2)

    fun setThumbQuality(context: Context, quality: Int) {
        prefs(context).edit().putInt(KEY_THUMB_QUALITY, quality).apply()
    }

    fun thumbQualityLabel(q: Int): String = when (q) {
        1 -> "Low (fast)"
        2 -> "Medium"
        3 -> "High (slow)"
        else -> "Medium"
    }

    // ========== FIRST LAUNCH ==========
    fun isFirstLaunchDone(context: Context): Boolean =
        prefs(context).getBoolean(KEY_FIRST_LAUNCH_DONE, false)

    fun setFirstLaunchDone(context: Context, done: Boolean) {
        prefs(context).edit().putBoolean(KEY_FIRST_LAUNCH_DONE, done).apply()
    }

    // ========== DEBUG ==========
    private const val KEY_DEBUG_MODE = "debug_mode"

    fun isDebugMode(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DEBUG_MODE, false)

    fun setDebugMode(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_DEBUG_MODE, enabled).apply()
    }

    // ========== EXPORT / IMPORT ==========
    fun exportToJson(context: Context): String {
        val all = prefs(context).all
        val sb = StringBuilder()
        sb.append("{\n")
        val entries = all.entries.toList()
        entries.forEachIndexed { i, e ->
            val valueStr = when (val v = e.value) {
                is String -> "\"${escapeJson(v)}\""
                is Boolean -> v.toString()
                is Int -> v.toString()
                is Long -> v.toString()
                is Float -> v.toString()
                else -> "\"${escapeJson(v.toString())}\""
            }
            sb.append("  \"${e.key}\": $valueStr")
            if (i < entries.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("}")
        return sb.toString()
    }

    private fun escapeJson(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")

    fun importFromJson(context: Context, json: String): Boolean {
        return try {
            val editor = prefs(context).edit()
            // Simple parser: extract key-value pairs
            val regex = Regex("\"([^\"]+)\"\\s*:\\s*(\"[^\"]*\"|true|false|-?\\d+(?:\\.\\d+)?)")
            regex.findAll(json).forEach { match ->
                val key = match.groupValues[1]
                var value = match.groupValues[2]
                when {
                    value == "true" || value == "false" -> {
                        editor.putBoolean(key, value.toBoolean())
                    }
                    value.startsWith("\"") && value.endsWith("\"") -> {
                        value = value.substring(1, value.length - 1)
                            .replace("\\n", "\n")
                            .replace("\\\"", "\"")
                            .replace("\\\\", "\\")
                        editor.putString(key, value)
                    }
                    value.contains(".") -> {
                        editor.putFloat(key, value.toFloatOrNull() ?: 0f)
                    }
                    else -> {
                        editor.putInt(key, value.toIntOrNull() ?: 0)
                    }
                }
            }
            editor.apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    // ========== RESET ==========
    fun resetAll(context: Context) {
        prefs(context).edit().clear().apply()
    }

    fun exportSettings(context: Context): String {
        val p = prefs(context).all
        return p.entries.joinToString("\n") { "${it.key}=${it.value}" }
    }

    // ========== THEME MODE ==========  ← YEH NAYA
    fun getThemeMode(context: Context): String =
        prefs(context).getString(KEY_THEME_MODE, "dark") ?: "dark"

    fun setThemeMode(context: Context, mode: String) {
        prefs(context).edit().putString(KEY_THEME_MODE, mode).apply()
    }
}


