package com.vishruu.vsfileexplorer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val pureBlack = Color(0xFF000000)
    val darkGrey = Color(0xFF0A0A0A)
    val mediumGrey = Color(0xFF1A1A1A)
    val lightGrey = Color(0xFF9E9E9E)
    val purple = Color(0xFF9C27B0)
    val softWhite = Color(0xFFF5F5F7)
    val pureWhite = Color(0xFFFFFFFF)

    val iconScale = remember { Animatable(0.5f) }
    val iconAlpha = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val versionAlpha = remember { Animatable(0f) }
    val glowAlpha = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    LaunchedEffect(Unit) {
        iconAlpha.animateTo(1f, tween(600))
        iconScale.animateTo(1f, tween(600))
        glowAlpha.animateTo(1f, tween(400))
        titleAlpha.animateTo(1f, tween(500))
        delay(150)
        subtitleAlpha.animateTo(1f, tween(500))
        delay(150)
        versionAlpha.animateTo(1f, tween(500))
        delay(700)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(pureBlack, darkGrey, mediumGrey, darkGrey, pureBlack)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(iconScale.value)
                    .alpha(iconAlpha.value),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(glowPulse)
                        .alpha(glowAlpha.value * 0.15f)
                        .clip(CircleShape)
                        .background(softWhite)
                )
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(1f + (glowPulse - 0.3f) * 0.3f)
                        .alpha(glowAlpha.value * 0.3f)
                        .clip(CircleShape)
                        .background(purple)
                )
                Icon(
                    imageVector = Icons.Filled.Storage,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(80.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "VS File Explorer",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = softWhite,
                modifier = Modifier
                    .alpha(titleAlpha.value)
                    .scale(0.9f + titleAlpha.value * 0.1f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "by Vishruu",
                fontSize = 14.sp,
                color = lightGrey,
                modifier = Modifier.alpha(subtitleAlpha.value)
            )

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "v1.0",
                fontSize = 11.sp,
                color = lightGrey.copy(alpha = 0.5f),
                modifier = Modifier.alpha(versionAlpha.value)
            )
        }
    }
}