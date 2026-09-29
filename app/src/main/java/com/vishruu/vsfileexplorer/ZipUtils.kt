package com.vishruu.vsfileexplorer

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import net.lingala.zip4j.ZipFile as Zip4jFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod

fun zipFiles(
    sources: List<File>,
    outputZip: File,
    onProgress: (Int) -> Unit,
    isCancelled: () -> Boolean
) {
    fun totalSize(f: File): Long {
        if (f.isFile) return f.length()
        var t = 0L
        f.listFiles()?.forEach { t += totalSize(it) }
        return t
    }

    val total = sources.sumOf { totalSize(it) }.coerceAtLeast(1L)
    var processed = 0L

    // Media extensions - already compressed, don't re-compress
    val alreadyCompressed = setOf(
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif",
        "mp4", "mkv", "avi", "mov", "webm", "3gp", "flv", "m4v",
        "mp3", "wav", "aac", "ogg", "flac", "m4a", "opus", "wma",
        "zip", "rar", "7z", "tar", "gz", "bz2",
        "apk", "xapk", "apks"
    )

    ZipOutputStream(FileOutputStream(outputZip)).use { zos ->
        sources.forEach { src ->
            if (src.isDirectory) {
                zipDirSmart(src, src.name, zos, alreadyCompressed,
                    onProgressBytes = { b ->
                        processed += b
                        onProgress(((processed * 100) / total).toInt().coerceIn(0, 100))
                    },
                    isCancelled = isCancelled)
            } else {
                // Single file — check extension
                val ext = src.extension.lowercase()
                val level = if (ext in alreadyCompressed) java.util.zip.Deflater.NO_COMPRESSION
                else java.util.zip.Deflater.BEST_SPEED
                zos.setLevel(level)

                val entry = ZipEntry(src.name)
                zos.putNextEntry(entry)
                FileInputStream(src).use { fis ->
                    val buf = ByteArray(64 * 1024)
                    var read: Int
                    while (fis.read(buf).also { read = it } != -1) {
                        if (isCancelled()) {
                            zos.closeEntry()
                            throw Exception("Cancelled")
                        }
                        zos.write(buf, 0, read)
                        processed += read
                        onProgress(((processed * 100) / total).toInt().coerceIn(0, 100))
                    }
                }
                zos.closeEntry()
            }
        }
    }
}

fun zipDirSmart(
    dir: File,
    basePath: String,
    zos: ZipOutputStream,
    alreadyCompressed: Set<String>,
    onProgressBytes: (Long) -> Unit,
    isCancelled: () -> Boolean
) {
    val children = dir.listFiles() ?: return
    if (children.isEmpty()) {
        zos.setLevel(java.util.zip.Deflater.BEST_SPEED)
        zos.putNextEntry(ZipEntry("$basePath/"))
        zos.closeEntry()
        return
    }
    for (child in children) {
        if (isCancelled()) throw Exception("Cancelled")
        val entryPath = "$basePath/${child.name}"
        if (child.isDirectory) {
            zipDirSmart(child, entryPath, zos, alreadyCompressed, onProgressBytes, isCancelled)
        } else {
            val ext = child.extension.lowercase()
            // Media files → no compression (fast)
            // Text files → compression (slow but smaller)
            val level = if (ext in alreadyCompressed)
                java.util.zip.Deflater.NO_COMPRESSION
            else
                java.util.zip.Deflater.BEST_SPEED
            zos.setLevel(level)

            val entry = ZipEntry(entryPath)
            zos.putNextEntry(entry)
            FileInputStream(child).use { fis ->
                val buf = ByteArray(64 * 1024)
                var read: Int
                while (fis.read(buf).also { read = it } != -1) {
                    if (isCancelled()) {
                        zos.closeEntry()
                        throw Exception("Cancelled")
                    }
                    zos.write(buf, 0, read)
                    onProgressBytes(read.toLong())
                }
            }
            zos.closeEntry()
        }
    }
}

// Extract into a new subfolder named after the zip (default behavior)
fun unzipFile(
    zipFile: File,
    onProgress: (Int) -> Unit,
    isCancelled: () -> Boolean
) {
    val destDir = File(zipFile.parentFile, zipFile.nameWithoutExtension)
    if (!destDir.exists()) destDir.mkdirs()
    extractZipTo(zipFile, destDir, onProgress, isCancelled)
}

