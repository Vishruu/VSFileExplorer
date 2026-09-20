package com.vishruu.vsfileexplorer

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {
    private const val MAX_LOGS = 200
    private val logs = mutableListOf<String>()

    fun log(tag: String, message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        synchronized(logs) {
            logs.add(0, "[$time] [$tag] $message")
            while (logs.size > MAX_LOGS) logs.removeAt(logs.size - 1)
        }
    }

    fun getLogs(): List<String> = synchronized(logs) { logs.toList() }

    fun clear() {
        synchronized(logs) { logs.clear() }
    }
}