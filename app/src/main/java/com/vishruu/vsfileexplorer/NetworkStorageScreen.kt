package com.vishruu.vsfileexplorer

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
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID

@Composable
fun NetworkStorageScreen(context: Context, onBack: () -> Unit) {
    var connections by remember { mutableStateOf(NetworkStorage.load(context)) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingConn by remember { mutableStateOf<NetworkConnection?>(null) }
    var activeConn by remember { mutableStateOf<NetworkConnection?>(null) }
    var refreshTick by remember { mutableIntStateOf(0) }

    if (activeConn != null) {
        NetworkBrowserScreen(
            context = context,
            conn = activeConn!!,
            onBack = {
                NetworkManager.disconnect()
                activeConn = null
            }
        )
        return
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
                Text("Network Storage", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
                Text("${connections.size} connection(s)", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
            IconButton(onClick = { editingConn = null; showAddDialog = true }) {
                Icon(Icons.Filled.Add, "Add", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(Modifier.height(8.dp))

        if (connections.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Cloud, null,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No connections", fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f))
                    Spacer(Modifier.height(6.dp))
                    Text("Tap + to add FTP or SFTP server",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f))
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(connections) { conn ->
                    Card(
                        Modifier.fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .clickable { activeConn = conn },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(44.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (conn.type == NetworkType.FTP)
                                        Icons.Filled.Language else Icons.Filled.Cloud,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(conn.name, fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
                                Spacer(Modifier.height(3.dp))
                                Text("${conn.type.label} • ${conn.username}@${conn.host}:${conn.port}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                                    maxLines = 1)
                            }
                            IconButton(onClick = { editingConn = conn; showAddDialog = true }) {
                                Icon(Icons.Filled.Add, "Edit",
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = {
                                NetworkStorage.delete(context, conn.id)
                                connections = NetworkStorage.load(context)
                                refreshTick++
                            }) {
                                Icon(Icons.Filled.Delete, "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        NetworkEditDialog(
            context = context,
            existing = editingConn,
            onDismiss = { showAddDialog = false },
            onSave = { conn ->
                if (editingConn != null) NetworkStorage.update(context, conn)
                else NetworkStorage.add(context, conn)
                connections = NetworkStorage.load(context)
                showAddDialog = false
                refreshTick++
            }
        )
    }
}

@Composable
fun NetworkEditDialog(
    context: Context,
    existing: NetworkConnection?,
    onDismiss: () -> Unit,
    onSave: (NetworkConnection) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var type by remember { mutableStateOf(existing?.type ?: NetworkType.FTP) }
    var host by remember { mutableStateOf(existing?.host ?: "") }
    var port by remember { mutableStateOf((existing?.port ?: 21).toString()) }
    var username by remember { mutableStateOf(existing?.username ?: "") }
    var password by remember { mutableStateOf(existing?.password ?: "") }
    var basePath by remember { mutableStateOf(existing?.basePath ?: "/") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing != null) "Edit Connection" else "New Connection") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = type == NetworkType.FTP,
                        onClick = { type = NetworkType.FTP; if (port == "21" || port.isBlank()) port = "21" })
                    Text("FTP", fontSize = 14.sp)
                    Spacer(Modifier.width(16.dp))
                    RadioButton(selected = type == NetworkType.SFTP,
                        onClick = { type = NetworkType.SFTP; if (port == "21" || port.isBlank()) port = "22" })
                    Text("SFTP", fontSize = 14.sp)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Name") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = host, onValueChange = { host = it },
                    label = { Text("Host / IP") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = port, onValueChange = { port = it },
                    label = { Text("Port") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = username, onValueChange = { username = it },
                    label = { Text("Username") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = password, onValueChange = { password = it },
                    label = { Text("Password") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = basePath, onValueChange = { basePath = it },
                    label = { Text("Base path (e.g. /)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error!!, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val p = port.toIntOrNull()
                when {
                    name.isBlank() -> error = "Name required"
                    host.isBlank() -> error = "Host required"
                    p == null || p !in 1..65535 -> error = "Port invalid"
                    username.isBlank() -> error = "Username required"
                    else -> onSave(
                        NetworkConnection(
                            id = existing?.id ?: UUID.randomUUID().toString(),
                            name = name.trim(),
                            type = type,
                            host = host.trim(),
                            port = p,
                            username = username.trim(),
                            password = password,
                            basePath = basePath.trim().ifEmpty { "/" }
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}