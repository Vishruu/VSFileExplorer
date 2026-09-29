package com.vishruu.vsfileexplorer

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/* =========================================================
   All dialogs — separate file to reduce register pressure
   ========================================================= */

@Composable
fun NewFolderDialog(
    currentPath: String,
    context: Context,
    onDismiss: () -> Unit,
    onCreated: () -> Unit
) {
    var newFolderText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = {
            onDismiss()
            newFolderText = ""
        },
        title = { Text("New Folder") },
        text = {
            Column {
                OutlinedTextField(
                    value = newFolderText,
                    onValueChange = { newFolderText = it },
                    label = { Text("Folder name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Location: $currentPath",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val n = newFolderText.trim()
                if (n.isEmpty()) {
                    Toast.makeText(context, "Enter a name", Toast.LENGTH_SHORT).show()
                    return@TextButton
                }
                onDismiss()
                scope.launch {
                    val err = createFolderSafely(context, File(currentPath), n)
                    if (err == null) {
                        Toast.makeText(context, "Folder created: $n",
                            Toast.LENGTH_SHORT).show()
                        delay(400)
                        onCreated()
                    } else {
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    }
                }
                newFolderText = ""
            }) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismiss()
                newFolderText = ""
            }) { Text("Cancel") }
        }
    )
}

