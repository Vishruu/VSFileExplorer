package com.vishruu.vsfileexplorer

import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Delete

data class DrawerItem(val label: String, val icon: ImageVector, val key: String)

@Composable
fun SideDrawerContent(
    onItemClick: (String) -> Unit,
    onClose: () -> Unit
) {
    val items = listOf(
        DrawerItem("Cloud Storage", Icons.Filled.Cloud, "cloud"),
        DrawerItem("Network Storage", Icons.Filled.Cloud, "network"),
        DrawerItem("Secure Notes", Icons.Filled.Description, "notes"),
        DrawerItem("Home", Icons.Filled.Storage, "home"),
        DrawerItem("Internal Storage", Icons.Filled.Folder, "internal"),
        DrawerItem("Downloads", Icons.Filled.Download, "downloads"),
        DrawerItem("Images", Icons.Filled.Image, "images"),
        DrawerItem("Videos", Icons.Filled.Movie, "videos"),
        DrawerItem("Audio", Icons.Filled.AudioFile, "audio"),
        DrawerItem("Documents", Icons.Filled.Description, "documents"),
        DrawerItem("Apps", Icons.Filled.Apps, "apps"),
        DrawerItem("Recycle Bin", Icons.Filled.Delete, "recycle"),
        DrawerItem("Toolbox", Icons.Filled.Build, "toolbox"),
        DrawerItem("Duplicate Finder", Icons.Filled.ContentCopy, "duplicates"),
        DrawerItem("App Icon", Icons.Filled.Palette, "icon_changer"),
        DrawerItem("Vault", Icons.Filled.Lock, "vault"),
        DrawerItem("WiFi Transfer", Icons.Filled.Wifi, "wifi"),
        DrawerItem("Favorites", Icons.Filled.Star, "favorites"),
        DrawerItem("Recent Files", Icons.Filled.History, "recent"),
        DrawerItem("Settings", Icons.Filled.Settings, "settings")


    )

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.width(280.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Storage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "VS File Explorer",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "v1.0",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            DrawerSectionLabel("NAVIGATION")

            items.take(3).forEach { item ->
                DrawerRow(item) {
                    onItemClick(item.key)
                    onClose()
                }
            }

            Spacer(Modifier.height(8.dp))
            DrawerSectionLabel("CATEGORIES")

            items.drop(3).take(5).forEach { item ->
                DrawerRow(item) {
                    onItemClick(item.key)
                    onClose()
                }
            }

            Spacer(Modifier.height(8.dp))
            DrawerSectionLabel("OTHER")

            items.drop(8).forEach { item ->
                DrawerRow(item) {
                    onItemClick(item.key)
                    onClose()
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DrawerSectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 6.dp)
    )
}

@Composable
private fun DrawerRow(item: DrawerItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = item.label,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}