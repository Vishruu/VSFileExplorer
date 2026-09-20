package com.vishruu.vsfileexplorer

import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.CoroutineScope
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.provider.OpenableColumns
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun VaultScreen(context: Context, onBack: () -> Unit) {
    var isSetup by remember { mutableStateOf(VaultManager.isSetup(context)) }
    var isUnlocked by remember { mutableStateOf(VaultSession.isUnlocked) }
    var vaultPassword by remember { mutableStateOf(VaultSession.password) }
    var isAutoLocking by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Auto-lock on background with delay
    DisposableEffect(lifecycleOwner) {
        var lockJob: kotlinx.coroutines.Job? = null
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    val delaySec = SettingsManager.getVaultLockDelay(context)
                    if (delaySec == -1) return@LifecycleEventObserver  // Never
                    if (!isUnlocked || vaultPassword.isEmpty() || isAutoLocking) {
                        return@LifecycleEventObserver
                    }
                    if (delaySec == 0) {
                        // Instant lock
                        isAutoLocking = true
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                VaultManager.lockVault(vaultPassword, {}, { false })
                            } catch (_: Exception) {}
                            withContext(Dispatchers.Main) {
                                VaultSession.lock()
                                isUnlocked = false
                                vaultPassword = ""
                                isAutoLocking = false
                            }
                        }
                    } else {
                        // Delayed lock
                        lockJob = CoroutineScope(Dispatchers.Main).launch {
                            delay(delaySec * 1000L)
                            isAutoLocking = true
                            withContext(Dispatchers.IO) {
                                try {
                                    VaultManager.lockVault(vaultPassword, {}, { false })
                                } catch (_: Exception) {}
                            }
                            VaultSession.lock()
                            isUnlocked = false
                            vaultPassword = ""
                            isAutoLocking = false
                        }
                    }
                }
                Lifecycle.Event.ON_START -> {
                    // User wapas aaya — pending lock cancel
                    lockJob?.cancel()
                    lockJob = null
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lockJob?.cancel()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    when {
        !isSetup -> VaultSetupScreen(
            context = context,
            onBack = onBack,
            onSetup = { pwd ->
                if (VaultManager.setupVault(context, pwd)) {
                    VaultSession.unlock(pwd)
                    vaultPassword = pwd
                    isSetup = true
                    isUnlocked = true
                    Toast.makeText(context, "Vault created", Toast.LENGTH_SHORT).show()
                } else Toast.makeText(context, "Setup failed", Toast.LENGTH_SHORT).show()
            }
        )
        !isUnlocked -> VaultUnlockScreen(
            context = context,
            onBack = onBack,
            onUnlocked = { pwd ->
                VaultSession.unlock(pwd)
                vaultPassword = pwd
                isUnlocked = true
            }
        )
        else -> VaultHomeScreen(
            context = context,
            password = vaultPassword,
            onLockAndExit = {
                // Just close — auto-lock will handle lock on background
                onBack()
            }
        )
    }
}

// ============ SETUP ============
@Composable
fun VaultSetupScreen(context: Context, onBack: () -> Unit, onSetup: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var showPwd by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                    tint = MaterialTheme.colorScheme.primary)
            }
            Text("Create Vault", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(40.dp))
        Box(Modifier.size(100.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Shield, null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("Secure your files", fontSize = 18.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(6.dp))
        Text("Create a password. This cannot be recovered if forgotten.",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f))
        Spacer(Modifier.height(30.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it; error = null },
            label = { Text("Password") }, singleLine = true,
            visualTransformation = if (showPwd) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showPwd = !showPwd }) {
                    Icon(if (showPwd) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, "Toggle")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = confirm, onValueChange = { confirm = it; error = null },
            label = { Text("Confirm password") }, singleLine = true,
            visualTransformation = if (showPwd) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        if (error != null) {
            Spacer(Modifier.height(10.dp))
            Text(error!!, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                when {
                    password.length < 4 -> error = "At least 4 characters"
                    password != confirm -> error = "Passwords do not match"
                    else -> onSetup(password)
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary)
        ) { Text("Create Vault", fontSize = 15.sp, fontWeight = FontWeight.Bold) }
    }
}

