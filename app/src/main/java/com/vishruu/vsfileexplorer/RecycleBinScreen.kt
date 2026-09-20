package com.vishruu.vsfileexplorer

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RecycleBinScreen(
    onBack: () -> Unit
) {
    var entries by remember { mutableStateOf(TrashStore.getEntries()) }
    var showEmptyDialog by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<TrashEntry?>(null) }

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
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                    tint = MaterialTheme.colorScheme.primary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Recycle Bin", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
                Text("${entries.size} item(s)", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
            IconButton(onClick = { entries = TrashStore.getEntries() }) {
                Icon(Icons.Filled.Refresh, "Refresh",
                    tint = MaterialTheme.colorScheme.onBackground)
            }
            if (entries.isNotEmpty()) {
                IconButton(onClick = { showEmptyDialog = true }) {
                    Icon(Icons.Filled.DeleteForever, "Empty",
                        tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Delete, null,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Recycle Bin is empty", fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f))
                    Spacer(Modifier.height(6.dp))
                    Text("Deleted files stay here for 30 days",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f))
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(entries) { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(44.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (entry.isDirectory) Icons.Filled.Folder
                                else Icons.Filled.InsertDriveFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(entry.name, fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Deleted ${formatDate(entry.deletedAt)}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                            Text(entry.originalPath, fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                                maxLines = 1)
                        }
                        IconButton(onClick = {
                            if (TrashStore.restore(entry)) {
                                entries = TrashStore.getEntries()
                            }
                        }) {
                            Icon(Icons.Filled.Restore, "Restore",
                                tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { confirmDelete = entry }) {
                            Icon(Icons.Filled.Delete, "Delete",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (showEmptyDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyDialog = false },
            title = { Text("Empty Recycle Bin?") },
            text = { Text("All ${entries.size} item(s) will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    TrashStore.emptyAll()
                    entries = TrashStore.getEntries()
                    showEmptyDialog = false
                }) { Text("Empty") }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyDialog = false }) { Text("Cancel") }
            }
        )
    }

    confirmDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete permanently?") },
            text = { Text("${entry.name} will be gone forever.") },
            confirmButton = {
                TextButton(onClick = {
                    TrashStore.deletePermanently(entry)
                    entries = TrashStore.getEntries()
                    confirmDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text("Cancel") }
            }
        )
    }
}