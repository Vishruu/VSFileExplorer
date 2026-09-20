package com.vishruu.vsfileexplorer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object GoogleDriveHub {
    var isOpen by mutableStateOf(false)
        private set

    fun open() { isOpen = true }
    fun close() { isOpen = false }
}