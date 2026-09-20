package com.vishruu.vsfileexplorer

import android.content.Context

object RecentFilesStore {
    private const val PREFS = "vs_recent_files"
    private const val KEY = "recent"
    private const val MAX_ITEMS = 50
    private const val SEPARATOR = "||"

    fun getRecents(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY, "") ?: ""
        if (raw.isEmpty()) return emptyList()
        return raw.split(SEPARATOR).filter { it.isNotBlank() }
    }

    fun addRecent(context: Context, path: String) {
        val current = getRecents(context).toMutableList()
        // Duplicate hatao (front pe le aao)
        current.removeAll { it == path }
        current.add(0, path)
        // Limit
        while (current.size > MAX_ITEMS) current.removeAt(current.size - 1)
        save(context, current)
    }

    fun removeRecent(context: Context, path: String) {
        val current = getRecents(context).toMutableList()
        current.removeAll { it == path }
        save(context, current)
    }

    fun clearAll(context: Context) {
        save(context, emptyList())
    }

    private fun save(context: Context, list: List<String>) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY, list.joinToString(SEPARATOR)).apply()
    }
}