package com.vishruu.vsfileexplorer

import android.content.Intent
import androidx.compose.runtime.mutableStateOf

object ShortcutRouter {
    private val _pendingAction = mutableStateOf<String?>(null)

    val pendingAction: String?
        get() = _pendingAction.value

    fun handleIntent(intent: Intent?) {
        intent ?: return
        when (intent.action) {
            "com.vishruu.vsfileexplorer.SHORTCUT_SEARCH" -> _pendingAction.value = "search"
            "com.vishruu.vsfileexplorer.SHORTCUT_VAULT" -> _pendingAction.value = "vault"
            "com.vishruu.vsfileexplorer.SHORTCUT_NEW_FILE" -> _pendingAction.value = "camera"
            "com.vishruu.vsfileexplorer.SHORTCUT_SETTINGS" -> _pendingAction.value = "settings"
        }
    }

    fun consume() {
        _pendingAction.value = null
    }
}