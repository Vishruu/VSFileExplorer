package com.vishruu.vsfileexplorer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp)
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerProgress"
    )

    val baseColor = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)

    val brush = Brush.linearGradient(
        colors = listOf(
            baseColor,
            highlight,
            baseColor
        ),
        start = Offset(progress * 600f - 300f, 0f),
        end = Offset(progress * 600f, 0f)
    )

    Box(modifier = modifier.clip(shape).background(brush))
}

@Composable
fun GridSkeleton() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(12) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ShimmerBox(modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(6.dp))
                ShimmerBox(
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                    shape = RoundedCornerShape(4.dp)
                )
                Spacer(Modifier.height(4.dp))
                ShimmerBox(
                    modifier = Modifier.width(30.dp).height(8.dp),
                    shape = RoundedCornerShape(4.dp)
                )
            }
        }
    }
}

@Composable
fun ListSkeleton() {
    Column(Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        repeat(8) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBox(modifier = Modifier.size(44.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    ShimmerBox(
                        modifier = Modifier.fillMaxWidth(0.6f).height(12.dp),
                        shape = RoundedCornerShape(4.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    ShimmerBox(
                        modifier = Modifier.width(80.dp).height(10.dp),
                        shape = RoundedCornerShape(4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FadeIn(content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(220)),
        exit = fadeOut(animationSpec = tween(120))
    ) {
        content()
    }
}