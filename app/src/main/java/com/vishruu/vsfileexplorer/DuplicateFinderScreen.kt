package com.vishruu.vsfileexplorer

import android.content.Context
import android.os.Environment
import android.widget.Toast
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

@Composable
fun DuplicateFinderScreen(
    context: Context,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var isScanning by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<DuplicateFinder.ScanResult?>(null) }
    // selected paths per group (jo delete karni hain)
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    var statusText by remember { mutableStateOf("") }

    fun startScan() {
        scope.launch {
            isScanning = true
            progress = 0
            statusText = "Starting…"
            result = null
            selected = emptySet()
            val r = DuplicateFinder.scan(
                root = Environment.getExternalStorageDirectory(),
                onProgress = { p -> progress = p },
                onStatus = { s -> statusText = s }
            )
            result = r
            isScanning = false
            statusText = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Top bar
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                    tint = MaterialTheme.colorScheme.primary)
            }
            Text(
                "Duplicate Finder",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))

        // Idle screen
        if (!isScanning && result == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        Icons.Filled.InsertDriveFile, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "Find Duplicate Files",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Scan your storage to find identical files\nand free up space",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { startScan() },
                        modifier = Modifier.fillMaxWidth(0.7f)
                    ) {
                        Text("Start Scan")
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "May take 1-3 minutes for large storage",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
            return
        }

        // Scanning screen
        if (isScanning) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        progress = { progress / 100f },
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp),
                        strokeWidth = 5.dp
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "Scanning…",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "$progress%",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (statusText.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            statusText,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            }
            return
        }

        // Result
        val r = result ?: return

        if (r.groups.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.CheckCircle, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No duplicates found",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Your storage is clean!\n${r.totalFilesScanned} files scanned",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))
                    TextButton(onClick = { startScan() }) { Text("Scan again") }
                }
            }
            return
        }

        // Summary card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "${r.totalGroups} groups • ${r.totalDuplicates} duplicate files",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Can free up ${formatSize(r.totalWasted)}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            // Auto-select: har group mein sabse purani file rakho, baaki select karo
                            val auto = mutableSetOf<String>()
                            r.groups.forEach { g ->
                                g.files.drop(1).forEach { auto.add(it.absolutePath) }
                            }
                            selected = auto
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Select all", fontSize = 13.sp) }
                    Button(
                        onClick = {
                            if (selected.isEmpty()) {
                                Toast.makeText(context, "Nothing selected",
                                    Toast.LENGTH_SHORT).show()
                            } else showDeleteDialog = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Delete (${selected.size})", fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Groups list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(r.groups) { group ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${group.files.size} copies",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "• ${formatSize(group.size)} each",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "Wasted: ${formatSize(group.wastedSpace)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        group.files.forEach { file ->
                            val isSel = selected.contains(file.absolutePath)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selected = if (isSel) selected - file.absolutePath
                                        else selected + file.absolutePath
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (isSel) Icons.Filled.CheckCircle
                                    else Icons.Filled.RadioButtonUnchecked,
                                    null,
                                    tint = if (isSel) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        file.name,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1
                                    )
                                    Text(
                                        file.parent ?: "",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                        maxLines = 1
                                    )
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
            onDismissRequest = { if (!isDeleting) showDeleteDialog = false },
            title = { Text("Delete ${selected.size} file(s)?") },
            text = { Text("Selected duplicates will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    enabled = !isDeleting,
                    onClick = {
                        scope.launch {
                            isDeleting = true
                            var deleted = 0
                            withContext(Dispatchers.IO) {
                                selected.forEach { path ->
                                    try {
                                        val f = File(path)
                                        if (f.exists() && f.delete()) deleted++
                                    } catch (_: Exception) {}
                                }
                            }
                            isDeleting = false
                            showDeleteDialog = false
                            Toast.makeText(
                                context,
                                "Deleted $deleted file(s)",
                                Toast.LENGTH_SHORT
                            ).show()
                            selected = emptySet()
                            // Re-scan
                            startScan()
                        }
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(
                    enabled = !isDeleting,
                    onClick = { showDeleteDialog = false }
                ) { Text("Cancel") }
            }
        )
    }
}