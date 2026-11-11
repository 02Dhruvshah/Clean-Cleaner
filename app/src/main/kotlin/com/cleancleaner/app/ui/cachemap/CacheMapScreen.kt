package com.cleancleaner.app.ui.cachemap

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cleancleaner.app.R
import com.cleancleaner.app.data.util.PermissionUtils
import com.cleancleaner.app.ui.components.AppInspectionCard
import com.cleancleaner.app.ui.components.GlobalStorageBar
import com.cleancleaner.app.ui.components.InteractiveDonutChart
import com.cleancleaner.app.ui.components.InteractiveTreemapChart
import com.cleancleaner.app.ui.components.PermissionGateCard
import com.cleancleaner.app.ui.components.RunningProcessesDialog
import com.cleancleaner.app.ui.theme.NeonCyan

@Composable
fun CacheMapScreen(
    viewModel: CacheMapViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermissionAndLoad(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (!uiState.hasUsagePermission) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            PermissionGateCard(
                title = stringResource(R.string.permission_usage_title),
                description = stringResource(R.string.permission_usage_desc),
                buttonText = stringResource(R.string.permission_usage_button),
                icon = Icons.Default.QueryStats,
                onGrantClick = {
                    val intent = PermissionUtils.getUsageAccessSettingsIntent()
                    context.startActivity(intent)
                }
            )
        }
        return
    }

    if (uiState.isLoading && uiState.apps.isEmpty()) {
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
                    text = "Analyzing app cache footprints...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // GLOBAL STORAGE VS APPS CACHE BAR
        GlobalStorageBar(breakdown = uiState.storageBreakdown)

        Spacer(modifier = Modifier.height(12.dp))

        // Visual Controls Toolbar: Chart mode selector + Running Processes + Refresh
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.height(36.dp)
            ) {
                SegmentedButton(
                    selected = uiState.visualizationMode == VisualizationMode.DONUT_RING,
                    onClick = { viewModel.setVisualizationMode(VisualizationMode.DONUT_RING) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.DonutLarge,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                ) {
                    Text("Ring", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                SegmentedButton(
                    selected = uiState.visualizationMode == VisualizationMode.TREEMAP,
                    onClick = { viewModel.setVisualizationMode(VisualizationMode.TREEMAP) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                ) {
                    Text("Treemap", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { viewModel.openRunningProcesses(context) },
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RAM", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { viewModel.loadCacheStats(context) },
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
        }

        Spacer(modifier = Modifier.height(12.dp))

        // TOP HALF: Compact Visual Chart (crisp, proportional arcs, no overlaps)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(290.dp),
            contentAlignment = Alignment.Center
        ) {
            when (uiState.visualizationMode) {
                VisualizationMode.DONUT_RING -> {
                    InteractiveDonutChart(
                        items = uiState.apps,
                        selectedItem = uiState.selectedApp,
                        totalCacheFormatted = uiState.totalCacheFormatted,
                        onItemSelected = { viewModel.selectApp(it) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                VisualizationMode.TREEMAP -> {
                    InteractiveTreemapChart(
                        items = uiState.apps.take(8),
                        selectedItem = uiState.selectedApp,
                        onItemSelected = { viewModel.selectApp(it) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // BOTTOM HALF: Interactive Inspection Card & Quick Selector
        AppInspectionCard(
            selectedApp = uiState.selectedApp,
            allApps = uiState.apps,
            onSelectApp = { viewModel.selectApp(it) }
        )

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Modal: Running Processes Inspector
    if (uiState.showRunningProcessesDialog) {
        RunningProcessesDialog(
            processes = uiState.runningProcesses,
            isLoading = uiState.isLoadingProcesses,
            onDismiss = { viewModel.closeRunningProcesses() },
            onRefresh = { viewModel.loadRunningProcesses(context) }
        )
    }
}
