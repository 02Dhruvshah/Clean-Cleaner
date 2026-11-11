package com.cleancleaner.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleancleaner.app.data.model.AppStorageItem
import com.cleancleaner.app.data.util.FormatUtils
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

private data class DonutSlice(
    val app: AppStorageItem,
    val startAngle: Float,
    val sweepAngle: Float
)

@Composable
fun InteractiveDonutChart(
    items: List<AppStorageItem>,
    selectedItem: AppStorageItem?,
    totalCacheFormatted: String,
    onItemSelected: (AppStorageItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalBytes = remember(items) { items.sumOf { it.cacheBytes } }
    val animationProgress = remember { Animatable(0f) }
    val density = LocalDensity.current

    LaunchedEffect(items) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }

    // Group slices occupying less than 2% of total cache into "Other Apps"
    val processedItems = remember(items, totalBytes) {
        if (totalBytes <= 0L || items.isEmpty()) {
            emptyList()
        } else {
            val mainList = mutableListOf<AppStorageItem>()
            var otherBytes = 0L
            var otherCount = 0

            for (app in items) {
                val ratio = app.cacheBytes.toDouble() / totalBytes.toDouble()
                if (ratio >= 0.02) {
                    mainList.add(app)
                } else {
                    otherBytes += app.cacheBytes
                    otherCount++
                }
            }

            if (otherBytes > 0L) {
                val otherPercentage = ((otherBytes.toDouble() / totalBytes.toDouble()) * 100.0).toFloat()
                mainList.add(
                    AppStorageItem(
                        packageName = "group.other.apps",
                        appLabel = if (otherCount > 1) "Other Apps ($otherCount)" else "Other App",
                        cacheBytes = otherBytes,
                        cacheFormatted = FormatUtils.formatBytes(null, otherBytes),
                        cachePercentage = otherPercentage,
                        color = Color(0xFF64748B) // Sleek slate grey for grouped others
                    )
                )
            }
            mainList
        }
    }

    // Calculate angular slices with flat ends and subtle divider gap (2dp gap)
    val slices = remember(processedItems, totalBytes) {
        if (totalBytes <= 0L || processedItems.isEmpty()) {
            emptyList()
        } else {
            var currentAngle = -90f // Start from 12 o'clock
            val sliceCount = processedItems.size
            val gapDegrees = if (sliceCount > 1) 1.5f else 0f
            val totalGaps = gapDegrees * sliceCount
            val availableDegrees = (360f - totalGaps).coerceAtLeast(10f)

            processedItems.map { item ->
                val sweep = (item.cacheBytes.toFloat() / totalBytes.toFloat()) * availableDegrees
                val sweepClamped = sweep.coerceAtLeast(1.0f)
                val slice = DonutSlice(
                    app = item,
                    startAngle = currentAngle,
                    sweepAngle = sweepClamped
                )
                currentAngle += sweepClamped + gapDegrees
                slice
            }
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(slices) {
                    detectTapGestures { offset ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val dx = offset.x - centerX
                        val dy = offset.y - centerY
                        val dist = sqrt(dx * dx + dy * dy)

                        val minDim = minOf(size.width, size.height)
                        val strokeW = minDim * 0.14f
                        val outerR = (minDim / 2f) - (strokeW / 2f)
                        val innerR = outerR - strokeW

                        if (dist in (innerR * 0.5f)..(outerR * 1.35f)) {
                            var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            var normAngle = angle + 90f
                            if (normAngle < 0f) normAngle += 360f

                            for (slice in slices) {
                                var sStart = slice.startAngle + 90f
                                if (sStart < 0f) sStart += 360f
                                val sEnd = sStart + slice.sweepAngle

                                if (sEnd > 360f) {
                                    if (normAngle >= sStart || normAngle <= (sEnd - 360f)) {
                                        onItemSelected(slice.app)
                                        break
                                    }
                                } else {
                                    if (normAngle in sStart..sEnd) {
                                        onItemSelected(slice.app)
                                        break
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            val canvasW = size.width
            val canvasH = size.height
            val minDim = minOf(canvasW, canvasH)
            val baseStrokeWidth = minDim * 0.14f
            val baseRadius = (minDim - baseStrokeWidth - 20.dp.toPx()) / 2f
            val center = Offset(canvasW / 2f, canvasH / 2f)

            if (slices.isEmpty()) {
                drawCircle(
                    color = Color(0xFF161B26),
                    radius = baseRadius,
                    center = center,
                    style = Stroke(width = baseStrokeWidth, cap = StrokeCap.Butt)
                )
                return@Canvas
            }

            // Dark subtle background track
            drawCircle(
                color = Color(0xFF10141D),
                radius = baseRadius,
                center = center,
                style = Stroke(width = baseStrokeWidth, cap = StrokeCap.Butt)
            )

            val progress = animationProgress.value

            for (slice in slices) {
                val isSelected = slice.app.packageName == selectedItem?.packageName
                val strokeW = if (isSelected) baseStrokeWidth * 1.3f else baseStrokeWidth
                val radiusOffset = if (isSelected) 5.dp.toPx() else 0f
                val arcRadius = baseRadius + radiusOffset

                val topLeft = Offset(center.x - arcRadius, center.y - arcRadius)
                val arcSize = Size(arcRadius * 2f, arcRadius * 2f)
                val sweep = slice.sweepAngle * progress

                // Draw crisp, contiguous arc slice with flat edges (StrokeCap.Butt)
                drawArc(
                    color = slice.app.color,
                    startAngle = slice.startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                )

                // Clean highlight border for selected slice
                if (isSelected) {
                    drawArc(
                        color = Color.White.copy(alpha = 0.6f),
                        startAngle = slice.startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(topLeft.x - 3.dp.toPx(), topLeft.y - 3.dp.toPx()),
                        size = Size(arcSize.width + 6.dp.toPx(), arcSize.height + 6.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Butt)
                    )
                }
            }
        }

        // Center Content inside Donut
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 28.dp)
        ) {
            if (selectedItem != null) {
                Text(
                    text = selectedItem.appLabel,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = selectedItem.cacheFormatted,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = selectedItem.color
                    ),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${String.format("%.1f", selectedItem.cachePercentage)}% of total cache",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Total Cache: $totalCacheFormatted",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap a segment to inspect",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview
@Composable
fun InteractiveDonutChartPreview() {
    val sampleApps = listOf(
        AppStorageItem(
            packageName = "com.spotify.music",
            appLabel = "Spotify",
            cacheBytes = 2500000000L,
            cacheFormatted = "2.33 GB",
            color = Color(0xFF00E676),
            cachePercentage = 50f
        ),
        AppStorageItem(
            packageName = "com.instagram.android",
            appLabel = "Instagram",
            cacheBytes = 1800000000L,
            cacheFormatted = "1.68 GB",
            color = Color(0xFF00E5FF),
            cachePercentage = 36f
        ),
        AppStorageItem(
            packageName = "com.google.android.youtube",
            appLabel = "YouTube",
            cacheBytes = 50000000L,
            cacheFormatted = "47 MB",
            color = Color(0xFFA855F7),
            cachePercentage = 1f
        )
    )

    com.cleancleaner.app.ui.theme.CleanCleanerTheme {
        InteractiveDonutChart(
            items = sampleApps,
            selectedItem = null,
            totalCacheFormatted = "4.05 GB",
            onItemSelected = {},
            modifier = Modifier.size(280.dp)
        )
    }
}
