package com.vishruu.vsfileexplorer

import android.os.Environment
import android.util.Base64
import java.io.File

data class SecureNote(
    val id: String,
    val title: String,
    val body: String,
    val timestamp: Long
)

object SecureNotesManager {
    private const val FILE_NAME = "notes.vs"

    fun notesDir(): File {
        val d = File(Environment.getExternalStorageDirectory(), "VSFileExplorer/SecureNotes")
        if (!d.exists()) d.mkdirs()
        val nomedia = File(d, ".nomedia")
        if (!nomedia.exists()) nomedia.createNewFile()
        return d
    }

    fun notesFile(): File = File(notesDir(), FILE_NAME)

    fun loadNotes(password: String): List<SecureNote>? {
        val file = notesFile()
        if (!file.exists()) return emptyList()
        val tempCopy = File(notesDir(), "load_tmp.vs")
        return try {
            if (tempCopy.exists()) tempCopy.delete()
            file.copyTo(tempCopy, overwrite = true)
            val decrypted = CryptoUtils.decryptFile(
                sourceFile = tempCopy,
                password = password,
                onProgress = { },
                isCancelled = { false }
            )
            val text = decrypted.readText()
            decrypted.delete()
            parseNotes(text)
        } catch (e: Exception) {
            try { tempCopy.delete() } catch (_: Exception) {}
            null
        }
    }

    fun saveNotes(notes: List<SecureNote>, password: String): Boolean {
        return try {
            val text = serializeNotes(notes)
            val tempTxt = File(notesDir(), "save_tmp.txt")
            if (tempTxt.exists()) tempTxt.delete()
            tempTxt.writeText(text)
            val encrypted = CryptoUtils.encryptFile(
                sourceFile = tempTxt,
                password = password,
                encryptFileName = false,
                onProgress = { },
                isCancelled = { false }
            )
            val finalFile = notesFile()
            if (finalFile.exists()) finalFile.delete()
            if (!encrypted.renameTo(finalFile)) {
                encrypted.copyTo(finalFile, overwrite = true)
                encrypted.delete()
            }
            true
        } catch (e: Exception) { false }
    }

    private fun serializeNotes(notes: List<SecureNote>): String {
        return notes.joinToString("\n") { n ->
            "${n.id}|${n.timestamp}|${b64(n.title)}|${b64(n.body)}"
        }
    }

    private fun parseNotes(text: String): List<SecureNote> {
        if (text.isBlank()) return emptyList()
        return text.split("\n").mapNotNull { line ->
            val parts = line.split("|", limit = 4)
            if (parts.size != 4) return@mapNotNull null
            try {
                SecureNote(
                    id = parts[0],
                    timestamp = parts[1].toLongOrNull() ?: 0L,
                    title = unB64(parts[2]),
                    body = unB64(parts[3])
                )
            } catch (e: Exception) { null }
        }.sortedByDescending { it.timestamp }
    }

    private fun b64(s: String): String =
        Base64.encodeToString(s.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

    private fun unB64(s: String): String =
        String(Base64.decode(s, Base64.NO_WRAP), Charsets.UTF_8)
}