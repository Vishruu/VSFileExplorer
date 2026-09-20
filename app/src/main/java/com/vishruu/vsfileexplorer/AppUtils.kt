package com.vishruu.vsfileexplorer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Movie
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

// Vault/cache paths to skip during scans
private fun isVaultPath(path: String): Boolean {
    return path.contains("/VSFileExplorer/Vault") || path.contains("/.vault_cache")
}

fun categoryFilterFor(label: String): CategoryFilter? {
    return when (label) {
        "Images" -> CategoryFilter(
            label = "Images",
            extensions = listOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif")
        )
        "Videos" -> CategoryFilter(
            label = "Videos",
            extensions = listOf("mp4", "mkv", "avi", "mov", "webm", "3gp", "flv", "m4v")
        )
        "Audio" -> CategoryFilter(
            label = "Audio",
            extensions = listOf("mp3", "wav", "aac", "ogg", "flac", "m4a", "opus", "wma")
        )
        "Documents" -> CategoryFilter(
            label = "Documents",
            extensions = listOf("pdf", "doc", "docx", "txt", "ppt", "pptx", "xls", "xlsx", "odt", "rtf")
        )
        "APK" -> CategoryFilter(
            label = "APK",
            extensions = listOf("apk", "xapk", "apks")
        )
        "ZIP" -> CategoryFilter(
            label = "ZIP",
            extensions = listOf("zip", "rar", "7z", "tar", "gz", "bz2")
        )
        "ZIP Tools" -> CategoryFilter(
            label = "ZIP Files",
            extensions = listOf("zip", "rar", "7z", "tar", "gz", "bz2")
        )
        "Encrypt" -> CategoryFilter(
            label = "Encrypted Files",
            isEncryptedOnly = true
        )
        "Hidden" -> CategoryFilter(
            label = "Hidden Files",
            isHiddenOnly = true
        )
        else -> null
    }
}

fun predicateForCategory(cat: CategoryFilter): (File) -> Boolean {
    if (cat.isEncryptedOnly) {
        return { f: File -> f.name.endsWith(CryptoUtils.VS_EXTENSION) }
    }
    if (cat.isHiddenOnly) {
        return { f: File -> f.name.startsWith(".") }
    }
    val extSet = cat.extensions.map { it.lowercase() }.toSet()
    return { f: File -> f.extension.lowercase() in extSet }
}

