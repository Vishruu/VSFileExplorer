@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.vishruu.vsfileexplorer

import androidx.compose.foundation.ExperimentalFoundationApi
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Environment
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

fun drawableToBitmap(drawable: Drawable): Bitmap? {
    if (drawable is BitmapDrawable) {
        drawable.bitmap?.let { return it }
    }
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

@Composable
fun AppIcon(
    context: Context,
    packageName: String,
    size: androidx.compose.ui.unit.Dp = 44.dp
) {
    var bitmap by remember(packageName) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(packageName) {
        bitmap = withContext(Dispatchers.IO) {
            try {
                val drawable = context.packageManager.getApplicationIcon(packageName)
                drawableToBitmap(drawable)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Apps,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppManagerScreen(
    context: Context,
    isLoading: Boolean,
    apps: List<AppInfo>,
    filterUserOnly: Boolean,
    searchQuery: String,
    selectedApp: AppInfo?,
    showMenuDialog: Boolean,
    backupRunning: Boolean,
    backupProgress: Int,
    onBack: () -> Unit,
    onFilterChange: (Boolean) -> Unit,
    onSearchChange: (String) -> Unit,
    onAppTap: (AppInfo) -> Unit,
    onMenuDismiss: () -> Unit,
    onOpenApp: (AppInfo) -> Unit,
    onAppInfo: (AppInfo) -> Unit,
    onUninstall: (AppInfo) -> Unit,
    onBackup: (AppInfo) -> Unit,
    onShareApk: (AppInfo) -> Unit,
    onShareLink: (AppInfo) -> Unit
) {
    // Tabs: 0 = User, 1 = System, 2 = Backed-up
    var tab by remember { mutableStateOf(0) }
    var selectedPaths by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showSelectionMenu by remember { mutableStateOf(false) }
    var backupFolderFiles by remember { mutableStateOf<List<File>>(emptyList()) }

    // Load backed-up files
    LaunchedEffect(tab) {
        if (tab == 2) {
            backupFolderFiles = withContext(Dispatchers.IO) {
                try {
                    val dir = File(
                        Environment.getExternalStorageDirectory(),
                        "VSFileExplorer/AppBackups"
                    )
                    if (dir.exists() && dir.isDirectory) {
                        dir.listFiles()
                            ?.filter { it.isFile && it.extension.lowercase() == "apk" }
                            ?.sortedByDescending { it.lastModified() }
                            ?: emptyList()
                    } else emptyList()
                } catch (e: Exception) { emptyList() }
            }
        }
    }

    val filtered = remember(apps, tab, searchQuery) {
        when (tab) {
            0 -> apps.filter { !it.isSystem }
            1 -> apps.filter { it.isSystem }
            else -> emptyList()
        }.filter {
            searchQuery.isEmpty() ||
                    it.name.lowercase().contains(searchQuery.lowercase()) ||
                    it.packageName.lowercase().contains(searchQuery.lowercase())
        }
    }

    val selectionActive = selectedPaths.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ===== TOP BAR =====
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionActive) {
                IconButton(onClick = { selectedPaths = emptySet() }) {
                    Text("✕", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                }
                Text("${selectedPaths.size} selected", fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f))
                Box {
                    IconButton(onClick = { showSelectionMenu = true }) {
                        Icon(Icons.Filled.MoreVert, "Menu",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                    DropdownMenu(
                        expanded = showSelectionMenu,
                        onDismissRequest = { showSelectionMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Backup APKs") },
                            leadingIcon = { Icon(Icons.Filled.Backup, null) },
                            onClick = {
                                showSelectionMenu = false
                                selectedPaths.forEach { pkg ->
                                    val app = apps.firstOrNull { it.packageName == pkg }
                                    if (app != null) onBackup(app)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Share APKs") },
                            leadingIcon = { Icon(Icons.Filled.Share, null) },
                            onClick = {
                                showSelectionMenu = false
                                val first = selectedPaths.firstOrNull()
                                if (first != null) {
                                    apps.firstOrNull { it.packageName == first }?.let {
                                        onShareApk(it)
                                    }
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("App Info") },
                            leadingIcon = { Icon(Icons.Filled.Info, null) },
                            onClick = {
                                showSelectionMenu = false
                                val first = selectedPaths.firstOrNull()
                                if (first != null) {
                                    apps.firstOrNull { it.packageName == first }?.let {
                                        onAppInfo(it)
                                    }
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Uninstall") },
                            leadingIcon = { Icon(Icons.Filled.Delete, null) },
                            onClick = {
                                showSelectionMenu = false
                                selectedPaths.forEach { pkg ->
                                    val app = apps.firstOrNull { it.packageName == pkg }
                                    if (app != null && !app.isSystem) onUninstall(app)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Select all") },
                            leadingIcon = { Icon(Icons.Filled.DoneAll, null) },
                            onClick = {
                                showSelectionMenu = false
                                selectedPaths = filtered.map { it.packageName }.toSet()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Deselect all") },
                            leadingIcon = { Icon(Icons.Filled.CheckCircle, null) },
                            onClick = {
                                showSelectionMenu = false
                                selectedPaths = emptySet()
                            }
                        )
                    }
                }
            } else {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                        tint = MaterialTheme.colorScheme.primary)
                }
                Text("App Manager", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f))
            }
        }

        // ===== SEARCH BAR =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Search, null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                modifier = Modifier.padding(end = 12.dp))
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text("Search apps...", fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier.weight(1f)
            )
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchChange("") }) {
                    Icon(Icons.Filled.Clear, "Clear",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f))
                }
            }
        }

        // ===== TABS =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppTab("User Apps", tab == 0) { tab = 0; selectedPaths = emptySet() }
            AppTab("System", tab == 1) { tab = 1; selectedPaths = emptySet() }
            AppTab("Backed-up", tab == 2) { tab = 2; selectedPaths = emptySet() }
        }

        Spacer(Modifier.height(6.dp))

        // ===== COUNT =====
        Text(
            text = when {
                isLoading -> "Loading..."
                tab == 2 -> "${backupFolderFiles.size} backed-up APK(s)"
                filtered.isEmpty() -> "No apps"
                filtered.size == 1 -> "1 app"
                else -> "${filtered.size} apps"
            },
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(6.dp))

        // ===== LIST =====
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    Text("Loading installed apps...", fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f))
                }
            }
        } else if (tab == 2) {
            // Backed-up APKs list
            if (backupFolderFiles.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Backup, null,
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("No backed-up apps", fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f))
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(backupFolderFiles) { file ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { openFile(context, file) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Android, null,
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(file.name, fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1)
                                Spacer(Modifier.height(2.dp))
                                Text(formatDate(file.lastModified()), fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                            }
                            Text(formatSize(file.length()), fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(filtered) { app ->
                    val isSelected = selectedPaths.contains(app.packageName)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.background
                            )
                            .combinedClickable(
                                onClick = {
                                    if (selectionActive) {
                                        selectedPaths = if (isSelected)
                                            selectedPaths - app.packageName
                                        else selectedPaths + app.packageName
                                    } else onAppTap(app)
                                },
                                onLongClick = {
                                    selectedPaths = selectedPaths + app.packageName
                                }
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectionActive) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = {
                                    selectedPaths = if (isSelected)
                                        selectedPaths - app.packageName
                                    else selectedPaths + app.packageName
                                }
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        AppIcon(
                            context = context,
                            packageName = app.packageName,
                            size = 48.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(app.name, fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1)
                            Spacer(Modifier.height(2.dp))
                            Text("v${app.versionName}", fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(formatSize(app.size), fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(2.dp))
                            Text(formatDate(app.updatedTime), fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }

    // App detail menu dialog (single tap)
    if (showMenuDialog && selectedApp != null) {
        AlertDialog(
            onDismissRequest = onMenuDismiss,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(context, selectedApp.packageName, 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(selectedApp.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(selectedApp.packageName, fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f))
                    Spacer(Modifier.height(4.dp))
                    Text("v${selectedApp.versionName}  •  ${formatSize(selectedApp.size)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                    if (selectedApp.isSystem) {
                        Spacer(Modifier.height(4.dp))
                        Text("System app", fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(16.dp))
                    AppActionRow("Open") { onOpenApp(selectedApp) }
                    AppActionRow("App Info") { onAppInfo(selectedApp) }
                    AppActionRow("Backup APK") { onBackup(selectedApp) }
                    AppActionRow("Share APK") { onShareApk(selectedApp) }
                    AppActionRow("Share Play Store link") { onShareLink(selectedApp) }
                    if (!selectedApp.isSystem) {
                        AppActionRow(
                            text = "Uninstall",
                            textColor = MaterialTheme.colorScheme.error
                        ) { onUninstall(selectedApp) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onMenuDismiss) { Text("Close") }
            }
        )
    }

    // Backup progress
    if (backupRunning) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Backing up...") },
            text = {
                Column {
                    LinearProgressIndicator(
                        progress = { backupProgress / 100f },
                        modifier = Modifier.fillMaxWidth().height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("$backupProgress%", fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground)
                }
            },
            confirmButton = { }
        )
    }
}

@Composable
private fun AppTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun AppActionRow(
    text: String,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    onClick: () -> Unit
) {
    Text(
        text = text,
        fontSize = 15.sp,
        color = textColor,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp)
    )
}

@Composable
fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) Color(0xFFFFFFFF) else Color(0xFF1A1A1F)
    val fg = if (selected) Color(0xFF000000) else Color(0xFFFFFFFF)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}