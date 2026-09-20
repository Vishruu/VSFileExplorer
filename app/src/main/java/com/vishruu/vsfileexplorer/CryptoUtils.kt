package com.vishruu.vsfileexplorer

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {

    private const val MAGIC = "VISH"
    private const val VERSION: Byte = 1
    private const val SALT_SIZE = 16
    private const val IV_SIZE = 12
    private const val GCM_TAG_BITS = 128
    private const val PBKDF2_ITERATIONS = 100_000
    private const val KEY_SIZE_BITS = 256
    private const val BUFFER_SIZE = 64 * 1024

    const val VS_EXTENSION = ".vs"

    class WrongPasswordException : Exception("Wrong password")
    class CancelledException : Exception("Cancelled")

    data class Header(
        val salt: ByteArray,
        val iv: ByteArray,
        val encryptFileName: Boolean,
        val originalNameBytes: ByteArray
    )

    private fun deriveKey(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_SIZE_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun generateRandomName(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val random = SecureRandom()
        val sb = StringBuilder()
        repeat(12) {
            sb.append(chars[random.nextInt(chars.length)])
        }
        return sb.toString()
    }

    private fun writeHeader(
        output: FileOutputStream,
        salt: ByteArray,
        iv: ByteArray,
        encryptFileName: Boolean,
        originalNameBytes: ByteArray
    ) {
        output.write(MAGIC.toByteArray(Charsets.US_ASCII))
        output.write(VERSION.toInt())
        output.write(salt)
        output.write(iv)
        output.write(if (encryptFileName) 1 else 0)
        val nameLen = originalNameBytes.size
        output.write((nameLen shr 8) and 0xFF)
        output.write(nameLen and 0xFF)
        if (nameLen > 0) output.write(originalNameBytes)
    }

    private fun readHeader(input: FileInputStream): Header {
        val magic = ByteArray(4)
        if (input.read(magic) != 4 || String(magic, Charsets.US_ASCII) != MAGIC) {
            throw WrongPasswordException()
        }
        val version = input.read()
        if (version != VERSION.toInt()) {
            throw WrongPasswordException()
        }
        val salt = ByteArray(SALT_SIZE)
        if (input.read(salt) != SALT_SIZE) throw WrongPasswordException()
        val iv = ByteArray(IV_SIZE)
        if (input.read(iv) != IV_SIZE) throw WrongPasswordException()
        val encryptNameByte = input.read()
        val encryptName = encryptNameByte == 1
        val lenHi = input.read()
        val lenLo = input.read()
        val nameLen = (lenHi shl 8) or lenLo
        val nameBytes = if (nameLen > 0) {
            val b = ByteArray(nameLen)
            if (input.read(b) != nameLen) throw WrongPasswordException()
            b
        } else {
            ByteArray(0)
        }
        return Header(salt, iv, encryptName, nameBytes)
    }

    fun encryptFile(
        sourceFile: File,
        password: String,
        encryptFileName: Boolean,
        onProgress: (Int) -> Unit,
        isCancelled: () -> Boolean
    ): File {
        val salt = ByteArray(SALT_SIZE).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_SIZE).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(password, salt)

        val originalName = sourceFile.name
        val originalNameBytes = if (encryptFileName) {
            originalName.toByteArray(Charsets.UTF_8)
        } else {
            ByteArray(0)
        }

        val baseName = if (encryptFileName) generateRandomName() else originalName
        val outFile = File(sourceFile.parentFile, baseName + VS_EXTENSION)

        val totalSize = sourceFile.length().coerceAtLeast(1L)
        var processed = 0L

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(key, "AES"),
            GCMParameterSpec(GCM_TAG_BITS, iv)
        )

        try {
            FileInputStream(sourceFile).use { input ->
                FileOutputStream(outFile).use { output ->
                    writeHeader(output, salt, iv, encryptFileName, originalNameBytes)

                    val buffer = ByteArray(BUFFER_SIZE)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        if (isCancelled()) {
                            throw CancelledException()
                        }
                        val enc = cipher.update(buffer, 0, read)
                        if (enc != null) output.write(enc)
                        processed += read
                        onProgress(((processed * 100) / totalSize).toInt().coerceIn(0, 100))
                    }
                    val last = cipher.doFinal()
                    if (last != null) output.write(last)
                }
            }
            sourceFile.delete()
            return outFile
        } catch (e: Exception) {
            outFile.delete()
            throw e
        }
    }

    fun decryptFile(
        sourceFile: File,
        password: String,
        onProgress: (Int) -> Unit,
        isCancelled: () -> Boolean
    ): File {
        FileInputStream(sourceFile).use { input ->
            val header = readHeader(input)
            val key = deriveKey(password, header.salt)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(key, "AES"),
                GCMParameterSpec(GCM_TAG_BITS, header.iv)
            )

            val originalName = if (header.encryptFileName) {
                String(header.originalNameBytes, Charsets.UTF_8)
            } else {
                sourceFile.name.removeSuffix(VS_EXTENSION)
            }

            val outFile = File(sourceFile.parentFile, originalName)
            val totalSize = sourceFile.length().coerceAtLeast(1L)
            var processed = 0L

            try {
                FileOutputStream(outFile).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        if (isCancelled()) {
                            throw CancelledException()
                        }
                        val dec = cipher.update(buffer, 0, read)
                        if (dec != null) output.write(dec)
                        processed += read
                        onProgress(((processed * 100) / totalSize).toInt().coerceIn(0, 100))
                    }
                    val last = cipher.doFinal()
                    if (last != null) output.write(last)
                }
                sourceFile.delete()
                return outFile
            } catch (e: Exception) {
                outFile.delete()
                throw WrongPasswordException()
            }
        }
    }
    /**
     * Encrypt a file OR folder (recursively) with same password.
     * Returns count of files encrypted.
     */
    fun encryptPath(
        source: File,
        password: String,
        encryptFileName: Boolean,
        onProgress: (Int) -> Unit,
        isCancelled: () -> Boolean
    ): Int {
        if (source.isFile) {
            encryptFile(source, password, encryptFileName, onProgress, isCancelled)
            return 1
        }
        if (source.isDirectory) {
            var count = 0
            val children = source.listFiles() ?: return 0
            children.forEach { child ->
                if (isCancelled()) return count
                count += encryptPath(child, password, encryptFileName, onProgress, isCancelled)
            }
            // Empty folder delete kar do
            try {
                if (source.listFiles()?.isEmpty() == true) source.delete()
            } catch (_: Exception) {}
            return count
        }
        return 0
    }
    fun decryptPath(
        source: File,
        password: String,
        onProgress: (Int) -> Unit,
        isCancelled: () -> Boolean
    ): Int {
        if (source.isFile && source.name.endsWith(VS_EXTENSION)) {
            decryptFile(source, password, onProgress, isCancelled)
            return 1
        }
        if (source.isDirectory) {
            var count = 0
            val children = source.listFiles() ?: return 0
            children.forEach { child ->
                if (isCancelled()) return count
                count += decryptPath(child, password, onProgress, isCancelled)
            }
            try {
                if (source.listFiles()?.isEmpty() == true) source.delete()
            } catch (_: Exception) {}
            return count
        }
        return 0
    }
}