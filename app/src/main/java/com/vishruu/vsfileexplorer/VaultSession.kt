package com.vishruu.vsfileexplorer

object VaultSession {
    var isUnlocked: Boolean = false
    var password: String = ""

    fun unlock(pwd: String) {
        isUnlocked = true
        password = pwd
    }

    fun lock() {
        isUnlocked = false
        password = ""
    }
}