// ============ UNLOCK ============
@Composable
fun VaultUnlockScreen(context: Context, onBack: () -> Unit, onUnlocked: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    var showPwd by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var attempts by remember { mutableIntStateOf(0) }
    var lockUntil by remember { mutableStateOf(0L) }
    var lockRemaining by remember { mutableIntStateOf(0) }
    var isUnlocking by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(lockUntil) {
        if (lockUntil > System.currentTimeMillis()) {
            while (System.currentTimeMillis() < lockUntil) {
                lockRemaining = ((lockUntil - System.currentTimeMillis()) / 1000).toInt() + 1
                delay(500)
            }
            lockRemaining = 0; lockUntil = 0L; attempts = 0; error = null
        }
    }
    val locked = lockRemaining > 0

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                    tint = MaterialTheme.colorScheme.primary)
            }
            Text("Vault", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(50.dp))
        Box(Modifier.size(100.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Lock, null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("Vault Locked", fontSize = 18.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(6.dp))
        Text("Enter password to unlock", fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f))
        Spacer(Modifier.height(30.dp))

        if (isUnlocking) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text("Unlocking... $progress%", fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary)
        } else if (locked) {
            Text("Too many wrong attempts", fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(6.dp))
            Text("Wait: ${lockRemaining}s", fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground)
        } else {
            OutlinedTextField(
                value = password, onValueChange = { password = it; error = null },
                label = { Text("Password") }, singleLine = true,
                visualTransformation = if (showPwd) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPwd = !showPwd }) {
                        Icon(if (showPwd) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, "Toggle")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            if (error != null) {
                Spacer(Modifier.height(10.dp))
                Text(error!!, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (password.isEmpty()) { error = "Enter password"; return@Button }
                    scope.launch {
                        isUnlocking = true; progress = 0
                        val ok = withContext(Dispatchers.IO) {
                            VaultManager.verifyPassword(context, password)
                        }
                        if (!ok) {
                            isUnlocking = false
                            attempts++
                            password = ""
                            if (attempts >= 3) {
                                lockUntil = System.currentTimeMillis() + 30_000L
                                error = null
                            } else error = "Wrong password"
                        } else {
                            withContext(Dispatchers.IO) {
                                VaultManager.unlockVault(password,
                                    onProgress = { p -> progress = p },
                                    isCancelled = { false })
                            }
                            isUnlocking = false
                            onUnlocked(password)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary)
            ) { Text("Unlock", fontSize = 15.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

// ============ HOME (Category Grid) ============
@Composable
fun VaultHomeScreen(context: Context, password: String, onLockAndExit: () -> Unit) {
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showTrash by remember { mutableStateOf(false) }
    var isLocking by remember { mutableStateOf(false) }
    var lockProgress by remember { mutableIntStateOf(0) }
    var refreshTick by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    fun manualLock() {
        scope.launch {
            isLocking = true; lockProgress = 0
            withContext(Dispatchers.IO) {
                VaultManager.lockVault(password,
                    onProgress = { p -> lockProgress = p },
                    isCancelled = { false })
            }
            isLocking = false
            VaultSession.lock()
            Toast.makeText(context, "Vault locked", Toast.LENGTH_SHORT).show()
            onLockAndExit()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val name = queryFileName(context, uri) ?: "imported_${System.currentTimeMillis()}"
                val category = VaultManager.categoryForFile(name)
                val dest = File(VaultManager.cacheCategoryDir(category), name)
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        dest.outputStream().use { output -> input.copyTo(output) }
                    }
                    try {
                        android.provider.DocumentsContract.deleteDocument(context.contentResolver, uri)
                    } catch (_: Exception) {
                        try {
                            val path = getPathFromUri(context, uri)
                            if (path != null) File(path).delete()
                        } catch (_: Exception) {}
                    }
                }
                refreshTick++
                Toast.makeText(context, "Saved to $category", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (selectedCategory != null) {
        VaultCategoryScreen(
            context = context,
            category = selectedCategory!!,
            onBack = { selectedCategory = null },
            onRefresh = { refreshTick++ }
        )
        return
    }

    if (showTrash) {
        VaultTrashScreen(
            context = context,
            onBack = { showTrash = false },
            onRefresh = { refreshTick++ }
        )
        return
    }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                // Just close, don't lock
                onLockAndExit()
            }, enabled = !isLocking) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                    tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f)) {
                Text("Vault Unlocked", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
                Text("Secure Second Space", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
            IconButton(onClick = { importLauncher.launch(arrayOf("*/*")) }, enabled = !isLocking) {
                Icon(Icons.Filled.Add, "Import", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { manualLock() }, enabled = !isLocking) {
                Icon(Icons.Filled.Lock, "Lock", tint = MaterialTheme.colorScheme.primary)
            }
        }

        if (isLocking) {
            LinearProgressIndicator(progress = { lockProgress / 100f },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = MaterialTheme.colorScheme.primary)
        }

        Spacer(Modifier.height(12.dp))

        val cats = listOf(
            Triple("Images", Icons.Filled.Image, "Images"),
            Triple("Videos", Icons.Filled.Movie, "Videos"),
            Triple("Audio", Icons.Filled.AudioFile, "Audio"),
            Triple("Documents", Icons.Filled.Description, "Documents"),
            Triple("Other", Icons.Filled.InsertDriveFile, "Other"),
            Triple("Trash", Icons.Filled.Delete, "Trash")
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(cats) { (label, icon, key) ->
                val count = if (key == "Trash") VaultManager.countInTrash()
                else VaultManager.countInCategory(key)
                Card(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            if (key == "Trash") showTrash = true
                            else selectedCategory = key
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(Modifier.size(56.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center) {
                            Icon(icon, null, tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(30.dp))
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground)
                        Spacer(Modifier.height(4.dp))
                        Text("$count file(s)", fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("About Vault", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(6.dp))
                Text("• Files are encrypted with AES-256", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                Text("• Hidden from gallery and other apps", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                Text("• Trash keeps deleted files for recovery", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                Text("• Auto-lock on background (see Settings)", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
            }
        }
    }
}

// ============ CATEGORY SCREEN ============
@Composable
fun VaultCategoryScreen(
    context: Context,
    category: String,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var refreshTick by remember { mutableIntStateOf(0) }
    var pendingDelete by remember { mutableStateOf<File?>(null) }
    var pendingExport by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(refreshTick) {
        files = withContext(Dispatchers.IO) {
            VaultManager.cacheCategoryDir(category).listFiles()
                ?.filter { it.isFile && it.name != ".nomedia" }
                ?.sortedBy { it.name.lowercase() } ?: emptyList()
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                    tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f)) {
                Text(category, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
                Text("${files.size} file(s)", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
        }

        Spacer(Modifier.height(8.dp))

        if (files.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No files in $category", fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(files) { file ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .clickable { openFile(context, file) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            FileThumbnail(
                                file = file,
                                type = getFileType(file),
                                size = 44.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(file.name, fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
                                Text(formatSize(file.length()), fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                            }
                            IconButton(onClick = { pendingExport = file }) {
                                Icon(Icons.Filled.FileUpload, "Export",
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { pendingDelete = file }) {
                                Icon(Icons.Filled.Delete, "Delete",
                                    tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (pendingExport != null) {
        val fileName = pendingExport!!.name
        val destFolder = VaultManager.exportDestinationFor(category).name
        AlertDialog(
            onDismissRequest = { pendingExport = null },
            title = { Text("Export to File Manager?") },
            text = { Text("$fileName\n\nWill be saved to /$destFolder/ folder and removed from Vault.") },
            confirmButton = {
                TextButton(onClick = {
                    val result = VaultManager.exportFile(category, pendingExport!!)
                    if (result != null) {
                        MediaScannerConnection.scanFile(
                            context,
                            arrayOf(result.absolutePath),
                            null, null
                        )
                        Toast.makeText(context, "Exported to /$destFolder/", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                    }
                    pendingExport = null
                    refreshTick++
                    onRefresh()
                }) { Text("Export") }
            },
            dismissButton = {
                TextButton(onClick = { pendingExport = null }) { Text("Cancel") }
            }
        )
    }

    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Move to Trash?") },
            text = { Text(pendingDelete!!.name) },
            confirmButton = {
                TextButton(onClick = {
                    VaultManager.moveToTrash(category, pendingDelete!!)
                    pendingDelete = null
                    refreshTick++
                    onRefresh()
                }) { Text("Move to Trash") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

// ============ TRASH SCREEN ============
@Composable
fun VaultTrashScreen(context: Context, onBack: () -> Unit, onRefresh: () -> Unit) {
    var allItems by remember { mutableStateOf<List<Pair<String, File>>>(emptyList()) }
    var refreshTick by remember { mutableIntStateOf(0) }
    var showEmptyDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Pair<String, File>?>(null) }

    LaunchedEffect(refreshTick) {
        allItems = withContext(Dispatchers.IO) {
            val list = mutableListOf<Pair<String, File>>()
            VaultManager.CATEGORIES.forEach { cat ->
                VaultManager.cacheTrashDir(cat).listFiles()
                    ?.filter { it.isFile && it.name != ".nomedia" }
                    ?.forEach { list.add(cat to it) }
            }
            list.sortedByDescending { it.second.lastModified() }
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                    tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f)) {
                Text("Trash", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
                Text("${allItems.size} file(s)", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
            if (allItems.isNotEmpty()) {
                IconButton(onClick = { showEmptyDialog = true }) {
                    Icon(Icons.Filled.DeleteForever, "Empty",
                        tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (allItems.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Delete, null,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                        modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("Trash is empty", fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(allItems) { (cat, file) ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            FileThumbnail(
                                file = file,
                                type = getFileType(file),
                                size = 40.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(file.name, fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
                                Text("From: $cat", fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                            }
                            IconButton(onClick = {
                                VaultManager.restoreFromTrash(cat, file)
                                refreshTick++
                                onRefresh()
                                Toast.makeText(context, "Restored", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Filled.Restore, "Restore",
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { pendingDelete = cat to file }) {
                                Icon(Icons.Filled.DeleteForever, "Delete",
                                    tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEmptyDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyDialog = false },
            title = { Text("Empty Trash?") },
            text = { Text("All files will be permanently deleted. Cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    VaultManager.CATEGORIES.forEach { cat ->
                        VaultManager.cacheTrashDir(cat).listFiles()?.forEach { it.delete() }
                    }
                    showEmptyDialog = false
                    refreshTick++
                    onRefresh()
                    Toast.makeText(context, "Trash emptied", Toast.LENGTH_SHORT).show()
                }) { Text("Empty") }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete permanently?") },
            text = { Text(pendingDelete!!.second.name) },
            confirmButton = {
                TextButton(onClick = {
                    VaultManager.deleteFromTrash(pendingDelete!!.second)
                    pendingDelete = null
                    refreshTick++
                    onRefresh()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

private fun queryFileName(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
        }
    } catch (e: Exception) { null }
}

private fun getPathFromUri(context: Context, uri: Uri): String? {
    return try {
        val projection = arrayOf(android.provider.MediaStore.MediaColumns.DATA)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DATA)
                if (idx >= 0) cursor.getString(idx) else null
            } else null
        }
    } catch (e: Exception) { null }
}