fun scanViaMediaStore(
    context: Context,
    cat: CategoryFilter,
    maxResults: Int = 500
): List<File>? {
    val extSet = cat.extensions.map { it.lowercase() }.toSet()
    val isImage = extSet.any { it in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif") }
    val isVideo = extSet.any { it in listOf("mp4", "mkv", "avi", "mov", "webm", "3gp", "flv", "m4v") }
    val isAudio = extSet.any { it in listOf("mp3", "wav", "aac", "ogg", "flac", "m4a", "opus", "wma") }

    if (!isImage && !isVideo && !isAudio) return null

    val uri = when {
        isImage -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        isVideo -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        else -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }

    val projection = arrayOf(MediaStore.MediaColumns.DATA)
    val results = mutableListOf<File>()

    try {
        context.contentResolver.query(
            uri, projection, null, null,
            MediaStore.MediaColumns.DATE_MODIFIED + " DESC"
        )?.use { cursor ->
            val dataIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
            if (dataIdx >= 0) {
                while (cursor.moveToNext() && results.size < maxResults) {
                    val path = cursor.getString(dataIdx) ?: continue
                    if (path.isEmpty()) continue
                    if (isVaultPath(path)) continue
                    val f = File(path)
                    if (f.exists() && f.extension.lowercase() in extSet) {
                        results.add(f)
                    }
                }
            }
        }
    } catch (e: Exception) {
        return null
    }

    return if (results.isEmpty()) null else results
}

fun scanByExtensions(dir: File, extensions: List<String>, maxResults: Int): List<File> {
    val results = mutableListOf<File>()
    val queue = ArrayDeque<File>()
    queue.add(dir)
    val extSet = extensions.map { it.lowercase() }.toSet()

    while (queue.isNotEmpty() && results.size < maxResults) {
        val current = queue.removeFirst()
        if (isVaultPath(current.absolutePath)) continue
        val children = try { current.listFiles() } catch (e: Exception) { null } ?: continue
        for (child in children) {
            if (child.isDirectory) {
                if (!child.name.startsWith(".") && !isVaultPath(child.absolutePath)) queue.add(child)
            } else {
                val ext = child.extension.lowercase()
                if (ext.isNotEmpty() && extSet.contains(ext)) {
                    results.add(child)
                    if (results.size >= maxResults) break
                }
            }
        }
    }
    return results.sortedByDescending { it.lastModified() }
}

fun scanEncryptedFiles(dir: File, maxResults: Int): List<File> {
    val results = mutableListOf<File>()
    val queue = ArrayDeque<File>()
    queue.add(dir)
    val ext = CryptoUtils.VS_EXTENSION

    while (queue.isNotEmpty() && results.size < maxResults) {
        val current = queue.removeFirst()
        if (isVaultPath(current.absolutePath)) continue
        val children = try { current.listFiles() } catch (e: Exception) { null } ?: continue
        for (child in children) {
            if (child.isDirectory) {
                if (!child.name.startsWith(".") && !isVaultPath(child.absolutePath)) queue.add(child)
            } else if (child.name.endsWith(ext)) {
                results.add(child)
                if (results.size >= maxResults) break
            }
        }
    }
    return results.sortedByDescending { it.lastModified() }
}

fun scanHiddenFiles(dir: File, maxResults: Int): List<File> {
    val results = mutableListOf<File>()
    val queue = ArrayDeque<File>()
    queue.add(dir)

    while (queue.isNotEmpty() && results.size < maxResults) {
        val current = queue.removeFirst()
        if (isVaultPath(current.absolutePath)) continue
        val children = try { current.listFiles() } catch (e: Exception) { null } ?: continue
        for (child in children) {
            if (child.name.startsWith(".")) {
                results.add(child)
                if (results.size >= maxResults) break
                if (child.isDirectory && !isVaultPath(child.absolutePath)) queue.add(child)
            } else if (child.isDirectory) {
                if (!isVaultPath(child.absolutePath)) queue.add(child)
            }
        }
    }
    return results.sortedByDescending { it.lastModified() }
}

fun scanFolders(
    root: File,
    predicate: (File) -> Boolean,
    maxResults: Int = 500
): List<CategoryFolder> {
    val rootPath = root.absolutePath

    val directCounts = HashMap<String, Int>(256)
    val queue = ArrayDeque<File>()
    queue.add(root)

    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        if (isVaultPath(current.absolutePath)) continue
        val children = try { current.listFiles() } catch (e: Exception) { null } ?: continue
        var count = 0
        for (child in children) {
            if (child.name.startsWith(".")) continue
            if (child.isDirectory) {
                if (!isVaultPath(child.absolutePath)) queue.add(child)
            } else if (predicate(child)) {
                count++
            }
        }
        if (count > 0) {
            directCounts[current.absolutePath] = count
        }
    }

    if (directCounts.isEmpty()) return emptyList()

    val cumulative = HashMap<String, Int>(directCounts)
    val sortedPaths = cumulative.keys.sortedByDescending {
        it.count { c -> c == File.separatorChar }
    }
    for (path in sortedPaths) {
        if (path == rootPath) continue
        val parentPath = File(path).parent ?: continue
        if (parentPath == rootPath || cumulative.containsKey(parentPath)) {
            val c = cumulative[path] ?: 0
            cumulative[parentPath] = (cumulative[parentPath] ?: 0) + c
        }
    }

    val results = mutableListOf<CategoryFolder>()
    val rootChildren = try { root.listFiles() } catch (e: Exception) { null } ?: return emptyList()
    for (child in rootChildren) {
        if (!child.isDirectory) continue
        if (child.name.startsWith(".")) continue
        if (isVaultPath(child.absolutePath)) continue
        val count = cumulative[child.absolutePath] ?: 0
        if (count > 0) {
            results.add(CategoryFolder(child, count))
            if (results.size >= maxResults) break
        }
    }
    return results.sortedByDescending { it.count }
}

