package com.vishruu.vsfileexplorer

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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

@Composable
fun WifiTransferScreen(context: Context, onBack: () -> Unit) {
    var isRunning by remember { mutableStateOf(WifiTransferManager.isRunning()) }
    var url by remember { mutableStateOf(WifiTransferManager.getUrl()) }

    // Stop server when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            // Don't auto-stop — user might want it running
        }
    }

    Column(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                    tint = MaterialTheme.colorScheme.primary)
            }
            Text("WiFi Transfer", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
        }

        Spacer(Modifier.height(24.dp))

        // Status circle
        Box(
            Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.size(140.dp)
                    .clip(CircleShape)
                    .background(
                        if (isRunning) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                    contentDescription = null,
                    tint = if (isRunning) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                    modifier = Modifier.size(72.dp)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = if (isRunning) "Server Running" else "Server Stopped",
            fontSize = 20.sp, fontWeight = FontWeight.Bold,
            color = if (isRunning) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = if (isRunning) "Open this URL in your PC browser"
            else "Tap Start to begin WiFi transfer",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
        )

        Spacer(Modifier.height(24.dp))

        // URL card
        if (isRunning && url != null) {
            Card(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("URL", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = url!!,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                                    as android.content.ClipboardManager
                            clipboard.setPrimaryClip(
                                android.content.ClipData.newPlainText("URL", url))
                            Toast.makeText(context, "URL copied", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Filled.ContentCopy, "Copy",
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            try {
                                val i = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(i)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No browser", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary)
                    ) { Text("Open in browser") }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Start / Stop button
        Button(
            onClick = {
                if (isRunning) {
                    WifiTransferManager.stop()
                    isRunning = false
                    url = null
                    Toast.makeText(context, "Server stopped", Toast.LENGTH_SHORT).show()
                } else {
                    val ok = WifiTransferManager.start(context)
                    if (ok) {
                        isRunning = true
                        url = WifiTransferManager.getUrl()
                        Toast.makeText(context, "Server started", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to start. Check WiFi.", Toast.LENGTH_LONG).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRunning) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(if (isRunning) "Stop Server" else "Start Server",
                fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(24.dp))

        // Info card
        Card(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("How to use", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(10.dp))
                InfoLine("1", "Phone and PC should be on same WiFi")
                InfoLine("2", "Tap Start Server above")
                InfoLine("3", "Open the URL in PC browser")
                InfoLine("4", "Download, upload, browse files")
                InfoLine("5", "No app needed on PC — just browser")
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun InfoLine(num: String, text: String) {
    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(22.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(num, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f))
    }
}