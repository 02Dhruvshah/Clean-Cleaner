package com.cleancleaner.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cleancleaner.app.R
import com.cleancleaner.app.ui.analyzer.StorageAnalyzerScreen
import com.cleancleaner.app.ui.analyzer.StorageAnalyzerViewModel
import com.cleancleaner.app.ui.cachemap.CacheMapScreen
import com.cleancleaner.app.ui.cachemap.CacheMapViewModel
import com.cleancleaner.app.ui.disktree.DiskTreeScreen
import com.cleancleaner.app.ui.disktree.DiskTreeViewModel
import com.cleancleaner.app.ui.theme.CleanCleanerTheme

enum class NavigationTab {
    CACHE_MAP,
    STORAGE_ANALYZER,
    DISK_TREE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    cacheMapViewModel: CacheMapViewModel = viewModel(),
    storageAnalyzerViewModel: StorageAnalyzerViewModel = viewModel(),
    diskTreeViewModel: DiskTreeViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableStateOf(NavigationTab.CACHE_MAP) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "AD-FREE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 9.sp,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                // Tab 1: Cache Map
                NavigationBarItem(
                    selected = selectedTab == NavigationTab.CACHE_MAP,
                    onClick = { selectedTab = NavigationTab.CACHE_MAP },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == NavigationTab.CACHE_MAP) Icons.Default.DonutLarge else Icons.Outlined.DonutLarge,
                            contentDescription = stringResource(R.string.tab_cache_map)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.tab_cache_map),
                            fontWeight = if (selectedTab == NavigationTab.CACHE_MAP) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                // Tab 2: Storage Analyzer
                NavigationBarItem(
                    selected = selectedTab == NavigationTab.STORAGE_ANALYZER,
                    onClick = { selectedTab = NavigationTab.STORAGE_ANALYZER },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == NavigationTab.STORAGE_ANALYZER) Icons.Default.SdStorage else Icons.Outlined.SdStorage,
                            contentDescription = stringResource(R.string.tab_storage_analyzer)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.tab_storage_analyzer),
                            fontWeight = if (selectedTab == NavigationTab.STORAGE_ANALYZER) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                // Tab 3: Disk Tree (WinDirStat-Style Visual File Tree)
                NavigationBarItem(
                    selected = selectedTab == NavigationTab.DISK_TREE,
                    onClick = { selectedTab = NavigationTab.DISK_TREE },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == NavigationTab.DISK_TREE) Icons.Default.AccountTree else Icons.Outlined.AccountTree,
                            contentDescription = "Disk Tree"
                        )
                    },
                    label = {
                        Text(
                            text = "Disk Tree",
                            fontWeight = if (selectedTab == NavigationTab.DISK_TREE) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                NavigationTab.CACHE_MAP -> {
                    CacheMapScreen(viewModel = cacheMapViewModel)
                }
                NavigationTab.STORAGE_ANALYZER -> {
                    StorageAnalyzerScreen(viewModel = storageAnalyzerViewModel)
                }
                NavigationTab.DISK_TREE -> {
                    DiskTreeScreen(viewModel = diskTreeViewModel)
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainAppScreenPreview() {
    CleanCleanerTheme {
        MainAppScreen()
    }
}
