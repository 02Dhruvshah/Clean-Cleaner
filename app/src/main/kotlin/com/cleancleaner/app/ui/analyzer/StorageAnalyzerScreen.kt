package com.cleancleaner.app.ui.analyzer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderDelete
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cleancleaner.app.R
import com.cleancleaner.app.data.model.EmptyDirectoryItem
import com.cleancleaner.app.data.model.FileCategory
import com.cleancleaner.app.data.model.StorageOverview
import com.cleancleaner.app.data.model.TopFileItem
import com.cleancleaner.app.data.util.PermissionUtils
import com.cleancleaner.app.ui.components.PermissionGateCard
import com.cleancleaner.app.ui.theme.NeonCyan
import com.cleancleaner.app.ui.theme.NeonEmerald
import kotlinx.coroutines.launch

@Composable
fun StorageAnalyzerScreen(
    viewModel: StorageAnalyzerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var showDeleteAllConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteSelectedConfirmDialog by remember { mutableStateOf(false) }
    var fileToDelete by remember { mutableStateOf<TopFileItem?>(null) }
    var singleDirToDelete by remember { mutableStateOf<EmptyDirectoryItem?>(null) }
    var showEmptyFoldersExpanded by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermissionAndScan(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(uiState.deletedCountMessage) {
        uiState.deletedCountMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    if (!uiState.hasStoragePermission) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            PermissionGateCard(
                title = stringResource(R.string.permission_storage_title),
                description = stringResource(R.string.permission_storage_desc),
                buttonText = stringResource(R.string.permission_storage_button),
                icon = Icons.Default.FolderShared,
                onGrantClick = {
                    val intent = PermissionUtils.getAllFilesAccessSettingsIntent(context)
                    context.startActivity(intent)
                }
            )
        }
        return
    }

    if (uiState.isScanning && uiState.overview == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Scanning ${uiState.selectedVolume?.name ?: "storage"}...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Toolbar Header with Storage Volume Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STORAGE VOLUMES",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    IconButton(
                        onClick = { viewModel.startScan(context) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.refresh),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Storage Volume Selector Pills: [ Internal Storage ] | [ SD Card ]
                val volumes = uiState.availableVolumes
                if (volumes.size > 1) {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) {
                        volumes.forEachIndexed { index, vol ->
                            SegmentedButton(
                                selected = vol.id == uiState.selectedVolume?.id,
                                onClick = { viewModel.selectVolume(vol, context) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = volumes.size),
                                icon = {
                                    Icon(
                                        imageVector = if (vol.isRemovable) Icons.Default.SdCard else Icons.Default.SdStorage,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            ) {
                                Text(
                                    text = vol.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                } else if (volumes.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SdStorage,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${volumes.first().name} (${volumes.first().formattedTotal})",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                        Text(
                            text = "No SD Card Detected",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Disk Storage Capacity Card
            item {
                uiState.overview?.let { overview ->
                    StorageOverviewCard(
                        volumeName = uiState.selectedVolume?.name ?: "Storage",
                        overview = overview
                    )
                }
            }

            // Dead / Empty Folder Cleaner Section with Granular Actions (NO TRUNCATION!)
            item {
                EmptyFoldersSection(
                    emptyDirs = uiState.emptyDirectories,
                    selectedPaths = uiState.selectedEmptyDirPaths,
                    isDeleting = uiState.isDeletingEmptyDirs,
                    isExpanded = showEmptyFoldersExpanded,
                    onToggleExpand = { showEmptyFoldersExpanded = !showEmptyFoldersExpanded },
                    onToggleSelectDir = { viewModel.toggleSelectEmptyDir(it) },
                    onSelectAll = { viewModel.selectAllEmptyDirs() },
                    onClearSelection = { viewModel.clearSelectedEmptyDirs() },
                    onDeleteSelected = { showDeleteSelectedConfirmDialog = true },
                    onDeleteAll = { showDeleteAllConfirmDialog = true },
                    onDeleteSingle = { singleDirToDelete = it }
                )
            }

            // Configurable Top Files Header + Limit Pills + Category Filter
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "LARGEST FILES",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.1.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Configurable Limit Pills: [ Top 20 | Top 50 | Top 100 ]
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.height(32.dp)
                        ) {
                            listOf(20, 50, 100).forEachIndexed { idx, count ->
                                SegmentedButton(
                                    selected = uiState.maxFilesLimit == count,
                                    onClick = { viewModel.setMaxFilesLimit(count) },
                                    shape = SegmentedButtonDefaults.itemShape(index = idx, count = 3)
                                ) {
                                    Text("Top $count", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Category Filter Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = uiState.selectedCategoryFilter == null,
                                onClick = { viewModel.setCategoryFilter(null) },
                                label = { Text("All", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = NeonCyan
                                )
                            )
                        }
                        items(listOf(
                            FileCategory.MEDIA,
                            FileCategory.APK,
                            FileCategory.ARCHIVE,
                            FileCategory.AUDIO,
                            FileCategory.DOCUMENTS,
                            FileCategory.IMAGES
                        )) { cat ->
                            FilterChip(
                                selected = uiState.selectedCategoryFilter == cat,
                                onClick = { viewModel.setCategoryFilter(cat) },
                                label = { Text(cat.displayName, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = NeonCyan
                                )
                            )
                        }
                    }
                }
            }

            // Configurable Files List with Full Paths and Copy/View actions
            itemsIndexed(uiState.displayedTopFiles, key = { _, item -> item.path }) { index, fileItem ->
                EnhancedTopFileCard(
                    rank = index + 1,
                    item = fileItem,
                    onCopyPath = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("File Path", fileItem.path))
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Path copied to clipboard")
                        }
                    },
                    onViewFile = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                val uri = Uri.fromFile(fileItem.file)
                                setDataAndType(uri, "*/*")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("No compatible app found to open file")
                            }
                        }
                    },
                    onDeleteClick = { fileToDelete = fileItem }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }

    // Confirmation Dialog for Batch Deleting ALL Empty Folders
    if (showDeleteAllConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirmDialog = false },
            title = {
                Text("Delete All Empty Folders?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Permanently delete all ${uiState.emptyDirectories.size} empty directories on ${uiState.selectedVolume?.name}?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAllConfirmDialog = false
                        viewModel.deleteEmptyFolders(context)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation Dialog for Deleting SELECTED Empty Folders
    if (showDeleteSelectedConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSelectedConfirmDialog = false },
            title = {
                Text("Delete Selected Folders?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Permanently delete ${uiState.selectedEmptyDirPaths.size} selected empty directories?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteSelectedConfirmDialog = false
                        viewModel.deleteSelectedEmptyFolders(context)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete Selected")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectedConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation Dialog for Single Directory Delete
    singleDirToDelete?.let { dir ->
        AlertDialog(
            onDismissRequest = { singleDirToDelete = null },
            title = {
                Text("Delete Empty Folder?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Delete empty folder \"${dir.name}\"?\n\n${dir.path}")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSingleEmptyFolder(context, dir)
                        singleDirToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { singleDirToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation Dialog for deleting a single large file
    fileToDelete?.let { fileItem ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = {
                Text("Delete File?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Permanently delete \"${fileItem.name}\" (${fileItem.formattedSize})?\n\n${fileItem.path}")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSingleFile(context, fileItem)
                        fileToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StorageOverviewCard(
    volumeName: String,
    overview: StorageOverview,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.SdStorage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = volumeName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "${overview.formattedUsed} used of ${overview.formattedTotal}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "${overview.usedPercentage.toInt()}%",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { (overview.usedPercentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Free: ${overview.formattedFree}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Used: ${overview.formattedUsed}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyFoldersSection(
    emptyDirs: List<EmptyDirectoryItem>,
    selectedPaths: Set<String>,
    isDeleting: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggleSelectDir: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onDeleteAll: () -> Unit,
    onDeleteSingle: (EmptyDirectoryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (emptyDirs.isNotEmpty()) MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
            )
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Main Summary Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (emptyDirs.isNotEmpty()) MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (emptyDirs.isNotEmpty()) Icons.Default.FolderDelete else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (emptyDirs.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (emptyDirs.isNotEmpty()) "${emptyDirs.size} Empty Folders" else "Storage Clean",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = if (emptyDirs.isNotEmpty()) "Dead directories left behind by apps" else "No orphaned empty directories found",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (emptyDirs.isNotEmpty()) {
                    Button(
                        onClick = onDeleteAll,
                        enabled = !isDeleting,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = Color.White
                        )
                    ) {
                        if (isDeleting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (emptyDirs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleExpand() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isExpanded) "Hide review list ▲" else "Review all ${emptyDirs.size} empty folders ▼",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        // Multi-selection Action Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val allSelected = selectedPaths.size == emptyDirs.size && emptyDirs.isNotEmpty()
                                Checkbox(
                                    checked = allSelected,
                                    onCheckedChange = { if (allSelected) onClearSelection() else onSelectAll() },
                                    colors = CheckboxDefaults.colors(checkedColor = NeonCyan)
                                )
                                Text(
                                    text = if (selectedPaths.isEmpty()) "Select All" else "${selectedPaths.size} Selected",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (selectedPaths.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = onDeleteSelected,
                                    enabled = !isDeleting,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(
                                        text = "Delete Selected (${selectedPaths.size})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Scrollable Container showing ALL empty folders (NO TRUNCATION!)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .padding(8.dp)
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(emptyDirs, key = { it.path }) { dir ->
                                    val isChecked = selectedPaths.contains(dir.path)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isChecked) NeonCyan.copy(alpha = 0.12f)
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .padding(horizontal = 6.dp, vertical = 6.dp)
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { onToggleSelectDir(dir.path) },
                                            colors = CheckboxDefaults.colors(checkedColor = NeonCyan)
                                        )

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = dir.name,
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = dir.path,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 9.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        IconButton(
                                            onClick = { onDeleteSingle(dir) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete this folder",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedTopFileCard(
    rank: Int,
    item: TopFileItem,
    onCopyPath: () -> Unit,
    onViewFile: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (categoryIcon, categoryColor) = when (item.category) {
        FileCategory.MEDIA -> Icons.Default.Movie to Color(0xFFF43F5E)
        FileCategory.APK -> Icons.Default.Android to Color(0xFFF59E0B)
        FileCategory.AUDIO -> Icons.Default.Audiotrack to Color(0xFF00E676)
        FileCategory.IMAGES -> Icons.Default.Image to Color(0xFF00E5FF)
        FileCategory.DOCUMENTS -> Icons.Default.Description to Color(0xFF38BDF8)
        FileCategory.ARCHIVE -> Icons.Default.Archive to Color(0xFFA855F7)
        FileCategory.DOWNLOADS -> Icons.Default.Download to Color(0xFF00E5FF)
        FileCategory.OTHER -> Icons.AutoMirrored.Filled.InsertDriveFile to Color(0xFF94A3B8)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Rank badge
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#$rank",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // File type icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(categoryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = null,
                        tint = categoryColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Name + Size Badge
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${item.category.displayName} • ${item.extension.uppercase()}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = categoryColor
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = item.formattedSize,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Delete button
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete File",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Full file path with Copy & Open actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.path,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onCopyPath,
                    modifier = Modifier.size(22.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Path",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onViewFile,
                    modifier = Modifier.size(22.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open File",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}
