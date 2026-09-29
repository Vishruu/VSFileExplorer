package com.vishruu.vsfileexplorer

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CloudBackupManager {

    private const val BACKUP_FOLDER = "VSFileExplorer_Backups"
    private const val BACKUP_PREFIX = "vs_backup_"
    private const val BACKUP_VERSION = 1
    private const val MAX_BACKUPS = 5

    data class BackupInfo(
        val fileId: String,
        val fileName: String,
        val size: Long,
        val modifiedTime: Long
    )

    /**
     * Backup banao + Drive pe upload karo.
     * Returns null on success, error message on failure.
     */
    suspend fun backupToDrive(
        context: Context,
        onProgress: (String) -> Unit = {}
    ): String? {
        return try {
            onProgress("Preparing backup...")

            // 1. JSON banao
            val json = buildBackupJson(context)

            // 2. Local temp file mein likho
            val tempFile = File(context.cacheDir, "vs_backup_temp.json")
            tempFile.writeText(json)

            // 3. Drive pe folder check/banao
            onProgress("Connecting to Drive...")
            if (!DriveAuthManager.isSignedIn(context)) {
                return "NOT_SIGNED_IN"
            }
            val token = DriveAuthManager.getAccessToken(context)
                ?: return "NOT_SIGNED_IN"

            val folderId = getOrCreateBackupFolder(token)
                ?: return "Cannot create backup folder on Drive"

            // 4. Upload
            onProgress("Uploading backup...")
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US).format(Date())
            val fileName = "$BACKUP_PREFIX$timestamp.json"

            val uploadedId = uploadBackupFile(context, token, tempFile, fileName, folderId)
            tempFile.delete()

            if (uploadedId == null) return "Upload failed"

            // 5. Save last backup timestamp
            SettingsManager.setLastBackupTime(context, System.currentTimeMillis())

            // 6. Cleanup old backups (keep max 5)
            onProgress("Cleaning old backups...")
            cleanupOldBackups(token, folderId)

            onProgress("Done")
            null
        } catch (e: Exception) {
            "Error: ${e.message ?: "Unknown"}"
        }
    }

    /**
     * Drive se latest backup restore karo.
     * Returns null on success, error message on failure.
     */
    suspend fun restoreFromDrive(
        context: Context,
        onProgress: (String) -> Unit = {}
    ): String? {
        return try {
            onProgress("Finding latest backup...")

            val token = DriveAuthManager.getAccessToken(context)
                ?: return "Not signed in to Google Drive"

            val folderId = findBackupFolder(token)
                ?: return "No backup folder found on Drive"

            // List backups
            val backups = listBackupsInFolder(token, folderId)
            if (backups.isEmpty()) return "No backups found on Drive"

            // Latest pehle
            val latest = backups.maxByOrNull { it.modifiedTime }
                ?: return "No backups found"

            onProgress("Downloading ${latest.fileName}...")

            // Download to temp
            val tempFile = File(context.cacheDir, "vs_restore_temp.json")
            val ok = DriveApiClient.downloadFile(token, latest.fileId, tempFile)
            if (!ok) {
                tempFile.delete()
                return "Download failed"
            }

            // Apply
            onProgress("Restoring...")
            val err = applyBackupJson(context, tempFile.readText())
            tempFile.delete()

            if (err != null) return err

            onProgress("Done")
            null
        } catch (e: Exception) {
            "Error: ${e.message ?: "Unknown"}"
        }
    }

    /**
     * Drive pe available backups list karo.
     */
    suspend fun listBackups(context: Context): List<BackupInfo> {
        return try {
            val token = DriveAuthManager.getAccessToken(context) ?: return emptyList()
            val folderId = findBackupFolder(token) ?: return emptyList()
            listBackupsInFolder(token, folderId)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ===== Internal helpers =====

    private fun buildBackupJson(context: Context): String {
        val root = JSONObject()
        root.put("version", BACKUP_VERSION)
        root.put("timestamp", System.currentTimeMillis())
        root.put("app", "VSFileExplorer")

        // Settings
        val settings = JSONObject()
        settings.put("themeMode", SettingsManager.getThemeMode(context))
        settings.put("defaultSortMode", SettingsManager.getDefaultSortMode(context))
        settings.put("defaultViewMode", SettingsManager.getDefaultViewMode(context))
        settings.put("defaultShowHidden", SettingsManager.getDefaultShowHidden(context))
        settings.put("amoled", SettingsManager.isAmoled(context))
        settings.put("vaultLockDelay", SettingsManager.getVaultLockDelay(context))
        settings.put("shredPasses", SettingsManager.getShredPasses(context))
        settings.put("confirmDelete", SettingsManager.isConfirmDeleteEnabled(context))
        settings.put("recycleAutoDeleteDays", SettingsManager.getRecycleAutoDeleteDays(context))
        settings.put("thumbQuality", SettingsManager.getThumbQuality(context))
        root.put("settings", settings)

        // Bookmarks
        val bookmarks = JSONArray()
        BookmarksStore.getBookmarks(context).forEach { bookmarks.put(it) }
        root.put("bookmarks", bookmarks)

        // Secure Notes (agar password diya ho toh — abhi skip karte hain)
        // Notes encrypted hain — user ka password chahiye
        root.put("notesIncluded", false)

        // Recycle bin index
        val trashIndex = File(
            android.os.Environment.getExternalStorageDirectory(),
            "VSFileExplorer/.vs_trash/index.json"
        )
        if (trashIndex.exists()) {
            try {
                root.put("trashIndex", trashIndex.readText())
            } catch (e: Exception) {
                root.put("trashIndex", "")
            }
        }

        return root.toString(2)
    }

    private fun applyBackupJson(context: Context, json: String): String? {
        return try {
            val root = JSONObject(json)
            if (root.optString("app") != "VSFileExplorer") {
                return "Invalid backup file"
            }

            // Settings
            val settings = root.optJSONObject("settings")
            if (settings != null) {
                settings.optString("themeMode", "").takeIf { it.isNotEmpty() }?.let {
                    SettingsManager.setThemeMode(context, it)
                }
                settings.optString("defaultSortMode", "").takeIf { it.isNotEmpty() }?.let {
                    SettingsManager.setDefaultSortMode(context, it)
                }
                settings.optString("defaultViewMode", "").takeIf { it.isNotEmpty() }?.let {
                    SettingsManager.setDefaultViewMode(context, it)
                }
                if (settings.has("defaultShowHidden")) {
                    SettingsManager.setDefaultShowHidden(context, settings.getBoolean("defaultShowHidden"))
                }
                if (settings.has("amoled")) {
                    SettingsManager.setAmoled(context, settings.getBoolean("amoled"))
                }
                if (settings.has("vaultLockDelay")) {
                    SettingsManager.setVaultLockDelay(context, settings.getInt("vaultLockDelay"))
                }
                if (settings.has("shredPasses")) {
                    SettingsManager.setShredPasses(context, settings.getInt("shredPasses"))
                }
                if (settings.has("confirmDelete")) {
                    SettingsManager.setConfirmDeleteEnabled(context, settings.getBoolean("confirmDelete"))
                }
                if (settings.has("recycleAutoDeleteDays")) {
                    SettingsManager.setRecycleAutoDeleteDays(context, settings.getInt("recycleAutoDeleteDays"))
                }
                if (settings.has("thumbQuality")) {
                    SettingsManager.setThumbQuality(context, settings.getInt("thumbQuality"))
                }
            }

            // Bookmarks — clear aur add
            val bookmarks = root.optJSONArray("bookmarks")
            if (bookmarks != null) {
                // Purane clear karo
                BookmarksStore.getBookmarks(context).forEach { path ->
                    BookmarksStore.removeBookmark(context, path)
                }
                // Naye add karo
                for (i in 0 until bookmarks.length()) {
                    val path = bookmarks.getString(i)
                    BookmarksStore.addBookmark(context, path)
                }
            }

            // Recycle bin index — restore
            val trashIndex = root.optString("trashIndex", "")
            if (trashIndex.isNotEmpty()) {
                try {
                    val f = File(
                        android.os.Environment.getExternalStorageDirectory(),
                        "VSFileExplorer/.vs_trash/index.json"
                    )
                    f.parentFile?.mkdirs()
                    f.writeText(trashIndex)
                } catch (e: Exception) {
                    // ignore
                }
            }

            null
        } catch (e: Exception) {
            "Invalid backup: ${e.message}"
        }
    }

    private suspend fun findBackupFolder(token: String): String? {
        return try {
            val query = "name = '$BACKUP_FOLDER' and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
            val files = DriveApiClient.searchFiles(token, query)
            files.firstOrNull()?.id
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun getOrCreateBackupFolder(token: String): String? {
        // Pehle dhundho
        val existing = findBackupFolder(token)
        if (existing != null) return existing

        // Nahi hai toh banao
        val ok = DriveApiClient.createFolder(token, BACKUP_FOLDER)
        if (!ok) return null

        // Wapas dhundho
        return findBackupFolder(token)
    }

    private suspend fun uploadBackupFile(
        context: Context,
        token: String,
        localFile: File,
        fileName: String,
        parentId: String
    ): String? {
        return try {
            DriveApiClient.uploadFileWithName(
                context = context,
                token = token,
                localFile = localFile,
                fileName = fileName,
                parentId = parentId
            )
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun listBackupsInFolder(token: String, folderId: String): List<BackupInfo> {
        return try {
            val query = "'$folderId' in parents and name contains '$BACKUP_PREFIX' and trashed = false"
            val files = DriveApiClient.searchFiles(token, query)
            files.map {
                BackupInfo(
                    fileId = it.id,
                    fileName = it.name,
                    size = it.size,
                    modifiedTime = it.modifiedTime
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun cleanupOldBackups(token: String, folderId: String) {
        try {
            val backups = listBackupsInFolder(token, folderId)
            if (backups.size <= MAX_BACKUPS) return

            // Purane delete karo — latest 5 rakho
            val sorted = backups.sortedByDescending { it.modifiedTime }
            sorted.drop(MAX_BACKUPS).forEach { old ->
                DriveApiClient.deleteFile(token, old.fileId)
            }
        } catch (e: Exception) {
            // ignore
        }
    }
}