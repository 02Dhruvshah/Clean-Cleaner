package com.cleancleaner.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleancleaner.app.data.model.AppStorageItem

@Composable
fun InteractiveTreemapChart(
    items: List<AppStorageItem>,
    selectedItem: AppStorageItem?,
    onItemSelected: (AppStorageItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No cache data to display",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    // Top consumers (up to 8 for maximum visual clarity on mobile screens)
    val topItems = remember(items) { items.take(8) }
    val totalWeight = remember(topItems) { topItems.sumOf { it.cacheBytes }.coerceAtLeast(1L) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F131C))
            .padding(4.dp)
    ) {
        // High-density 2-row or 2-column proportional layout
        // First 2 items get the top half or left half, rest share the other section
        val firstGroup = topItems.take(2)
        val secondGroup = topItems.drop(2)

        val firstGroupWeight = firstGroup.sumOf { it.cacheBytes }
        val secondGroupWeight = secondGroup.sumOf { it.cacheBytes }
        val firstRatio = (firstGroupWeight.toFloat() / totalWeight.toFloat()).coerceIn(0.35f, 0.65f)

        Column(modifier = Modifier.fillMaxSize()) {
            // First Row / Main consumers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(firstRatio)
            ) {
                firstGroup.forEach { item ->
                    val weight = (item.cacheBytes.toFloat() / firstGroupWeight.coerceAtLeast(1L).toFloat()).coerceAtLeast(0.1f)
                    TreemapBlock(
                        item = item,
                        isSelected = item.packageName == selectedItem?.packageName,
                        onClick = { onItemSelected(item) },
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(weight)
                            .padding(2.dp)
                    )
                }
            }

            // Second Row / Secondary consumers
            if (secondGroup.isNotEmpty()) {
                val subGroup1 = secondGroup.take(3)
                val subGroup2 = secondGroup.drop(3)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f - firstRatio)
                ) {
                    if (subGroup1.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f)
                        ) {
                            subGroup1.forEach { item ->
                                TreemapBlock(
                                    item = item,
                                    isSelected = item.packageName == selectedItem?.packageName,
                                    onClick = { onItemSelected(item) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .padding(2.dp)
                                )
                            }
                        }
                    }

                    if (subGroup2.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f)
                        ) {
                            subGroup2.forEach { item ->
                                TreemapBlock(
                                    item = item,
                                    isSelected = item.packageName == selectedItem?.packageName,
                                    onClick = { onItemSelected(item) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .padding(2.dp)
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
private fun TreemapBlock(
    item: AppStorageItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val blockShape = RoundedCornerShape(10.dp)
    val bgColor = item.color.copy(alpha = if (isSelected) 0.35f else 0.18f)
    val borderColor = if (isSelected) Color.White else item.color.copy(alpha = 0.5f)
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Box(
        modifier = modifier
            .clip(blockShape)
            .background(bgColor)
            .border(borderWidth, borderColor, blockShape)
            .clickable(onClick = onClick)
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIconImage(
                    drawable = item.icon,
                    contentDescription = item.appLabel,
                    size = 20.dp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = item.appLabel,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.cacheFormatted,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = item.color,
                    fontSize = 11.sp
                )
            )
        }
    }
}

