package com.vishruu.vsfileexplorer

import androidx.compose.ui.graphics.vector.ImageVector
import java.io.File

data class ClipboardData(
    val paths: List<String>,
    val isMove: Boolean
)

data class CategoryFilter(
    val label: String,
    val extensions: List<String> = emptyList(),
    val isEncryptedOnly: Boolean = false,
    val isHiddenOnly: Boolean = false
)

data class CategoryFolder(
    val folder: File,
    val count: Int
)

data class StorageCategory(
    val label: String,
    val icon: ImageVector,
    val bytes: Long,
    val count: Int
)

data class FolderStat(
    val folder: File,
    val bytes: Long,
    val fileCount: Int
)

data class BigFileEntry(
    val file: File,
    val type: String
)

data class AppInfo(
    val name: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val apkPath: String,
    val size: Long,
    val isSystem: Boolean,
    val installedTime: Long,
    val updatedTime: Long
)

data class AnalyzerResult(
    val categories: List<StorageCategory>,
    val largestFiles: List<File>,
    val usedBytes: Long,
    val freeBytes: Long,
    val largestFolder: Pair<String, Long>?,
    val folderStats: List<FolderStat>,
    val filesByCategory: Map<String, List<File>>
)

enum class SortMode(val label: String) {
    NAME_ASC("Name (A → Z)"),
    NAME_DESC("Name (Z → A)"),
    SIZE_ASC("Size (Small → Large)"),
    SIZE_DESC("Size (Large → Small)"),
    DATE_ASC("Date (Old → New)"),
    DATE_DESC("Date (New → Old)"),
    TYPE("Type")
}

enum class ViewMode(val label: String) {
    LARGE_ICON("Large Icons"),
    MEDIUM_ICON("Medium Icons"),
    SMALL_ICON("Small Icons"),
    LARGE_LIST("Large List"),
    MEDIUM_LIST("Medium List"),
    SMALL_LIST("Small List"),
    LARGE_DETAIL("Large Details"),
    MEDIUM_DETAIL("Medium Details"),
    SMALL_DETAIL("Small Details")
}

enum class FileType(val label: String) {
    FOLDER("Folder"),
    IMAGE("Image"),
    VIDEO("Video"),
    AUDIO("Audio"),
    PDF("PDF"),
    APK("App"),
    ZIP("Archive"),
    TEXT("Text"),
    WORD("Word"),
    EXCEL("Excel"),
    PPT("Presentation"),
    CODE("Code"),
    ENCRYPTED("Encrypted"),
    UNKNOWN("File")
}

enum class SearchSizeFilter(val label: String) {
    ANY("Any size"),
    SMALL("< 1 MB"),
    MEDIUM("1 - 10 MB"),
    LARGE("10 - 100 MB"),
    HUGE("> 100 MB")
}

enum class SearchTypeFilter(val label: String) {
    ANY("Any type"),
    FILES("Files only"),
    FOLDERS("Folders only"),
    IMAGES("Images"),
    VIDEOS("Videos"),
    AUDIO("Audio"),
    DOCS("Documents")
}

enum class SearchDateFilter(val label: String) {
    ANY("Any time"),
    TODAY("Today"),
    WEEK("This week"),
    MONTH("This month"),
    OLDER("Older")
}