// ===== FAST analyzer using MediaStore (1 second) =====

fun scanStorageAnalyzerFast(context: Context, root: File): AnalyzerResult {
    val imageExt = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif")
    val videoExt = setOf("mp4", "mkv", "avi", "mov", "webm", "3gp", "flv", "m4v")
    val audioExt = setOf("mp3", "wav", "aac", "ogg", "flac", "m4a", "opus", "wma")
    val docExt = setOf("pdf", "doc", "docx", "txt", "ppt", "pptx", "xls", "xlsx", "odt", "rtf")
    val apkExt = setOf("apk", "xapk", "apks")
    val zipExt = setOf("zip", "rar", "7z", "tar", "gz", "bz2")

    val catStats = HashMap<String, LongArray>(8).apply {
        put("Images", longArrayOf(0L, 0L))
        put("Videos", longArrayOf(0L, 0L))
        put("Audio", longArrayOf(0L, 0L))
        put("Documents", longArrayOf(0L, 0L))
        put("APK", longArrayOf(0L, 0L))
        put("ZIP", longArrayOf(0L, 0L))
        put("Other", longArrayOf(0L, 0L))
    }

    val topFiles = ArrayList<File>(128)
    val maxTop = 100

    val filesByCategory = HashMap<String, MutableList<File>>(8).apply {
        put("Images", mutableListOf())
        put("Videos", mutableListOf())
        put("Audio", mutableListOf())
        put("Documents", mutableListOf())
        put("APK", mutableListOf())
        put("ZIP", mutableListOf())
        put("Other", mutableListOf())
    }

    val folderDirect = HashMap<String, LongArray>(512)

    val rootPath = root.absolutePath
    val rootPrefix = rootPath + File.separator

    val projection = arrayOf(
        MediaStore.Files.FileColumns.DATA,
        MediaStore.Files.FileColumns.SIZE,
        MediaStore.Files.FileColumns.DISPLAY_NAME
    )
    val uri = MediaStore.Files.getContentUri("external")

    var cursorOk = false
    try {
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            cursorOk = true
            val dataIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
            val sizeIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
            val nameIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
            if (dataIdx < 0 || sizeIdx < 0) return@use

            while (cursor.moveToNext()) {
                val path = cursor.getString(dataIdx) ?: continue
                if (path.isEmpty()) continue
                // Skip vault and cache
                if (isVaultPath(path)) continue
                val size = cursor.getLong(sizeIdx)
                val name = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "" else ""
                if (name.startsWith(".")) continue

                val file = File(path)

                val ext = file.extension.lowercase()
                val key = when {
                    imageExt.contains(ext) -> "Images"
                    videoExt.contains(ext) -> "Videos"
                    audioExt.contains(ext) -> "Audio"
                    docExt.contains(ext) -> "Documents"
                    apkExt.contains(ext) -> "APK"
                    zipExt.contains(ext) -> "ZIP"
                    else -> "Other"
                }
                val arr = catStats[key]!!
                arr[0] += size
                arr[1] += 1

                if (topFiles.size < maxTop) {
                    topFiles.add(file)
                    if (topFiles.size == maxTop) {
                        topFiles.sortByDescending { it.length() }
                    }
                } else if (size > topFiles.last().length()) {
                    topFiles[topFiles.size - 1] = file
                    var i = topFiles.size - 1
                    while (i > 0 && topFiles[i].length() > topFiles[i - 1].length()) {
                        val tmp = topFiles[i]
                        topFiles[i] = topFiles[i - 1]
                        topFiles[i - 1] = tmp
                        i--
                    }
                }

                val catList = filesByCategory[key]!!
                if (catList.size < 50) {
                    catList.add(file)
                } else {
                    var minIdx = 0
                    var minSize = catList[0].length()
                    for (i in 1 until catList.size) {
                        if (catList[i].length() < minSize) {
                            minSize = catList[i].length()
                            minIdx = i
                        }
                    }
                    if (size > minSize) catList[minIdx] = file
                }

                val parentPath = file.parent ?: continue
                if (!parentPath.startsWith(rootPrefix) && parentPath != rootPath) continue
                val farr = folderDirect.getOrPut(parentPath) { longArrayOf(0L, 0L) }
                farr[0] += size
                farr[1] += 1
            }
        }
    } catch (e: Exception) {
        cursorOk = false
    }

    if (!cursorOk) {
        return scanStorageAnalyzer(root)
    }

    val cumulativeSizes = HashMap<String, LongArray>(folderDirect)
    val sortedPaths = cumulativeSizes.keys.sortedByDescending {
        it.count { c -> c == File.separatorChar }
    }
    for (path in sortedPaths) {
        if (path == rootPath) continue
        val parentPath = File(path).parent ?: continue
        if (parentPath == rootPath || cumulativeSizes.containsKey(parentPath)) {
            val a = cumulativeSizes[path] ?: continue
            val p = cumulativeSizes.getOrPut(parentPath) { longArrayOf(0L, 0L) }
            p[0] += a[0]
            p[1] += a[1]
        }
    }

    val topLevelFolderStats = mutableListOf<FolderStat>()
    try {
        root.listFiles()?.forEach { child ->
            if (child.isDirectory && !child.name.startsWith(".")) {
                if (isVaultPath(child.absolutePath)) return@forEach
                val a = cumulativeSizes[child.absolutePath]
                if (a != null && a[0] > 0) {
                    topLevelFolderStats.add(FolderStat(child, a[0], a[1].toInt()))
                }
            }
        }
    } catch (e: Exception) { }

    val sortedFolderStats = topLevelFolderStats.sortedByDescending { it.bytes }

    val iconMap = mapOf(
        "Images" to Icons.Filled.Image,
        "Videos" to Icons.Filled.Movie,
        "Audio" to Icons.Filled.AudioFile,
        "Documents" to Icons.Filled.Description,
        "APK" to Icons.Filled.Apps,
        "ZIP" to Icons.Filled.FolderOpen,
        "Other" to Icons.Filled.InsertDriveFile
    )

    val categories = catStats
        .map { (k, v) ->
            StorageCategory(
                label = k,
                icon = iconMap[k] ?: Icons.Filled.InsertDriveFile,
                bytes = v[0],
                count = v[1].toInt()
            )
        }
        .sortedByDescending { it.bytes }

    val total = root.totalSpace
    val free = root.freeSpace
    val used = total - free

    return AnalyzerResult(
        categories = categories,
        largestFiles = topFiles,
        usedBytes = used,
        freeBytes = free,
        largestFolder = sortedFolderStats.firstOrNull()?.let { it.folder.name to it.bytes },
        folderStats = sortedFolderStats,
        filesByCategory = filesByCategory.mapValues { (_, list) ->
            list.sortedByDescending { it.length() }
        }
    )
}

