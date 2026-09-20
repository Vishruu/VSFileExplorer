package com.vishruu.vsfileexplorer

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.FileProvider
import java.io.File

fun isEditableTextFile(file: File): Boolean {
    if (file.isDirectory) return false
    val ext = file.extension.lowercase()
    return ext in listOf(
        "txt", "md", "log", "json", "xml", "csv", "html", "htm",
        "css", "js", "kt", "java", "py", "c", "cpp", "h", "sh",
        "yml", "yaml", "ini", "cfg", "properties", "gradle"
    )
}

fun isImageFile(file: File): Boolean {
    val ext = file.extension.lowercase()
    return ext in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif")
}

fun getFileType(file: File): FileType {
    if (file.isDirectory) return FileType.FOLDER
    if (file.name.endsWith(CryptoUtils.VS_EXTENSION)) return FileType.ENCRYPTED
    return when (file.extension.lowercase()) {
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif" -> FileType.IMAGE
        "mp4", "mkv", "avi", "mov", "webm", "3gp", "flv", "m4v" -> FileType.VIDEO
        "mp3", "wav", "aac", "ogg", "flac", "m4a", "opus", "wma" -> FileType.AUDIO
        "pdf" -> FileType.PDF
        "apk", "xapk", "apks" -> FileType.APK
        "zip", "rar", "7z", "tar", "gz", "bz2" -> FileType.ZIP
        "txt", "log", "md" -> FileType.TEXT
        "doc", "docx", "odt", "rtf" -> FileType.WORD
        "xls", "xlsx", "csv", "ods" -> FileType.EXCEL
        "ppt", "pptx", "odp" -> FileType.PPT
        "kt", "java", "py", "js", "html", "css", "xml", "json", "c", "cpp", "h" -> FileType.CODE
        else -> FileType.UNKNOWN
    }
}

fun getFileIcon(type: FileType): ImageVector {
    return when (type) {
        FileType.FOLDER -> Icons.Filled.Folder
        FileType.IMAGE -> Icons.Filled.Image
        FileType.VIDEO -> Icons.Filled.Movie
        FileType.AUDIO -> Icons.Filled.MusicNote
        FileType.PDF -> Icons.Filled.PictureAsPdf
        FileType.APK -> Icons.Filled.Android
        FileType.ZIP -> Icons.Filled.FolderZip
        FileType.TEXT -> Icons.Filled.Description
        FileType.WORD -> Icons.Filled.Description
        FileType.EXCEL -> Icons.Filled.TableChart
        FileType.PPT -> Icons.Filled.Slideshow
        FileType.CODE -> Icons.Filled.Code
        FileType.ENCRYPTED -> Icons.Filled.Lock
        else -> Icons.Filled.InsertDriveFile
    }
}

fun getFileIconColor(type: FileType): Color {
    return when (type) {
        FileType.FOLDER -> Color(0xFFFFB74D)
        FileType.IMAGE -> Color(0xFF4CAF50)
        FileType.VIDEO -> Color(0xFFE91E63)
        FileType.AUDIO -> Color(0xFF9C27B0)
        FileType.PDF -> Color(0xFFE53935)
        FileType.APK -> Color(0xFF3DDC84)
        FileType.ZIP -> Color(0xFFFF9800)
        FileType.TEXT -> Color(0xFF90A4AE)
        FileType.WORD -> Color(0xFF2196F3)
        FileType.EXCEL -> Color(0xFF66BB6A)
        FileType.PPT -> Color(0xFFFF5722)
        FileType.CODE -> Color(0xFF00BCD4)
        FileType.ENCRYPTED -> Color(0xFF8B5CF6)
        else -> Color(0xFF9E9E9E)
    }
}

fun loadImageThumbnail(file: File, targetSize: Int): Bitmap? {
    return try {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, opts)
        var sample = 1
        while (opts.outWidth / sample > targetSize || opts.outHeight / sample > targetSize) {
            sample *= 2
        }
        val opts2 = BitmapFactory.Options().apply { inSampleSize = sample }
        BitmapFactory.decodeFile(file.absolutePath, opts2)
    } catch (e: Exception) {
        null
    }
}

fun loadAudioArt(file: File): Bitmap? {
    return try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(file.absolutePath)
        val art = retriever.embeddedPicture
        retriever.release()
        if (art != null) BitmapFactory.decodeByteArray(art, 0, art.size) else null
    } catch (e: Exception) {
        null
    }
}

fun loadVideoThumb(file: File): Bitmap? {
    return try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(file.absolutePath)
        val frame = retriever.getFrameAtTime(
            1_000_000L,
            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
        )
        retriever.release()
        frame
    } catch (e: Exception) {
        null
    }
}

fun formatDate(millis: Long): String {
    return try {
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
        sdf.format(java.util.Date(millis))
    } catch (e: Exception) {
        "—"
    }
}

