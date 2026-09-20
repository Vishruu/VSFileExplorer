package com.vishruu.vsfileexplorer

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

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

    ZipOutputStream(FileOutputStream(outputZip)).use { zos ->
        sources.forEach { src ->
            if (src.isDirectory) {
                zipDir(src, src.name, zos,
                    onProgressBytes = { b ->
                        processed += b
                        onProgress(((processed * 100) / total).toInt().coerceIn(0, 100))
                    },
                    isCancelled = isCancelled)
            } else {
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

fun zipDir(
    dir: File,
    basePath: String,
    zos: ZipOutputStream,
    onProgressBytes: (Long) -> Unit,
    isCancelled: () -> Boolean
) {
    val children = dir.listFiles() ?: return
    if (children.isEmpty()) {
        zos.putNextEntry(ZipEntry("$basePath/"))
        zos.closeEntry()
        return
    }
    for (child in children) {
        if (isCancelled()) throw Exception("Cancelled")
        val entryPath = "$basePath/${child.name}"
        if (child.isDirectory) {
            zipDir(child, entryPath, zos, onProgressBytes, isCancelled)
        } else {
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