@Composable
fun DeleteConfirmDialog(
    selectedPaths: Set<String>,
    context: Context,
    onDismiss: () -> Unit,
    onDeleted: () -> Unit,
    onOpenRecycleBin: () -> Unit
) {
    var moveToTrash by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete ${selectedPaths.size} item(s)?") },
        text = {
            Column {
                Text(
                    "This action cannot be undone if Recycle Bin is unchecked.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = moveToTrash,
                        onCheckedChange = { moveToTrash = it }
                    )
                    Text(
                        "Move to Recycle Bin", fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        onDismiss()
                        onOpenRecycleBin()
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("View Recycle Bin", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                selectedPaths.forEach { path ->
                    val f = File(path)
                    if (f.exists()) {
                        if (moveToTrash) TrashStore.moveToTrash(f)
                        else deleteRecursive(f)
                    }
                }
                Toast.makeText(
                    context,
                    if (moveToTrash) "Moved to Recycle Bin" else "Deleted permanently",
                    Toast.LENGTH_SHORT
                ).show()
                onDismiss()
                onDeleted()
            }) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun RenameDialog(
    selectedPaths: Set<String>,
    context: Context,
    onDismiss: () -> Unit,
    onRenamed: () -> Unit
) {
    var renameText by remember {
        mutableStateOf(selectedPaths.firstOrNull()?.let { File(it).name } ?: "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename") },
        text = {
            OutlinedTextField(
                value = renameText,
                onValueChange = { renameText = it },
                label = { Text("New name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (selectedPaths.isNotEmpty()) {
                    val oldF = File(selectedPaths.first())
                    val newF = File(oldF.parent, renameText)
                    if (oldF.renameTo(newF)) {
                        Toast.makeText(context, "Renamed", Toast.LENGTH_SHORT).show()
                        onRenamed()
                    } else {
                        Toast.makeText(context, "Rename failed", Toast.LENGTH_SHORT).show()
                    }
                }
                onDismiss()
            }) { Text("Rename") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ZipCreateDialog(
    selectedPaths: Set<String>,
    currentPath: String,
    context: Context,
    onDismiss: () -> Unit,
    onCreated: () -> Unit,
    onZipStart: (String) -> Unit,
    onZipProgress: (Int) -> Unit,
    onZipComplete: () -> Unit,
    onHide: () -> Unit
) {
    var zipName by remember {
        val firstName = selectedPaths.firstOrNull()?.let {
            File(it).name.substringBeforeLast('.')
        } ?: "archive"
        mutableStateOf(if (selectedPaths.size == 1) firstName else "archive")
    }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var usePassword by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showProgress by remember { mutableStateOf(false) }
    var progressPercent by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!showProgress) onDismiss() },
        title = { Text("Create ZIP") },
        text = {
            Column {
                Text(
                    "${selectedPaths.size} item(s) will be zipped",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = zipName,
                    onValueChange = { zipName = it; error = null },
                    label = { Text("ZIP file name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !showProgress
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth().clickable { usePassword = !usePassword },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = usePassword,
                        onCheckedChange = { usePassword = it; error = null },
                        enabled = !showProgress
                    )
                    Text("Password protect (AES-256)", fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground)
                }
                if (usePassword) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; error = null },
                        label = { Text("Password") },
                        singleLine = true,
                        enabled = !showProgress,
                        visualTransformation = if (showPassword) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Filled.VisibilityOff
                                    else Icons.Filled.Visibility,
                                    contentDescription = "Toggle"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; error = null },
                        label = { Text("Confirm password") },
                        singleLine = true,
                        enabled = !showProgress,
                        visualTransformation = if (showPassword) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error!!, fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error)
                }
                if (showProgress) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { progressPercent / 100f },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("Compressing… $progressPercent%", fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Large files may take a while",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        },
        confirmButton = {
            if (showProgress) {
                TextButton(onClick = {
                    // Hide dialog — ZIP continues
                    showProgress = false
                    onHide()
                }) { Text("Hide") }
            } else {
                TextButton(
                    enabled = !showProgress,
                    onClick = {
                        if (usePassword) {
                            if (password.isEmpty()) {
                                error = "Password cannot be empty"
                                return@TextButton
                            }
                            if (password != confirmPassword) {
                                error = "Passwords do not match"
                                return@TextButton
                            }
                        }
                        val base = zipName.trim().ifEmpty { "archive" }
                        val finalName = if (base.lowercase().endsWith(".zip")) base else "$base.zip"
                        val outZip = getUniqueFile(File(currentPath), finalName)
                        val sources = selectedPaths.map { File(it) }
                        val pwd = if (usePassword) password else null

                        scope.launch {
                            showProgress = true
                            progressPercent = 0
                            onZipStart(outZip.name)
                            val err = withContext(Dispatchers.IO) {
                                if (pwd.isNullOrEmpty()) {
                                    try {
                                        zipFiles(sources, outZip,
                                            onProgress = { p ->
                                                progressPercent = p
                                                onZipProgress(p)
                                            },
                                            isCancelled = { false })
                                        null
                                    } catch (e: Exception) {
                                        "Failed: ${e.message ?: "Unknown"}"
                                    }
                                } else {
                                    PasswordZipUtils.createEncryptedZip(
                                        sources = sources,
                                        outputZip = outZip,
                                        password = pwd,
                                        onProgress = { p ->
                                            progressPercent = p
                                            onZipProgress(p)
                                        }
                                    )
                                }
                            }
                            showProgress = false
                            if (err == null) {
                                Toast.makeText(context, "ZIP created", Toast.LENGTH_SHORT).show()
                                onZipComplete()
                                onDismiss()
                                onCreated()
                            } else {
                                error = err
                                onZipComplete()
                            }
                        }
                    }
                ) { Text("Create") }
            }
        },
        dismissButton = {
            if (!showProgress) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
fun ZipPasswordDialog(
    zipFile: File?,
    context: Context,
    onDismiss: () -> Unit,
    onExtract: (File, String) -> Unit
) {
    if (zipFile == null) return

    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Password Required") },
        text = {
            Column {
                Text(
                    zipFile.name,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    maxLines = 1
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "This ZIP is password protected.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; error = null },
                    label = { Text("Enter password") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Filled.VisibilityOff
                                else Icons.Filled.Visibility,
                                contentDescription = "Toggle"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error!!, fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (password.isEmpty()) {
                    error = "Password cannot be empty"
                    return@TextButton
                }
                onDismiss()
                onExtract(zipFile, password)
            }) { Text("Extract") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ShredConfirmDialog(
    selectedPaths: Set<String>,
    context: Context,
    onDismiss: () -> Unit,
    onShredded: () -> Unit
) {
    var isShredding by remember { mutableStateOf(false) }
    var shredProgress by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isShredding) onDismiss() },
        title = { Text("Shred ${selectedPaths.size} item(s)?") },
        text = {
            Column {
                Text(
                    "Files will be permanently destroyed with 3-pass overwrite.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "This cannot be undone. No recovery possible.",
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )
                if (isShredding) {
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { shredProgress / 100f },
                        modifier = Modifier.fillMaxWidth().height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("$shredProgress%", fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isShredding,
                onClick = {
                    val paths = selectedPaths.toList()
                    scope.launch {
                        isShredding = true
                        shredProgress = 0
                        val total = paths.size.coerceAtLeast(1)
                        withContext(Dispatchers.IO) {
                            paths.forEachIndexed { i, path ->
                                FileShredder.shred(
                                    file = File(path),
                                    onProgress = { p ->
                                        shredProgress = ((i * 100) + p) / total
                                    },
                                    isCancelled = { false }
                                )
                            }
                        }
                        shredProgress = 100
                        isShredding = false
                        Toast.makeText(context, "Files shredded permanently",
                            Toast.LENGTH_SHORT).show()
                        onDismiss()
                        onShredded()
                    }
                }
            ) { Text("Shred") }
        },
        dismissButton = {
            TextButton(
                enabled = !isShredding,
                onClick = onDismiss
            ) { Text("Cancel") }
        }
    )
}

@Composable
fun EncryptChoiceDialog(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onContinue: (Boolean) -> Unit
) {
    var onePasswordForAll by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Encrypt $selectedCount items") },
        text = {
            Column {
                Text(
                    "How do you want to encrypt?",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth().clickable { onePasswordForAll = true }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = onePasswordForAll,
                        onClick = { onePasswordForAll = true }
                    )
                    Text("One password for all", fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground)
                }
                Row(
                    Modifier.fillMaxWidth().clickable { onePasswordForAll = false }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !onePasswordForAll,
                        onClick = { onePasswordForAll = false }
                    )
                    Text("Different password for each", fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onContinue(onePasswordForAll)
            }) { Text("Continue") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EncryptDialog(
    pendingEncryptPaths: List<String>,
    onePasswordForAll: Boolean,
    currentEncryptIndex: Int,
    context: Context,
    onDismiss: () -> Unit,
    onComplete: () -> Unit,
    onNext: () -> Unit
) {
    var encryptPassword by remember { mutableStateOf("") }
    var encryptPasswordConfirm by remember { mutableStateOf("") }
    var encryptFileName by remember { mutableStateOf(false) }
    var showEncryptPassword by remember { mutableStateOf(false) }
    var encryptError by remember { mutableStateOf<String?>(null) }
    var showProgress by remember { mutableStateOf(false) }
    var progressPercent by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    val total = pendingEncryptPaths.size
    val isMulti = total > 1
    val currentName = if (currentEncryptIndex < total)
        File(pendingEncryptPaths[currentEncryptIndex]).name else ""

    AlertDialog(
        onDismissRequest = { if (!showProgress) onDismiss() },
        title = {
            Text(
                if (isMulti && !onePasswordForAll) "Encrypt file ${currentEncryptIndex + 1} of $total"
                else if (isMulti) "Encrypt $total items"
                else "Encrypt file"
            )
        },
        text = {
            Column {
                if (isMulti && !onePasswordForAll) {
                    Text(currentName, fontSize = 13.sp, maxLines = 1,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                    Spacer(Modifier.height(10.dp))
                }
                OutlinedTextField(
                    value = encryptPassword,
                    onValueChange = { encryptPassword = it; encryptError = null },
                    label = { Text("Enter password") },
                    singleLine = true,
                    enabled = !showProgress,
                    visualTransformation = if (showEncryptPassword) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showEncryptPassword = !showEncryptPassword }) {
                            Icon(
                                imageVector = if (showEncryptPassword) Icons.Filled.VisibilityOff
                                else Icons.Filled.Visibility,
                                contentDescription = "Toggle"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = encryptPasswordConfirm,
                    onValueChange = { encryptPasswordConfirm = it; encryptError = null },
                    label = { Text("Re-enter password") },
                    singleLine = true,
                    enabled = !showProgress,
                    visualTransformation = if (showEncryptPassword) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth().clickable { encryptFileName = !encryptFileName },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = encryptFileName,
                        onCheckedChange = { encryptFileName = it },
                        enabled = !showProgress
                    )
                    Text("Encrypt file name", fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground)
                }
                if (encryptError != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(encryptError!!, fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error)
                }
                if (showProgress) {
                    Spacer(Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { progressPercent / 100f },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("$progressPercent%", fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !showProgress,
                onClick = {
                    if (encryptPassword.isEmpty()) {
                        encryptError = "Password cannot be empty"
                        return@TextButton
                    }
                    if (encryptPassword != encryptPasswordConfirm) {
                        encryptError = "Passwords do not match"
                        return@TextButton
                    }

                    if (onePasswordForAll) {
                        val allPaths = pendingEncryptPaths.toList()
                        val pwd = encryptPassword
                        val encName = encryptFileName
                        scope.launch {
                            showProgress = true
                            progressPercent = 0
                            val totalItems = allPaths.size.coerceAtLeast(1)
                            var successCount = 0
                            var failCount = 0

                            withContext(Dispatchers.IO) {
                                allPaths.forEachIndexed { i, path ->
                                    try {
                                        val c = CryptoUtils.encryptPath(
                                            source = File(path),
                                            password = pwd,
                                            encryptFileName = encName,
                                            onProgress = { },
                                            isCancelled = { false }
                                        )
                                        successCount += c
                                    } catch (e: Exception) {
                                        failCount++
                                    }
                                    progressPercent = ((i + 1) * 100) / totalItems
                                }
                            }

                            showProgress = false
                            val msg = if (failCount == 0) "$successCount encrypted"
                            else "$successCount encrypted, $failCount failed"
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            onDismiss()
                            onComplete()
                        }
                    } else {
                        val pathTo = pendingEncryptPaths[currentEncryptIndex]
                        val pwd = encryptPassword
                        val encName = encryptFileName
                        scope.launch {
                            showProgress = true
                            progressPercent = 0
                            val r = withContext(Dispatchers.IO) {
                                try {
                                    CryptoUtils.encryptPath(
                                        source = File(pathTo),
                                        password = pwd,
                                        encryptFileName = encName,
                                        onProgress = { p -> progressPercent = p },
                                        isCancelled = { false }
                                    )
                                    Result.success(Unit)
                                } catch (e: Exception) { Result.failure(e) }
                            }
                            showProgress = false
                            if (r.isSuccess) {
                                if (currentEncryptIndex < total - 1) {
                                    onNext()
                                } else {
                                    Toast.makeText(context, "All files encrypted",
                                        Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                    onComplete()
                                }
                            } else {
                                encryptError = "Encryption failed"
                            }
                        }
                    }
                }
            ) { Text("Encrypt") }
        },
        dismissButton = {
            TextButton(
                enabled = !showProgress,
                onClick = onDismiss
            ) { Text("Cancel") }
        }
    )
}

@Composable
fun DecryptDialog(
    targetPath: String,
    context: Context,
    onDismiss: () -> Unit,
    onDecrypted: () -> Unit
) {
    var decryptPassword by remember { mutableStateOf("") }
    var showDecryptPassword by remember { mutableStateOf(false) }
    var decryptError by remember { mutableStateOf<String?>(null) }
    var decryptAttempts by remember { mutableIntStateOf(0) }
    var decryptLockRemaining by remember { mutableIntStateOf(0) }
    var showProgress by remember { mutableStateOf(false) }
    var progressPercent by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    val targetFile = File(targetPath)
    val locked = decryptLockRemaining > 0

    AlertDialog(
        onDismissRequest = { if (!showProgress) onDismiss() },
        title = { Text("Decrypt file") },
        text = {
            Column {
                Text(
                    targetFile.name, fontSize = 13.sp, maxLines = 1,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(10.dp))
                if (locked) {
                    Text("Too many wrong attempts.", fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(6.dp))
                    Text("Wait: ${decryptLockRemaining}s", fontSize = 14.sp)
                } else {
                    OutlinedTextField(
                        value = decryptPassword,
                        onValueChange = { decryptPassword = it; decryptError = null },
                        label = { Text("Enter password") },
                        singleLine = true,
                        enabled = !showProgress,
                        visualTransformation = if (showDecryptPassword) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showDecryptPassword = !showDecryptPassword }) {
                                Icon(
                                    imageVector = if (showDecryptPassword) Icons.Filled.VisibilityOff
                                    else Icons.Filled.Visibility,
                                    contentDescription = "Toggle"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Attempt ${decryptAttempts + 1} of 3", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f))
                    if (decryptError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(decryptError!!, fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.error)
                    }
                    if (showProgress) {
                        Spacer(Modifier.height(14.dp))
                        LinearProgressIndicator(
                            progress = { progressPercent / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (!locked) {
                TextButton(
                    enabled = !showProgress,
                    onClick = {
                        if (decryptPassword.isEmpty()) {
                            decryptError = "Password cannot be empty"
                            return@TextButton
                        }
                        val pwd = decryptPassword
                        scope.launch {
                            showProgress = true
                            progressPercent = 0
                            val r = withContext(Dispatchers.IO) {
                                try {
                                    CryptoUtils.decryptFile(
                                        sourceFile = targetFile, password = pwd,
                                        onProgress = { p -> progressPercent = p },
                                        isCancelled = { false })
                                    Result.success(Unit)
                                } catch (e: Exception) { Result.failure(e) }
                            }
                            showProgress = false
                            if (r.isSuccess) {
                                Toast.makeText(context, "Decryption complete",
                                    Toast.LENGTH_SHORT).show()
                                onDismiss()
                                onDecrypted()
                            } else {
                                decryptAttempts++
                                decryptPassword = ""
                                if (decryptAttempts >= 3) {
                                    decryptLockRemaining = 30
                                    scope.launch {
                                        while (decryptLockRemaining > 0) {
                                            delay(1000)
                                            decryptLockRemaining--
                                        }
                                        decryptAttempts = 0
                                        decryptError = null
                                    }
                                } else {
                                    decryptError = "Wrong password"
                                }
                            }
                        }
                    }
                ) { Text("Decrypt") }
            } else {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
        dismissButton = {
            if (!locked) {
                TextButton(
                    enabled = !showProgress,
                    onClick = onDismiss
                ) { Text("Cancel") }
            }
        }
    )
}

@Composable
fun DecryptMultiDialog(
    selectedPaths: Set<String>,
    context: Context,
    onDismiss: () -> Unit,
    onDecrypted: () -> Unit
) {
    var decryptMultiPassword by remember { mutableStateOf("") }
    var showDecryptMultiPassword by remember { mutableStateOf(false) }
    var decryptMultiError by remember { mutableStateOf<String?>(null) }
    var decryptMultiRunning by remember { mutableStateOf(false) }
    var decryptMultiProgress by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!decryptMultiRunning) onDismiss() },
        title = { Text("Decrypt ${selectedPaths.size} file(s)") },
        text = {
            Column {
                Text(
                    "All files will be decrypted with the same password.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = decryptMultiPassword,
                    onValueChange = { decryptMultiPassword = it; decryptMultiError = null },
                    label = { Text("Enter password") },
                    singleLine = true,
                    enabled = !decryptMultiRunning,
                    visualTransformation = if (showDecryptMultiPassword)
                        VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = {
                            showDecryptMultiPassword = !showDecryptMultiPassword
                        }) {
                            Icon(
                                imageVector = if (showDecryptMultiPassword)
                                    Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = "Toggle"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                if (decryptMultiError != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(decryptMultiError!!, fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error)
                }
                if (decryptMultiRunning) {
                    Spacer(Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { decryptMultiProgress / 100f },
                        modifier = Modifier.fillMaxWidth().height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("$decryptMultiProgress%", fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !decryptMultiRunning,
                onClick = {
                    if (decryptMultiPassword.isEmpty()) {
                        decryptMultiError = "Password cannot be empty"
                        return@TextButton
                    }
                    val paths = selectedPaths.toList()
                    val pwd = decryptMultiPassword
                    scope.launch {
                        decryptMultiRunning = true
                        decryptMultiProgress = 0
                        val totalItems = paths.size.coerceAtLeast(1)
                        var successCount = 0
                        var failCount = 0

                        withContext(Dispatchers.IO) {
                            paths.forEachIndexed { i, path ->
                                try {
                                    val c = CryptoUtils.decryptPath(
                                        source = File(path),
                                        password = pwd,
                                        onProgress = { },
                                        isCancelled = { false }
                                    )
                                    successCount += c
                                } catch (e: Exception) {
                                    failCount++
                                }
                                decryptMultiProgress = ((i + 1) * 100) / totalItems
                            }
                        }

                        decryptMultiRunning = false
                        val msg = if (failCount == 0) "$successCount decrypted"
                        else "$successCount decrypted, $failCount failed"
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        onDismiss()
                        onDecrypted()
                    }
                }
            ) { Text("Decrypt") }
        },
        dismissButton = {
            TextButton(
                enabled = !decryptMultiRunning,
                onClick = onDismiss
            ) { Text("Cancel") }
        }
    )
}

@Composable
fun ProgressDialog(
    progressLabel: String,
    progressPercent: Int,
    progressHideable: Boolean,
    progressHidden: Boolean,
    onHide: () -> Unit,
    onCancel: () -> Unit,
    onUnhide: () -> Unit
) {
    if (!progressHidden) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(progressLabel) },
            text = {
                Column {
                    LinearProgressIndicator(
                        progress = { progressPercent / 100f },
                        modifier = Modifier.fillMaxWidth().height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("$progressPercent%", fontSize = 14.sp)
                }
            },
            confirmButton = {
                if (progressHideable) {
                    TextButton(onClick = onHide) { Text("Hide") }
                }
            },
            dismissButton = {
                TextButton(onClick = onCancel) { Text("Cancel") }
            }
        )
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Card(
                modifier = Modifier.padding(20.dp).clip(RoundedCornerShape(12.dp))
                    .clickable { onUnhide() },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        Modifier.size(16.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("$progressPercent% — tap", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
fun ApkBackupDialog(progress: Int) {
    AlertDialog(
        onDismissRequest = { },
        title = { Text("Backing up APK...") },
        text = {
            Column {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth().height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(10.dp))
                Text("$progress%", fontSize = 14.sp)
            }
        },
        confirmButton = { }
    )
}