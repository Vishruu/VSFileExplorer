package com.vishruu.vsfileexplorer

import android.content.Context
import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

// Category color tints (subtle, on dark bg)
private val catImages = Color(0xFFFFB300)
private val catMovies = Color(0xFFE91E63)
private val catDocs = Color(0xFF2196F3)
private val catMusic = Color(0xFFFF5252)
private val catApps = Color(0xFF4CAF50)
private val catZip = Color(0xFFFF9800)
private val catLock = Color(0xFF9C27B0)
private val catNewFolder = Color(0xFF00BCD4)
private val catDownload = Color(0xFFFFC107)
private val catAnalyzer = Color(0xFF3F51B5)
private val catStar = Color(0xFFFFC107)
private val catRecycle = Color(0xFF9C27B0)
private val catHidden = Color(0xFF607D8B)
private val catTools = Color(0xFF00ACC1)

@Composable
fun HomeScreen(
    context: Context,
    onOpenDrawer: () -> Unit,
    onStorageClick: () -> Unit,
    onCategoryClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAnalyzerClick: () -> Unit,
    onOpenAppManager: () -> Unit,
    onQuickAccessClick: (String) -> Unit
) {
    val rootPath = Environment.getExternalStorageDirectory().absolutePath
    val internal = File(rootPath)
    val totalBytes = internal.totalSpace
    val freeBytes = internal.freeSpace
    val usedBytes = totalBytes - freeBytes
    val usedPercent = if (totalBytes > 0) ((usedBytes * 100) / totalBytes).toInt() else 0

    val recentFiles: List<File> = remember {
        try {
            internal.listFiles()
                ?.filter { !it.isDirectory && !it.name.startsWith(".") }
                ?.sortedByDescending { it.lastModified() }
                ?.take(6) ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    var showTopMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ===== TOP BAR =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onOpenDrawer) {
                Icon(Icons.Filled.Menu, "Menu",
                    tint = MaterialTheme.colorScheme.primary)
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Storage, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Home",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Filled.Search, "Search",
                    tint = MaterialTheme.colorScheme.onBackground)
            }
            Box {
                IconButton(onClick = { showTopMenu = true }) {
                    Icon(Icons.Filled.MoreVert, "More",
                        tint = MaterialTheme.colorScheme.onBackground)
                }
                DropdownMenu(
                    expanded = showTopMenu,
                    onDismissRequest = { showTopMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("File Analyzer") },
                        onClick = { showTopMenu = false; onAnalyzerClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Settings") },
                        onClick = { showTopMenu = false; onSettingsClick() }
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 20.dp)
        ) {
            // ===== GREETING CARD =====
            val greeting = remember {
                val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                when {
                    hour < 12 -> "Good morning"
                    hour < 17 -> "Good afternoon"
                    hour < 21 -> "Good evening"
                    else -> "Good night"
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = greeting,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "Welcome to VS File Explorer",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                    Icon(
                        Icons.Filled.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // ===== CATEGORIES =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CategoryItem("Images", Icons.Filled.Image, catImages, "Images", onCategoryClick, Modifier.weight(1f))
                CategoryItem("Movies", Icons.Filled.Movie, catMovies, "Videos", onCategoryClick, Modifier.weight(1f))
                CategoryItem("Documents", Icons.Filled.Description, catDocs, "Documents", onCategoryClick, Modifier.weight(1f))
                CategoryItem("Music", Icons.Filled.AudioFile, catMusic, "Audio", onCategoryClick, Modifier.weight(1f))
                CategoryItem("APP", Icons.Filled.Apps, catApps, "APK", onCategoryClick, Modifier.weight(1f))
            }

            // ===== STORAGE CARDS ROW =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Internal Storage card
                Row(
                    modifier = Modifier
                        .weight(1.4f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onStorageClick() }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(52.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { usedPercent / 100f },
                            modifier = Modifier.size(52.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            strokeWidth = 5.dp
                        )
                        Text(
                            text = "$usedPercent%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Internal Storage",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "${formatSize(usedBytes)} / ${formatSize(totalBytes)}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                            maxLines = 1
                        )
                    }
                }

                // Space Analyzer card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onAnalyzerClick() }
                        .padding(14.dp)
                ) {
                    Icon(Icons.Filled.PieChart, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Space Analyzer",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1
                    )
                    Text(
                        text = "more files to clean",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        maxLines = 1
                    )
                }
            }

            // ===== TOOLS CARD =====
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(vertical = 12.dp)
            ) {
                // Row 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ToolItem("Encrypt", Icons.Filled.Lock, catLock) { onQuickAccessClick("encrypt") }
                    ToolItem("Zip", Icons.Filled.FolderZip, catZip) { onQuickAccessClick("zip") }
                    ToolItem("Apps", Icons.Filled.Apps, catApps) { onOpenAppManager() }
                    ToolItem("Download", Icons.Filled.Download, catDownload) { onQuickAccessClick("downloads") }
                    ToolItem("Analyzer", Icons.Filled.Analytics, catAnalyzer) { onAnalyzerClick() }
                }
                Spacer(Modifier.height(14.dp))
                // Row 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ToolItem("Bookmarks", Icons.Filled.Star, catStar) {
                        onQuickAccessClick("bookmarks")
                    }
                    ToolItem("Recycle", Icons.Filled.Delete, catRecycle) {
                        onQuickAccessClick("recycle")
                    }
                    ToolItem("New", Icons.Filled.CreateNewFolder, catNewFolder) {
                        onQuickAccessClick("new")
                    }
                    ToolItem("Hidden", Icons.Filled.VisibilityOff, catHidden) {
                        onQuickAccessClick("hidden")
                    }
                    ToolItem("Tools", Icons.Filled.GridView, catTools) {
                        onQuickAccessClick("toolbox")
                    }
                }
            }

            // ===== NEW FILES SECTION =====
            if (recentFiles.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "New Files",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Filled.Visibility, null,
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(12.dp))
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                            modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "A moment ago  •  ${recentFiles.size} files",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        recentFiles.forEach { file ->
                            val type = getFileType(file)
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable { openFile(context, file) },
                                contentAlignment = Alignment.Center
                            ) {
                                FileThumbnail(file = file, type = type, size = 72.dp)
                            }
                        }
                    }
                }
            }

            // ===== BOOKMARKS SECTION =====
            val bookmarks = remember { BookmarksStore.getBookmarks(context) }
            if (bookmarks.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Bookmarks",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    bookmarks.forEach { path ->
                        val f = File(path)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onQuickAccessClick(path) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(f.name, fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1)
                                Text(path, fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    maxLines = 1)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CategoryItem(
    label: String,
    icon: ImageVector,
    tint: Color,
    key: String,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick(key) }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ToolItem(
    label: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(64.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tint.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1
        )
    }
}