package com.vishruu.vsfileexplorer

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.rendering.PDFRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PdfViewerScreen(
    context: Context,
    file: File,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var document by remember { mutableStateOf<PDDocument?>(null) }
    var renderer by remember { mutableStateOf<PDFRenderer?>(null) }
    var totalPages by remember { mutableIntStateOf(0) }
    var currentPage by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Password states
    var needsPassword by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var showPasswordText by remember { mutableStateOf(false) }
    var passwordAttempt by remember { mutableStateOf<String?>(null) }

    // Page cache (lazy render)
    val pageCache = remember { mutableStateMapOf<Int, Bitmap>() }

    // Load document
    LaunchedEffect(file.absolutePath, passwordAttempt) {
        isLoading = true
        error = null
        passwordError = null

        val result = withContext(Dispatchers.IO) {
            try {
                document?.close()
                val doc = if (passwordAttempt.isNullOrEmpty()) {
                    PDDocument.load(file)
                } else {
                    PDDocument.load(file, passwordAttempt)
                }
                Triple(doc, doc.numberOfPages, null as String?)
            } catch (e: InvalidPasswordException) {
                Triple(null, 0, "PASSWORD_REQUIRED")
            } catch (e: Exception) {
                Triple(null, 0, e.message ?: "Cannot open PDF")
            }
        }

        val (doc, pages, err) = result
        if (doc != null) {
            document = doc
            renderer = PDFRenderer(doc)
            totalPages = pages
            needsPassword = false
            passwordError = null
            pageCache.clear()
        } else if (err == "PASSWORD_REQUIRED") {
            needsPassword = true
            if (passwordAttempt != null) {
                passwordError = "Wrong password. Try again."
            }
        } else {
            error = err
        }
        isLoading = false
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            try { renderer = null } catch (_: Exception) {}
            try { document?.close() } catch (_: Exception) {}
            pageCache.clear()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ===== Top Bar =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                try { document?.close() } catch (_: Exception) {}
                onBack()
            }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack, "Back",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    file.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )
                if (totalPages > 0) {
                    Text(
                        "Page ${currentPage + 1} of $totalPages",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }

            if (totalPages > 1) {
                IconButton(onClick = {
                    scope.launch {
                        if (currentPage > 0) listState.animateScrollToItem(currentPage - 1)
                    }
                }) {
                    Icon(Icons.Filled.ArrowUpward, "Previous",
                        tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = {
                    scope.launch {
                        if (currentPage < totalPages - 1) listState.animateScrollToItem(currentPage + 1)
                    }
                }) {
                    Icon(Icons.Filled.ArrowDownward, "Next",
                        tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // ===== Content =====
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Loading PDF...",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            error!!,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            totalPages == 0 -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "PDF is empty",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }

            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(totalPages) { index ->
                        PdfPageItem(
                            index = index,
                            renderer = renderer,
                            pageCache = pageCache
                        )
                        // Update current page on scroll
                        LaunchedEffect(index, listState.firstVisibleItemIndex) {
                            if (listState.firstVisibleItemIndex == index) {
                                currentPage = index
                            }
                        }
                    }
                }
            }
        }
    }

    // ===== Password Dialog =====
    if (needsPassword) {
        AlertDialog(
            onDismissRequest = {
                try { document?.close() } catch (_: Exception) {}
                onBack()
            },
            icon = {
                Icon(
                    Icons.Filled.Lock, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Password Protected") },
            text = {
                Column {
                    Text(
                        "This PDF requires a password to open.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            passwordError = null
                        },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (showPasswordText)
                            VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPasswordText = !showPasswordText }) {
                                Icon(
                                    imageVector = if (showPasswordText) Icons.Filled.VisibilityOff
                                    else Icons.Filled.Visibility,
                                    contentDescription = "Toggle"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (passwordError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            passwordError!!,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (passwordInput.isEmpty()) {
                        passwordError = "Password cannot be empty"
                        return@Button
                    }
                    passwordAttempt = passwordInput
                    passwordInput = ""
                }) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    try { document?.close() } catch (_: Exception) {}
                    onBack()
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PdfPageItem(
    index: Int,
    renderer: PDFRenderer?,
    pageCache: MutableMap<Int, Bitmap>
) {
    val cached = pageCache[index]

    if (cached != null) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                bitmap = cached.asImageBitmap(),
                contentDescription = "Page ${index + 1}",
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White),
                contentScale = ContentScale.FillWidth
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Page ${index + 1}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(600.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }

        LaunchedEffect(index, renderer) {
            val r = renderer ?: return@LaunchedEffect
            val bmp = withContext(Dispatchers.IO) {
                try {
                    r.renderImageWithDPI(index, 120f)
                } catch (e: Exception) {
                    null
                }
            }
            if (bmp != null) pageCache[index] = bmp
        }
    }
}