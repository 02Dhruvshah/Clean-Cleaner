package com.cleancleaner.app.ui.disktree

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
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
import com.cleancleaner.app.data.model.DirectoryNode
import com.cleancleaner.app.data.model.FileCategory

@Composable
fun WinDirStatTreemap(
    nodes: List<DirectoryNode>,
    selectedNode: DirectoryNode?,
    onSelectNode: (DirectoryNode) -> Unit,
    onZoomNode: (DirectoryNode) -> Unit,
    modifier: Modifier = Modifier
) {
    if (nodes.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0F131C)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Empty Directory",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val topNodes = remember(nodes) { nodes.take(10) }
    val totalWeight = remember(topNodes) { topNodes.sumOf { it.sizeBytes }.coerceAtLeast(1L) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0C0F16))
            .padding(4.dp)
    ) {
        val firstHalf = topNodes.take(2)
        val secondHalf = topNodes.drop(2)

        val firstWeight = firstHalf.sumOf { it.sizeBytes }
        val secondWeight = secondHalf.sumOf { it.sizeBytes }
        val firstRatio = if (secondHalf.isEmpty()) 1f else (firstWeight.toFloat() / totalWeight.toFloat()).coerceIn(0.35f, 0.65f)

        Column(modifier = Modifier.fillMaxSize()) {
            // First Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(firstRatio)
            ) {
                firstHalf.forEach { node ->
                    val weight = (node.sizeBytes.toFloat() / firstWeight.coerceAtLeast(1L).toFloat()).coerceAtLeast(0.1f)
                    TreemapDirBlock(
                        node = node,
                        isSelected = node.path == selectedNode?.path,
                        onClick = {
                            if (node.path == selectedNode?.path && node.file.isDirectory) {
                                onZoomNode(node)
                            } else {
                                onSelectNode(node)
                            }
                        },
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(weight)
                            .padding(2.dp)
                    )
                }
            }

            // Second Row / Sub-grid
            if (secondHalf.isNotEmpty()) {
                val group1 = secondHalf.take(4)
                val group2 = secondHalf.drop(4)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f - firstRatio)
                ) {
                    if (group1.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f)
                        ) {
                            group1.forEach { node ->
                                TreemapDirBlock(
                                    node = node,
                                    isSelected = node.path == selectedNode?.path,
                                    onClick = {
                                        if (node.path == selectedNode?.path && node.file.isDirectory) {
                                            onZoomNode(node)
                                        } else {
                                            onSelectNode(node)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .padding(2.dp)
                                )
                            }
                        }
                    }

                    if (group2.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f)
                        ) {
                            group2.forEach { node ->
                                TreemapDirBlock(
                                    node = node,
                                    isSelected = node.path == selectedNode?.path,
                                    onClick = {
                                        if (node.path == selectedNode?.path && node.file.isDirectory) {
                                            onZoomNode(node)
                                        } else {
                                            onSelectNode(node)
                                        }
                                    },
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
private fun TreemapDirBlock(
    node: DirectoryNode,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val blockColor = getCategoryColor(node.dominantCategory)
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(blockColor.copy(alpha = if (isSelected) 0.38f else 0.18f))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color.White else blockColor.copy(alpha = 0.5f),
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (node.file.isDirectory) Icons.Default.Folder else getCategoryIcon(node.dominantCategory),
                    contentDescription = null,
                    tint = blockColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = node.name,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = node.formattedSize,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = blockColor,
                    fontSize = 10.sp
                )
            )
        }
    }
}

fun getCategoryColor(category: FileCategory): Color {
    return when (category) {
        FileCategory.MEDIA -> Color(0xFFF43F5E) // Red/Rose
        FileCategory.APK -> Color(0xFFF59E0B) // Amber
        FileCategory.AUDIO -> Color(0xFF00E676) // Emerald
        FileCategory.IMAGES -> Color(0xFF00E5FF) // Cyan
        FileCategory.DOCUMENTS -> Color(0xFF38BDF8) // Sky
        FileCategory.ARCHIVE -> Color(0xFFA855F7) // Purple
        FileCategory.DOWNLOADS -> Color(0xFF00E5FF)
        FileCategory.OTHER -> Color(0xFF64748B) // Slate
    }
}

fun getCategoryIcon(category: FileCategory): androidx.compose.ui.graphics.vector.ImageVector {
    return when (category) {
        FileCategory.MEDIA -> Icons.Default.Movie
        FileCategory.APK -> Icons.Default.Android
        FileCategory.AUDIO -> Icons.Default.Audiotrack
        FileCategory.IMAGES -> Icons.Default.Image
        FileCategory.DOCUMENTS -> Icons.Default.Description
        FileCategory.ARCHIVE -> Icons.Default.Archive
        FileCategory.DOWNLOADS -> Icons.Default.Folder
        FileCategory.OTHER -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
}

