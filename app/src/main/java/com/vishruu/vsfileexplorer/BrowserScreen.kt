package com.vishruu.vsfileexplorer

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

fun iconSizeFor(mode: ViewMode): Dp = when (mode) {
    ViewMode.LARGE_ICON -> 96.dp
    ViewMode.MEDIUM_ICON -> 64.dp
    ViewMode.SMALL_ICON -> 40.dp
    else -> 48.dp
}

fun gridColumnsFor(mode: ViewMode): Int = when (mode) {
    ViewMode.LARGE_ICON -> 3
    ViewMode.MEDIUM_ICON -> 4
    ViewMode.SMALL_ICON -> 6
    else -> 1
}

fun isIconMode(mode: ViewMode): Boolean = mode in listOf(
    ViewMode.LARGE_ICON, ViewMode.MEDIUM_ICON, ViewMode.SMALL_ICON
)

@Composable
fun FileThumbnail(
    file: File,
    type: FileType,
    size: Dp,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(file.absolutePath) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(file.absolutePath, size) {
        bitmap = withContext(Dispatchers.IO) {
            val bmp = when (type) {
                FileType.IMAGE -> loadImageThumbnail(file, size.value.toInt() * 2)
                FileType.AUDIO -> loadAudioArt(file)
                FileType.VIDEO -> loadVideoThumb(file)
                else -> null
            }
            bmp?.asImageBitmap()
        }
    }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = getFileIcon(type),
                contentDescription = null,
                tint = getFileIconColor(type),
                modifier = Modifier.size(size * 0.65f)
            )
        }
    }
}

