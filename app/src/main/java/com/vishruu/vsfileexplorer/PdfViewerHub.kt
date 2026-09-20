package com.vishruu.vsfileexplorer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

object PdfViewerHub {
    var pendingFile by mutableStateOf<File?>(null)
        private set

    fun open(file: File) {
        pendingFile = file
    }

    fun consume() {
        pendingFile = null
    }
}