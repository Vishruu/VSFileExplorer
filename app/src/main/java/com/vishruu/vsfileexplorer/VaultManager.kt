package com.vishruu.vsfileexplorer

import android.content.Context
import android.os.Environment
import android.util.Base64
import java.io.File
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object VaultManager {
    private const val PREFS = "vs_vault_prefs"
    private const val KEY_HASH = "vault_hash"
    private const val KEY_SALT = "vault_salt"
    private const val SALT_SIZE = 16
    private const val ITERATIONS = 100_000
    private const val KEY_BITS = 256

    val CATEGORIES = listOf("Images", "Videos", "Audio", "Documents", "Other")

    fun vaultRoot(): File {
        val d = File(Environment.getExternalStorageDirectory(), "VSFileExplorer/Vault")
        if (!d.exists()) d.mkdirs()
        val nomedia = File(d, ".nomedia")
        if (!nomedia.exists()) nomedia.createNewFile()
        CATEGORIES.forEach { cat ->
            val cd = File(d, cat)
            if (!cd.exists()) cd.mkdirs()
        }
        val trash = File(d, ".trash")
        if (!trash.exists()) trash.mkdirs()
        CATEGORIES.forEach { cat ->
            val cd = File(trash, cat)
            if (!cd.exists()) cd.mkdirs()
        }
        return d
    }

    fun cacheRoot(): File {
        val d = File(Environment.getExternalStorageDirectory(), "VSFileExplorer/.vault_cache")
        if (!d.exists()) d.mkdirs()
        val nomedia = File(d, ".nomedia")
        if (!nomedia.exists()) nomedia.createNewFile()
        CATEGORIES.forEach { cat ->
            val cd = File(d, cat)
            if (!cd.exists()) cd.mkdirs()
        }
        val trash = File(d, ".trash")
        if (!trash.exists()) trash.mkdirs()
        CATEGORIES.forEach { cat ->
            val cd = File(trash, cat)
            if (!cd.exists()) cd.mkdirs()
        }
        return d
    }

    fun vaultCategoryDir(cat: String): File {
        val d = File(vaultRoot(), cat)
        if (!d.exists()) d.mkdirs()
        return d
    }

    fun cacheCategoryDir(cat: String): File {
        val d = File(cacheRoot(), cat)
        if (!d.exists()) d.mkdirs()
        return d
    }

    fun vaultTrashDir(cat: String): File {
        val d = File(vaultRoot(), ".trash/$cat")
        if (!d.exists()) d.mkdirs()
        return d
    }

    fun cacheTrashDir(cat: String): File {
        val d = File(cacheRoot(), ".trash/$cat")
        if (!d.exists()) d.mkdirs()
        return d
    }

    fun categoryForFile(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when {
            ext in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif") -> "Images"
            ext in listOf("mp4", "mkv", "avi", "mov", "webm", "3gp", "flv", "m4v") -> "Videos"
            ext in listOf("mp3", "wav", "aac", "ogg", "flac", "m4a", "opus", "wma") -> "Audio"
            ext in listOf("pdf", "doc", "docx", "txt", "ppt", "pptx", "xls", "xlsx", "odt", "rtf") -> "Documents"
            else -> "Other"
        }
    }

    // ===== EXPORT (Vault -> File Manager) =====
    fun exportDestinationFor(cat: String): File {
        val root = Environment.getExternalStorageDirectory()
        val folderName = when (cat) {
            "Images" -> "Pictures"
            "Videos" -> "Movies"
            "Audio" -> "Music"
            "Documents" -> "Documents"
            else -> "Download"
        }
        val d = File(root, folderName)
        if (!d.exists()) d.mkdirs()
        return d
    }

    fun exportFile(cat: String, file: File): File? {
        return try {
            val destDir = exportDestinationFor(cat)
            var dest = File(destDir, file.name)
            if (dest.exists()) {
                val base = file.name.substringBeforeLast('.', file.name)
                val ext = if (file.name.contains('.')) "." + file.name.substringAfterLast('.') else ""
                var i = 1
                while (dest.exists()) {
                    dest = File(destDir, "$base ($i)$ext")
                    i++
                }
            }
            file.copyTo(dest, overwrite = false)
            file.delete()
            dest
        } catch (e: Exception) { null }
    }

    private fun hashPassword(password: String, salt: ByteArray): String {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    fun isSetup(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.contains(KEY_HASH) && prefs.contains(KEY_SALT)
    }

    fun setupVault(context: Context, password: String): Boolean {
        return try {
            val salt = ByteArray(SALT_SIZE).also { SecureRandom().nextBytes(it) }
            val hash = hashPassword(password, salt)
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_HASH, hash)
                .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
                .apply()
            vaultRoot()
            cacheRoot()
            true
        } catch (e: Exception) { false }
    }

    fun verifyPassword(context: Context, password: String): Boolean {
        return try {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val storedHash = prefs.getString(KEY_HASH, null) ?: return false
            val saltB64 = prefs.getString(KEY_SALT, null) ?: return false
            val salt = Base64.decode(saltB64, Base64.NO_WRAP)
            hashPassword(password, salt) == storedHash
        } catch (e: Exception) { false }
    }

    fun changePassword(context: Context, oldPwd: String, newPwd: String): Boolean {
        if (!verifyPassword(context, oldPwd)) return false
        return setupVault(context, newPwd)
    }

    private fun decryptFolder(
        fromDir: File, toDir: File,
        password: String,
        onTick: () -> Unit,
        isCancelled: () -> Boolean
    ) {
        val files = fromDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(CryptoUtils.VS_EXTENSION) }
            ?: return
        for (encFile in files) {
            if (isCancelled()) return
            val tempVs = File(toDir, encFile.name)
            try {
                if (tempVs.exists()) tempVs.delete()
                if (!encFile.renameTo(tempVs)) {
                    encFile.copyTo(tempVs, overwrite = true)
                    encFile.delete()
                }
                CryptoUtils.decryptFile(
                    sourceFile = tempVs,
                    password = password,
                    onProgress = { },
                    isCancelled = isCancelled
                )
            } catch (e: Exception) {
                try {
                    val back = File(fromDir, tempVs.name)
                    if (!back.exists() && tempVs.exists()) tempVs.renameTo(back)
                } catch (_: Exception) {}
            }
            onTick()
        }
    }

    private fun encryptFolder(
        fromDir: File, toDir: File,
        password: String,
        onTick: () -> Unit,
        isCancelled: () -> Boolean
    ) {
        val files = fromDir.listFiles()
            ?.filter { it.isFile && !it.name.endsWith(CryptoUtils.VS_EXTENSION) && it.name != ".nomedia" }
            ?: return
        for (plainFile in files) {
            if (isCancelled()) return
            try {
                val outVs = CryptoUtils.encryptFile(
                    sourceFile = plainFile,
                    password = password,
                    encryptFileName = true,
                    onProgress = { },
                    isCancelled = isCancelled
                )
                val dest = File(toDir, outVs.name)
                if (dest.exists()) dest.delete()
                if (!outVs.renameTo(dest)) {
                    outVs.copyTo(dest, overwrite = true)
                    outVs.delete()
                }
            } catch (e: Exception) {}
            onTick()
        }
    }

    fun unlockVault(
        password: String,
        onProgress: (Int) -> Unit,
        isCancelled: () -> Boolean
    ): Int {
        val vault = vaultRoot()
        val cache = cacheRoot()

        var total = 0
        CATEGORIES.forEach { cat ->
            total += File(vault, cat).listFiles()
                ?.count { it.isFile && it.name.endsWith(CryptoUtils.VS_EXTENSION) } ?: 0
            total += File(vault, ".trash/$cat").listFiles()
                ?.count { it.isFile && it.name.endsWith(CryptoUtils.VS_EXTENSION) } ?: 0
        }
        if (total == 0) { onProgress(100); return 0 }

        var processed = 0
        val tick = { processed++; onProgress((processed * 100) / total) }

        CATEGORIES.forEach { cat ->
            decryptFolder(File(vault, cat), cacheCategoryDir(cat), password, tick, isCancelled)
        }
        CATEGORIES.forEach { cat ->
            decryptFolder(File(vault, ".trash/$cat"), cacheTrashDir(cat), password, tick, isCancelled)
        }
        onProgress(100)
        return total
    }

    fun lockVault(
        password: String,
        onProgress: (Int) -> Unit,
        isCancelled: () -> Boolean
    ): Int {
        val vault = vaultRoot()
        val cache = cacheRoot()

        var total = 0
        CATEGORIES.forEach { cat ->
            total += File(cache, cat).listFiles()
                ?.count { it.isFile && it.name != ".nomedia" } ?: 0
            total += File(cache, ".trash/$cat").listFiles()
                ?.count { it.isFile && it.name != ".nomedia" } ?: 0
        }
        if (total == 0) { onProgress(100); return 0 }

        var processed = 0
        val tick = { processed++; onProgress((processed * 100) / total) }

        CATEGORIES.forEach { cat ->
            encryptFolder(cacheCategoryDir(cat), vaultCategoryDir(cat), password, tick, isCancelled)
        }
        CATEGORIES.forEach { cat ->
            encryptFolder(cacheTrashDir(cat), vaultTrashDir(cat), password, tick, isCancelled)
        }
        onProgress(100)
        return total
    }

    fun countInCategory(cat: String): Int {
        return cacheCategoryDir(cat).listFiles()
            ?.count { it.isFile && it.name != ".nomedia" } ?: 0
    }

    fun countInTrash(): Int {
        return CATEGORIES.sumOf { cat ->
            cacheTrashDir(cat).listFiles()?.count { it.isFile && it.name != ".nomedia" } ?: 0
        }
    }

    fun moveToTrash(cat: String, file: File): Boolean {
        return try {
            val dest = File(cacheTrashDir(cat), file.name)
            if (dest.exists()) dest.delete()
            file.renameTo(dest) || run {
                file.copyTo(dest, overwrite = true); file.delete()
            }
        } catch (e: Exception) { false }
    }

    fun restoreFromTrash(cat: String, file: File): Boolean {
        return try {
            val dest = File(cacheCategoryDir(cat), file.name)
            if (dest.exists()) dest.delete()
            file.renameTo(dest) || run {
                file.copyTo(dest, overwrite = true); file.delete()
            }
        } catch (e: Exception) { false }
    }

    fun deleteFromTrash(file: File): Boolean {
        return try { file.delete() } catch (e: Exception) { false }
    }

    fun clearCache() {
        cacheRoot().listFiles()?.forEach {
            if (it.name != ".nomedia") it.deleteRecursively()
        }
    }
}