// Fallback (old file-traversal method)
fun scanStorageAnalyzer(root: File): AnalyzerResult {
    val imageExt = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif")
    val videoExt = setOf("mp4", "mkv", "avi", "mov", "webm", "3gp", "flv", "m4v")
    val audioExt = setOf("mp3", "wav", "aac", "ogg", "flac", "m4a", "opus", "wma")
    val docExt = setOf("pdf", "doc", "docx", "txt", "ppt", "pptx", "xls", "xlsx", "odt", "rtf")
    val apkExt = setOf("apk", "xapk", "apks")
    val zipExt = setOf("zip", "rar", "7z", "tar", "gz", "bz2")

    val catStats = HashMap<String, LongArray>(8).apply {
        put("Images", longArrayOf(0L, 0L))
        put("Videos", longArrayOf(0L, 0L))
        put("Audio", longArrayOf(0L, 0L))
        put("Documents", longArrayOf(0L, 0L))
        put("APK", longArrayOf(0L, 0L))
        put("ZIP", longArrayOf(0L, 0L))
        put("Other", longArrayOf(0L, 0L))
    }
    val filesByCategory = HashMap<String, MutableList<File>>(8).apply {
        put("Images", mutableListOf())
        put("Videos", mutableListOf())
        put("Audio", mutableListOf())
        put("Documents", mutableListOf())
        put("APK", mutableListOf())
        put("ZIP", mutableListOf())
        put("Other", mutableListOf())
    }

    val topFiles = ArrayList<File>(128)
    val maxTop = 100
    val directFolderSizes = HashMap<String, LongArray>(512)
    val rootPath = root.absolutePath
    val queue = ArrayDeque<File>()
    queue.add(root)

    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        if (isVaultPath(current.absolutePath)) continue
        val children = try { current.listFiles() } catch (e: Exception) { null } ?: continue
        var folderBytes = 0L
        var folderCount = 0
        for (child in children) {
            if (child.name.startsWith(".")) continue
            if (child.isDirectory) {
                if (!isVaultPath(child.absolutePath)) queue.add(child)
            } else {
                val size = child.length()
                folderBytes += size
                folderCount++
                val ext = child.extension.lowercase()
                val key = when {
                    imageExt.contains(ext) -> "Images"
                    videoExt.contains(ext) -> "Videos"
                    audioExt.contains(ext) -> "Audio"
                    docExt.contains(ext) -> "Documents"
                    apkExt.contains(ext) -> "APK"
                    zipExt.contains(ext) -> "ZIP"
                    else -> "Other"
                }
                val arr = catStats[key]!!
                arr[0] += size
                arr[1] += 1
                val catList = filesByCategory[key]!!
                if (catList.size < 50) catList.add(child)

                if (topFiles.size < maxTop) {
                    topFiles.add(child)
                    if (topFiles.size == maxTop) topFiles.sortByDescending { it.length() }
                } else if (size > topFiles.last().length()) {
                    topFiles[topFiles.size - 1] = child
                    var i = topFiles.size - 1
                    while (i > 0 && topFiles[i].length() > topFiles[i - 1].length()) {
                        val tmp = topFiles[i]; topFiles[i] = topFiles[i - 1]; topFiles[i - 1] = tmp; i--
                    }
                }
            }
        }
        if (folderBytes > 0) directFolderSizes[current.absolutePath] = longArrayOf(folderBytes, folderCount.toLong())
    }

    val cumulativeSizes = HashMap<String, LongArray>(directFolderSizes)
    val sortedPaths = cumulativeSizes.keys.sortedByDescending { it.count { c -> c == File.separatorChar } }
    for (path in sortedPaths) {
        if (path == rootPath) continue
        val parentPath = File(path).parent ?: continue
        if (parentPath == rootPath || cumulativeSizes.containsKey(parentPath)) {
            val a = cumulativeSizes[path] ?: continue
            val p = cumulativeSizes.getOrPut(parentPath) { longArrayOf(0L, 0L) }
            p[0] += a[0]; p[1] += a[1]
        }
    }

    val topLevelFolderStats = mutableListOf<FolderStat>()
    try {
        root.listFiles()?.forEach { child ->
            if (child.isDirectory && !child.name.startsWith(".")) {
                if (isVaultPath(child.absolutePath)) return@forEach
                val a = cumulativeSizes[child.absolutePath]
                if (a != null && a[0] > 0) topLevelFolderStats.add(FolderStat(child, a[0], a[1].toInt()))
            }
        }
    } catch (e: Exception) { }

    val sortedFolderStats = topLevelFolderStats.sortedByDescending { it.bytes }

    val iconMap = mapOf(
        "Images" to Icons.Filled.Image,
        "Videos" to Icons.Filled.Movie,
        "Audio" to Icons.Filled.AudioFile,
        "Documents" to Icons.Filled.Description,
        "APK" to Icons.Filled.Apps,
        "ZIP" to Icons.Filled.FolderOpen,
        "Other" to Icons.Filled.InsertDriveFile
    )

    val categories = catStats.map { (k, v) ->
        StorageCategory(
            label = k,
            icon = iconMap[k] ?: Icons.Filled.InsertDriveFile,
            bytes = v[0],
            count = v[1].toInt()
        )
    }.sortedByDescending { it.bytes }

    val total = root.totalSpace
    val free = root.freeSpace
    val used = total - free

    return AnalyzerResult(
        categories = categories,
        largestFiles = topFiles,
        usedBytes = used,
        freeBytes = free,
        largestFolder = sortedFolderStats.firstOrNull()?.let { it.folder.name to it.bytes },
        folderStats = sortedFolderStats,
        filesByCategory = filesByCategory.mapValues { (_, list) ->
            list.sortedByDescending { it.length() }
        }
    )
}

