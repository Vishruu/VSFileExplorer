package com.vishruu.vsfileexplorer

import androidx.compose.material.icons.filled.Home
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.vishruu.vsfileexplorer.ui.theme.DefaultAccent
import com.vishruu.vsfileexplorer.ui.theme.DefaultFont
import com.vishruu.vsfileexplorer.ui.theme.VSFileExplorerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


fun chipsForCategory(catLabel: String): List<Pair<String, List<String>>> {
    return when (catLabel) {
        "Images" -> listOf(
            "All" to emptyList(),
            "JPG" to listOf("jpg", "jpeg"),
            "PNG" to listOf("png"),
            "GIF" to listOf("gif"),
            "WEBP" to listOf("webp"),
            "BMP" to listOf("bmp")
        )
        "Videos" -> listOf(
            "All" to emptyList(),
            "MP4" to listOf("mp4"),
            "MKV" to listOf("mkv"),
            "AVI" to listOf("avi"),
            "MOV" to listOf("mov"),
            "3GP" to listOf("3gp")
        )
        "Audio" -> listOf(
            "All" to emptyList(),
            "MP3" to listOf("mp3"),
            "WAV" to listOf("wav"),
            "M4A" to listOf("m4a"),
            "OGG" to listOf("ogg"),
            "FLAC" to listOf("flac")
        )
        "Documents" -> listOf(
            "All" to emptyList(),
            "PDF" to listOf("pdf"),
            "DOC" to listOf("doc", "docx"),
            "XLS" to listOf("xls", "xlsx"),
            "PPT" to listOf("ppt", "pptx"),
            "TXT" to listOf("txt"),
            "EBOOK" to listOf("epub", "mobi", "azw3")
        )
        "ZIP" -> listOf(
            "All" to emptyList(),
            "ZIP" to listOf("zip"),
            "RAR" to listOf("rar"),
            "7Z" to listOf("7z"),
            "TAR" to listOf("tar", "gz")
        )
        "APK" -> listOf(
            "All" to emptyList(),
            "APK" to listOf("apk"),
            "XAPK" to listOf("xapk", "apks")
        )
        else -> listOf("All" to emptyList())
    }
}

class MainActivity : androidx.fragment.app.FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.google.android.gms.ads.MobileAds.initialize(this) { }
        ShortcutRouter.handleIntent(intent)
        setContent { AppRoot() }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        ShortcutRouter.handleIntent(intent)
    }
}

