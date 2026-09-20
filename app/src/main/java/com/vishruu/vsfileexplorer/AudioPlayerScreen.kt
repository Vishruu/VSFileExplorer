package com.vishruu.vsfileexplorer

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
fun AudioPlayerScreen(
    context: Context,
    audioList: List<File>,
    startIndex: Int,
    onBack: () -> Unit
) {
    if (audioList.isEmpty()) {
        onBack()
        return
    }

    var currentIndex by remember { mutableIntStateOf(startIndex.coerceIn(0, audioList.size - 1)) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var albumArt by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var artist by remember { mutableStateOf("") }
    var isShuffle by remember { mutableStateOf(false) }
    var isRepeat by remember { mutableStateOf(false) }

    val currentFile = audioList.getOrNull(currentIndex)

    // Load album art + artist
    LaunchedEffect(currentIndex) {
        withContext(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(currentFile?.absolutePath)
                val art = retriever.embeddedPicture
                albumArt = art?.let {
                    android.graphics.BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap()
                }
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: ""
                retriever.release()
            } catch (e: Exception) {
                albumArt = null
                artist = ""
            }
        }
    }

    // MediaPlayer
    DisposableEffect(currentIndex) {
        val player = MediaPlayer()
        try {
            player.setDataSource(context, Uri.fromFile(currentFile))
            player.prepare()
            duration = player.duration.toLong()
            player.setOnCompletionListener {
                if (isRepeat) {
                    player.seekTo(0)
                    player.start()
                } else if (currentIndex < audioList.size - 1) {
                    currentIndex++
                } else {
                    isPlaying = false
                }
            }
            mediaPlayer = player
            player.start()
            isPlaying = true
        } catch (e: Exception) {
            e.printStackTrace()
        }

        onDispose {
            try {
                player.stop()
                player.release()
            } catch (e: Exception) {}
            mediaPlayer = null
        }
    }

    // Position loop
    LaunchedEffect(isPlaying, currentIndex) {
        while (isPlaying) {
            try {
                currentPosition = mediaPlayer?.currentPosition?.toLong() ?: 0L
            } catch (e: Exception) {}
            delay(500)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TOP BAR
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                try {
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                } catch (e: Exception) {}
                mediaPlayer = null
                onBack()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                    tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f)) {
                Text("Now Playing", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f))
                Text(currentFile?.name ?: "", fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1)
            }
        }

        Spacer(Modifier.weight(0.5f))

        // ALBUM ART
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp)
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (albumArt != null) {
                Image(
                    bitmap = albumArt!!,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Filled.MusicNote, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(120.dp))
            }
        }

        Spacer(Modifier.height(28.dp))

        // SONG INFO
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = currentFile?.nameWithoutExtension ?: "",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (artist.isNotEmpty()) artist else "<unknown>",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                maxLines = 1
            )
        }

        Spacer(Modifier.height(24.dp))

        // SEEK BAR
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Slider(
                value = currentPosition.toFloat(),
                onValueChange = {
                    try {
                        mediaPlayer?.seekTo(it.toInt())
                    } catch (e: Exception) {}
                    currentPosition = it.toLong()
                },
                valueRange = 0f..(duration.toFloat().coerceAtLeast(1f))
            )
            Row(Modifier.fillMaxWidth()) {
                Text(formatTime(currentPosition), fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f))
                Spacer(Modifier.weight(1f))
                Text(formatTime(duration), fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f))
            }
        }

        Spacer(Modifier.height(20.dp))

        // CONTROLS
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { isShuffle = !isShuffle }) {
                Icon(Icons.Filled.Shuffle, "Shuffle",
                    tint = if (isShuffle) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
            IconButton(onClick = {
                if (currentIndex > 0) currentIndex--
            }) {
                Icon(Icons.Filled.SkipPrevious, "Previous",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(36.dp))
            }
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable {
                        if (isPlaying) {
                            mediaPlayer?.pause()
                            isPlaying = false
                        } else {
                            mediaPlayer?.start()
                            isPlaying = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }
            IconButton(onClick = {
                if (currentIndex < audioList.size - 1) currentIndex++
            }) {
                Icon(Icons.Filled.SkipNext, "Next",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(36.dp))
            }
            IconButton(onClick = { isRepeat = !isRepeat }) {
                Icon(Icons.Filled.Repeat, "Repeat",
                    tint = if (isRepeat) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
        }

        Spacer(Modifier.weight(1f))
    }
}