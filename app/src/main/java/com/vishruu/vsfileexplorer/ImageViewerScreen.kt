package com.vishruu.vsfileexplorer

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

fun loadImageBitmap(file: File): ImageBitmap? {
    return try {
        val opts = android.graphics.BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        android.graphics.BitmapFactory.decodeFile(file.absolutePath, opts)
        var sample = 1
        val maxDim = 2048
        while (opts.outWidth / sample > maxDim || opts.outHeight / sample > maxDim) {
            sample *= 2
        }
        val opts2 = android.graphics.BitmapFactory.Options().apply {
            inSampleSize = sample
        }
        android.graphics.BitmapFactory.decodeFile(file.absolutePath, opts2)?.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}

@Composable
fun ImageViewerScreen(
    context: Context,
    imageList: List<File>,
    startIndex: Int,
    onBack: () -> Unit
) {
    if (imageList.isEmpty()) {
        onBack()
        return
    }

    val pagerState = rememberPagerState(
        initialPage = startIndex.coerceIn(0, imageList.size - 1),
        pageCount = { imageList.size }
    )

    var isZoomed by remember { mutableStateOf(false) }
    var showUi by remember { mutableStateOf(true) }

    // Cache for loaded bitmaps — shared across all pages
    val bitmapCache = remember { mutableStateMapOf<String, ImageBitmap?>() }

    // Preload current + next image
    LaunchedEffect(pagerState.currentPage, imageList) {
        val current = imageList.getOrNull(pagerState.currentPage)
        val next = imageList.getOrNull(pagerState.currentPage + 1)
        val prev = imageList.getOrNull(pagerState.currentPage - 1)

        listOfNotNull(prev, current, next).forEach { file ->
            if (!bitmapCache.containsKey(file.absolutePath)) {
                val bmp = withContext(Dispatchers.IO) { loadImageBitmap(file) }
                bitmapCache[file.absolutePath] = bmp
            }
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        isZoomed = false
    }

    val currentFile = imageList.getOrNull(pagerState.currentPage)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !isZoomed
        ) { page ->
            val file = imageList[page]
            val cached = bitmapCache[file.absolutePath]
            val isLoaded = bitmapCache.containsKey(file.absolutePath)

            ZoomableImage(
                file = file,
                bitmap = cached,
                isLoaded = isLoaded,
                isCurrentPage = page == pagerState.currentPage,
                onZoomChanged = { zoomed ->
                    if (page == pagerState.currentPage) isZoomed = zoomed
                },
                onTap = { showUi = !showUi }
            )
        }

        if (showUi) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .align(Alignment.TopCenter),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentFile?.name ?: "",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 1
                    )
                    Text(
                        text = "${pagerState.currentPage + 1} / ${imageList.size}",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                IconButton(onClick = {
                    currentFile?.let { shareFile(context, it) }
                }) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Share",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ZoomableImage(
    file: File,
    bitmap: ImageBitmap?,
    isLoaded: Boolean,
    isCurrentPage: Boolean,
    onZoomChanged: (Boolean) -> Unit,
    onTap: () -> Unit
) {
    var scale by remember(file.absolutePath) { mutableFloatStateOf(1f) }
    var offsetX by remember(file.absolutePath) { mutableFloatStateOf(0f) }
    var offsetY by remember(file.absolutePath) { mutableFloatStateOf(0f) }

    var lastTapTime by remember(file.absolutePath) { mutableLongStateOf(0L) }
    var lastTapPos by remember(file.absolutePath) { mutableStateOf(Offset.Zero) }

    LaunchedEffect(scale) {
        if (isCurrentPage) onZoomChanged(scale > 1.05f)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(file.absolutePath) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val downPos = down.position
                            var totalMove = 0f
                            var maxPointers = 1
                            var didConsume = false

                            while (true) {
                                val event = awaitPointerEvent()
                                val pressedCount = event.changes.count { it.pressed }
                                if (pressedCount > maxPointers) maxPointers = pressedCount

                                if (pressedCount == 0) break

                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()
                                totalMove += pan.getDistance()

                                if (pressedCount >= 2) {
                                    val newScale = (scale * zoom).coerceIn(1f, 6f)
                                    scale = newScale
                                    if (newScale > 1f) {
                                        offsetX += pan.x
                                        offsetY += pan.y
                                    }
                                    event.changes.forEach { it.consume() }
                                    didConsume = true
                                } else if (scale > 1f) {
                                    offsetX += pan.x
                                    offsetY += pan.y
                                    event.changes.forEach { it.consume() }
                                    didConsume = true
                                }
                            }

                            if (!didConsume && maxPointers == 1 && totalMove < 20f) {
                                val now = System.currentTimeMillis()
                                val timeSinceLastTap = now - lastTapTime
                                val distance = (downPos - lastTapPos).getDistance()

                                if (timeSinceLastTap in 1..350 && distance < 80f) {
                                    // Double tap
                                    if (scale > 1f) {
                                        scale = 1f
                                        offsetX = 0f
                                        offsetY = 0f
                                    } else {
                                        scale = 2.5f
                                    }
                                    lastTapTime = 0L
                                    lastTapPos = Offset.Zero
                                } else {
                                    lastTapTime = now
                                    lastTapPos = downPos
                                    onTap()
                                }
                            }
                        }
                    }
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    ),
                contentScale = ContentScale.Fit
            )
        } else if (!isLoaded) {
            CircularProgressIndicator(color = Color.White)
        }
        // If isLoaded but bitmap is null → broken image, show nothing (or icon)
    }
}