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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun NetworkBrowserScreen(
    context: Context,
    conn: NetworkConnection,
    onBack: () -> Unit
) {
    var currentPath by remember { mutableStateOf(conn.basePath) }
    var files by remember { mutableStateOf<List<RemoteFile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var refreshTick by remember { mutableIntStateOf(0) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadLabel by remember { mutableStateOf("") }
    var showNewFolder by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<RemoteFile?>(null) }
    var showUploadPicker by remember { mutableStateOf(false) }
    var uploadList by remember { mutableStateOf<List<File>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshTick, currentPath) {
        isLoading = true
        errorMsg = null
        val result = withContext(Dispatchers.IO) {
            if (!NetworkManager.isConnected()) {
                val err = NetworkManager.connect(conn)
                if (err != null) return@withContext null to err
            }
            NetworkManager.list(currentPath)
        }
        files = result.first ?: emptyList()
        errorMsg = result.second
        isLoading = false
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (currentPath != conn.basePath) {
                    val parent = currentPath.substringBeforeLast('/', "")
                    currentPath = if (parent.isEmpty() || parent.length < conn.basePath.length) conn.basePath else parent
                } else onBack()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                    tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f)) {
                Text(conn.name, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary, maxLines = 1)
                Text(currentPath, fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    maxLines = 1)
            }
            IconButton(onClick = { refreshTick++ }) {
                Icon(Icons.Filled.Refresh, "Refresh",
                    tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { newFolderName = ""; showNewFolder = true }) {
                Icon(Icons.Filled.Add, "New folder",
                    tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = {
                scope.launch {
                    val list = withContext(Dispatchers.IO) {
                        val localRoot = Environment.getExternalStorageDirectory()
                        localRoot.listFiles()?.filter { it.isFile }?.take(50) ?: emptyList()
                    }
                    uploadList = list
                    showUploadPicker = true
                }
            }) {
                Icon(Icons.Filled.Upload, "Upload",
                    tint = MaterialTheme.colorScheme.primary)
            }
        }

        if (isDownloading) {
            Row(Modifier.fillMaxWidth().background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(downloadLabel, fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground)
            }
        }

        Spacer(Modifier.height(8.dp))

        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            errorMsg != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Connection failed", fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(6.dp))
                    Text(errorMsg!!, fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f))
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { refreshTick++ }) { Text("Retry") }
                }
            }
            files.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Empty folder", fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(files) { rf ->
                    Card(
                        Modifier.fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .clickable {
                                if (rf.isDirectory) {
                                    currentPath = rf.path
                                } else {
                                    scope.launch {
                                        isDownloading = true
                                        downloadLabel = "Downloading ${rf.name}..."
                                        val dest = withContext(Dispatchers.IO) {
                                            val dl = File(Environment.getExternalStorageDirectory(),
                                                "Download/${rf.name}")
                                            val err = NetworkManager.download(rf.path, dl)
                                            if (err != null) null else dl
                                        }
                                        isDownloading = false
                                        if (dest != null) {
                                            Toast.makeText(context,
                                                "Saved: ${dest.name}",
                                                Toast.LENGTH_SHORT).show()
                                        } else Toast.makeText(context,
                                            "Download failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(40.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (rf.isDirectory) Icons.Filled.Folder
                                    else Icons.Filled.InsertDriveFile,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(rf.name, fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = if (rf.isDirectory) "Folder"
                                    else NetworkManager.formatRemoteSize(rf.size) +
                                            " • " + NetworkManager.formatRemoteDate(rf.modified),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                            }
                            if (!rf.isDirectory) {
                                IconButton(onClick = {
                                    scope.launch {
                                        isDownloading = true
                                        downloadLabel = "Downloading ${rf.name}..."
                                        val dest = withContext(Dispatchers.IO) {
                                            val dl = File(Environment.getExternalStorageDirectory(),
                                                "Download/${rf.name}")
                                            val err = NetworkManager.download(rf.path, dl)
                                            if (err != null) null else dl
                                        }
                                        isDownloading = false
                                        if (dest != null) {
                                            Toast.makeText(context,
                                                "Saved: ${dest.name}",
                                                Toast.LENGTH_SHORT).show()
                                        } else Toast.makeText(context,
                                            "Download failed", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(Icons.Filled.Download, "Download",
                                        tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            IconButton(onClick = { pendingDelete = rf }) {
                                Icon(Icons.Filled.Delete, "Delete",
                                    tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewFolder) {
        AlertDialog(
            onDismissRequest = { showNewFolder = false },
            title = { Text("New Folder") },
            text = {
                OutlinedTextField(value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Name") }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    val n = newFolderName.trim()
                    if (n.isNotEmpty()) {
                        scope.launch {
                            val path = if (currentPath.endsWith("/")) "$currentPath$n"
                            else "$currentPath/$n"
                            val err = withContext(Dispatchers.IO) { NetworkManager.mkdir(path) }
                            showNewFolder = false
                            if (err == null) refreshTick++
                            else Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                        }
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolder = false }) { Text("Cancel") }
            }
        )
    }

    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete?") },
            text = { Text(pendingDelete!!.name) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val err = withContext(Dispatchers.IO) {
                            NetworkManager.delete(pendingDelete!!.path, pendingDelete!!.isDirectory)
                        }
                        pendingDelete = null
                        if (err == null) refreshTick++
                        else Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (showUploadPicker) {
        AlertDialog(
            onDismissRequest = { showUploadPicker = false },
            title = { Text("Choose file to upload") },
            text = {
                if (uploadList.isEmpty()) {
                    Text("No files in root storage", fontSize = 13.sp)
                } else {
                    LazyColumn(Modifier.height(300.dp)) {
                        items(uploadList) { f ->
                            Row(Modifier.fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        isDownloading = true
                                        downloadLabel = "Uploading ${f.name}..."
                                        val remote = if (currentPath.endsWith("/"))
                                            "$currentPath${f.name}" else "$currentPath/${f.name}"
                                        val err = withContext(Dispatchers.IO) {
                                            NetworkManager.upload(f, remote)
                                        }
                                        isDownloading = false
                                        showUploadPicker = false
                                        if (err == null) {
                                            Toast.makeText(context, "Uploaded: ${f.name}",
                                                Toast.LENGTH_SHORT).show()
                                            refreshTick++
                                        } else Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.InsertDriveFile, null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(f.name, fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showUploadPicker = false }) { Text("Close") }
            }
        )
    }
}