package com.vishruu.vsfileexplorer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun CloudScreen(
    context: Context,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isSignedIn by remember { mutableStateOf(DriveAuthManager.isSignedIn(context)) }
    var userEmail by remember { mutableStateOf(DriveAuthManager.getEmail(context)) }
    var files by remember { mutableStateOf<List<DriveFile>>(emptyList()) }
    var currentFolderId by remember { mutableStateOf("root") }
    var folderStack by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var uploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableIntStateOf(0) }
    var downloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableIntStateOf(0) }
    var downloadingName by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<DriveFile?>(null) }
    var showNewFolder by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn
                .getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(Exception::class.java)
                DriveAuthManager.saveEmail(context, account?.email)
                userEmail = account?.email
                isSignedIn = true
            } catch (e: Exception) {
                Toast.makeText(context, "Sign-in failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Upload file picker
    val uploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            uploading = true
            uploadProgress = 0
            val tmp = File(context.cacheDir, "drive_upload_tmp")
            val ok = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { inp ->
                        tmp.outputStream().use { inp.copyTo(it) }
                    }
                    val token = DriveAuthManager.getAccessToken(context) ?: return@withContext false
                    DriveApiClient.uploadFile(
                        context, token, tmp, currentFolderId
                    ) { p -> uploadProgress = p }
                } catch (e: Exception) { false }
            }
            tmp.delete()
            uploading = false
            if (ok) {
                Toast.makeText(context, "Uploaded", Toast.LENGTH_SHORT).show()
                files = refreshFiles(context, currentFolderId)
            } else {
                Toast.makeText(context, "Upload failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Load files jab signed in ho
    LaunchedEffect(isSignedIn, currentFolderId) {
        if (isSignedIn) {
            isLoading = true
            files = refreshFiles(context, currentFolderId)
            isLoading = false
        }
    }

    // ===== Sign-in screen =====
    if (!isSignedIn) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Filled.Cloud, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(80.dp)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "Google Drive",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Access your Drive files from anywhere",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(30.dp))

            Button(
                onClick = {
                    val intent = DriveAuthManager.getClient(context).signInIntent
                    signInLauncher.launch(intent)
                },
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("Sign in with Google")
            }

            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onBack) { Text("Back") }
        }
        return
    }

    // ===== Main screen =====
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (folderStack.isNotEmpty()) {
                    val prev = folderStack.last()
                    folderStack = folderStack.dropLast(1)
                    currentFolderId = prev.first
                } else onBack()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                    tint = MaterialTheme.colorScheme.primary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (folderStack.isEmpty()) "Google Drive" else folderStack.last().second,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )
                Text(
                    userEmail ?: "",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    maxLines = 1
                )
            }
            IconButton(onClick = { uploadLauncher.launch(arrayOf("*/*")) }) {
                Icon(Icons.Filled.CloudUpload, "Upload",
                    tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { showNewFolder = true }) {
                Icon(Icons.Filled.CreateNewFolder, "New Folder",
                    tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = {
                scope.launch {
                    isLoading = true
                    files = refreshFiles(context, currentFolderId)
                    isLoading = false
                }
            }) {
                Icon(Icons.Filled.Refresh, "Refresh",
                    tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = {
                DriveAuthManager.signOut(context)
                isSignedIn = false
                userEmail = null
                files = emptyList()
                folderStack = emptyList()
                currentFolderId = "root"
            }) {
                Icon(Icons.Filled.Logout, "Sign out",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
            }
        }

        // Upload progress
        if (uploading) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Text("Uploading… $uploadProgress%", fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary)
            }
        }

        // Download progress
        if (downloading) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Text("Downloading $downloadingName… $downloadProgress%",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.primary,
                    maxLines = 1)
            }
        }

        // List
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            files.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Folder, null,
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Empty folder", fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)
                ) {
                    items(files) { file ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    if (file.isFolder) {
                                        folderStack = folderStack + (currentFolderId to (file.name))
                                        currentFolderId = file.id
                                    } else {
                                        // Download
                                        scope.launch {
                                            downloading = true
                                            downloadingName = file.name
                                            downloadProgress = 0
                                            val dest = File(
                                                Environment.getExternalStorageDirectory(),
                                                "Download/VSFileExplorer"
                                            ).apply { mkdirs() }
                                            var target = File(dest, file.name)
                                            if (target.exists()) {
                                                target = getUniqueFile(dest, file.name)
                                            }
                                            val ok = withContext(Dispatchers.IO) {
                                                val token = DriveAuthManager.getAccessToken(context)
                                                    ?: return@withContext false
                                                DriveApiClient.downloadFile(
                                                    token, file.id, target
                                                ) { p -> downloadProgress = p }
                                            }
                                            downloading = false
                                            if (ok) {
                                                Toast.makeText(
                                                    context,
                                                    "Saved to Download/VSFileExplorer",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            } else {
                                                Toast.makeText(context, "Download failed",
                                                    Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (file.isFolder) Icons.Filled.Folder
                                    else Icons.Filled.InsertDriveFile,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(file.name, fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1)
                                if (!file.isFolder && file.size > 0) {
                                    Text(formatSize(file.size), fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                                }
                            }
                            if (!file.isFolder) {
                                IconButton(onClick = {
                                    scope.launch {
                                        downloading = true
                                        downloadingName = file.name
                                        downloadProgress = 0
                                        val dest = File(
                                            Environment.getExternalStorageDirectory(),
                                            "Download/VSFileExplorer"
                                        ).apply { mkdirs() }
                                        var target = File(dest, file.name)
                                        if (target.exists()) target = getUniqueFile(dest, file.name)
                                        val ok = withContext(Dispatchers.IO) {
                                            val token = DriveAuthManager.getAccessToken(context)
                                                ?: return@withContext false
                                            DriveApiClient.downloadFile(token, file.id, target) { p ->
                                                downloadProgress = p
                                            }
                                        }
                                        downloading = false
                                        Toast.makeText(
                                            context,
                                            if (ok) "Saved to Download/VSFileExplorer" else "Failed",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }) {
                                    Icon(Icons.Filled.CloudDownload, "Download",
                                        tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            IconButton(onClick = { pendingDelete = file }) {
                                Icon(Icons.Filled.Delete, "Delete",
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }
        }
    }

    // New folder dialog
    if (showNewFolder) {
        AlertDialog(
            onDismissRequest = { showNewFolder = false },
            title = { Text("New folder") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = newFolderName.trim()
                    if (name.isEmpty()) return@TextButton
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            val token = DriveAuthManager.getAccessToken(context)
                                ?: return@withContext false
                            DriveApiClient.createFolder(token, name, currentFolderId)
                        }
                        if (ok) {
                            files = refreshFiles(context, currentFolderId)
                        } else {
                            Toast.makeText(context, "Failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                    newFolderName = ""
                    showNewFolder = false
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolder = false }) { Text("Cancel") }
            }
        )
    }

    // Delete confirm
    if (pendingDelete != null) {
        val f = pendingDelete!!
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete \"${f.name}\"?") },
            text = { Text("This will move the file to Google Drive trash.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            val token = DriveAuthManager.getAccessToken(context)
                                ?: return@withContext false
                            DriveApiClient.deleteFile(token, f.id)
                        }
                        if (ok) {
                            files = refreshFiles(context, currentFolderId)
                        } else {
                            Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

private suspend fun refreshFiles(context: Context, parentId: String): List<DriveFile> =
    withContext(Dispatchers.IO) {
        val token = DriveAuthManager.getAccessToken(context) ?: return@withContext emptyList()
        DriveApiClient.listFiles(token, parentId)
    }