package com.vishruu.vsfileexplorer

import androidx.compose.foundation.ExperimentalFoundationApi
import android.content.Context
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

fun getFolderSizeRecursive(folder: File): Pair<Long, Int> {
    var bytes = 0L
    var count = 0
    val q = ArrayDeque<File>()
    q.add(folder)
    while (q.isNotEmpty()) {
        val c = q.removeFirst()
        val kids = try { c.listFiles() } catch (e: Exception) { null } ?: continue
        for (k in kids) {
            if (k.isDirectory) q.add(k)
            else { bytes += k.length(); count++ }
        }
    }
    return bytes to count
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StorageAnalyzerScreen(
    context: Context,
    isRunning: Boolean,
    categories: List<StorageCategory>,
    largestFiles: List<File>,
    usedBytes: Long,
    freeBytes: Long,
    largestFolder: Pair<String, Long>?,
    folderStats: List<FolderStat>,
    filesByCategory: Map<String, List<File>>,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val totalBytes = usedBytes + freeBytes
    val usedPercent = if (totalBytes > 0) ((usedBytes * 100) / totalBytes).toInt() else 0

    // Navigation
    var currentFolder by remember { mutableStateOf<File?>(null) }
    var currentSubFolders by remember { mutableStateOf<List<FolderStat>>(emptyList()) }
    var currentFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var isLoadingFolder by remember { mutableStateOf(false) }

    // Selection
    var selectedPaths by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    // Category drill-down dialog
    var drillCategory by remember { mutableStateOf<String?>(null) }

    // Load folder contents when currentFolder changes
    LaunchedEffect(currentFolder) {
        val folder = currentFolder ?: return@LaunchedEffect
        isLoadingFolder = true
        val result = withContext(Dispatchers.IO) {
            val children = try { folder.listFiles() } catch (e: Exception) { null } ?: emptyArray<File>()
            val subFolders = mutableListOf<FolderStat>()
            val files = mutableListOf<File>()
            for (c in children) {
                if (c.name.startsWith(".")) continue
                if (c.isDirectory) {
                    val (size, count) = getFolderSizeRecursive(c)
                    if (size > 0) subFolders.add(FolderStat(c, size, count))
                } else {
                    files.add(c)
                }
            }
            subFolders.sortedByDescending { it.bytes } to files.sortedByDescending { it.length() }
        }
        currentSubFolders = result.first
        currentFiles = result.second
        isLoadingFolder = false
    }

    // ===== FOLDER DETAIL VIEW =====
    if (currentFolder != null) {
        val folder = currentFolder!!
        val selectionActive = selectedPaths.isNotEmpty()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // TOP BAR
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
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Filled.MoreVert, "Menu",
                                tint = MaterialTheme.colorScheme.primary)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Select all") },
                                leadingIcon = { Icon(Icons.Filled.DoneAll, null) },
                                onClick = {
                                    showMenu = false
                                    selectedPaths = (currentSubFolders.map { it.folder.absolutePath } +
                                            currentFiles.map { it.absolutePath }).toSet()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Deselect all") },
                                leadingIcon = { Icon(Icons.Filled.CheckCircle, null) },
                                onClick = {
                                    showMenu = false
                                    selectedPaths = emptySet()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                leadingIcon = { Icon(Icons.Filled.Delete, null) },
                                onClick = {
                                    showMenu = false
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                } else {
                    IconButton(onClick = {
                        val parent = folder.parentFile
                        if (parent != null && parent.absolutePath !=
                            android.os.Environment.getExternalStorageDirectory().absolutePath
                        ) {
                            currentFolder = parent
                        } else {
                            currentFolder = null
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(folder.name, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary, maxLines = 1)
                        Text(folder.absolutePath, fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            maxLines = 1)
                    }
                }
            }

            if (isLoadingFolder) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(12.dp))
                        Text("Calculating sizes...", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f))
                    }
                }
            } else {
                val totalInFolder = currentSubFolders.sumOf { it.bytes } +
                        currentFiles.sumOf { it.length() }

                // Summary line
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${currentSubFolders.size} folders • ${currentFiles.size} files • ${formatSizePrecise(totalInFolder)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                    )
                }

                LazyColumn(Modifier.fillMaxSize()) {
                    // Subfolders
                    if (currentSubFolders.isNotEmpty()) {
                        item {
                            Text("Folders", fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp))
                        }
                        items(currentSubFolders) { stat ->
                            val isSel = selectedPaths.contains(stat.folder.absolutePath)
                            val pct = if (totalInFolder > 0) ((stat.bytes * 100) / totalInFolder).toInt() else 0

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                    else MaterialTheme.colorScheme.background)
                                    .combinedClickable(
                                        onClick = {
                                            if (selectionActive) {
                                                selectedPaths = if (isSel)
                                                    selectedPaths - stat.folder.absolutePath
                                                else selectedPaths + stat.folder.absolutePath
                                            } else {
                                                currentFolder = stat.folder
                                            }
                                        },
                                        onLongClick = {
                                            selectedPaths = selectedPaths + stat.folder.absolutePath
                                        }
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (selectionActive) {
                                    Checkbox(checked = isSel, onCheckedChange = {
                                        selectedPaths = if (isSel)
                                            selectedPaths - stat.folder.absolutePath
                                        else selectedPaths + stat.folder.absolutePath
                                    })
                                    Spacer(Modifier.width(4.dp))
                                }
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Folder, null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(stat.folder.name, fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1)
                                    Spacer(Modifier.height(2.dp))
                                    Text("${stat.fileCount} files • $pct%", fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                                    Spacer(Modifier.height(3.dp))
                                    LinearProgressIndicator(
                                        progress = { pct / 100f },
                                        modifier = Modifier.fillMaxWidth().height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(formatSizePrecise(stat.bytes), fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    // Files
                    if (currentFiles.isNotEmpty()) {
                        item {
                            Text("Files", fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp))
                        }
                        items(currentFiles) { file ->
                            val isSel = selectedPaths.contains(file.absolutePath)
                            val type = getFileType(file)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                    else MaterialTheme.colorScheme.background)
                                    .combinedClickable(
                                        onClick = {
                                            if (selectionActive) {
                                                selectedPaths = if (isSel)
                                                    selectedPaths - file.absolutePath
                                                else selectedPaths + file.absolutePath
                                            } else openFile(context, file)
                                        },
                                        onLongClick = {
                                            selectedPaths = selectedPaths + file.absolutePath
                                        }
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (selectionActive) {
                                    Checkbox(checked = isSel, onCheckedChange = {
                                        selectedPaths = if (isSel)
                                            selectedPaths - file.absolutePath
                                        else selectedPaths + file.absolutePath
                                    })
                                    Spacer(Modifier.width(4.dp))
                                }
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(getFileIcon(type), null,
                                        tint = getFileIconColor(type),
                                        modifier = Modifier.size(22.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(file.name, fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1)
                                    Spacer(Modifier.height(2.dp))
                                    Text(type.label, fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                                }
                                Text(formatSizePrecise(file.length()), fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }

        // Delete dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete ${selectedPaths.size} item(s)?") },
                text = { Text("This action cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        selectedPaths.forEach { deleteRecursive(File(it)) }
                        selectedPaths = emptySet()
                        showDeleteDialog = false
                        // Reload current folder
                        val folder = currentFolder
                        if (folder != null) {
                            scope.launch {
                                isLoadingFolder = true
                                val result = withContext(Dispatchers.IO) {
                                    val children = try { folder.listFiles() } catch (e: Exception) { null } ?: emptyArray<File>()
                                    val subFolders = mutableListOf<FolderStat>()
                                    val files = mutableListOf<File>()
                                    for (c in children) {
                                        if (c.name.startsWith(".")) continue
                                        if (c.isDirectory) {
                                            val (size, count) = getFolderSizeRecursive(c)
                                            if (size > 0) subFolders.add(FolderStat(c, size, count))
                                        } else {
                                            files.add(c)
                                        }
                                    }
                                    subFolders.sortedByDescending { it.bytes } to files.sortedByDescending { it.length() }
                                }
                                currentSubFolders = result.first
                                currentFiles = result.second
                                isLoadingFolder = false
                            }
                        }
                    }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                }
            )
        }
        return
    }

    // ===== MAIN SUMMARY VIEW =====
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                    tint = MaterialTheme.colorScheme.primary)
            }
            Text("Storage Analyzer", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))

        if (isRunning) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    Text("Scanning storage...", fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f))
                }
            }
            return@Column
        }

        // Total card
        Card(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Storage, null,
                            tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Internal Storage", fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground)
                        Spacer(Modifier.height(3.dp))
                        Text("${formatSizePrecise(usedBytes)} used  •  ${formatSizePrecise(freeBytes)} free",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f))
                    }
                    Text("$usedPercent%", fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = { usedPercent / 100f },
                    modifier = Modifier.fillMaxWidth().height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
                )
            }
        }

        // ===== WHERE IS SPACE USED (Top folders) =====
        if (folderStats.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            SectionHeader("Where space is used", "Tap to open")
            Spacer(Modifier.height(8.dp))

            folderStats.forEach { stat ->
                val pct = if (usedBytes > 0) ((stat.bytes * 100) / usedBytes).toInt() else 0

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { currentFolder = stat.folder }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Folder, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stat.folder.name, fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1)
                            Spacer(Modifier.width(8.dp))
                            Text("(${stat.fileCount})", fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { pct / 100f },
                            modifier = Modifier.fillMaxWidth().height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(formatSizePrecise(stat.bytes), fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // ===== BY CATEGORY =====
        Spacer(Modifier.height(24.dp))
        SectionHeader("By Category", "Tap to see files")
        Spacer(Modifier.height(8.dp))

        categories.forEach { cat ->
            if (cat.count == 0) return@forEach
            val pct = if (usedBytes > 0) ((cat.bytes * 100) / usedBytes).toInt() else 0

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { drillCategory = cat.label }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(cat.icon, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(cat.label, fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground)
                        Spacer(Modifier.width(8.dp))
                        Text("(${cat.count})", fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { pct / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(formatSizePrecise(cat.bytes), fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground)
            }
        }

        // ===== BIG FILES =====
        val bigFiles = largestFiles.filter { it.length() > 10 * 1024 * 1024 }
        if (bigFiles.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            SectionHeader("Big Files (> 10 MB)", "${bigFiles.size} files")
            Spacer(Modifier.height(8.dp))

            bigFiles.take(30).forEach { file ->
                val type = getFileType(file)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { openFile(context, file) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(getFileIcon(type), null,
                            tint = getFileIconColor(type),
                            modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(file.name, fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
                        Spacer(Modifier.height(2.dp))
                        Text("${type.label} • ${file.parent ?: ""}", fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            maxLines = 1)
                    }
                    Text(formatSizePrecise(file.length()), fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    // ===== Category drill-down dialog =====
    drillCategory?.let { catLabel ->
        val catFiles = filesByCategory[catLabel] ?: emptyList()
        AlertDialog(
            onDismissRequest = { drillCategory = null },
            title = { Text("$catLabel (${catFiles.size})", fontSize = 16.sp,
                fontWeight = FontWeight.Bold) },
            text = {
                if (catFiles.isEmpty()) {
                    Text("No files", fontSize = 14.sp)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)
                    ) {
                        items(catFiles) { file ->
                            val type = getFileType(file)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { openFile(context, file) }
                                    .padding(vertical = 8.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(getFileIcon(type), null,
                                    tint = getFileIconColor(type),
                                    modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(file.name, fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1)
                                    Spacer(Modifier.height(2.dp))
                                    Text(file.parent ?: "", fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                        maxLines = 1)
                                }
                                Text(formatSizePrecise(file.length()), fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { drillCategory = null }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String = "") {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary)
        if (subtitle.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Text(subtitle, fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
        }
    }
}