@Composable
fun ViewModeTile(
    mode: ViewMode,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val bg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    else MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth().height(40.dp), contentAlignment = Alignment.Center) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(2) {
                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = mode.label,
            fontSize = 10.sp,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            textAlign = TextAlign.Center,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun SortOptionTile(mode: SortMode, selected: Boolean, onClick: () -> Unit) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val bg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    else MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = mode.label,
            fontSize = 11.sp,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 2
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    context: Context,
    currentPath: String,
    files: List<File>,
    selectedPaths: Set<String>,
    clipboard: ClipboardData?,
    sortMode: SortMode,
    viewMode: ViewMode,
    showHidden: Boolean,
    rootPath: String,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onPathChange: (String) -> Unit,
    onSelectionChange: (Set<String>) -> Unit,
    onClipboardChange: (ClipboardData?) -> Unit,
    onOpenSearch: () -> Unit,
    onSortChange: (SortMode) -> Unit,
    onViewModeChange: (ViewMode) -> Unit,
    onOpenNewFolder: () -> Unit,
    onOpenNewFile: (String) -> Unit,
    onOpenRename: (String) -> Unit,
    onOpenDelete: () -> Unit,
    onOpenEncrypt: () -> Unit,
    onOpenDecrypt: (String) -> Unit,
    onOpenDecryptMulti: () -> Unit,
    onOpenZip: () -> Unit,
    onOpenUnzip: (String) -> Unit,
    onOpenImage: (File) -> Unit,
    onOpenAudio: (File) -> Unit,
    onOpenAnalyzer: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onShowProperties: (File) -> Unit,
    onAddBookmark: () -> Unit,
    onHideSelected: () -> Unit,
    onUnhideSelected: () -> Unit,
    onOpenShred: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showViewDialog by remember { mutableStateOf(false) }
    var showSelectionMenu by remember { mutableStateOf(false) }

    val selectionActive = selectedPaths.isNotEmpty()
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionActive) {
                IconButton(onClick = { onSelectionChange(emptySet()) }) {
                    Text(
                        text = "✕",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "${selectedPaths.size} selected",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
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
                            text = { Text("Cut") },
                            leadingIcon = { Icon(Icons.Filled.ContentCut, null) },
                            onClick = {
                                showSelectionMenu = false
                                onClipboardChange(ClipboardData(selectedPaths.toList(), isMove = true))
                                onSelectionChange(emptySet())
                                Toast.makeText(context, "Cut. Navigate and paste.", Toast.LENGTH_SHORT).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Copy") },
                            leadingIcon = { Icon(Icons.Filled.ContentCopy, null) },
                            onClick = {
                                showSelectionMenu = false
                                onClipboardChange(ClipboardData(selectedPaths.toList(), isMove = false))
                                onSelectionChange(emptySet())
                                Toast.makeText(context, "Copied. Navigate and paste.", Toast.LENGTH_SHORT).show()
                            }
                        )

                        if (selectedPaths.size == 1) {
                            val singlePath = selectedPaths.first()
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                leadingIcon = { Icon(Icons.Filled.Edit, null) },
                                onClick = {
                                    showSelectionMenu = false
                                    onOpenRename(singlePath)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share") },
                                leadingIcon = { Icon(Icons.Filled.Share, null) },
                                onClick = {
                                    showSelectionMenu = false
                                    shareFile(context, File(singlePath))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Bookmark") },
                                leadingIcon = { Icon(Icons.Filled.Bookmark, null) },
                                onClick = {
                                    showSelectionMenu = false
                                    onAddBookmark()
                                }
                            )
                            if (singlePath.endsWith(CryptoUtils.VS_EXTENSION)) {
                                DropdownMenuItem(
                                    text = { Text("Decrypt") },
                                    leadingIcon = { Icon(Icons.Filled.LockOpen, null) },
                                    onClick = {
                                        showSelectionMenu = false
                                        onOpenDecrypt(singlePath)
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Encrypt") },
                                    leadingIcon = { Icon(Icons.Filled.Lock, null) },
                                    onClick = {
                                        showSelectionMenu = false
                                        onOpenEncrypt()
                                    }
                                )
                            }
                            if (singlePath.lowercase().endsWith(".zip")) {
                                DropdownMenuItem(
                                    text = { Text("Unzip") },
                                    leadingIcon = { Icon(Icons.Filled.FolderZip, null) },
                                    onClick = {
                                        showSelectionMenu = false
                                        onOpenUnzip(singlePath)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Extract here") },
                                    leadingIcon = { Icon(Icons.Filled.Download, null) },
                                    onClick = {
                                        showSelectionMenu = false
                                        onOpenUnzip(singlePath)
                                    }
                                )
                            } else if (!File(singlePath).isDirectory) {
                                DropdownMenuItem(
                                    text = { Text("Zip") },
                                    leadingIcon = { Icon(Icons.Filled.FolderZip, null) },
                                    onClick = {
                                        showSelectionMenu = false
                                        onOpenZip()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Properties") },
                                onClick = {
                                    showSelectionMenu = false
                                    onShowProperties(File(singlePath))
                                }
                            )
                        } else {
                            val allEncrypted = selectedPaths.all {
                                it.endsWith(CryptoUtils.VS_EXTENSION)
                            }
                            if (allEncrypted) {
                                DropdownMenuItem(
                                    text = { Text("Decrypt all") },
                                    leadingIcon = { Icon(Icons.Filled.LockOpen, null) },
                                    onClick = {
                                        showSelectionMenu = false
                                        onOpenDecryptMulti()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Zip") },
                                leadingIcon = { Icon(Icons.Filled.FolderZip, null) },
                                onClick = {
                                    showSelectionMenu = false
                                    onOpenZip()
                                }
                            )
                            val canEncrypt = selectedPaths.none {
                                it.endsWith(CryptoUtils.VS_EXTENSION)
                            }
                            if (canEncrypt) {
                                DropdownMenuItem(
                                    text = { Text("Encrypt") },
                                    leadingIcon = { Icon(Icons.Filled.Lock, null) },
                                    onClick = {
                                        showSelectionMenu = false
                                        onOpenEncrypt()
                                    }
                                )
                            }
                        }

                        val anyVisible = selectedPaths.any { !File(it).name.startsWith(".") }
                        val anyHidden = selectedPaths.any { File(it).name.startsWith(".") }
                        if (anyVisible) {
                            DropdownMenuItem(
                                text = { Text("Hide") },
                                leadingIcon = { Icon(Icons.Filled.VisibilityOff, null) },
                                onClick = {
                                    showSelectionMenu = false
                                    onHideSelected()
                                }
                            )
                        }
                        if (anyHidden) {
                            DropdownMenuItem(
                                text = { Text("Unhide") },
                                leadingIcon = { Icon(Icons.Filled.Visibility, null) },
                                onClick = {
                                    showSelectionMenu = false
                                    onUnhideSelected()
                                }
                            )
                        }

                        DropdownMenuItem(
                            text = { Text("Shred (permanent)") },
                            leadingIcon = { Icon(Icons.Filled.DeleteForever, null) },
                            onClick = {
                                showSelectionMenu = false
                                onOpenShred()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = { Icon(Icons.Filled.Delete, null) },
                            onClick = {
                                showSelectionMenu = false
                                onOpenDelete()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Select all") },
                            leadingIcon = { Icon(Icons.Filled.DoneAll, null) },
                            onClick = {
                                showSelectionMenu = false
                                onSelectionChange(files.map { it.absolutePath }.toSet())
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Deselect all") },
                            leadingIcon = { Icon(Icons.Filled.CheckCircle, null) },
                            onClick = {
                                showSelectionMenu = false
                                onSelectionChange(emptySet())
                            }
                        )
                    }
                }
            } else {
                if (currentPath == rootPath) {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, "Menu",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    IconButton(onClick = {
                        val parent = File(currentPath).parentFile
                        if (parent != null) onPathChange(parent.absolutePath)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = File(currentPath).name.ifEmpty { "Internal Storage" },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                    Text(
                        text = "${files.size} items",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
                IconButton(onClick = onOpenSearch) {
                    Icon(Icons.Filled.Search, "Search",
                        tint = MaterialTheme.colorScheme.onBackground)
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, "Menu",
                            tint = MaterialTheme.colorScheme.onBackground)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("New folder") },
                            onClick = { showMenu = false; onOpenNewFolder() }
                        )
                        DropdownMenuItem(
                            text = { Text("New text file") },
                            onClick = { showMenu = false; onOpenNewFile("Text File") }
                        )
                        DropdownMenuItem(
                            text = { Text("New Excel file") },
                            onClick = { showMenu = false; onOpenNewFile("Excel File") }
                        )
                        DropdownMenuItem(
                            text = { Text("Refresh") },
                            leadingIcon = { Icon(Icons.Filled.Refresh, null) },
                            onClick = { showMenu = false; onRefresh() }
                        )
                        DropdownMenuItem(
                            text = { Text("View & Sort") },
                            leadingIcon = { Icon(Icons.Filled.GridView, null) },
                            onClick = { showMenu = false; showViewDialog = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Select all") },
                            leadingIcon = { Icon(Icons.Filled.DoneAll, null) },
                            onClick = {
                                showMenu = false
                                onSelectionChange(files.map { it.absolutePath }.toSet())
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("File Analyzer") },
                            leadingIcon = { Icon(Icons.Filled.Analytics, null) },
                            onClick = { showMenu = false; onOpenAnalyzer() }
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            leadingIcon = { Icon(Icons.Filled.Settings, null) },
                            onClick = { showMenu = false; onOpenSettings() }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onRefresh()
            },
            modifier = Modifier.weight(1f)
        ) {
            FadeIn {
            if (isRefreshing) {
                if (isIconMode(viewMode)) GridSkeleton() else ListSkeleton()
            } else if (files.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.FolderOpen,
                    title = "This folder is empty",
                    subtitle = "No files or folders here"
                )
            } else if (isIconMode(viewMode)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumnsFor(viewMode)),
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(files) { file ->
                        val isSelected = selectedPaths.contains(file.absolutePath)
                        val type = getFileType(file)
                        val iconSize = iconSizeFor(viewMode)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .combinedClickable(
                                    onClick = {
                                        if (selectionActive) {
                                            onSelectionChange(
                                                if (isSelected) selectedPaths - file.absolutePath
                                                else selectedPaths + file.absolutePath
                                            )
                                        } else if (file.isDirectory) {
                                            onPathChange(file.absolutePath)
                                        } else if (type == FileType.ENCRYPTED) {
                                            onOpenDecrypt(file.absolutePath)
                                        } else if (type == FileType.ZIP) {
                                            onOpenUnzip(file.absolutePath)
                                        } else if (type == FileType.IMAGE) {
                                            onOpenImage(file)
                                        } else if (type == FileType.AUDIO) {
                                            onOpenAudio(file)
                                        } else {
                                            openFile(context, file)
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSelectionChange(selectedPaths + file.absolutePath)
                                    }
                                )
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            FileThumbnail(file = file, type = type, size = iconSize)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = file.name,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(files) { file ->
                        val isSelected = selectedPaths.contains(file.absolutePath)
                        val type = getFileType(file)

                        val iconSize = when (viewMode) {
                            ViewMode.LARGE_LIST, ViewMode.LARGE_DETAIL -> 56.dp
                            ViewMode.MEDIUM_LIST, ViewMode.MEDIUM_DETAIL -> 44.dp
                            ViewMode.SMALL_LIST, ViewMode.SMALL_DETAIL -> 32.dp
                            else -> 44.dp
                        }
                        val showDetails = viewMode in listOf(
                            ViewMode.LARGE_DETAIL, ViewMode.MEDIUM_DETAIL, ViewMode.SMALL_DETAIL
                        )

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                .combinedClickable(
                                    onClick = {
                                        if (selectionActive) {
                                            onSelectionChange(
                                                if (isSelected) selectedPaths - file.absolutePath
                                                else selectedPaths + file.absolutePath
                                            )
                                        } else if (file.isDirectory) {
                                            onPathChange(file.absolutePath)
                                        } else if (type == FileType.ENCRYPTED) {
                                            onOpenDecrypt(file.absolutePath)
                                        } else if (type == FileType.ZIP) {
                                            onOpenUnzip(file.absolutePath)
                                        } else if (type == FileType.IMAGE) {
                                            onOpenImage(file)
                                        } else if (type == FileType.AUDIO) {
                                            onOpenAudio(file)
                                        } else {
                                            openFile(context, file)
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSelectionChange(selectedPaths + file.absolutePath)
                                    }
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (selectionActive) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = {
                                            onSelectionChange(
                                                if (isSelected) selectedPaths - file.absolutePath
                                                else selectedPaths + file.absolutePath
                                            )
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    Spacer(Modifier.width(4.dp))
                                }
                                FileThumbnail(file = file, type = type, size = iconSize)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.name,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1
                                    )
                                    if (showDetails) {
                                        Spacer(Modifier.height(3.dp))
                                        Text(
                                            text = buildString {
                                                append(type.label)
                                                if (!file.isDirectory) {
                                                    append(" • ")
                                                    append(formatSize(file.length()))
                                                }
                                            },
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                            maxLines = 1
                                        )
                                    } else if (!file.isDirectory && viewMode != ViewMode.SMALL_LIST) {
                                        Spacer(Modifier.height(3.dp))
                                        Text(
                                            text = formatSize(file.length()),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }
        }

        if (clipboard != null && !selectionActive) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${clipboard.paths.size} ready",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        val count = pasteFiles(clipboard, File(currentPath))
                        Toast.makeText(context,
                            if (count == 1) "Pasted" else "$count pasted",
                            Toast.LENGTH_SHORT).show()
                        onClipboardChange(null)
                        onPathChange(currentPath)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) { Text("Paste here") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onClipboardChange(null) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onBackground
                    )
                ) { Text("Cancel") }
            }
        }
    }

    if (showViewDialog) {
        AlertDialog(
            onDismissRequest = { showViewDialog = false },
            title = { Text("View & Sort", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().height(480.dp)) {
                    Text("View Mode", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(ViewMode.entries) { mode ->
                            ViewModeTile(mode, mode == viewMode) { onViewModeChange(mode) }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Sort by", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(SortMode.entries) { mode ->
                            SortOptionTile(mode, mode == sortMode) { onSortChange(mode) }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showViewDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}