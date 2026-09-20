package com.vishruu.vsfileexplorer

import android.content.Context
import android.os.Environment
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupManager {

    private const val BACKUP_VERSION = 1
    private const val EXTENSION = ".vsbackup"

    // Saare SharedPreferences files jo backup honge
    private val PREFS_FILES = listOf(
        "vs_settings_prefs",   // SettingsManager
        "vs_bookmarks",        // BookmarksStore
        "vs_applock_prefs",    // AppLockManager
        "vs_vault_prefs"       // VaultManager (hash+salt — password same rahega)
    )

    // Saare folders jo backup honge
    private val FOLDERS_TO_BACKUP = listOf(
        "SecureNotes",     // notes.vs (encrypted)
        ".vs_trash",       // Recycle bin (index + files)
        "Vault",           // Locked vault files (encrypted)
        ".vault_cache"     // Unlocked vault files (agar unlock hai tab)
    )

    data class RestoreResult(
        val success: Boolean,
        val message: String
    )

    fun suggestedFileName(): String {
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "VSFileExplorer_$ts$EXTENSION"
    }

    /**
     * Backup banao. Output kisi bhi stream pe likha jayega
     * (URI ya File dono se kaam karega).
     */
    fun createBackup(context: Context, outputStream: OutputStream): Boolean {
        return try {
            ZipOutputStream(outputStream).use { zip ->

                // 1. Meta
                val meta = JSONObject()
                meta.put("version", BACKUP_VERSION)
                meta.put("timestamp", System.currentTimeMillis())
                meta.put("app", "VSFileExplorer")
                zip.putNextEntry(ZipEntry("meta.json"))
                zip.write(meta.toString().toByteArray())
                zip.closeEntry()

                // 2. SharedPreferences
                PREFS_FILES.forEach { prefName ->
                    val prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
                    val obj = JSONObject()
                    prefs.all.forEach { (k, v) ->
                        // Type prefix — restore pe exact type wapas milega
                        when (v) {
                            is String -> obj.put(k, "S:$v")
                            is Boolean -> obj.put(k, "B:$v")
                            is Int -> obj.put(k, "I:$v")
                            is Long -> obj.put(k, "L:$v")
                            is Float -> obj.put(k, "F:$v")
                            else -> obj.put(k, "S:$v")
                        }
                    }
                    zip.putNextEntry(ZipEntry("prefs/$prefName.json"))
                    zip.write(obj.toString().toByteArray())
                    zip.closeEntry()
                }

                // 3. Folders (files raw bytes — efficient, no base64 bloat)
                val appRoot = File(Environment.getExternalStorageDirectory(), "VSFileExplorer")
                FOLDERS_TO_BACKUP.forEach { sub ->
                    val src = File(appRoot, sub)
                    if (src.exists()) {
                        addDirectoryToZip(zip, src, "files/$sub")
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun addDirectoryToZip(zip: ZipOutputStream, dir: File, entryPrefix: String) {
        val children = dir.listFiles() ?: return
        for (child in children) {
            // .nomedia skip (system file hai)
            if (child.name == ".nomedia") continue
            val childEntry = "$entryPrefix/${child.name}"
            if (child.isDirectory) {
                // Empty directory marker
                zip.putNextEntry(ZipEntry("$childEntry/"))
                zip.closeEntry()
                addDirectoryToZip(zip, child, childEntry)
            } else {
                zip.putNextEntry(ZipEntry(childEntry))
                FileInputStream(child).use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    /**
     * Backup restore karo. Input kisi bhi stream se
     * (URI ya File dono se kaam karega).
     */
    fun restoreBackup(context: Context, inputStream: InputStream): RestoreResult {
        return try {
            ZipInputStream(inputStream).use { zip ->
                val appRoot = File(Environment.getExternalStorageDirectory(), "VSFileExplorer")
                var prefsRestored = 0
                var filesRestored = 0
                var metaOk = false

                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name

                    when {
                        name == "meta.json" -> {
                            val content = zip.readBytes().toString(Charsets.UTF_8)
                            try {
                                val obj = JSONObject(content)
                                if (obj.optString("app") == "VSFileExplorer") {
                                    metaOk = true
                                }
                            } catch (_: Exception) {}
                        }

                        name.startsWith("prefs/") && name.endsWith(".json") -> {
                            val prefName = name.removePrefix("prefs/").removeSuffix(".json")
                            val content = zip.readBytes().toString(Charsets.UTF_8)
                            if (restorePrefs(context, prefName, content)) prefsRestored++
                        }

                        name.startsWith("files/") -> {
                            val relative = name.removePrefix("files/")
                            val target = File(appRoot, relative)

                            if (entry.isDirectory) {
                                target.mkdirs()
                            } else {
                                target.parentFile?.mkdirs()
                                FileOutputStream(target).use { fos ->
                                    zip.copyTo(fos)
                                }
                                filesRestored++
                            }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }

                if (!metaOk) {
                    return RestoreResult(false, "Invalid backup file")
                }

                RestoreResult(
                    success = true,
                    message = "Restored $prefsRestored settings, $filesRestored files"
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            RestoreResult(false, "Failed: ${e.message ?: "unknown error"}")
        }
    }

    private fun restorePrefs(context: Context, prefName: String, json: String): Boolean {
        return try {
            val obj = JSONObject(json)
            val prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.clear()  // Purane settings hatao, naye daalo

            obj.keys().forEach { key ->
                val v = obj.getString(key)
                when {
                    v.startsWith("S:") -> editor.putString(key, v.substring(2))
                    v.startsWith("B:") -> editor.putBoolean(key, v.substring(2).toBoolean())
                    v.startsWith("I:") -> editor.putInt(key, v.substring(2).toIntOrNull() ?: 0)
                    v.startsWith("L:") -> editor.putLong(key, v.substring(2).toLongOrNull() ?: 0L)
                    v.startsWith("F:") -> editor.putFloat(key, v.substring(2).toFloatOrNull() ?: 0f)
                }
            }
            editor.apply()
            true
        } catch (e: Exception) {
            false
        }
    }
}