// Extract directly into current folder (no subfolder)
fun unzipHere(
    zipFile: File,
    onProgress: (Int) -> Unit,
    isCancelled: () -> Boolean
) {
    val destDir = zipFile.parentFile ?: return
    extractZipTo(zipFile, destDir, onProgress, isCancelled)
}

private fun extractZipTo(
    zipFile: File,
    destDir: File,
    onProgress: (Int) -> Unit,
    isCancelled: () -> Boolean
) {
    if (!destDir.exists()) destDir.mkdirs()

    val total = zipFile.length().coerceAtLeast(1L)
    var processed = 0L

    ZipInputStream(FileInputStream(zipFile)).use { zis ->
        val buffer = ByteArray(64 * 1024)
        var entry: ZipEntry? = zis.nextEntry
        while (entry != null) {
            if (isCancelled()) throw Exception("Cancelled")
            val outFile = File(destDir, entry.name)
            if (!outFile.canonicalPath.startsWith(destDir.canonicalPath)) {
                throw Exception("Unsafe zip entry")
            }
            if (entry.isDirectory) {
                outFile.mkdirs()
            } else {
                outFile.parentFile?.mkdirs()
                FileOutputStream(outFile).use { fos ->
                    var read: Int
                    while (zis.read(buffer).also { read = it } != -1) {
                        if (isCancelled()) throw Exception("Cancelled")
                        fos.write(buffer, 0, read)
                        processed += read
                        onProgress(((processed * 100) / total).toInt().coerceIn(0, 100))
                    }
                }
            }
            zis.closeEntry()
            entry = zis.nextEntry
        }
    }
}

// ===== Password-protected ZIP (AES-256) =====



object PasswordZipUtils {

    /**
     * Password-protected ZIP banao.
     * Returns null on success, error message on failure.
     */
    fun createEncryptedZip(
        sources: List<File>,
        outputZip: File,
        password: String,
        onProgress: (Int) -> Unit = {}
    ): String? {
        return try {
            if (outputZip.exists()) outputZip.delete()

            val zipFile = Zip4jFile(outputZip, password.toCharArray())

            val params = ZipParameters().apply {
                compressionMethod = CompressionMethod.DEFLATE
                compressionLevel = CompressionLevel.NORMAL
                isEncryptFiles = true
                encryptionMethod = EncryptionMethod.AES
                aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
            }

            val total = sources.size.coerceAtLeast(1)
            sources.forEachIndexed { i, source ->
                if (!source.exists()) return@forEachIndexed

                if (source.isDirectory) {
                    zipFile.addFolder(source, params)
                } else {
                    zipFile.addFile(source, params)
                }

                onProgress(((i + 1) * 100) / total)
            }

            null
        } catch (e: Exception) {
            "Failed: ${e.message ?: "Unknown"}"
        }
    }

    /**
     * Password-protected ZIP extract karo.
     * Returns null on success, "WRONG_PASSWORD", "PASSWORD_REQUIRED", or error message.
     */
    fun extractEncryptedZip(
        zipFile: File,
        destDir: File,
        password: String? = null,
        onProgress: (Int) -> Unit = {}
    ): String? {
        return try {
            if (!destDir.exists()) destDir.mkdirs()

            val zip = if (password.isNullOrEmpty()) {
                Zip4jFile(zipFile)
            } else {
                Zip4jFile(zipFile, password.toCharArray())
            }

            // Check if encrypted
            if (zip.isEncrypted && password.isNullOrEmpty()) {
                return "PASSWORD_REQUIRED"
            }

            // Extract
            zip.extractAll(destDir.absolutePath)
            onProgress(100)

            null
        } catch (e: net.lingala.zip4j.exception.ZipException) {
            val msg = e.message ?: ""
            if (msg.contains("password", ignoreCase = true) ||
                msg.contains("Wrong Password", ignoreCase = true)) {
                "WRONG_PASSWORD"
            } else {
                "Failed: $msg"
            }
        } catch (e: Exception) {
            "Failed: ${e.message ?: "Unknown"}"
        }
    }

    /**
     * ZIP encrypted hai ya nahi.
     */
    fun isZipEncrypted(zipFile: File): Boolean {
        return try {
            Zip4jFile(zipFile).isEncrypted
        } catch (e: Exception) {
            false
        }
    }
}