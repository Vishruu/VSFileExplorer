package com.vishruu.vsfileexplorer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class ToolEntry(val key: String, val label: String, val icon: ImageVector, val color: Color)

@Composable
fun ToolboxScreen(onBack: () -> Unit) {
    var activeTool by remember { mutableStateOf<String?>(null) }

    // BackHandler: tool ke andar back -> Toolbox main; Toolbox main se back -> onBack()
    BackHandler {
        if (activeTool != null) {
            activeTool = null
        } else {
            onBack()
        }
    }

    when (activeTool) {
        "empty" -> { EmptyFoldersScreen(onBack = { activeTool = null }); return }
        "duplicates" -> { DuplicateFinderScreen(onBack = { activeTool = null }); return }
        "note" -> { NoteEditorScreen(onBack = { activeTool = null }); return }
        "large" -> { LargeFilesToolScreen(onBack = { activeTool = null }); return }
        else -> { }
    }

    val tools = listOf(
        ToolEntry("empty", "Empty Folders", Icons.Filled.FolderOpen, Color(0xFF00BCD4)),
        ToolEntry("duplicates", "Duplicate Files", Icons.Filled.ContentCopy, Color(0xFFFF9800)),
        ToolEntry("large", "Large Files", Icons.Filled.Storage, Color(0xFF3F51B5)),
        ToolEntry("note", "Note Editor", Icons.Filled.NoteAdd, Color(0xFF4CAF50))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                    tint = MaterialTheme.colorScheme.primary)
            }
            Text("Toolbox", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(tools) { tool ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { activeTool = tool.key },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(52.dp).clip(CircleShape)
                                .background(tool.color.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(tool.icon, null,
                                tint = tool.color, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(tool.label, fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("About Toolbox", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Text("• Empty Folders — remove empty directories to clean up storage",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                Text("• Duplicate Files — find identical files by size + name",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                Text("• Large Files — biggest files eating your storage",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                Text("• Note Editor — quick text notes saved to storage",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
            }
        }
    }
}

// ============ EMPTY FOLDERS ============
@Composable
fun EmptyFoldersScreen(onBack: () -> Unit) {
    var isLoading by remember { mutableStateOf(true) }
    var emptyFolders by remember { mutableStateOf<List<File>>(emptyList()) }
    var selectedPaths by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        emptyFolders = withContext(Dispatchers.IO) {
            val root = android.os.Environment.getExternalStorageDirectory()
            val result = mutableListOf<File>()
            val queue = ArrayDeque<File>()
            queue.add(root)
            while (queue.isNotEmpty() && result.size < 500) {
                val cur = queue.removeFirst()
                val kids = try { cur.listFiles() } catch (e: Exception) { null } ?: continue
                var hasAnyFile = false
                for (k in kids) {
                    if (k.name.startsWith(".")) continue
                    if (k.isDirectory) {
                        queue.add(k)
                    } else {
                        hasAnyFile = true
                    }
                }
                if (!hasAnyFile && kids.none { it.isDirectory && !it.name.startsWith(".") } && cur.absolutePath != root.absolutePath) {
                    result.add(cur)
                }
            }
            result.sortedBy { it.absolutePath }
        }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                    tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f)) {
                Text("Empty Folders", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
                Text("${emptyFolders.size} empty", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
            if (selectedPaths.isNotEmpty()) {
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Filled.Delete, "Delete",
                        tint = MaterialTheme.colorScheme.error)
                }
            }
            if (emptyFolders.isNotEmpty() && selectedPaths.isEmpty()) {
                TextButton(onClick = { selectedPaths = emptyFolders.map { it.absolutePath }.toSet() }) {
                    Text("Select all")
                }
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (emptyFolders.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No empty folders found", fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f))
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(emptyFolders) { folder ->
                    val isSel = selectedPaths.contains(folder.absolutePath)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.background)
                            .clickable {
                                selectedPaths = if (isSel)
                                    selectedPaths - folder.absolutePath
                                else selectedPaths + folder.absolutePath
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSel) {
                            Icon(Icons.Filled.CheckCircle, null,
                                tint = MaterialTheme.colorScheme.primary)
                        } else {
                            Icon(Icons.Filled.Folder, null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(folder.name, fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
                            Text(folder.absolutePath, fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                maxLines = 1)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete ${selectedPaths.size} folder(s)?") },
            text = { Text("They are empty and safe to delete.") },
            confirmButton = {
                TextButton(onClick = {
                    selectedPaths.forEach { deleteRecursive(File(it)) }
                    selectedPaths = emptySet()
                    emptyFolders = emptyFolders.filter { it.exists() }
                    showDeleteDialog = false
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ============ DUPLICATE FILES ============
data class DuplicateGroup(val key: String, val files: List<File>)

@Composable
fun DuplicateFinderScreen(onBack: () -> Unit) {
    var isLoading by remember { mutableStateOf(true) }
    var duplicates by remember { mutableStateOf<List<DuplicateGroup>>(emptyList()) }
    var selectedPaths by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        duplicates = withContext(Dispatchers.IO) {
            val root = android.os.Environment.getExternalStorageDirectory()
            val map = mutableMapOf<String, MutableList<File>>()
            val queue = ArrayDeque<File>()
            queue.add(root)
            var count = 0
            while (queue.isNotEmpty() && count < 20000) {
                val cur = queue.removeFirst()
                val kids = try { cur.listFiles() } catch (e: Exception) { null } ?: continue
                for (k in kids) {
                    if (k.name.startsWith(".")) continue
                    if (k.isDirectory) queue.add(k)
                    else {
                        count++
                        val key = "${k.length()}_${k.name.lowercase()}"
                        map.getOrPut(key) { mutableListOf() }.add(k)
                    }
                }
            }
            map.filter { it.value.size > 1 }
                .map { DuplicateGroup(it.key, it.value) }
                .sortedByDescending { it.files.size }
        }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                    tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f)) {
                Text("Duplicate Files", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
                Text("${duplicates.size} group(s)", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
            if (selectedPaths.isNotEmpty()) {
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Filled.Delete, "Delete",
                        tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    Text("Scanning for duplicates...", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f))
                }
            }
        } else if (duplicates.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.CheckCircle, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No duplicates found", fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f))
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(duplicates) { group ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp)
                    ) {
                        Text("${group.files.size} copies  •  ${formatSize(group.files.first().length())}",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        group.files.forEachIndexed { idx, file ->
                            val isSel = selectedPaths.contains(file.absolutePath)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (idx == 0) return@clickable
                                        selectedPaths = if (isSel)
                                            selectedPaths - file.absolutePath
                                        else selectedPaths + file.absolutePath
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (idx == 0) {
                                    Icon(Icons.Filled.CheckCircle, null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp))
                                } else {
                                    Icon(
                                        imageVector = if (isSel) Icons.Filled.CheckCircle
                                        else Icons.Filled.InsertDriveFile,
                                        contentDescription = null,
                                        tint = if (isSel) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(file.name + if (idx == 0) "  (keep)" else "",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1)
                                    Text(file.parent ?: "", fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                        maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete ${selectedPaths.size} duplicate(s)?") },
            text = { Text("First copy of each group is kept. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    selectedPaths.forEach { deleteRecursive(File(it)) }
                    selectedPaths = emptySet()
                    duplicates = duplicates.mapNotNull { g ->
                        val remaining = g.files.filter { it.exists() }
                        if (remaining.size > 1) DuplicateGroup(g.key, remaining) else null
                    }
                    showDeleteDialog = false
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ============ LARGE FILES (updated with select/delete) ============
@Composable
fun LargeFilesToolScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var bigFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var selectedPaths by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        bigFiles = withContext(Dispatchers.IO) {
            val root = android.os.Environment.getExternalStorageDirectory()
            val result = mutableListOf<File>()
            val queue = ArrayDeque<File>()
            queue.add(root)
            var count = 0
            while (queue.isNotEmpty() && count < 20000) {
                val cur = queue.removeFirst()
                val kids = try { cur.listFiles() } catch (e: Exception) { null } ?: continue
                for (k in kids) {
                    if (k.name.startsWith(".")) continue
                    if (k.isDirectory) queue.add(k)
                    else {
                        count++
                        if (k.length() > 10 * 1024 * 1024) result.add(k)
                    }
                }
            }
            result.sortedByDescending { it.length() }.take(100)
        }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                    tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f)) {
                Text("Large Files", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
                Text("${bigFiles.size} files > 10 MB", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
            if (selectedPaths.isNotEmpty()) {
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Filled.Delete, "Delete",
                        tint = MaterialTheme.colorScheme.error)
                }
            }
            if (bigFiles.isNotEmpty() && selectedPaths.isEmpty()) {
                TextButton(onClick = { selectedPaths = bigFiles.map { it.absolutePath }.toSet() }) {
                    Text("Select all")
                }
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (bigFiles.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No large files found", fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f))
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(bigFiles) { file ->
                    val isSel = selectedPaths.contains(file.absolutePath)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.background)
                            .clickable {
                                selectedPaths = if (isSel)
                                    selectedPaths - file.absolutePath
                                else selectedPaths + file.absolutePath
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSel) {
                            Icon(Icons.Filled.CheckCircle, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp))
                        } else {
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.InsertDriveFile,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(file.name, fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1)
                            Text(file.parent ?: "", fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                maxLines = 1)
                        }
                        Text(formatSize(file.length()), fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete ${selectedPaths.size} file(s)?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    selectedPaths.forEach { deleteRecursive(File(it)) }
                    selectedPaths = emptySet()
                    bigFiles = bigFiles.filter { it.exists() }
                    showDeleteDialog = false
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ============ NOTE EDITOR (updated with delete) ============
@Composable
fun NoteEditorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val notesDir = remember {
        val d = File(
            android.os.Environment.getExternalStorageDirectory(),
            "VSFileExplorer/Notes"
        )
        if (!d.exists()) d.mkdirs()
        d
    }
    var noteText by remember { mutableStateOf("") }
    var noteName by remember { mutableStateOf("note_${System.currentTimeMillis()}") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                    tint = MaterialTheme.colorScheme.primary)
            }
            Text("Note Editor", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
            // Delete current note file
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Filled.Delete, "Delete",
                    tint = MaterialTheme.colorScheme.error)
            }
            IconButton(onClick = {
                try {
                    val name = if (noteName.endsWith(".txt")) noteName else "$noteName.txt"
                    val f = File(notesDir, name)
                    f.writeText(noteText)
                    android.widget.Toast.makeText(context, "Saved: $name",
                        android.widget.Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Save failed",
                        android.widget.Toast.LENGTH_SHORT).show()
                }
            }) {
                Icon(Icons.Filled.Save, "Save",
                    tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = noteName,
            onValueChange = { noteName = it },
            label = { Text("File name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = noteText,
            onValueChange = { noteText = it },
            label = { Text("Write your note...") },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )

        Spacer(Modifier.height(8.dp))
        Text("Saved to: ${notesDir.absolutePath}", fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete this note?") },
            text = { Text("The saved file will be deleted from storage.") },
            confirmButton = {
                TextButton(onClick = {
                    val name = if (noteName.endsWith(".txt")) noteName else "$noteName.txt"
                    val f = File(notesDir, name)
                    if (f.exists()) {
                        f.delete()
                        android.widget.Toast.makeText(context, "Deleted: $name",
                            android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        android.widget.Toast.makeText(context, "File not found",
                            android.widget.Toast.LENGTH_SHORT).show()
                    }
                    noteText = ""
                    showDeleteDialog = false
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}