// ===== APP MANAGER =====

fun loadInstalledApps(context: Context): List<AppInfo> {
    val pm = context.packageManager
    val apps = mutableListOf<AppInfo>()

    val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        pm.getInstalledPackages(0)
    }

    for (pkg in packages) {
        try {
            val appInfo = pkg.applicationInfo ?: continue
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            val apkPath = appInfo.sourceDir ?: ""
            val size = try {
                File(apkPath).length()
            } catch (e: Exception) {
                0L
            }

            val appName = try {
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                pkg.packageName
            }

            apps.add(
                AppInfo(
                    name = appName,
                    packageName = pkg.packageName,
                    versionName = pkg.versionName ?: "—",
                    versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        pkg.longVersionCode
                    } else {
                        @Suppress("DEPRECATION")
                        pkg.versionCode.toLong()
                    },
                    apkPath = apkPath,
                    size = size,
                    isSystem = isSystem,
                    installedTime = pkg.firstInstallTime,
                    updatedTime = pkg.lastUpdateTime
                )
            )
        } catch (e: Exception) {
        }
    }

    return apps.sortedBy { it.name.lowercase() }
}

fun backupApk(app: AppInfo, onProgress: (Int) -> Unit): File? {
    try {
        val source = File(app.apkPath)
        if (!source.exists()) return null

        val backupDir = File(
            Environment.getExternalStorageDirectory(),
            "VSFileExplorer/AppBackups"
        )
        if (!backupDir.exists()) backupDir.mkdirs()

        val safeName = app.name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val outFile = File(
            backupDir,
            "${safeName}_${app.versionName}_${System.currentTimeMillis()}.apk"
        )

        val total = source.length().coerceAtLeast(1L)
        var copied = 0L

        FileInputStream(source).use { fis ->
            FileOutputStream(outFile).use { fos ->
                val buf = ByteArray(64 * 1024)
                var read: Int
                while (fis.read(buf).also { read = it } != -1) {
                    fos.write(buf, 0, read)
                    copied += read
                    onProgress(((copied * 100) / total).toInt().coerceIn(0, 100))
                }
            }
        }
        return outFile
    } catch (e: Exception) {
        return null
    }
}

fun formatSizePrecise(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.6f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.6f MB", mb)
    val gb = mb / 1024.0
    return String.format("%.6f GB", gb)
}

@Composable
fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String = ""
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
            )
            if (subtitle.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }
    }
}