fun openFile(context: Context, file: File) {
    try {
        // Recent files mein add karo
        try { RecentFilesStore.addRecent(context, file.absolutePath) } catch (_: Exception) {}

        // Text file → built-in editor
        if (isEditableTextFile(file)) {
            TextEditorHub.open(file)
            return
        }

        // PDF file → built-in PDF viewer
        if (file.extension.lowercase() == "pdf") {
            PdfViewerHub.open(file)
            return
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val mime = getMimeType(file.name)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: android.content.ActivityNotFoundException) {
        Toast.makeText(
            context,
            "No app found to open ${file.extension.uppercase()} files",
            Toast.LENGTH_LONG
        ).show()
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "Cannot open: ${e.message ?: "Unknown error"}",
            Toast.LENGTH_LONG
        ).show()
    }
}

fun searchFiles(dir: File, query: String, maxResults: Int): List<File> {
    val results = mutableListOf<File>()
    val queue = ArrayDeque<File>()
    queue.add(dir)
    val lowerQuery = query.lowercase()

    while (queue.isNotEmpty() && results.size < maxResults) {
        val current = queue.removeFirst()
        val children = current.listFiles() ?: continue
        for (child in children) {
            if (child.name.lowercase().contains(lowerQuery)) {
                results.add(child)
                if (results.size >= maxResults) break
            }
            if (child.isDirectory) queue.add(child)
        }
    }
    return results
}

fun shareFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = getMimeType(file.name)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share via"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun getMimeType(fileName: String): String {
    return when (fileName.substringAfterLast('.').lowercase()) {
        "jpg", "jpeg", "png", "gif", "webp", "bmp" -> "image/*"
        "mp4", "mkv", "avi", "mov", "webm" -> "video/*"
        "mp3", "wav", "aac", "ogg", "flac" -> "audio/*"
        "pdf" -> "application/pdf"
        "txt" -> "text/plain"
        "zip" -> "application/zip"
        "apk" -> "application/vnd.android.package-archive"
        else -> "*/*"
    }
}

fun loadFiles(path: String, sortMode: SortMode, showHidden: Boolean): List<File> {
    val fileList: Array<File> = File(path).listFiles() ?: return emptyList()
    val visible: List<File> = if (showHidden) {
        fileList.toList()
    } else {
        fileList.filter { f -> !f.name.startsWith(".") }
    }
    val folders: List<File> = visible.filter { f -> f.isDirectory }
    val filesOnly: List<File> = visible.filter { f -> !f.isDirectory }

    val sortedFolders = sortFileList(folders, sortMode)
    val sortedFiles = sortFileList(filesOnly, sortMode)

    return sortedFolders + sortedFiles
}

fun sortFileList(list: List<File>, sortMode: SortMode): List<File> {
    return when (sortMode) {
        SortMode.NAME_ASC -> list.sortedBy { it.name.lowercase() }
        SortMode.NAME_DESC -> list.sortedByDescending { it.name.lowercase() }
        SortMode.SIZE_ASC -> list.sortedBy { it.length() }
        SortMode.SIZE_DESC -> list.sortedByDescending { it.length() }
        SortMode.DATE_ASC -> list.sortedBy { it.lastModified() }
        SortMode.DATE_DESC -> list.sortedByDescending { it.lastModified() }
        SortMode.TYPE -> list.sortedBy {
            it.extension.lowercase() + "_" + it.name.lowercase()
        }
    }
}

fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.2f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.2f MB", mb)
    val gb = mb / 1024.0
    return String.format("%.2f GB", gb)
}

fun deleteRecursive(file: File) {
    if (file.isDirectory) {
        file.listFiles()?.forEach { deleteRecursive(it) }
    }
    file.delete()
}

fun getUniqueFile(destFolder: File, name: String): File {
    var newFile = File(destFolder, name)
    if (!newFile.exists()) return newFile
    val baseName = name.substringBeforeLast('.', name)
    val ext = if (name.contains('.')) "." + name.substringAfterLast('.') else ""
    var i = 1
    while (newFile.exists()) {
        newFile = File(destFolder, "$baseName ($i)$ext")
        i++
    }
    return newFile
}

fun copyDirectory(src: File, dest: File) {
    if (!dest.exists()) dest.mkdirs()
    src.listFiles()?.forEach { child ->
        val newChild = File(dest, child.name)
        if (child.isDirectory) copyDirectory(child, newChild)
        else child.copyTo(newChild, overwrite = false)
    }
}

fun pasteFiles(clipboard: ClipboardData, destFolder: File): Int {
    var count = 0
    clipboard.paths.forEach { path ->
        val src = File(path)
        if (!src.exists()) return@forEach
        val dest = getUniqueFile(destFolder, src.name)
        try {
            if (clipboard.isMove) {
                if (src.renameTo(dest)) {
                    count++
                } else {
                    if (src.isDirectory) copyDirectory(src, dest)
                    else src.copyTo(dest, overwrite = false)
                    deleteRecursive(src)
                    count++
                }
            } else {
                if (src.isDirectory) copyDirectory(src, dest)
                else src.copyTo(dest, overwrite = false)
                count++
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return count
}