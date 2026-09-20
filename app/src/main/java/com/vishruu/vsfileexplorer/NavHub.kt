package com.vishruu.vsfileexplorer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object NavHub {
    private var _goHomeTrigger by mutableStateOf(0L)

    val goHomeTrigger: Long
        get() = _goHomeTrigger

    fun goHome() {
        _goHomeTrigger = System.currentTimeMillis()
    }
}