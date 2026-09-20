package com.vishruu.vsfileexplorer

import android.content.Context
import android.content.SharedPreferences

object BookmarksStore {
    private const val PREFS = "vs_bookmarks"
    private const val KEY = "bookmarks"

    fun getBookmarks(context: Context): List<String> {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY, "") ?: ""
        if (raw.isEmpty()) return emptyList()
        return raw.split("|").filter { it.isNotBlank() }
    }

    fun addBookmark(context: Context, path: String) {
        val current = getBookmarks(context).toMutableList()
        if (!current.contains(path)) {
            current.add(path)
            save(context, current)
        }
    }

    fun removeBookmark(context: Context, path: String) {
        val current = getBookmarks(context).toMutableList()
        current.remove(path)
        save(context, current)
    }

    private fun save(context: Context, list: List<String>) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY, list.joinToString("|")).apply()
    }
}