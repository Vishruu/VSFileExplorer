package com.vishruu.vsfileexplorer

import androidx.compose.runtime.LaunchedEffect
import android.content.Context
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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SecureNotesScreen(context: Context, onBack: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var isUnlocked by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf<List<SecureNote>>(emptyList()) }
    var editingNote by remember { mutableStateOf<SecureNote?>(null) }
    var isNewNote by remember { mutableStateOf(false) }

    when {
        !VaultManager.isSetup(context) -> NotesNotSetupScreen(onBack = onBack)
        !isUnlocked -> NotesUnlockScreen(
            context = context,
            onBack = onBack,
            onUnlocked = { pwd ->
                val loaded = SecureNotesManager.loadNotes(pwd)
                if (loaded != null) {
                    password = pwd
                    notes = loaded
                    isUnlocked = true
                } else {
                    Toast.makeText(context, "Failed to load notes", Toast.LENGTH_SHORT).show()
                }
            }
        )
        isNewNote || editingNote != null -> NoteEditorScreen(
            note = editingNote,
            onSave = { title, body ->
                val now = System.currentTimeMillis()
                val saved = if (editingNote != null) {
                    editingNote!!.copy(title = title, body = body, timestamp = now)
                } else {
                    SecureNote(
                        id = "n_$now",
                        title = title,
                        body = body,
                        timestamp = now
                    )
                }
                val updated = if (editingNote != null) {
                    notes.map { if (it.id == saved.id) saved else it }
                } else {
                    listOf(saved) + notes
                }
                if (SecureNotesManager.saveNotes(updated, password)) {
                    notes = updated.sortedByDescending { it.timestamp }
                    editingNote = null
                    isNewNote = false
                    Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                } else Toast.makeText(context, "Save failed", Toast.LENGTH_SHORT).show()
            },
            onDelete = {
                if (editingNote != null) {
                    val updated = notes.filter { it.id != editingNote!!.id }
                    if (SecureNotesManager.saveNotes(updated, password)) {
                        notes = updated
                        editingNote = null
                        isNewNote = false
                        Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onCancel = { editingNote = null; isNewNote = false }
        )
        else -> NotesListScreen(
            notes = notes,
            onAdd = { editingNote = null; isNewNote = true },
            onSelect = { editingNote = it },
            onBack = onBack
        )
    }
}

// ============ NOT SETUP ============
@Composable
fun NotesNotSetupScreen(onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                    tint = MaterialTheme.colorScheme.primary)
            }
            Text("Secure Notes", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(80.dp))
        Box(Modifier.size(100.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Shield, null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("Vault not set up", fontSize = 18.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        Text("Secure Notes uses Vault password.\nPlease set up Vault first.",
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

// ============ UNLOCK ============
@Composable
fun NotesUnlockScreen(
    context: Context,
    onBack: () -> Unit,
    onUnlocked: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var showPwd by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var attempts by remember { mutableIntStateOf(0) }
    var lockUntil by remember { mutableStateOf(0L) }
    var lockRemaining by remember { mutableIntStateOf(0) }
    var isVerifying by remember { mutableStateOf(false) }
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
            Text("Secure Notes", fontSize = 20.sp, fontWeight = FontWeight.Bold,
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
        Text("Notes Locked", fontSize = 18.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(6.dp))
        Text("Enter Vault password", fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f))
        Spacer(Modifier.height(30.dp))

        if (isVerifying) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        } else if (locked) {
            Text("Too many wrong attempts", fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(6.dp))
            Text("Wait: ${lockRemaining}s", fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground)
        } else {
            OutlinedTextField(
                value = password, onValueChange = { password = it; error = null },
                label = { Text("Vault password") }, singleLine = true,
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
                        isVerifying = true
                        val ok = withContext(Dispatchers.IO) {
                            VaultManager.verifyPassword(context, password)
                        }
                        isVerifying = false
                        if (ok) {
                            onUnlocked(password)
                        } else {
                            attempts++
                            password = ""
                            if (attempts >= 3) {
                                lockUntil = System.currentTimeMillis() + 30_000L
                                error = null
                            } else error = "Wrong password"
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

// ============ LIST ============
@Composable
fun NotesListScreen(
    notes: List<SecureNote>,
    onAdd: () -> Unit,
    onSelect: (SecureNote) -> Unit,
    onBack: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                    tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f)) {
                Text("Secure Notes", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
                Text("${notes.size} note(s)", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
            IconButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, "Add",
                    tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(Modifier.height(8.dp))

        if (notes.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Description, null,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                        modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("No notes yet", fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    Spacer(Modifier.height(6.dp))
                    Text("Tap + to add", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f))
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(notes) { note ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .clickable { onSelect(note) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(40.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Description, null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = note.title.ifBlank { "(no title)" },
                                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground, maxLines = 1
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = formatDate(note.timestamp),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                                if (note.body.isNotBlank()) {
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = note.body.replace("\n", " ").take(80),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                                        maxLines = 2
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

// ============ EDITOR ============
@Composable
fun NoteEditorScreen(
    note: SecureNote?,
    onSave: (String, String) -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf(note?.title ?: "") }
    var body by remember { mutableStateOf(note?.body ?: "") }
    var showDelete by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCancel) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                    tint = MaterialTheme.colorScheme.primary)
            }
            Text(
                text = if (note != null) "Edit Note" else "New Note",
                fontSize = 18.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            if (note != null) {
                IconButton(onClick = { showDelete = true }) {
                    Icon(Icons.Filled.Delete, "Delete",
                        tint = MaterialTheme.colorScheme.error)
                }
            }
            IconButton(onClick = {
                if (title.isBlank() && body.isBlank()) {
                    return@IconButton
                }
                onSave(title.trim(), body)
            }) {
                Icon(Icons.Filled.Save, "Save",
                    tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = title, onValueChange = { title = it },
            label = { Text("Title") }, singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = body, onValueChange = { body = it },
            label = { Text("Write your note...") },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete note?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { showDelete = false; onDelete() }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("Cancel") }
            }
        )
    }
}

private fun formatNoteDate(timestamp: Long): String {
    return try {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        sdf.format(Date(timestamp))
    } catch (e: Exception) { "" }
}