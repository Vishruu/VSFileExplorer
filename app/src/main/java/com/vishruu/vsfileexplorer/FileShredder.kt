package com.vishruu.vsfileexplorer

import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.security.SecureRandom

object FileShredder {

    private const val BUFFER = 64 * 1024

    /**
     * 3-pass overwrite then delete.
     * @return true if successful
     */
    fun shred(
        file: File,
        onProgress: (Int) -> Unit,
        isCancelled: () -> Boolean
    ): Boolean {
        return try {
            if (file.isDirectory) {
                shredDirectory(file, onProgress, isCancelled)
            } else {
                shredFile(file, onProgress, isCancelled)
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun shredFile(
        file: File,
        onProgress: (Int) -> Unit,
        isCancelled: () -> Boolean
    ): Boolean {
        if (!file.exists() || !file.isFile) return false
        val size = file.length()
        if (size == 0L) {
            file.delete()
            onProgress(100)
            return true
        }

        // Pass 1: random bytes
        overwrite(file, size, Pass.RANDOM, onProgress, 0, isCancelled)
        if (isCancelled()) return false
        // Pass 2: zeros
        overwrite(file, size, Pass.ZEROS, onProgress, 33, isCancelled)
        if (isCancelled()) return false
        // Pass 3: ones
        overwrite(file, size, Pass.ONES, onProgress, 66, isCancelled)
        if (isCancelled()) return false

        val deleted = file.delete()
        onProgress(100)
        return deleted
    }

    private fun shredDirectory(
        dir: File,
        onProgress: (Int) -> Unit,
        isCancelled: () -> Boolean
    ): Boolean {
        val children = dir.listFiles() ?: return dir.delete()
        val total = children.size.coerceAtLeast(1)
        children.forEachIndexed { i, child ->
            if (isCancelled()) return false
            if (child.isDirectory) shredDirectory(child, { }, isCancelled)
            else shredFile(child, { }, isCancelled)
            onProgress(((i + 1) * 100) / total)
        }
        return dir.delete()
    }

    private enum class Pass { RANDOM, ZEROS, ONES }

    private fun overwrite(
        file: File,
        size: Long,
        pass: Pass,
        onProgress: (Int) -> Unit,
        baseProgress: Int,
        isCancelled: () -> Boolean
    ) {
        val raf = RandomAccessFile(file, "rws")
        try {
            raf.seek(0)
            val buffer = ByteArray(BUFFER)
            val random = if (pass == Pass.RANDOM) SecureRandom() else null
            var written = 0L
            while (written < size) {
                if (isCancelled()) return
                val chunk = minOf(BUFFER.toLong(), size - written).toInt()
                when (pass) {
                    Pass.RANDOM -> random!!.nextBytes(buffer)
                    Pass.ZEROS -> buffer.fill(0)
                    Pass.ONES -> buffer.fill(0xFF.toByte())
                }
                raf.write(buffer, 0, chunk)
                written += chunk
                val pct = baseProgress + ((written * 33) / size).toInt()
                onProgress(pct.coerceIn(0, 100))
            }
            raf.fd.sync()
        } finally {
            try { raf.close() } catch (_: Exception) {}
        }
    }
}