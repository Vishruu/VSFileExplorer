package com.vishruu.vsfileexplorer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@Composable
fun FileContextMenuDialog(
    file: File,
    onDismiss: () -> Unit,
    onAction: (String) -> Unit
) {
    val isEncrypted = file.name.endsWith(CryptoUtils.VS_EXTENSION)
    val isZip = !file.isDirectory && file.extension.lowercase() == "zip"

    val actions = mutableListOf<Pair<String, ImageVector>>()

    // File operations
    actions.add("Copy" to Icons.Filled.ContentCopy)
    actions.add("Move" to Icons.Filled.DriveFileMove)
    actions.add("Copy to" to Icons.Filled.FileCopy)
    actions.add("Move to" to Icons.Filled.FolderOpen)

    // Encrypt / Decrypt (conditional)
    if (isEncrypted) {
        actions.add("Decrypt" to Icons.Filled.LockOpen)
    } else {
        actions.add("Encrypt" to Icons.Filled.Lock)
    }

    // Zip / Unzip (conditional)
    if (isZip) {
        actions.add("Unzip" to Icons.Filled.FolderZip)
        actions.add("Extract here" to Icons.Filled.Download)
        actions.add("Extract to" to Icons.Filled.Unarchive)
    } else {
        actions.add("Zip" to Icons.Filled.FolderZip)
    }

    // Edit operations
    actions.add("Rename" to Icons.Filled.Edit)
    actions.add("Delete" to Icons.Filled.Delete)

    // Share
    actions.add("Share" to Icons.Filled.Share)
    actions.add("Send" to Icons.Filled.Send)

    // Open
    actions.add("Open as" to Icons.Filled.OpenInNew)
    actions.add("Web Search" to Icons.Filled.Language)

    // Misc
    actions.add("Add to Favorite" to Icons.Filled.StarBorder)
    actions.add("Properties" to Icons.Filled.Info)
    actions.add("Select" to Icons.Filled.CheckCircle)
    actions.add("Select All" to Icons.Filled.DoneAll)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = file.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                actions.forEach { (label, icon) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAction(label) }
                            .padding(vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text = label,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun FilePropertiesDialog(
    file: File,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Properties", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                PropertyRow("Name", file.name)
                PropertyRow("Path", file.absolutePath)
                PropertyRow("Type", if (file.isDirectory) "Folder" else file.extension.ifEmpty { "File" })
                PropertyRow("Size", if (file.isDirectory) "—" else formatSize(file.length()))
                PropertyRow("Modified", formatDate(file.lastModified()))
                PropertyRow("Readable", if (file.canRead()) "Yes" else "No")
                PropertyRow("Writable", if (file.canWrite()) "Yes" else "No")
                PropertyRow("Hidden", if (file.isHidden) "Yes" else "No")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun PropertyRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
            modifier = Modifier.width(85.dp)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}