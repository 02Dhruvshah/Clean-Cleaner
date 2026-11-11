package com.cleancleaner.app.ui.cachemap

import android.content.ContextWrapper
import androidx.compose.ui.graphics.Color
import com.cleancleaner.app.data.model.AppStorageItem
import com.cleancleaner.app.data.model.DeviceStorageBreakdown
import com.cleancleaner.app.data.model.RunningProcessItem
import com.cleancleaner.app.data.repository.CacheRepository
import com.cleancleaner.app.data.repository.CacheScanResult
import com.cleancleaner.app.data.repository.ProcessInspectorRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CacheMapViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState() {
        val viewModel = CacheMapViewModel()
        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(VisualizationMode.DONUT_RING, state.visualizationMode)
        assertTrue(state.apps.isEmpty())
        assertFalse(state.showRunningProcessesDialog)
    }

    @Test
    fun testSelectApp() {
        val viewModel = CacheMapViewModel()
        val sampleApp = AppStorageItem(
            packageName = "com.example.test",
            appLabel = "Test App",
            cacheBytes = 50000000L,
            cacheFormatted = "50 MB",
            color = Color.Cyan
        )

        viewModel.selectApp(sampleApp)
        assertEquals(sampleApp, viewModel.uiState.value.selectedApp)
    }

    @Test
    fun testToggleVisualizationMode() {
        val viewModel = CacheMapViewModel()
        assertEquals(VisualizationMode.DONUT_RING, viewModel.uiState.value.visualizationMode)

        viewModel.setVisualizationMode(VisualizationMode.TREEMAP)
        assertEquals(VisualizationMode.TREEMAP, viewModel.uiState.value.visualizationMode)

        viewModel.setVisualizationMode(VisualizationMode.DONUT_RING)
        assertEquals(VisualizationMode.DONUT_RING, viewModel.uiState.value.visualizationMode)
    }

    @Test
    fun testLoadCacheStatsSuccess() = runTest {
        val sampleApp = AppStorageItem(
            packageName = "com.example.media",
            appLabel = "Media Player",
            cacheBytes = 120000000L,
            cacheFormatted = "120 MB",
            color = Color.Green
        )

        val fakeRepo = object : CacheRepository {
            override suspend fun getAppStorageStats(): Result<CacheScanResult> {
                return Result.success(
                    CacheScanResult(
                        apps = listOf(sampleApp),
                        breakdown = DeviceStorageBreakdown(
                            totalBytes = 64000000000L,
                            freeBytes = 20000000000L,
                            usedBytes = 44000000000L,
                            totalAppsCacheBytes = 120000000L,
                            totalAppsCodeAndDataBytes = 300000000L,
                            systemAndOtherBytes = 43580000000L
                        )
                    )
                )
            }
        }

        val viewModel = CacheMapViewModel(repositoryFactory = { fakeRepo })

        val fakeContext = object : ContextWrapper(null) {}
        viewModel.loadCacheStats(fakeContext)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(1, state.apps.size)
        assertEquals("com.example.media", state.apps.first().packageName)
        assertEquals(120000000L, state.totalCacheBytes)
        assertEquals(120000000L, state.storageBreakdown.totalAppsCacheBytes)
    }

    @Test
    fun testRunningProcessesInspection() = runTest {
        val sampleProc = RunningProcessItem(
            packageName = "com.example.bg",
            appLabel = "Background Tool",
            pid = 1234,
            processName = "com.example.bg",
            memoryBytes = 45000000L,
            memoryFormatted = "45 MB",
            importanceLabel = "Visible",
            isForeground = false
        )

        val fakeProcRepo = object : ProcessInspectorRepository {
            override suspend fun getRunningProcesses(): Result<List<RunningProcessItem>> {
                return Result.success(listOf(sampleProc))
            }
        }

        val viewModel = CacheMapViewModel(processRepositoryFactory = { fakeProcRepo })
        val fakeContext = object : ContextWrapper(null) {}

        viewModel.openRunningProcesses(fakeContext)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showRunningProcessesDialog)
        assertEquals(1, viewModel.uiState.value.runningProcesses.size)
        assertEquals("com.example.bg", viewModel.uiState.value.runningProcesses.first().packageName)

        viewModel.closeRunningProcesses()
        assertFalse(viewModel.uiState.value.showRunningProcessesDialog)
    }
}
