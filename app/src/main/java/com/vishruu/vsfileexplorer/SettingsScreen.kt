package com.vishruu.vsfileexplorer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vishruu.vsfileexplorer.ui.theme.AccentOptions
import com.vishruu.vsfileexplorer.ui.theme.FontOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    context: Context,
    accentColor: Color,
    fontFamily: FontFamily,
    themeMode: String,
    onAccentChange: (Color) -> Unit,
    onFontChange: (FontFamily) -> Unit,
    onThemeChange: (String) -> Unit,
    onBack: () -> Unit
) {
    val versionName = remember {
        try {
            context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName ?: "1.0"
        } catch (e: Exception) { "1.0" }
    }

    // ===== States =====
    var amoledEnabled by remember { mutableStateOf(SettingsManager.isAmoled(context)) }
    var appLockEnabled by remember { mutableStateOf(AppLockManager.isEnabled(context)) }
    var thumbCacheEnabled by remember { mutableStateOf(SettingsManager.isThumbCacheEnabled(context)) }
    var vaultDelay by remember { mutableIntStateOf(SettingsManager.getVaultLockDelay(context)) }
    var shredPasses by remember { mutableIntStateOf(SettingsManager.getShredPasses(context)) }
    var lockMethod by remember { mutableStateOf(AppLockManager.getMethod(context)) }
    var defaultViewMode by remember { mutableStateOf(SettingsManager.getDefaultViewMode(context)) }
    var defaultSortMode by remember { mutableStateOf(SettingsManager.getDefaultSortMode(context)) }
    var defaultShowHidden by remember { mutableStateOf(SettingsManager.getDefaultShowHidden(context)) }
    var confirmDelete by remember { mutableStateOf(SettingsManager.isConfirmDeleteEnabled(context)) }
    var recycleAutoDays by remember { mutableIntStateOf(SettingsManager.getRecycleAutoDeleteDays(context)) }
    var thumbQuality by remember { mutableIntStateOf(SettingsManager.getThumbQuality(context)) }
    var debugMode by remember { mutableStateOf(SettingsManager.isDebugMode(context)) }

    // ===== Dialog states =====
    var showLangDialog by remember { mutableStateOf(false) }
    var showAccentDialog by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }
    var showLockMethodDialog by remember { mutableStateOf(false) }
    var showVaultDelayDialog by remember { mutableStateOf(false) }
    var showShredPassesDialog by remember { mutableStateOf(false) }
    var showViewModeDialog by remember { mutableStateOf(false) }
    var showSortModeDialog by remember { mutableStateOf(false) }
    var showRecycleDaysDialog by remember { mutableStateOf(false) }
    var showThumbQualityDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showCacheDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    var showLogsScreen by remember { mutableStateOf(false) }
    var showImportSuccess by remember { mutableStateOf<String?>(null) }
    var currentLang by remember { mutableStateOf("English") }

    // ===== Backup/Restore states =====
    var isBackupRunning by remember { mutableStateOf(false) }
    var isRestoreRunning by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var backupStatusMsg by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    // ===== Backup launcher =====
    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            isBackupRunning = true
            val ok = try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        BackupManager.createBackup(context, out)
                    } ?: false
                }
            } catch (e: Exception) {
                false
            }
            isBackupRunning = false
            backupStatusMsg = if (ok) "Backup created successfully. Keep it safe."
                              else "Backup failed. Try again."
        }
    }

    // ===== Restore launcher (confirmation dialog ke through) =====
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        pendingRestoreUri = uri
    }

    val currentAccentName = AccentOptions.firstOrNull { it.color == accentColor }?.name ?: "Custom"
    val currentFontName = FontOptions.firstOrNull { it.family == fontFamily }?.name ?: "Default"

    if (showPrivacyPolicy) {
        PrivacyPolicyScreen(onBack = { showPrivacyPolicy = false })
        return
    }

    if (showLogsScreen) {
        AppLogsScreen(onBack = { showLogsScreen = false })
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                    tint = MaterialTheme.colorScheme.primary)
            }
            Text("Settings", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(72.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Storage, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("VS File Explorer", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Version $versionName", fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ===== APPEARANCE =====
        SectionHeader("APPEARANCE")
        Spacer(modifier = Modifier.height(8.dp))

        SettingsItem(icon = Icons.Filled.Palette, title = "Accent Color",
            subtitle = currentAccentName,
            onClick = { showAccentDialog = true })

        SettingsItem(icon = Icons.Filled.FormatSize, title = "Font",
            subtitle = currentFontName,
            onClick = { showFontDialog = true })

        SettingsItem(
            icon = Icons.Filled.Palette,
            title = "Theme Mode",
            subtitle = if (themeMode == "dark") "Dark" else "Light",
            onClick = {
                val newMode = if (themeMode == "dark") "light" else "dark"
                onThemeChange(newMode)
            }
        )

        SettingsToggle(
            icon = Icons.Filled.Image,
            title = "AMOLED Pure Black",
            subtitle = if (amoledEnabled) "Enabled" else "Disabled",
            checked = amoledEnabled,
            onCheckedChange = {
                amoledEnabled = it
                SettingsManager.setAmoled(context, it)
                Toast.makeText(context, "Restart app to apply", Toast.LENGTH_SHORT).show()
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ===== DEFAULTS =====
        SectionHeader("DEFAULTS")
        Spacer(modifier = Modifier.height(8.dp))

        SettingsItem(
            icon = Icons.Filled.GridView,
            title = "Default View Mode",
            subtitle = SettingsManager.viewModeLabel(defaultViewMode),
            onClick = { showViewModeDialog = true }
        )

        SettingsItem(
            icon = Icons.Filled.Sort,
            title = "Default Sort",
            subtitle = SettingsManager.sortModeLabel(defaultSortMode),
            onClick = { showSortModeDialog = true }
        )

        SettingsToggle(
            icon = Icons.Filled.Visibility,
            title = "Show Hidden Files",
            subtitle = if (defaultShowHidden) "Always show" else "Always hide",
            checked = defaultShowHidden,
            onCheckedChange = {
                defaultShowHidden = it
                SettingsManager.setDefaultShowHidden(context, it)
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ===== GENERAL =====
        SectionHeader("GENERAL")
        Spacer(modifier = Modifier.height(8.dp))

        SettingsItem(icon = Icons.Filled.Language, title = "Language",
            subtitle = currentLang,
            onClick = { showLangDialog = true })

        Spacer(modifier = Modifier.height(20.dp))

        // ===== SECURITY =====
        SectionHeader("SECURITY")
        Spacer(modifier = Modifier.height(8.dp))

        SettingsToggle(
            icon = Icons.Filled.Lock,
            title = "App Lock",
            subtitle = if (appLockEnabled) "Enabled" else "Disabled",
            checked = appLockEnabled,
            onCheckedChange = { enabled ->
                appLockEnabled = enabled
                if (enabled) {
                    AppLockManager.setEnabled(context, true)
                    if (!AppLockManager.isPasswordSetup(context)) {
                        Toast.makeText(context,
                            "Lock setup will appear next time you open the app",
                            Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "App Lock enabled", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    AppLockManager.setEnabled(context, false)
                    Toast.makeText(context, "App Lock disabled", Toast.LENGTH_SHORT).show()
                }
            }
        )

        if (appLockEnabled) {
            SettingsItem(
                icon = Icons.Filled.Fingerprint,
                title = "Lock Method",
                subtitle = lockMethod.label,
                onClick = { showLockMethodDialog = true }
            )
        }

        SettingsItem(
            icon = Icons.Filled.Schedule,
            title = "Vault Auto-Lock Delay",
            subtitle = SettingsManager.vaultLockDelayLabel(vaultDelay),
            onClick = { showVaultDelayDialog = true }
        )

        SettingsItem(
            icon = Icons.Filled.DeleteForever,
            title = "Shredder Passes",
            subtitle = SettingsManager.shredPassesLabel(shredPasses),
            onClick = { showShredPassesDialog = true }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ===== SAFETY =====
        SectionHeader("SAFETY")
        Spacer(modifier = Modifier.height(8.dp))

        SettingsToggle(
            icon = Icons.Filled.Shield,
            title = "Confirm Before Delete",
            subtitle = if (confirmDelete) "Ask before deleting" else "Delete without asking",
            checked = confirmDelete,
            onCheckedChange = {
                confirmDelete = it
                SettingsManager.setConfirmDeleteEnabled(context, it)
            }
        )

        SettingsItem(
            icon = Icons.Filled.Delete,
            title = "Recycle Bin Auto-Delete",
            subtitle = SettingsManager.recycleAutoDeleteLabel(recycleAutoDays),
            onClick = { showRecycleDaysDialog = true }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ===== PERFORMANCE =====
        SectionHeader("PERFORMANCE")
        Spacer(modifier = Modifier.height(8.dp))

        SettingsToggle(
            icon = Icons.Filled.Image,
            title = "Thumbnail Cache",
            subtitle = if (thumbCacheEnabled) "Faster loading" else "Disabled",
            checked = thumbCacheEnabled,
            onCheckedChange = {
                thumbCacheEnabled = it
                SettingsManager.setThumbCacheEnabled(context, it)
            }
        )

        SettingsItem(
            icon = Icons.Filled.Image,
            title = "Thumbnail Quality",
            subtitle = SettingsManager.thumbQualityLabel(thumbQuality),
            onClick = { showThumbQualityDialog = true }
        )

        SettingsItem(
            icon = Icons.Filled.CleaningServices,
            title = "Clear Cache",
            subtitle = "Free up storage",
            onClick = { showCacheDialog = true }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ===== ABOUT =====
        SectionHeader("ABOUT")
        Spacer(modifier = Modifier.height(8.dp))

        SettingsItem(icon = Icons.Filled.Person, title = "Developer",
            subtitle = "Vishruu", onClick = { })

        SettingsItem(icon = Icons.Filled.Shield, title = "Encryption",
            subtitle = "AES-256-GCM • .vs extension",
            onClick = {
                Toast.makeText(context,
                    "Files encrypted with master password. Recovery: none.",
                    Toast.LENGTH_LONG).show()
            })

        SettingsItem(
            icon = Icons.Filled.Share,
            title = "Share App",
            subtitle = "Recommend to friends",
            onClick = {
                try {
                    val i = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT,
                            "Check out VS File Explorer — by Vishruu")
                    }
                    context.startActivity(Intent.createChooser(i, "Share via"))
                } catch (e: Exception) {}
            }
        )

        SettingsItem(
            icon = Icons.Filled.PrivacyTip,
            title = "Privacy Policy",
            subtitle = "How we handle your data",
            onClick = { showPrivacyPolicy = true }
        )

        SettingsItem(
            icon = Icons.Filled.SystemUpdate,
            title = "Check for Updates",
            subtitle = "Version $versionName",
            onClick = {
                Toast.makeText(context,
                    "You're on the latest version",
                    Toast.LENGTH_SHORT).show()
            }
        )

        SettingsItem(
            icon = Icons.Filled.Email,
            title = "Contact / Feedback",
            subtitle = "vishalsalve699@gmail.com",
            onClick = {
                try {
                    val i = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:vishalsalve699@gmail.com")
                        putExtra(Intent.EXTRA_SUBJECT, "VS File Explorer Feedback")
                    }
                    context.startActivity(i)
                } catch (e: Exception) {
                    Toast.makeText(context, "No email app found",
                        Toast.LENGTH_SHORT).show()
                }
            }
        )

        SettingsItem(icon = Icons.Filled.Info, title = "About App",
            subtitle = "All-in-one file manager",
            onClick = {
                Toast.makeText(context, "VS File Explorer — by Vishruu",
                    Toast.LENGTH_SHORT).show()
            })

        Spacer(modifier = Modifier.height(20.dp))

        // ===== ADVANCED =====
        SectionHeader("ADVANCED")
        Spacer(modifier = Modifier.height(8.dp))

        SettingsItem(
            icon = Icons.Filled.FileUpload,
            title = "Full Backup",
            subtitle = if (isBackupRunning) "Creating backup..."
                       else "Backup settings, bookmarks, vault, notes",
            onClick = {
                if (!isBackupRunning) {
                    backupLauncher.launch(BackupManager.suggestedFileName())
                }
            }
        )

        SettingsItem(
            icon = Icons.Filled.FileDownload,
            title = "Restore from Backup",
            subtitle = if (isRestoreRunning) "Restoring..."
                       else "Restore from .vsbackup file",
            onClick = {
                if (!isRestoreRunning) {
                    restoreLauncher.launch(arrayOf("*/*"))
                }
            }
        )

        SettingsToggle(
            icon = Icons.Filled.BugReport,
            title = "Debug Mode",
            subtitle = if (debugMode) "Enabled" else "Disabled",
            checked = debugMode,
            onCheckedChange = {
                debugMode = it
                SettingsManager.setDebugMode(context, it)
            }
        )

        if (debugMode) {
            SettingsItem(
                icon = Icons.Filled.Info,
                title = "App Logs",
                subtitle = "${AppLogger.getLogs().size} entries",
                onClick = { showLogsScreen = true }
            )
        }

        SettingsItem(
            icon = Icons.Filled.Refresh,
            title = "Reset All Settings",
            subtitle = "Restore defaults",
            onClick = { showResetDialog = true }
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text("Made with 🤍 by Vishruu", fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(24.dp))
    }

    // ===== DIALOGS =====

    if (showAccentDialog) {
        AlertDialog(
            onDismissRequest = { showAccentDialog = false },
            title = { Text("Accent Color") },
            text = {
                Column {
                    AccentOptions.forEach { option ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onAccentChange(option.color)
                                    showAccentDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(28.dp).clip(CircleShape).background(option.color))
                            Spacer(Modifier.width(14.dp))
                            Text(option.name, fontSize = 15.sp,
                                color = if (option.color == accentColor)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onBackground)
                            Spacer(Modifier.weight(1f))
                            if (option.color == accentColor) {
                                Icon(Icons.Filled.Check, null,
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAccentDialog = false }) { Text("Close") }
            }
        )
    }

    if (showFontDialog) {
        AlertDialog(
            onDismissRequest = { showFontDialog = false },
            title = { Text("Font") },
            text = {
                Column {
                    FontOptions.forEach { option ->
                        Text(
                            text = option.name + if (option.family == fontFamily) "  ✓" else "",
                            fontSize = 16.sp, fontFamily = option.family,
                            color = if (option.family == fontFamily)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onFontChange(option.family)
                                    showFontDialog = false
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFontDialog = false }) { Text("Close") }
            }
        )
    }

    if (showLangDialog) {
        AlertDialog(
            onDismissRequest = { showLangDialog = false },
            title = { Text("Choose Language") },
            text = {
                Column {
                    listOf("English", "हिन्दी (Hindi)", "मराठी (Marathi)").forEach { lang ->
                        Text(
                            text = lang + if (lang.startsWith(currentLang)) "  ✓" else "",
                            fontSize = 16.sp,
                            color = if (lang.startsWith(currentLang))
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    currentLang = lang.split(" ").first()
                                    showLangDialog = false
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLangDialog = false }) { Text("Close") }
            }
        )
    }

    if (showLockMethodDialog) {
        AlertDialog(
            onDismissRequest = { showLockMethodDialog = false },
            title = { Text("Lock Method") },
            text = {
                Column {
                    AppLockManager.Method.values().forEach { m ->
                        Text(
                            text = m.label + if (m == lockMethod) "  ✓" else "",
                            fontSize = 15.sp,
                            color = if (m == lockMethod)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    lockMethod = m
                                    AppLockManager.setMethod(context, m)
                                    showLockMethodDialog = false
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLockMethodDialog = false }) { Text("Close") }
            }
        )
    }

    if (showVaultDelayDialog) {
        val delays = listOf(
            -1 to "Never", 0 to "Instant",
            30 to "30 seconds", 60 to "1 minute",
            300 to "5 minutes", 900 to "15 minutes"
        )
        AlertDialog(
            onDismissRequest = { showVaultDelayDialog = false },
            title = { Text("Vault Auto-Lock Delay") },
            text = {
                Column {
                    delays.forEach { (sec, label) ->
                        Text(
                            text = label + if (sec == vaultDelay) "  ✓" else "",
                            fontSize = 15.sp,
                            color = if (sec == vaultDelay)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    vaultDelay = sec
                                    SettingsManager.setVaultLockDelay(context, sec)
                                    showVaultDelayDialog = false
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVaultDelayDialog = false }) { Text("Close") }
            }
        )
    }

    if (showShredPassesDialog) {
        val passes = listOf(
            1 to "1 pass (fast)",
            3 to "3 passes (recommended)",
            7 to "7 passes (maximum)"
        )
        AlertDialog(
            onDismissRequest = { showShredPassesDialog = false },
            title = { Text("Shredder Passes") },
            text = {
                Column {
                    passes.forEach { (p, label) ->
                        Text(
                            text = label + if (p == shredPasses) "  ✓" else "",
                            fontSize = 15.sp,
                            color = if (p == shredPasses)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    shredPasses = p
                                    SettingsManager.setShredPasses(context, p)
                                    showShredPassesDialog = false
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showShredPassesDialog = false }) { Text("Close") }
            }
        )
    }

    if (showViewModeDialog) {
        val modes = listOf(
            "LARGE_ICON", "MEDIUM_ICON", "SMALL_ICON",
            "LARGE_LIST", "MEDIUM_LIST", "SMALL_LIST",
            "LARGE_DETAIL", "MEDIUM_DETAIL", "SMALL_DETAIL"
        )
        AlertDialog(
            onDismissRequest = { showViewModeDialog = false },
            title = { Text("Default View Mode") },
            text = {
                Column {
                    modes.forEach { m ->
                        Text(
                            text = SettingsManager.viewModeLabel(m) +
                                    if (m == defaultViewMode) "  ✓" else "",
                            fontSize = 15.sp,
                            color = if (m == defaultViewMode)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    defaultViewMode = m
                                    SettingsManager.setDefaultViewMode(context, m)
                                    showViewModeDialog = false
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showViewModeDialog = false }) { Text("Close") }
            }
        )
    }

    if (showSortModeDialog) {
        val modes = listOf(
            "NAME_ASC", "NAME_DESC", "SIZE_ASC", "SIZE_DESC",
            "DATE_ASC", "DATE_DESC", "TYPE"
        )
        AlertDialog(
            onDismissRequest = { showSortModeDialog = false },
            title = { Text("Default Sort") },
            text = {
                Column {
                    modes.forEach { m ->
                        Text(
                            text = SettingsManager.sortModeLabel(m) +
                                    if (m == defaultSortMode) "  ✓" else "",
                            fontSize = 15.sp,
                            color = if (m == defaultSortMode)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    defaultSortMode = m
                                    SettingsManager.setDefaultSortMode(context, m)
                                    showSortModeDialog = false
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSortModeDialog = false }) { Text("Close") }
            }
        )
    }

    if (showRecycleDaysDialog) {
        val days = listOf(
            0 to "Never", 7 to "7 days",
            15 to "15 days", 30 to "30 days", 60 to "60 days"
        )
        AlertDialog(
            onDismissRequest = { showRecycleDaysDialog = false },
            title = { Text("Recycle Bin Auto-Delete") },
            text = {
                Column {
                    days.forEach { (d, label) ->
                        Text(
                            text = label + if (d == recycleAutoDays) "  ✓" else "",
                            fontSize = 15.sp,
                            color = if (d == recycleAutoDays)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    recycleAutoDays = d
                                    SettingsManager.setRecycleAutoDeleteDays(context, d)
                                    showRecycleDaysDialog = false
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRecycleDaysDialog = false }) { Text("Close") }
            }
        )
    }

    if (showThumbQualityDialog) {
        val qualities = listOf(
            1 to "Low (fast)", 2 to "Medium", 3 to "High (slow)"
        )
        AlertDialog(
            onDismissRequest = { showThumbQualityDialog = false },
            title = { Text("Thumbnail Quality") },
            text = {
                Column {
                    qualities.forEach { (q, label) ->
                        Text(
                            text = label + if (q == thumbQuality) "  ✓" else "",
                            fontSize = 15.sp,
                            color = if (q == thumbQuality)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    thumbQuality = q
                                    SettingsManager.setThumbQuality(context, q)
                                    showThumbQualityDialog = false
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThumbQualityDialog = false }) { Text("Close") }
            }
        )
    }

    if (showCacheDialog) {
        AlertDialog(
            onDismissRequest = { showCacheDialog = false },
            title = { Text("Clear Cache?") },
            text = { Text("Thumbnail cache and temporary files will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        coil.Coil.imageLoader(context).memoryCache?.clear()
                        coil.Coil.imageLoader(context).diskCache?.clear()
                        VaultManager.cacheRoot().listFiles()?.forEach { file ->
                            if (file.name != ".nomedia") file.deleteRecursively()
                        }
                        Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                    }
                    showCacheDialog = false
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showCacheDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset All Settings?") },
            text = { Text("All settings will return to defaults.") },
            confirmButton = {
                TextButton(onClick = {
                    SettingsManager.resetAll(context)
                    showResetDialog = false
                    Toast.makeText(context, "Settings reset. Restart app.",
                        Toast.LENGTH_LONG).show()
                }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showImportSuccess != null) {
        AlertDialog(
            onDismissRequest = { showImportSuccess = null },
            title = { Text("Success") },
            text = { Text(showImportSuccess!!) },
            confirmButton = {
                TextButton(onClick = { showImportSuccess = null }) { Text("OK") }
            }
        )
    }

    // ===== RESTORE CONFIRM DIALOG =====
    if (pendingRestoreUri != null) {
        AlertDialog(
            onDismissRequest = {
                if (!isRestoreRunning) pendingRestoreUri = null
            },
            title = { Text("Restore Backup?") },
            text = {
                Text(
                    "This will REPLACE all existing data — settings, bookmarks, " +
                    "app lock, vault, notes, and recycle bin.\n\n" +
                    "Your Vault and App Lock passwords will remain the same. " +
                    "After restore, restart the app to apply everything.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isRestoreRunning,
                    onClick = {
                        val uri = pendingRestoreUri ?: return@TextButton
                        scope.launch {
                            isRestoreRunning = true
                            val result = try {
                                withContext(Dispatchers.IO) {
                                    context.contentResolver.openInputStream(uri)?.use { inp ->
                                        BackupManager.restoreBackup(context, inp)
                                    } ?: BackupManager.RestoreResult(false, "Cannot open file")
                                }
                            } catch (e: Exception) {
                                BackupManager.RestoreResult(false, "Failed: ${e.message}")
                            }
                            isRestoreRunning = false
                            pendingRestoreUri = null
                            backupStatusMsg = if (result.success)
                                "${result.message}\n\nRestart the app to apply."
                            else result.message
                        }
                    }
                ) { Text(if (isRestoreRunning) "Restoring..." else "Restore") }
            },
            dismissButton = {
                TextButton(
                    enabled = !isRestoreRunning,
                    onClick = { pendingRestoreUri = null }
                ) { Text("Cancel") }
            }
        )
    }

    // ===== BACKUP/RESTORE STATUS DIALOG =====
    if (backupStatusMsg != null) {
        AlertDialog(
            onDismissRequest = { backupStatusMsg = null },
            title = { Text("Backup") },
            text = { Text(backupStatusMsg!!) },
            confirmButton = {
                TextButton(onClick = { backupStatusMsg = null }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
}

@Composable
private fun SettingsToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
        }
    }
}