@Composable
fun AppRoot() {
    val context = LocalContext.current
    var accentColor by remember { mutableStateOf(DefaultAccent) }
    var fontFamily by remember { mutableStateOf(DefaultFont) }
    var showSplash by remember { mutableStateOf(true) }
    var isAppUnlocked by remember { mutableStateOf(!AppLockManager.isEnabled(context)) }
    var themeMode by remember { mutableStateOf(SettingsManager.getThemeMode(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                if (AppLockManager.isEnabled(context)) {
                    isAppUnlocked = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val isDark = themeMode == "dark"

    VSFileExplorerTheme(
        accentColor = accentColor,
        fontFamily = fontFamily,
        isDark = isDark
    ) {
        when {
            showSplash -> SplashScreen(onFinished = { showSplash = false })
            !isAppUnlocked -> AppLockScreen(context = context, onUnlocked = { isAppUnlocked = true })
            else -> Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                FileExplorerScreen(
                    accentColor = accentColor,
                    fontFamily = fontFamily,
                    themeMode = themeMode,
                    onAccentChange = { accentColor = it },
                    onFontChange = { fontFamily = it },
                    onThemeChange = { newMode ->
                        themeMode = newMode
                        SettingsManager.setThemeMode(context, newMode)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FileExplorerScreen(
    accentColor: Color,
    fontFamily: FontFamily,
    themeMode: String,
    onAccentChange: (Color) -> Unit,
    onFontChange: (FontFamily) -> Unit,
    onThemeChange: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val haptic = LocalHapticFeedback.current

    var searchSizeFilter by remember { mutableStateOf(SearchSizeFilter.ANY) }
    var searchTypeFilter by remember { mutableStateOf(SearchTypeFilter.ANY) }
    var searchDateFilter by remember { mutableStateOf(SearchDateFilter.ANY) }
    var hasPermission by remember { mutableStateOf(false) }
    var permissionChecked by remember { mutableStateOf(false) }
    val rootPath = Environment.getExternalStorageDirectory().absolutePath
    var currentPath by remember { mutableStateOf(rootPath) }
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var selectedPaths by remember { mutableStateOf<Set<String>>(emptySet()) }
    var clipboard by remember { mutableStateOf<ClipboardData?>(null) }
    var sortMode by remember {
        val saved = SettingsManager.getDefaultSortMode(context)
        mutableStateOf(
            try { SortMode.valueOf(saved) } catch (e: Exception) { SortMode.NAME_ASC }
        )
    }
    var viewMode by remember {
        val saved = SettingsManager.getDefaultViewMode(context)
        mutableStateOf(
            try { ViewMode.valueOf(saved) } catch (e: Exception) { ViewMode.MEDIUM_ICON }
        )
    }
    var showHidden by remember {
        mutableStateOf(SettingsManager.getDefaultShowHidden(context))
    }
    var currentScreen by remember { mutableStateOf("home") }
    var isRefreshing by remember { mutableStateOf(false) }

    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<File>>(emptyList()) }
    var isSearchRunning by remember { mutableStateOf(false) }

    var activeCategory by remember { mutableStateOf<CategoryFilter?>(null) }
    var categoryCurrentFolder by remember { mutableStateOf<File?>(null) }
    var categoryFolders by remember { mutableStateOf<List<CategoryFolder>>(emptyList()) }
    var categoryFolderFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var isCategoryRunning by remember { mutableStateOf(false) }
    var categoryViewMode by remember { mutableStateOf(ViewMode.MEDIUM_ICON) }
    var categorySelectedPaths by remember { mutableStateOf<Set<String>>(emptySet()) }
    var categorySubFilter by remember { mutableStateOf("All") }

    var propertiesFile by remember { mutableStateOf<File?>(null) }

    var showAnalyzer by remember { mutableStateOf(false) }
    var isAnalyzerRunning by remember { mutableStateOf(false) }
    var analyzerCategories by remember { mutableStateOf<List<StorageCategory>>(emptyList()) }
    var analyzerLargestFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var analyzerTotalUsed by remember { mutableStateOf(0L) }
    var analyzerTotalFree by remember { mutableStateOf(0L) }
    var analyzerLargestFolder by remember { mutableStateOf<Pair<String, Long>?>(null) }
    var analyzerFolderStats by remember { mutableStateOf<List<FolderStat>>(emptyList()) }
    var analyzerFilesByCategory by remember { mutableStateOf<Map<String, List<File>>>(emptyMap()) }

    var showAppManager by remember { mutableStateOf(false) }
    var isAppsLoading by remember { mutableStateOf(false) }
    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var appFilterUser by remember { mutableStateOf(true) }
    var appSearchQuery by remember { mutableStateOf("") }
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var showAppMenuDialog by remember { mutableStateOf(false) }
    var appBackupRunning by remember { mutableStateOf(false) }
    var appBackupProgress by remember { mutableStateOf(0) }

    var showImageViewer by remember { mutableStateOf(false) }
    var imageViewerList by remember { mutableStateOf<List<File>>(emptyList()) }
    var imageViewerIndex by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    var showRecycleBin by remember { mutableStateOf(false) }
    var showToolbox by remember { mutableStateOf(false) }
    var showVault by remember { mutableStateOf(false) }
    var showWifiTransfer by remember { mutableStateOf(false) }
    var showSecureNotes by remember { mutableStateOf(false) }
    var showNetworkStorage by remember { mutableStateOf(false) }
    var showBookmarks by remember { mutableStateOf(false) }
    var showDuplicateFinder by remember { mutableStateOf(false) }
    var showIconChanger by remember { mutableStateOf(false) }
    var showRecents by remember { mutableStateOf(false) }
    var showAudioPlayer by remember { mutableStateOf(false) }
    var audioPlayerList by remember { mutableStateOf<List<File>>(emptyList()) }
    var audioPlayerIndex by remember { mutableIntStateOf(0) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showShredDialog by remember { mutableStateOf(false) }
    var isShredding by remember { mutableStateOf(false) }
    var shredProgress by remember { mutableIntStateOf(0) }
    var moveToTrash by remember { mutableStateOf(true) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showZipDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    var newFolderText by remember { mutableStateOf("") }
    var zipName by remember { mutableStateOf("") }

    var showEncryptChoice by remember { mutableStateOf(false) }
    var onePasswordForAll by remember { mutableStateOf(true) }
    var showEncryptDialog by remember { mutableStateOf(false) }
    var encryptPassword by remember { mutableStateOf("") }
    var encryptPasswordConfirm by remember { mutableStateOf("") }
    var encryptFileName by remember { mutableStateOf(false) }
    var showEncryptPassword by remember { mutableStateOf(false) }
    var encryptError by remember { mutableStateOf<String?>(null) }
    var pendingEncryptPaths by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentEncryptIndex by remember { mutableStateOf(0) }

    var showDecryptDialog by remember { mutableStateOf(false) }
    var decryptPassword by remember { mutableStateOf("") }
    var showDecryptPassword by remember { mutableStateOf(false) }
    var decryptError by remember { mutableStateOf<String?>(null) }
    var decryptTargetPath by remember { mutableStateOf<String?>(null) }
    var decryptAttempts by remember { mutableStateOf(0) }
    var decryptLockUntil by remember { mutableStateOf(0L) }
    var decryptLockRemaining by remember { mutableStateOf(0) }
    var showDecryptMultiDialog by remember { mutableStateOf(false) }
    var decryptMultiPassword by remember { mutableStateOf("") }
    var showDecryptMultiPassword by remember { mutableStateOf(false) }
    var decryptMultiError by remember { mutableStateOf<String?>(null) }
    var decryptMultiRunning by remember { mutableStateOf(false) }
    var decryptMultiProgress by remember { mutableIntStateOf(0) }

    var showProgress by remember { mutableStateOf(false) }
    var progressPercent by remember { mutableStateOf(0) }
    var progressLabel by remember { mutableStateOf("") }
    var progressCancelled by remember { mutableStateOf(false) }
    var progressHideable by remember { mutableStateOf(false) }
    var progressHidden by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) files = loadFiles(currentPath, sortMode, showHidden)
    }

    LaunchedEffect(ShortcutRouter.pendingAction) {
        val action = ShortcutRouter.pendingAction ?: return@LaunchedEffect
        when (action) {
            "search" -> isSearching = true
            "vault" -> showVault = true
            "settings" -> showSettings = true
            "cloud" -> GoogleDriveHub.open()
            "camera" -> {
                try {
                    val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                    context.startActivity(cameraIntent)
                } catch (e: Exception) {
                    Toast.makeText(context, "No camera app found", Toast.LENGTH_SHORT).show()
                }
            }
        }
        ShortcutRouter.consume()
    }

    val lifecycleOwnerForPermission = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwnerForPermission) {
        val checkPermission: () -> Unit = {
            hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
            permissionChecked = true
        }
        checkPermission()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkPermission()
                if (hasPermission && files.isEmpty()) {
                    files = loadFiles(currentPath, sortMode, showHidden)
                }
            }
        }
        lifecycleOwnerForPermission.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwnerForPermission.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(sortMode, showHidden) {
        if (hasPermission) {
            files = withContext(Dispatchers.IO) {
                loadFiles(currentPath, sortMode, showHidden)
            }
        }
    }

    LaunchedEffect(searchQuery, isSearching, searchSizeFilter, searchTypeFilter, searchDateFilter) {
        if (isSearching && searchQuery.length >= 2) {
            isSearchRunning = true
            val rawResults = withContext(Dispatchers.IO) {
                searchFiles(File(rootPath), searchQuery, 2000)
            }
            searchResults = rawResults.filter { file ->
                val sizeOk = when (searchSizeFilter) {
                    SearchSizeFilter.ANY -> true
                    SearchSizeFilter.SMALL -> file.length() in 0..(1024L * 1024L)
                    SearchSizeFilter.MEDIUM -> file.length() in (1024L * 1024L)..(10L * 1024L * 1024L)
                    SearchSizeFilter.LARGE -> file.length() in (10L * 1024L * 1024L)..(100L * 1024L * 1024L)
                    SearchSizeFilter.HUGE -> file.length() > 100L * 1024L * 1024L
                }
                val typeOk = when (searchTypeFilter) {
                    SearchTypeFilter.ANY -> true
                    SearchTypeFilter.FILES -> !file.isDirectory
                    SearchTypeFilter.FOLDERS -> file.isDirectory
                    SearchTypeFilter.IMAGES -> !file.isDirectory && file.extension.lowercase() in
                            listOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif")
                    SearchTypeFilter.VIDEOS -> !file.isDirectory && file.extension.lowercase() in
                            listOf("mp4", "mkv", "avi", "mov", "webm", "3gp", "flv", "m4v")
                    SearchTypeFilter.AUDIO -> !file.isDirectory && file.extension.lowercase() in
                            listOf("mp3", "wav", "aac", "ogg", "flac", "m4a", "opus", "wma")
                    SearchTypeFilter.DOCS -> !file.isDirectory && file.extension.lowercase() in
                            listOf("pdf", "doc", "docx", "txt", "ppt", "pptx", "xls", "xlsx", "odt", "rtf")
                }
                val now = System.currentTimeMillis()
                val dayMs = 24L * 60 * 60 * 1000
                val dateOk = when (searchDateFilter) {
                    SearchDateFilter.ANY -> true
                    SearchDateFilter.TODAY -> (now - file.lastModified()) < dayMs
                    SearchDateFilter.WEEK -> (now - file.lastModified()) < dayMs * 7
                    SearchDateFilter.MONTH -> (now - file.lastModified()) < dayMs * 30
                    SearchDateFilter.OLDER -> (now - file.lastModified()) >= dayMs * 30
                }
                sizeOk && typeOk && dateOk
            }.take(500)
            isSearchRunning = false
        } else if (isSearching && searchQuery.length < 2) {
            searchResults = emptyList()
        }
    }

    LaunchedEffect(activeCategory, categoryCurrentFolder) {
        val cat = activeCategory
        if (cat != null) {
            categoryFolders = emptyList()
            categoryFolderFiles = emptyList()
            isCategoryRunning = true

            val predicate = predicateForCategory(cat)
            val baseDir = categoryCurrentFolder ?: File(rootPath)

            val folders = withContext(Dispatchers.IO) {
                scanFolders(baseDir, predicate, 500)
            }

            val loadedFiles = withContext(Dispatchers.IO) {
                if (categoryCurrentFolder == null) {
                    val fast = scanViaMediaStore(context, cat, 500)
                    fast ?: when {
                        cat.isEncryptedOnly -> scanEncryptedFiles(File(rootPath), 500)
                        cat.isHiddenOnly -> scanHiddenFiles(File(rootPath), 500)
                        else -> scanByExtensions(File(rootPath), cat.extensions, 500)
                    }
                } else {
                    categoryCurrentFolder!!.listFiles()
                        ?.filter { it.isFile && predicate(it) }
                        ?.sortedBy { it.name.lowercase() } ?: emptyList()
                }
            }

            categoryFolders = folders
            categoryFolderFiles = loadedFiles
            isCategoryRunning = false
        }
    }

    LaunchedEffect(showAnalyzer) {
        if (showAnalyzer) {
            isAnalyzerRunning = true
            val r = withContext(Dispatchers.IO) { scanStorageAnalyzerFast(context, File(rootPath)) }
            analyzerCategories = r.categories
            analyzerLargestFiles = r.largestFiles
            analyzerTotalUsed = r.usedBytes
            analyzerTotalFree = r.freeBytes
            analyzerLargestFolder = r.largestFolder
            analyzerFolderStats = r.folderStats
            analyzerFilesByCategory = r.filesByCategory
            isAnalyzerRunning = false
        }
    }

    LaunchedEffect(showAppManager) {
        if (showAppManager) {
            isAppsLoading = true
            installedApps = withContext(Dispatchers.IO) { loadInstalledApps(context) }
            isAppsLoading = false
        }
    }

    LaunchedEffect(decryptLockUntil) {
        if (decryptLockUntil > System.currentTimeMillis()) {
            while (System.currentTimeMillis() < decryptLockUntil) {
                decryptLockRemaining = ((decryptLockUntil - System.currentTimeMillis()) / 1000).toInt() + 1
                delay(500)
            }
            decryptLockRemaining = 0
            decryptLockUntil = 0L
            decryptAttempts = 0
            decryptError = null
        }
    }

    fun navigateDrawer(key: String) {
        when (key) {
            "home" -> { currentScreen = "home"; activeCategory = null }
            "internal" -> {
                activeCategory = null
                try { RecentFilesStore.addRecent(context, rootPath) } catch (_: Exception) {}
                currentPath = rootPath
                scope.launch {
                    files = withContext(Dispatchers.IO) {
                        loadFiles(rootPath, sortMode, showHidden)
                    }
                }
                currentScreen = "browse"
            }
            "downloads" -> {
                activeCategory = null
                val dl = File(Environment.getExternalStorageDirectory(), "Download")
                val path = if (dl.exists()) dl.absolutePath else rootPath
                try { RecentFilesStore.addRecent(context, path) } catch (_: Exception) {}
                currentPath = path
                scope.launch {
                    files = withContext(Dispatchers.IO) {
                        loadFiles(path, sortMode, showHidden)
                    }
                }
                currentScreen = "browse"
            }
            "images" -> {
                categoryCurrentFolder = null
                categoryViewMode = ViewMode.MEDIUM_ICON
                categorySelectedPaths = emptySet()
                categorySubFilter = "All"
                activeCategory = categoryFilterFor("Images")
            }
            "videos" -> {
                categoryCurrentFolder = null
                categoryViewMode = ViewMode.MEDIUM_ICON
                categorySelectedPaths = emptySet()
                categorySubFilter = "All"
                activeCategory = categoryFilterFor("Videos")
            }
            "audio" -> {
                categoryCurrentFolder = null
                categoryViewMode = ViewMode.MEDIUM_ICON
                categorySelectedPaths = emptySet()
                categorySubFilter = "All"
                activeCategory = categoryFilterFor("Audio")
            }
            "documents" -> {
                categoryCurrentFolder = null
                categoryViewMode = ViewMode.MEDIUM_ICON
                categorySelectedPaths = emptySet()
                categorySubFilter = "All"
                activeCategory = categoryFilterFor("Documents")
            }
            "apps" -> showAppManager = true
            "favorites" -> showBookmarks = true
            "recent" -> showRecents = true
            "recycle" -> showRecycleBin = true
            "toolbox" -> showToolbox = true
            "vault" -> showVault = true
            "wifi" -> showWifiTransfer = true
            "notes" -> showSecureNotes = true
            "network" -> showNetworkStorage = true
            "settings" -> showSettings = true
            "cloud" -> GoogleDriveHub.open()
            "duplicates" -> showDuplicateFinder = true
            "icon_changer" -> showIconChanger = true
        }
    }

    BackHandler(
        enabled = currentPath != rootPath || selectedPaths.isNotEmpty() ||
                categorySelectedPaths.isNotEmpty() ||
                clipboard != null || isSearching || currentScreen == "browse" ||
                activeCategory != null || categoryCurrentFolder != null ||
                showAnalyzer || showAppManager || showImageViewer || showSettings ||
                propertiesFile != null || showRecycleBin || showToolbox || showAudioPlayer ||
                showVault || showWifiTransfer || showSecureNotes || showNetworkStorage ||
                showBookmarks || showRecents || TextEditorHub.pendingFile != null ||
                PdfViewerHub.pendingFile != null || GoogleDriveHub.isOpen ||
                showDuplicateFinder || showIconChanger
    ) {
        when {
            propertiesFile != null -> propertiesFile = null
            showSettings -> showSettings = false
            showRecycleBin -> showRecycleBin = false
            showToolbox -> showToolbox = false
            showVault -> showVault = false
            showWifiTransfer -> { WifiTransferManager.stop(); showWifiTransfer = false }
            showSecureNotes -> showSecureNotes = false
            showNetworkStorage -> { NetworkManager.disconnect(); showNetworkStorage = false }
            showBookmarks -> showBookmarks = false
            showRecents -> showRecents = false
            TextEditorHub.pendingFile != null -> TextEditorHub.consume()
            PdfViewerHub.pendingFile != null -> PdfViewerHub.consume()
            GoogleDriveHub.isOpen -> GoogleDriveHub.close()
            showDuplicateFinder -> showDuplicateFinder = false
            showIconChanger -> showIconChanger = false
            showAudioPlayer -> showAudioPlayer = false
            showImageViewer -> showImageViewer = false
            showAppManager -> { showAppManager = false; selectedApp = null }
            showAnalyzer -> showAnalyzer = false
            categoryCurrentFolder != null -> {
                val parent = categoryCurrentFolder!!.parentFile
                categoryCurrentFolder =
                    if (parent == null || parent.absolutePath == rootPath) null else parent
            }
            activeCategory != null -> activeCategory = null
            categorySelectedPaths.isNotEmpty() -> categorySelectedPaths = emptySet()
            isSearching -> { isSearching = false; searchQuery = ""; searchResults = emptyList() }
            selectedPaths.isNotEmpty() -> selectedPaths = emptySet()
            clipboard != null -> clipboard = null
            currentScreen == "browse" && currentPath != rootPath -> {
                val parent = File(currentPath).parentFile
                if (parent != null) {
                    currentPath = parent.absolutePath
                    files = loadFiles(currentPath, sortMode, showHidden)
                }
            }
            currentScreen == "browse" -> currentScreen = "home"
            else -> {}
        }
    }

    if (!permissionChecked) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }

    if (!hasPermission) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Lock, null, modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text("Storage permission required", fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val i = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        i.data = Uri.parse("package:${context.packageName}")
                        context.startActivity(i)
                    } else permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }) { Text("Grant Permission") }
            }
        }
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SideDrawerContent(
                onItemClick = { key -> navigateDrawer(key) },
                onClose = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        if (showSettings) {
            SettingsScreen(
                context = context,
                accentColor = accentColor,
                fontFamily = fontFamily,
                themeMode = themeMode,
                onAccentChange = onAccentChange,
                onFontChange = onFontChange,
                onThemeChange = onThemeChange,
                onBack = { showSettings = false }
            )
            return@ModalNavigationDrawer
        }

        if (showRecycleBin) {
            RecycleBinScreen(onBack = { showRecycleBin = false })
            return@ModalNavigationDrawer
        }

        if (showToolbox) {
            ToolboxScreen(onBack = { showToolbox = false })
            return@ModalNavigationDrawer
        }

        if (showVault) {
            VaultScreen(context = context, onBack = { showVault = false })
            return@ModalNavigationDrawer
        }

        if (showWifiTransfer) {
            WifiTransferScreen(context = context, onBack = {
                WifiTransferManager.stop()
                showWifiTransfer = false
            })
            return@ModalNavigationDrawer
        }

        if (showSecureNotes) {
            SecureNotesScreen(context = context, onBack = { showSecureNotes = false })
            return@ModalNavigationDrawer
        }

        if (showNetworkStorage) {
            NetworkStorageScreen(context = context, onBack = {
                NetworkManager.disconnect()
                showNetworkStorage = false
            })
            return@ModalNavigationDrawer
        }

        if (showRecents) {
            RecentFilesScreen(
                context = context,
                onBack = { showRecents = false },
                onOpenFile = { file ->
                    if (file.isDirectory) {
                        // Folder hai — usme navigate karo
                        showRecents = false
                        currentScreen = "browse"
                        currentPath = file.absolutePath
                        scope.launch {
                            files = withContext(Dispatchers.IO) {
                                loadFiles(file.absolutePath, sortMode, showHidden)
                            }
                        }
                    } else {
                        // File hai — external app se kholo
                        openFile(context, file)
                    }
                }
            )
            return@ModalNavigationDrawer
        }

        val viewingPdf = PdfViewerHub.pendingFile
        if (viewingPdf != null) {
            PdfViewerScreen(
                context = context,
                file = viewingPdf,
                onBack = { PdfViewerHub.consume() }
            )
            return@ModalNavigationDrawer
        }

        if (GoogleDriveHub.isOpen) {
            CloudScreen(
                context = context,
                onBack = { GoogleDriveHub.close() }
            )
            return@ModalNavigationDrawer
        }

        if (showDuplicateFinder) {
            DuplicateFinderScreen(
                context = context,
                onBack = { showDuplicateFinder = false }
            )
            return@ModalNavigationDrawer
        }

        if (showIconChanger) {
            IconChangerScreen(
                context = context,
                onBack = { showIconChanger = false }
            )
            return@ModalNavigationDrawer
        }

        if (showImageViewer) {
            ImageViewerScreen(context, imageViewerList, imageViewerIndex,
                onBack = { showImageViewer = false })
            return@ModalNavigationDrawer
        }

        if (showAudioPlayer) {
            AudioPlayerScreen(
                context = context,
                audioList = audioPlayerList,
                startIndex = audioPlayerIndex,
                onBack = { showAudioPlayer = false }
            )
            return@ModalNavigationDrawer
        }

        if (showAppManager) {
            AppManagerScreen(
                context = context,
                isLoading = isAppsLoading,
                apps = installedApps,
                filterUserOnly = appFilterUser,
                searchQuery = appSearchQuery,
                selectedApp = selectedApp,
                showMenuDialog = showAppMenuDialog,
                backupRunning = appBackupRunning,
                backupProgress = appBackupProgress,
                onBack = { showAppManager = false; selectedApp = null },
                onFilterChange = { appFilterUser = it },
                onSearchChange = { appSearchQuery = it },
                onAppTap = { app -> selectedApp = app; showAppMenuDialog = true },
                onMenuDismiss = { showAppMenuDialog = false; selectedApp = null },
                onOpenApp = { app ->
                    try {
                        val i = context.packageManager.getLaunchIntentForPackage(app.packageName)
                        if (i != null) context.startActivity(i)
                        else Toast.makeText(context, "Cannot open", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Cannot open", Toast.LENGTH_SHORT).show()
                    }
                    showAppMenuDialog = false; selectedApp = null
                },
                onAppInfo = { app ->
                    try {
                        val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        i.data = Uri.parse("package:${app.packageName}")
                        context.startActivity(i)
                    } catch (e: Exception) {}
                    showAppMenuDialog = false; selectedApp = null
                },
                onUninstall = { app ->
                    if (!app.isSystem) {
                        try {
                            val i = Intent(Intent.ACTION_DELETE).apply {
                                data = Uri.parse("package:${app.packageName}")
                            }
                            context.startActivity(i)
                        } catch (e: Exception) {}
                    }
                    showAppMenuDialog = false; selectedApp = null
                },
                onBackup = { app ->
                    scope.launch {
                        appBackupRunning = true; appBackupProgress = 0
                        val r = withContext(Dispatchers.IO) {
                            backupApk(app) { p -> appBackupProgress = p }
                        }
                        appBackupRunning = false
                        if (r != null) Toast.makeText(context, "Saved: ${r.name}",
                            Toast.LENGTH_LONG).show()
                        else Toast.makeText(context, "Backup failed", Toast.LENGTH_SHORT).show()
                    }
                    showAppMenuDialog = false; selectedApp = null
                },
                onShareApk = { app ->
                    try {
                        val f = File(app.apkPath)
                        if (f.exists()) {
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context, "${context.packageName}.fileprovider", f)
                            val i = Intent(Intent.ACTION_SEND).apply {
                                type = "application/vnd.android.package-archive"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(i, "Share APK"))
                        }
                    } catch (e: Exception) {}
                    showAppMenuDialog = false; selectedApp = null
                },
                onShareLink = { app ->
                    try {
                        val i = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT,
                                "https://play.google.com/store/apps/details?id=${app.packageName}")
                        }
                        context.startActivity(Intent.createChooser(i, "Share link"))
                    } catch (e: Exception) {}
                    showAppMenuDialog = false; selectedApp = null
                }
            )
            return@ModalNavigationDrawer
        }

        if (showAnalyzer) {
            StorageAnalyzerScreen(
                context = context,
                isRunning = isAnalyzerRunning,
                categories = analyzerCategories,
                largestFiles = analyzerLargestFiles,
                usedBytes = analyzerTotalUsed,
                freeBytes = analyzerTotalFree,
                largestFolder = analyzerLargestFolder,
                folderStats = analyzerFolderStats,
                filesByCategory = analyzerFilesByCategory,
                onBack = { showAnalyzer = false }
            )
            return@ModalNavigationDrawer
        }

        if (activeCategory != null) {
            val cat = activeCategory!!
            val currentFolder = categoryCurrentFolder
            var showCategoryMenu by remember { mutableStateOf(false) }
            var showCategorySelectionMenu by remember { mutableStateOf(false) }
            var showCategoryViewDialog by remember { mutableStateOf(false) }

            Column(
                Modifier.fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        if (categorySelectedPaths.isNotEmpty()) {
                            categorySelectedPaths = emptySet()
                        } else if (currentFolder != null) {
                            val parent = currentFolder.parentFile
                            categoryCurrentFolder =
                                if (parent == null || parent.absolutePath == rootPath) null else parent
                        } else {
                            activeCategory = null
                        }
                    }) {
                        if (categorySelectedPaths.isNotEmpty()) {
                            Text("✕", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (categorySelectedPaths.isNotEmpty()) "${categorySelectedPaths.size} selected"
                            else currentFolder?.name ?: cat.label,
                            fontSize = 20.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary, maxLines = 1
                        )
                        if (categorySelectedPaths.isEmpty() && currentFolder != null) {
                            Text(currentFolder.absolutePath, fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                maxLines = 1)
                        } else if (categorySelectedPaths.isEmpty() && !isCategoryRunning) {
                            Text("${categoryFolders.size} folders • ${categoryFolderFiles.size} files",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                        }
                    }
                    Box {
                        IconButton(onClick = {
                            if (categorySelectedPaths.isNotEmpty()) showCategorySelectionMenu = true
                            else showCategoryMenu = true
                        }) {
                            Icon(Icons.Filled.MoreVert, "Menu",
                                tint = MaterialTheme.colorScheme.onBackground)
                        }
                        DropdownMenu(
                            expanded = showCategoryMenu,
                            onDismissRequest = { showCategoryMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Refresh") },
                                leadingIcon = { Icon(Icons.Filled.Refresh, null) },
                                onClick = {
                                    showCategoryMenu = false
                                    scope.launch {
                                        isRefreshing = true
                                        val predicate = predicateForCategory(cat)
                                        val baseDir = currentFolder ?: File(rootPath)
                                        categoryFolders = withContext(Dispatchers.IO) {
                                            scanFolders(baseDir, predicate, 500)
                                        }
                                        categoryFolderFiles = withContext(Dispatchers.IO) {
                                            if (currentFolder == null) {
                                                val fast = scanViaMediaStore(context, cat, 500)
                                                fast ?: when {
                                                    cat.isEncryptedOnly -> scanEncryptedFiles(File(rootPath), 500)
                                                    cat.isHiddenOnly -> scanHiddenFiles(File(rootPath), 500)
                                                    else -> scanByExtensions(File(rootPath), cat.extensions, 500)
                                                }
                                            } else currentFolder.listFiles()
                                                ?.filter { it.isFile && predicate(it) } ?: emptyList()
                                        }
                                        delay(400); isRefreshing = false
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("View") },
                                leadingIcon = { Icon(Icons.Filled.GridView, null) },
                                onClick = { showCategoryMenu = false; showCategoryViewDialog = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Select all") },
                                leadingIcon = { Icon(Icons.Filled.DoneAll, null) },
                                onClick = {
                                    showCategoryMenu = false
                                    categorySelectedPaths = (categoryFolders.map { it.folder.absolutePath } +
                                            categoryFolderFiles.map { it.absolutePath }).toSet()
                                }
                            )
                        }
                        DropdownMenu(
                            expanded = showCategorySelectionMenu,
                            onDismissRequest = { showCategorySelectionMenu = false }
                        ) {
                            val allEncrypted = categorySelectedPaths.all {
                                it.endsWith(CryptoUtils.VS_EXTENSION)
                            }
                            if (allEncrypted && categorySelectedPaths.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Decrypt all") },
                                    leadingIcon = { Icon(Icons.Filled.LockOpen, null) },
                                    onClick = {
                                        showCategorySelectionMenu = false
                                        selectedPaths = categorySelectedPaths
                                        showDecryptMultiDialog = true
                                        categorySelectedPaths = emptySet()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Cut") },
                                leadingIcon = { Icon(Icons.Filled.ContentCopy, null) },
                                onClick = {
                                    showCategorySelectionMenu = false
                                    clipboard = ClipboardData(categorySelectedPaths.toList(), isMove = true)
                                    categorySelectedPaths = emptySet()
                                    activeCategory = null
                                    currentScreen = "browse"
                                    currentPath = rootPath
                                    files = loadFiles(rootPath, sortMode, showHidden)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Copy") },
                                leadingIcon = { Icon(Icons.Filled.ContentCopy, null) },
                                onClick = {
                                    showCategorySelectionMenu = false
                                    clipboard = ClipboardData(categorySelectedPaths.toList(), isMove = false)
                                    categorySelectedPaths = emptySet()
                                    activeCategory = null
                                    currentScreen = "browse"
                                    currentPath = rootPath
                                    files = loadFiles(rootPath, sortMode, showHidden)
                                }
                            )
                            if (categorySelectedPaths.any { !File(it).name.startsWith(".") }) {
                                DropdownMenuItem(
                                    text = { Text("Hide") },
                                    leadingIcon = { Icon(Icons.Filled.VisibilityOff, null) },
                                    onClick = {
                                        showCategorySelectionMenu = false
                                        categorySelectedPaths.forEach { path ->
                                            val f = File(path)
                                            if (!f.name.startsWith(".")) {
                                                f.renameTo(File(f.parent, "." + f.name))
                                            }
                                        }
                                        categorySelectedPaths = emptySet()
                                        Toast.makeText(context, "Hidden", Toast.LENGTH_SHORT).show()
                                        scope.launch {
                                            val predicate = predicateForCategory(cat)
                                            val baseDir = currentFolder ?: File(rootPath)
                                            categoryFolders = withContext(Dispatchers.IO) {
                                                scanFolders(baseDir, predicate, 500)
                                            }
                                        }
                                    }
                                )
                            }
                            if (categorySelectedPaths.any { File(it).name.startsWith(".") }) {
                                DropdownMenuItem(
                                    text = { Text("Unhide") },
                                    leadingIcon = { Icon(Icons.Filled.Visibility, null) },
                                    onClick = {
                                        showCategorySelectionMenu = false
                                        categorySelectedPaths.forEach { path ->
                                            val f = File(path)
                                            if (f.name.startsWith(".")) {
                                                f.renameTo(File(f.parent, f.name.substring(1)))
                                            }
                                        }
                                        categorySelectedPaths = emptySet()
                                        Toast.makeText(context, "Unhidden", Toast.LENGTH_SHORT).show()
                                        scope.launch {
                                            val predicate = predicateForCategory(cat)
                                            val baseDir = currentFolder ?: File(rootPath)
                                            categoryFolders = withContext(Dispatchers.IO) {
                                                scanFolders(baseDir, predicate, 500)
                                            }
                                        }
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                leadingIcon = { Icon(Icons.Filled.Delete, null) },
                                onClick = {
                                    showCategorySelectionMenu = false
                                    categorySelectedPaths.forEach { path ->
                                        val f = File(path)
                                        if (f.exists()) TrashStore.moveToTrash(f)
                                    }
                                    categorySelectedPaths = emptySet()
                                    Toast.makeText(context, "Moved to Recycle Bin", Toast.LENGTH_SHORT).show()
                                    scope.launch {
                                        val predicate = predicateForCategory(cat)
                                        val baseDir = currentFolder ?: File(rootPath)
                                        categoryFolders = withContext(Dispatchers.IO) {
                                            scanFolders(baseDir, predicate, 500)
                                        }
                                        categoryFolderFiles = withContext(Dispatchers.IO) {
                                            if (currentFolder == null) emptyList()
                                            else currentFolder.listFiles()
                                                ?.filter { it.isFile && predicate(it) } ?: emptyList()
                                        }
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Select all") },
                                leadingIcon = { Icon(Icons.Filled.DoneAll, null) },
                                onClick = {
                                    showCategorySelectionMenu = false
                                    categorySelectedPaths = (categoryFolders.map { it.folder.absolutePath } +
                                            categoryFolderFiles.map { it.absolutePath }).toSet()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Deselect all") },
                                leadingIcon = { Icon(Icons.Filled.CheckCircle, null) },
                                onClick = {
                                    showCategorySelectionMenu = false
                                    categorySelectedPaths = emptySet()
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                val chips = chipsForCategory(cat.label)
                if (chips.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        chips.forEach { (label, _) ->
                            val selected = label == categorySubFilter
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { categorySubFilter = label }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }

                val currentExts = chips.firstOrNull { it.first == categorySubFilter }?.second ?: emptyList()
                val displayedFiles = if (categorySubFilter == "All" || currentExts.isEmpty()) {
                    categoryFolderFiles
                } else {
                    categoryFolderFiles.filter { it.extension.lowercase() in currentExts }
                }

                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch {
                            isRefreshing = true
                            val predicate = predicateForCategory(cat)
                            val baseDir = currentFolder ?: File(rootPath)
                            categoryFolders = withContext(Dispatchers.IO) {
                                scanFolders(baseDir, predicate, 500)
                            }
                            categoryFolderFiles = withContext(Dispatchers.IO) {
                                if (currentFolder == null) {
                                    val fast = scanViaMediaStore(context, cat, 500)
                                    fast ?: when {
                                        cat.isEncryptedOnly -> scanEncryptedFiles(File(rootPath), 500)
                                        cat.isHiddenOnly -> scanHiddenFiles(File(rootPath), 500)
                                        else -> scanByExtensions(File(rootPath), cat.extensions, 500)
                                    }
                                } else currentFolder.listFiles()
                                    ?.filter { it.isFile && predicate(it) } ?: emptyList()
                            }
                            delay(400); isRefreshing = false
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    if (isCategoryRunning) {
                        GridSkeleton()
                    } else if (categoryFolders.isEmpty() && displayedFiles.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.FolderOpen, null,
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                                    modifier = Modifier.size(64.dp))
                                Spacer(Modifier.height(12.dp))
                                Text("Nothing here yet", fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f))
                            }
                        }
                    } else {
                        val cols = when (categoryViewMode) {
                            ViewMode.LARGE_ICON -> 3
                            ViewMode.MEDIUM_ICON -> 4
                            ViewMode.SMALL_ICON -> 6
                            else -> 4
                        }
                        val iconSize = when (categoryViewMode) {
                            ViewMode.LARGE_ICON -> 96.dp
                            ViewMode.MEDIUM_ICON -> 64.dp
                            ViewMode.SMALL_ICON -> 40.dp
                            else -> 64.dp
                        }
                        val selActive = categorySelectedPaths.isNotEmpty()

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(cols),
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(categoryFolders) { item ->
                                val isSel = categorySelectedPaths.contains(item.folder.absolutePath)
                                Column(
                                    Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSel)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                        else MaterialTheme.colorScheme.surfaceVariant)
                                        .combinedClickable(
                                            onClick = {
                                                if (selActive) {
                                                    categorySelectedPaths = if (isSel)
                                                        categorySelectedPaths - item.folder.absolutePath
                                                    else categorySelectedPaths + item.folder.absolutePath
                                                } else categoryCurrentFolder = item.folder
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                categorySelectedPaths = categorySelectedPaths + item.folder.absolutePath
                                            }
                                        )
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(Modifier.size(iconSize), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.Folder, null,
                                            tint = getFileIconColor(FileType.FOLDER),
                                            modifier = Modifier.size(iconSize * 0.7f))
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(item.folder.name, fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 2, modifier = Modifier.fillMaxWidth())
                                    Text("${item.count}", fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            items(displayedFiles) { file ->
                                val type = getFileType(file)
                                val isSel = categorySelectedPaths.contains(file.absolutePath)
                                Column(
                                    Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSel)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                        else MaterialTheme.colorScheme.surfaceVariant)
                                        .combinedClickable(
                                            onClick = {
                                                if (selActive) {
                                                    categorySelectedPaths = if (isSel)
                                                        categorySelectedPaths - file.absolutePath
                                                    else categorySelectedPaths + file.absolutePath
                                                } else if (type == FileType.IMAGE) {
                                                    val imgList = displayedFiles.filter { isImageFile(it) }
                                                    val idx = imgList.indexOfFirst { it.absolutePath == file.absolutePath }
                                                    if (idx >= 0) {
                                                        imageViewerList = imgList
                                                        imageViewerIndex = idx
                                                        showImageViewer = true
                                                    }
                                                } else if (type == FileType.AUDIO) {
                                                    val audioList = displayedFiles.filter { getFileType(it) == FileType.AUDIO }
                                                    val idx = audioList.indexOfFirst { it.absolutePath == file.absolutePath }
                                                    if (idx >= 0) {
                                                        audioPlayerList = audioList
                                                        audioPlayerIndex = idx
                                                        showAudioPlayer = true
                                                    }
                                                } else openFile(context, file)
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                categorySelectedPaths = categorySelectedPaths + file.absolutePath
                                            }
                                        )
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    FileThumbnail(file = file, type = type, size = iconSize)
                                    Spacer(Modifier.height(4.dp))
                                    Text(file.name, fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 2, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                }
            }

            if (showCategoryViewDialog) {
                AlertDialog(
                    onDismissRequest = { showCategoryViewDialog = false },
                    title = { Text("View", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(Modifier.fillMaxWidth().height(400.dp)) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(ViewMode.entries) { mode ->
                                    ViewModeTile(mode, mode == categoryViewMode) {
                                        categoryViewMode = mode
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showCategoryViewDialog = false }) { Text("Close") }
                    }
                )
            }

            val propFileCat = propertiesFile
            if (propFileCat != null) {
                FilePropertiesDialog(file = propFileCat, onDismiss = { propertiesFile = null })
            }
            return@ModalNavigationDrawer
        }

        if (isSearching) {
            ModernSearchScreen(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                results = searchResults,
                isRunning = isSearchRunning,
                context = context,
                sizeFilter = searchSizeFilter,
                typeFilter = searchTypeFilter,
                dateFilter = searchDateFilter,
                onSizeFilterChange = { searchSizeFilter = it },
                onTypeFilterChange = { searchTypeFilter = it },
                onDateFilterChange = { searchDateFilter = it },
                onCancel = {
                    isSearching = false
                    searchQuery = ""
                    searchResults = emptyList()
                    searchSizeFilter = SearchSizeFilter.ANY
                    searchTypeFilter = SearchTypeFilter.ANY
                    searchDateFilter = SearchDateFilter.ANY
                },
                onOpenFolder = { folder ->
                    isSearching = false
                    searchQuery = ""
                    searchResults = emptyList()
                    currentPath = folder.absolutePath
                    files = loadFiles(currentPath, sortMode, showHidden)
                    currentScreen = "browse"
                }
            )
            return@ModalNavigationDrawer
        }

        if (currentScreen == "home") {
            HomeScreen(
                context = context,
                onOpenDrawer = { scope.launch { drawerState.open() } },
                onStorageClick = {
                    currentPath = rootPath
                    scope.launch {
                        files = withContext(Dispatchers.IO) {
                            loadFiles(rootPath, sortMode, showHidden)
                        }
                    }
                    currentScreen = "browse"
                },
                onCategoryClick = { label ->
                    val filter = categoryFilterFor(label)
                    if (filter != null) {
                        categoryCurrentFolder = null
                        categoryViewMode = ViewMode.MEDIUM_ICON
                        categorySelectedPaths = emptySet()
                        categorySubFilter = "All"
                        activeCategory = filter
                    } else {
                        Toast.makeText(context, "$label — Coming soon", Toast.LENGTH_SHORT).show()
                    }
                },
                onSearchClick = { isSearching = true },
                onSettingsClick = { showSettings = true },
                onAnalyzerClick = { showAnalyzer = true },
                onOpenAppManager = { showAppManager = true },
                onQuickAccessClick = { key ->
                    when (key) {
                        "encrypt" -> {
                            categoryCurrentFolder = null
                            categoryViewMode = ViewMode.MEDIUM_ICON
                            categorySelectedPaths = emptySet()
                            categorySubFilter = "All"
                            activeCategory = categoryFilterFor("Encrypt")
                        }
                        "zip" -> {
                            categoryCurrentFolder = null
                            categoryViewMode = ViewMode.MEDIUM_ICON
                            categorySelectedPaths = emptySet()
                            categorySubFilter = "All"
                            activeCategory = categoryFilterFor("ZIP Tools")
                        }
                        "hidden" -> {
                            categoryCurrentFolder = null
                            categoryViewMode = ViewMode.MEDIUM_ICON
                            categorySelectedPaths = emptySet()
                            categorySubFilter = "All"
                            activeCategory = categoryFilterFor("Hidden")
                        }
                        "downloads" -> {
                            activeCategory = null
                            val dl = File(Environment.getExternalStorageDirectory(), "Download")
                            val path = if (dl.exists()) dl.absolutePath else rootPath
                            currentPath = path
                            scope.launch {
                                files = withContext(Dispatchers.IO) {
                                    loadFiles(path, sortMode, showHidden)
                                }
                            }
                            currentScreen = "browse"
                        }
                        "new" -> {
                            activeCategory = null
                            currentPath = rootPath
                            scope.launch {
                                files = withContext(Dispatchers.IO) {
                                    loadFiles(rootPath, sortMode, showHidden)
                                }
                            }
                            currentScreen = "browse"
                            newFolderText = ""
                            showNewFolderDialog = true
                        }
                        "toolbox" -> { showToolbox = true }
                        "recycle" -> { showRecycleBin = true }
                        "bookmarks" -> {
                            Toast.makeText(context, "Bookmarks on home screen", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            activeCategory = null
                            currentPath = key
                            scope.launch {
                                files = withContext(Dispatchers.IO) {
                                    loadFiles(key, sortMode, showHidden)
                                }
                            }
                            currentScreen = "browse"
                        }
                    }
                }
            )
        } else {
            BrowserScreen(
                context = context,
                currentPath = currentPath,
                files = files,
                selectedPaths = selectedPaths,
                clipboard = clipboard,
                sortMode = sortMode,
                viewMode = viewMode,
                showHidden = showHidden,
                rootPath = rootPath,
                isRefreshing = isRefreshing,
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        files = withContext(Dispatchers.IO) {
                            loadFiles(currentPath, sortMode, showHidden)
                        }
                        delay(300); isRefreshing = false
                    }
                },
                onPathChange = { newPath ->
                    try { RecentFilesStore.addRecent(context, newPath) } catch (_: Exception) {}
                    currentPath = newPath
                    scope.launch {
                        files = withContext(Dispatchers.IO) {
                            loadFiles(newPath, sortMode, showHidden)
                        }
                    }
                },
                onSelectionChange = { selectedPaths = it },
                onClipboardChange = { clipboard = it },
                onOpenSearch = { isSearching = true },
                onSortChange = { sortMode = it; SettingsManager.setDefaultSortMode(context, it.name) },
                onViewModeChange = { viewMode = it; SettingsManager.setDefaultViewMode(context, it.name) },
                onOpenNewFolder = { newFolderText = ""; showNewFolderDialog = true },
                onOpenNewFile = { type ->
                    when (type) {
                        "Folder" -> { newFolderText = ""; showNewFolderDialog = true }
                        "Text File" -> {
                            val f = getUniqueFile(File(currentPath), "newfile.txt")
                            try {
                                f.createNewFile()
                                files = loadFiles(currentPath, sortMode, showHidden)
                                Toast.makeText(context, "Created ${f.name}", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) { Toast.makeText(context, "Failed", Toast.LENGTH_SHORT).show() }
                        }
                        "Excel File" -> {
                            val f = getUniqueFile(File(currentPath), "newfile.csv")
                            try {
                                f.createNewFile()
                                files = loadFiles(currentPath, sortMode, showHidden)
                                Toast.makeText(context, "Created ${f.name}", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) { Toast.makeText(context, "Failed", Toast.LENGTH_SHORT).show() }
                        }
                    }
                },
                onOpenRename = { path -> renameText = File(path).name; showRenameDialog = true },
                onOpenDelete = { showDeleteDialog = true },
                onOpenShred = { showShredDialog = true },
                onOpenEncrypt = {
                    if (selectedPaths.size == 1) {
                        pendingEncryptPaths = selectedPaths.toList()
                        currentEncryptIndex = 0; onePasswordForAll = true
                        encryptPassword = ""; encryptPasswordConfirm = ""
                        encryptFileName = false; encryptError = null
                        showEncryptDialog = true
                    } else {
                        onePasswordForAll = true; showEncryptChoice = true
                    }
                },
                onOpenDecrypt = { path ->
                    decryptTargetPath = path
                    decryptPassword = ""; decryptError = null
                    showDecryptPassword = false; showDecryptDialog = true
                },
                onOpenDecryptMulti = { showDecryptMultiDialog = true },
                onOpenZip = {
                    val firstName = File(selectedPaths.first()).name.substringBeforeLast('.')
                    zipName = if (selectedPaths.size == 1) firstName else "archive"
                    showZipDialog = true
                },
                onOpenUnzip = { path ->
                    val target = File(path)
                    scope.launch {
                        progressLabel = "Extracting..."; progressPercent = 0
                        progressCancelled = false; progressHidden = false
                        progressHideable = true; showProgress = true
                        val r = withContext(Dispatchers.IO) {
                            try {
                                unzipFile(target, onProgress = { p -> progressPercent = p },
                                    isCancelled = { progressCancelled })
                                Result.success(Unit)
                            } catch (e: Exception) { Result.failure(e) }
                        }
                        showProgress = false; progressHideable = false
                        if (r.isSuccess) {
                            files = loadFiles(currentPath, sortMode, showHidden)
                            selectedPaths = emptySet()
                            Toast.makeText(context, "Extracted", Toast.LENGTH_SHORT).show()
                        } else Toast.makeText(context, "Failed", Toast.LENGTH_SHORT).show()
                    }
                },
                onOpenImage = { file: File ->
                    try { RecentFilesStore.addRecent(context, file.absolutePath) } catch (_: Exception) {}
                    val imgList = files.filter { !it.isDirectory && isImageFile(it) }
                    val idx = imgList.indexOfFirst { it.absolutePath == file.absolutePath }
                    if (idx >= 0) {
                        imageViewerList = imgList; imageViewerIndex = idx
                        showImageViewer = true
                    }
                },
                onOpenAudio = { file: File ->
                    try { RecentFilesStore.addRecent(context, file.absolutePath) } catch (_: Exception) {}
                    val audioList = files.filter { !it.isDirectory && getFileType(it) == FileType.AUDIO }
                    val idx = audioList.indexOfFirst { it.absolutePath == file.absolutePath }
                    if (idx >= 0) {
                        audioPlayerList = audioList
                        audioPlayerIndex = idx
                        showAudioPlayer = true
                    }
                },
                onOpenAnalyzer = { showAnalyzer = true },
                onOpenDrawer = { scope.launch { drawerState.open() } },
                onOpenSettings = { showSettings = true },
                onShowProperties = { file -> propertiesFile = file },
                onAddBookmark = {
                    selectedPaths.forEach { path ->
                        val f = File(path)
                        if (f.isDirectory) BookmarksStore.addBookmark(context, path)
                    }
                    Toast.makeText(context, "Added to Bookmarks", Toast.LENGTH_SHORT).show()
                    selectedPaths = emptySet()
                },
                onHideSelected = {
                    selectedPaths.forEach { path ->
                        val f = File(path)
                        if (!f.name.startsWith(".")) {
                            f.renameTo(File(f.parent, "." + f.name))
                        }
                    }
                    selectedPaths = emptySet()
                    files = loadFiles(currentPath, sortMode, showHidden)
                    Toast.makeText(context, "Hidden", Toast.LENGTH_SHORT).show()
                },
                onUnhideSelected = {
                    selectedPaths.forEach { path ->
                        val f = File(path)
                        if (f.name.startsWith(".")) {
                            f.renameTo(File(f.parent, f.name.substring(1)))
                        }
                    }
                    selectedPaths = emptySet()
                    files = loadFiles(currentPath, sortMode, showHidden)
                    Toast.makeText(context, "Unhidden", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    val propFile = propertiesFile
    if (propFile != null && activeCategory == null) {
        FilePropertiesDialog(file = propFile, onDismiss = { propertiesFile = null })
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete ${selectedPaths.size} item(s)?") },
            text = {
                Column {
                    Text("This action cannot be undone if Recycle Bin is unchecked.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                    Spacer(Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = moveToTrash,
                            onCheckedChange = { moveToTrash = it }
                        )
                        Text("Move to Recycle Bin", fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground)
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            showRecycleBin = true
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
                            if (moveToTrash) {
                                TrashStore.moveToTrash(f)
                            } else {
                                deleteRecursive(f)
                            }
                        }
                    }
                    selectedPaths = emptySet()
                    files = loadFiles(currentPath, sortMode, showHidden)
                    showDeleteDialog = false
                    Toast.makeText(
                        context,
                        if (moveToTrash) "Moved to Recycle Bin" else "Deleted permanently",
                        Toast.LENGTH_SHORT
                    ).show()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename") },
            text = {
                OutlinedTextField(value = renameText, onValueChange = { renameText = it },
                    label = { Text("New name") }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    if (selectedPaths.isNotEmpty()) {
                        val oldF = File(selectedPaths.first())
                        val newF = File(oldF.parent, renameText)
                        if (oldF.renameTo(newF)) {
                            selectedPaths = emptySet()
                            files = loadFiles(currentPath, sortMode, showHidden)
                        }
                    }
                    showRenameDialog = false
                }) { Text("Rename") }
            },
            dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") } }
        )
    }

    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("New Folder") },
            text = {
                OutlinedTextField(value = newFolderText, onValueChange = { newFolderText = it },
                    label = { Text("Folder name") }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    val n = newFolderText.trim()
                    if (n.isNotEmpty()) {
                        val d = File(currentPath, n)
                        val created = if (!d.exists()) d.mkdirs() else true
                        if (created) {
                            MediaScannerConnection.scanFile(context, arrayOf(d.absolutePath), null, null)
                            Toast.makeText(context, "Folder created", Toast.LENGTH_SHORT).show()
                        }
                        files = loadFiles(currentPath, sortMode, showHidden)
                    }
                    showNewFolderDialog = false
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showNewFolderDialog = false }) { Text("Cancel") } }
        )
    }

    if (showZipDialog) {
        AlertDialog(
            onDismissRequest = { showZipDialog = false },
            title = { Text("Create ZIP") },
            text = {
                Column {
                    Text("${selectedPaths.size} item(s) will be zipped", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = zipName, onValueChange = { zipName = it },
                        label = { Text("ZIP file name") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val base = zipName.trim().ifEmpty { "archive" }
                    val finalName = if (base.lowercase().endsWith(".zip")) base else "$base.zip"
                    val outZip = getUniqueFile(File(currentPath), finalName)
                    val sources = selectedPaths.map { File(it) }
                    scope.launch {
                        progressLabel = "Zipping..."; progressPercent = 0
                        progressCancelled = false; progressHidden = false
                        progressHideable = true; showProgress = true
                        val r = withContext(Dispatchers.IO) {
                            try {
                                zipFiles(sources, outZip,
                                    onProgress = { p -> progressPercent = p },
                                    isCancelled = { progressCancelled })
                                Result.success(Unit)
                            } catch (e: Exception) { Result.failure(e) }
                        }
                        showProgress = false; progressHideable = false
                        if (r.isSuccess) {
                            files = loadFiles(currentPath, sortMode, showHidden)
                            selectedPaths = emptySet()
                            Toast.makeText(context, "ZIP created", Toast.LENGTH_SHORT).show()
                        }
                    }
                    showZipDialog = false
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showZipDialog = false }) { Text("Cancel") } }
        )
    }

    if (showEncryptChoice) {
        AlertDialog(
            onDismissRequest = { showEncryptChoice = false },
            title = { Text("Encrypt ${selectedPaths.size} items") },
            text = {
                Column {
                    Text("How do you want to encrypt?", fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth().clickable { onePasswordForAll = true }
                        .padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = onePasswordForAll, onClick = { onePasswordForAll = true })
                        Text("One password for all", fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onBackground)
                    }
                    Row(Modifier.fillMaxWidth().clickable { onePasswordForAll = false }
                        .padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = !onePasswordForAll, onClick = { onePasswordForAll = false })
                        Text("Different password for each", fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingEncryptPaths = selectedPaths.toList()
                    currentEncryptIndex = 0
                    encryptPassword = ""; encryptPasswordConfirm = ""
                    encryptFileName = false; encryptError = null
                    showEncryptChoice = false; showEncryptDialog = true
                }) { Text("Continue") }
            },
            dismissButton = { TextButton(onClick = { showEncryptChoice = false }) { Text("Cancel") } }
        )
    }

    if (showEncryptDialog) {
        val total = pendingEncryptPaths.size
        val isMulti = total > 1
        val currentName = if (currentEncryptIndex < total)
            File(pendingEncryptPaths[currentEncryptIndex]).name else ""

        AlertDialog(
            onDismissRequest = {
                showEncryptDialog = false
                encryptPassword = ""; encryptPasswordConfirm = ""; encryptError = null
            },
            title = {
                Text(
                    if (isMulti && !onePasswordForAll) "Encrypt file ${currentEncryptIndex + 1} of $total"
                    else if (isMulti) "Encrypt $total items" else "Encrypt file"
                )
            },
            text = {
                Column {
                    if (isMulti && !onePasswordForAll) {
                        Text(currentName, fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), maxLines = 1)
                        Spacer(Modifier.height(10.dp))
                    }
                    OutlinedTextField(
                        value = encryptPassword,
                        onValueChange = { encryptPassword = it; encryptError = null },
                        label = { Text("Enter password") }, singleLine = true,
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
                        label = { Text("Re-enter password") }, singleLine = true,
                        visualTransformation = if (showEncryptPassword) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth().clickable { encryptFileName = !encryptFileName },
                        verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = encryptFileName, onCheckedChange = { encryptFileName = it })
                        Text("Encrypt file name", fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground)
                    }
                    if (encryptError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(encryptError!!, fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
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
                            progressLabel = "Encrypting..."
                            progressPercent = 0
                            progressCancelled = false
                            progressHidden = false
                            progressHideable = true
                            showProgress = true

                            val totalItems = allPaths.size.coerceAtLeast(1)
                            var successCount = 0
                            var failCount = 0

                            withContext(Dispatchers.IO) {
                                allPaths.forEachIndexed { i, path ->
                                    if (progressCancelled) return@withContext
                                    try {
                                        val c = CryptoUtils.encryptPath(
                                            source = File(path),
                                            password = pwd,
                                            encryptFileName = encName,
                                            onProgress = { },
                                            isCancelled = { progressCancelled }
                                        )
                                        successCount += c
                                    } catch (e: Exception) {
                                        failCount++
                                    }
                                    progressPercent = ((i + 1) * 100) / totalItems
                                }
                            }

                            showProgress = false
                            progressHideable = false
                            files = loadFiles(currentPath, sortMode, showHidden)
                            selectedPaths = emptySet()
                            showEncryptDialog = false
                            pendingEncryptPaths = emptyList()
                            currentEncryptIndex = 0
                            encryptPassword = ""
                            encryptPasswordConfirm = ""
                            encryptFileName = false
                            encryptError = null

                            val msg = if (failCount == 0)
                                "$successCount file(s) encrypted"
                            else
                                "$successCount encrypted, $failCount failed"
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    } else {
                        val pathTo = pendingEncryptPaths[currentEncryptIndex]
                        val pwd = encryptPassword
                        val encName = encryptFileName
                        scope.launch {
                            progressLabel = "Encrypting..."
                            progressPercent = 0
                            progressCancelled = false
                            progressHidden = false
                            progressHideable = true
                            showProgress = true

                            val r = withContext(Dispatchers.IO) {
                                try {
                                    CryptoUtils.encryptPath(
                                        source = File(pathTo),
                                        password = pwd,
                                        encryptFileName = encName,
                                        onProgress = { p -> progressPercent = p },
                                        isCancelled = { progressCancelled }
                                    )
                                    Result.success(Unit)
                                } catch (e: Exception) {
                                    Result.failure(e)
                                }
                            }

                            showProgress = false
                            progressHideable = false

                            if (r.isSuccess) {
                                files = loadFiles(currentPath, sortMode, showHidden)
                                if (currentEncryptIndex < total - 1) {
                                    currentEncryptIndex++
                                    encryptPassword = ""
                                    encryptPasswordConfirm = ""
                                    encryptError = null
                                } else {
                                    showEncryptDialog = false
                                    pendingEncryptPaths = emptyList()
                                    currentEncryptIndex = 0
                                    selectedPaths = emptySet()
                                    encryptPassword = ""
                                    encryptPasswordConfirm = ""
                                    encryptFileName = false
                                    encryptError = null
                                    Toast.makeText(context, "All files encrypted",
                                        Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                encryptError = "Encryption failed"
                            }
                        }
                    }
                }) { Text("Encrypt") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showEncryptDialog = false
                    encryptPassword = ""
                    encryptPasswordConfirm = ""
                    encryptError = null
                }) { Text("Cancel") }
            }
        )
    }

    if (showDecryptDialog) {
        val targetFile = decryptTargetPath?.let { File(it) }
        val locked = decryptLockRemaining > 0

        AlertDialog(
            onDismissRequest = {
                showDecryptDialog = false
                decryptPassword = ""; decryptError = null; decryptAttempts = 0
            },
            title = { Text("Decrypt file") },
            text = {
                Column {
                    Text(targetFile?.name ?: "", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), maxLines = 1)
                    Spacer(Modifier.height(10.dp))
                    if (locked) {
                        Text("Too many wrong attempts.", fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(6.dp))
                        Text("Wait: ${decryptLockRemaining}s", fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground)
                    } else {
                        OutlinedTextField(
                            value = decryptPassword,
                            onValueChange = { decryptPassword = it; decryptError = null },
                            label = { Text("Enter password") }, singleLine = true,
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
                    }
                }
            },
            confirmButton = {
                if (!locked) {
                    TextButton(onClick = {
                        val path = decryptTargetPath ?: return@TextButton
                        if (decryptPassword.isEmpty()) {
                            decryptError = "Password cannot be empty"; return@TextButton
                        }
                        val pwd = decryptPassword
                        scope.launch {
                            progressLabel = "Decrypting..."; progressPercent = 0
                            progressCancelled = false; progressHidden = false
                            progressHideable = true; showProgress = true
                            val r = withContext(Dispatchers.IO) {
                                try {
                                    CryptoUtils.decryptFile(
                                        sourceFile = File(path), password = pwd,
                                        onProgress = { p -> progressPercent = p },
                                        isCancelled = { progressCancelled })
                                    Result.success(Unit)
                                } catch (e: Exception) { Result.failure(e) }
                            }
                            showProgress = false; progressHideable = false
                            if (r.isSuccess) {
                                files = loadFiles(currentPath, sortMode, showHidden)
                                selectedPaths = emptySet()
                                showDecryptDialog = false
                                decryptPassword = ""; decryptError = null; decryptAttempts = 0
                                Toast.makeText(context, "Decryption complete", Toast.LENGTH_SHORT).show()
                            } else {
                                decryptAttempts++
                                decryptPassword = ""
                                if (decryptAttempts >= 3) {
                                    decryptLockUntil = System.currentTimeMillis() + 30_000L
                                    decryptError = null
                                } else decryptError = "Wrong password"
                            }
                        }
                    }) { Text("Decrypt") }
                } else {
                    TextButton(onClick = {
                        showDecryptDialog = false
                        decryptPassword = ""; decryptError = null
                    }) { Text("Close") }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDecryptDialog = false
                    decryptPassword = ""; decryptError = null; decryptAttempts = 0
                }) { Text("Cancel") }
            }
        )
    }

    if (showProgress && !progressHidden) {
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
                    Text("$progressPercent%", fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground)
                }
            },
            confirmButton = {
                if (progressHideable) {
                    TextButton(onClick = { progressHidden = true }) { Text("Hide") }
                }
            },
            dismissButton = {
                TextButton(onClick = { progressCancelled = true }) { Text("Cancel") }
            }
        )
    }

    if (showProgress && progressHidden) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Card(
                modifier = Modifier.padding(20.dp).clip(RoundedCornerShape(12.dp))
                    .clickable { progressHidden = false },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(10.dp))
                    Text("$progressPercent% — tap", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }

    if (appBackupRunning) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Backing up APK...") },
            text = {
                Column {
                    LinearProgressIndicator(
                        progress = { appBackupProgress / 100f },
                        modifier = Modifier.fillMaxWidth().height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("$appBackupProgress%", fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground)
                }
            },
            confirmButton = { }
        )
    }

    if (showShredDialog) {
        AlertDialog(
            onDismissRequest = { if (!isShredding) showShredDialog = false },
            title = { Text("Shred ${selectedPaths.size} item(s)?") },
            text = {
                Column {
                    Text("Files will be permanently destroyed with 3-pass overwrite.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.height(8.dp))
                    Text("This cannot be undone. No recovery possible.",
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error)

                    if (isShredding) {
                        Spacer(Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { shredProgress / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("$shredProgress%", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground)
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
                            selectedPaths = emptySet()
                            files = loadFiles(currentPath, sortMode, showHidden)
                            showShredDialog = false
                            Toast.makeText(context, "Files shredded permanently",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text("Shred") }
            },
            dismissButton = {
                TextButton(
                    enabled = !isShredding,
                    onClick = { showShredDialog = false }
                ) { Text("Cancel") }
            }
        )
    }

    if (showDecryptMultiDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!decryptMultiRunning) {
                    showDecryptMultiDialog = false
                    decryptMultiPassword = ""
                    decryptMultiError = null
                }
            },
            title = { Text("Decrypt ${selectedPaths.size} file(s)") },
            text = {
                Column {
                    Text("All files will be decrypted with the same password.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
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
                            IconButton(onClick = { showDecryptMultiPassword = !showDecryptMultiPassword }) {
                                Icon(
                                    imageVector = if (showDecryptMultiPassword) Icons.Filled.VisibilityOff
                                    else Icons.Filled.Visibility,
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
                        Text("$decryptMultiProgress%", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground)
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
                            files = loadFiles(currentPath, sortMode, showHidden)
                            selectedPaths = emptySet()
                            showDecryptMultiDialog = false
                            decryptMultiPassword = ""
                            decryptMultiError = null

                            val msg = if (failCount == 0)
                                "$successCount file(s) decrypted"
                            else
                                "$successCount decrypted, $failCount failed"
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                ) { Text("Decrypt") }
            },
            dismissButton = {
                TextButton(
                    enabled = !decryptMultiRunning,
                    onClick = {
                        showDecryptMultiDialog = false
                        decryptMultiPassword = ""
                        decryptMultiError = null
                    }
                ) { Text("Cancel") }
            }
        )
    }

    // ===== Global Home Button handler =====
    LaunchedEffect(NavHub.goHomeTrigger) {
        if (NavHub.goHomeTrigger > 0L) {
            if (showWifiTransfer) WifiTransferManager.stop()
            if (showNetworkStorage) NetworkManager.disconnect()
            currentScreen = "home"
            activeCategory = null
            categoryCurrentFolder = null
            isSearching = false
            searchQuery = ""
            searchResults = emptyList()
            selectedPaths = emptySet()
            categorySelectedPaths = emptySet()
            showSettings = false
            showRecycleBin = false
            showToolbox = false
            showVault = false
            showWifiTransfer = false
            showSecureNotes = false
            showNetworkStorage = false
            showBookmarks = false
            showRecents = false
            showAnalyzer = false
            showAppManager = false
            showImageViewer = false
            showAudioPlayer = false
            propertiesFile = null
        }
    }

    // Home button — har screen pe visible (home chhod ke)
    val showHomeButton = currentScreen != "home" || activeCategory != null ||
            isSearching || showSettings || showRecycleBin || showToolbox ||
            showVault || showWifiTransfer || showSecureNotes || showNetworkStorage ||
            showBookmarks || showRecents || showAnalyzer || showAppManager ||
            showImageViewer || showAudioPlayer || propertiesFile != null

    HomeFloatingButton(visible = showHomeButton)
}
@Composable
fun ModernSearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<File>,
    isRunning: Boolean,
    context: Context,
    sizeFilter: SearchSizeFilter,
    typeFilter: SearchTypeFilter,
    dateFilter: SearchDateFilter,
    onSizeFilterChange: (SearchSizeFilter) -> Unit,
    onTypeFilterChange: (SearchTypeFilter) -> Unit,
    onDateFilterChange: (SearchDateFilter) -> Unit,
    onCancel: () -> Unit,
    onOpenFolder: (File) -> Unit
) {
    var showSizeMenu by remember { mutableStateOf(false) }
    var showTypeMenu by remember { mutableStateOf(false) }
    var showDateMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(end = 12.dp)
            )

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search files & folders...",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier.weight(1f)
            )

            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box {
                FilterChipSmall(
                    label = sizeFilter.label,
                    active = sizeFilter != SearchSizeFilter.ANY,
                    onClick = { showSizeMenu = true }
                )
                DropdownMenu(
                    expanded = showSizeMenu,
                    onDismissRequest = { showSizeMenu = false }
                ) {
                    SearchSizeFilter.entries.forEach { f ->
                        DropdownMenuItem(
                            text = { Text(f.label) },
                            onClick = { onSizeFilterChange(f); showSizeMenu = false }
                        )
                    }
                }
            }

            Box {
                FilterChipSmall(
                    label = typeFilter.label,
                    active = typeFilter != SearchTypeFilter.ANY,
                    onClick = { showTypeMenu = true }
                )
                DropdownMenu(
                    expanded = showTypeMenu,
                    onDismissRequest = { showTypeMenu = false }
                ) {
                    SearchTypeFilter.entries.forEach { f ->
                        DropdownMenuItem(
                            text = { Text(f.label) },
                            onClick = { onTypeFilterChange(f); showTypeMenu = false }
                        )
                    }
                }
            }

            Box {
                FilterChipSmall(
                    label = dateFilter.label,
                    active = dateFilter != SearchDateFilter.ANY,
                    onClick = { showDateMenu = true }
                )
                DropdownMenu(
                    expanded = showDateMenu,
                    onDismissRequest = { showDateMenu = false }
                ) {
                    SearchDateFilter.entries.forEach { f ->
                        DropdownMenuItem(
                            text = { Text(f.label) },
                            onClick = { onDateFilterChange(f); showDateMenu = false }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Searching...",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            } else {
                Text(
                    text = when {
                        query.isEmpty() -> "Start typing to search"
                        query.length < 2 -> "Type at least 2 letters"
                        results.isEmpty() -> "No results found"
                        results.size == 500 -> "Showing first 500 results"
                        results.size == 1 -> "1 result"
                        else -> "${results.size} results"
                    },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Cancel",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onCancel() }
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        when {
            query.length >= 2 && results.isEmpty() && !isRunning -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "No files match \"$query\"",
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            query.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Search across all your storage",
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            else -> {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(results) { file ->
                        val isFolder = file.isDirectory
                        val isEncrypted = file.name.endsWith(CryptoUtils.VS_EXTENSION)
                        val iconBg = if (isFolder || isEncrypted)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                        val iconVector = when {
                            isEncrypted -> Icons.Filled.Lock
                            isFolder -> Icons.Filled.Folder
                            else -> Icons.Filled.InsertDriveFile
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    if (file.isDirectory) onOpenFolder(file)
                                    else openFile(context, file)
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(iconBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = file.parent ?: "",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                    maxLines = 1
                                )
                            }

                            if (!isFolder) {
                                Text(
                                    text = formatSize(file.length()),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChipSmall(
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = if (active) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onBackground
        )
    }
}