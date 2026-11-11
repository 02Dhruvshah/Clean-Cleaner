package com.cleancleaner.app.ui.analyzer

import android.content.ContextWrapper
import com.cleancleaner.app.data.model.EmptyDirectoryItem
import com.cleancleaner.app.data.model.FileCategory
import com.cleancleaner.app.data.model.StorageOverview
import com.cleancleaner.app.data.model.StorageVolumeItem
import com.cleancleaner.app.data.model.TopFileItem
import com.cleancleaner.app.data.repository.FileScannerRepository
import com.cleancleaner.app.data.repository.ScanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class StorageAnalyzerViewModelTest {

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
        val viewModel = StorageAnalyzerViewModel()
        val state = viewModel.uiState.value
        assertEquals(false, state.isScanning)
        assertTrue(state.emptyDirectories.isEmpty())
        assertTrue(state.allTopFiles.isEmpty())
        assertEquals(20, state.maxFilesLimit)
    }

    @Test
    fun testVolumeSwitchingAndGranularEmptyFolderDeletion() = runTest {
        val internalVolume = StorageVolumeItem(
            id = "internal",
            name = "Internal Storage",
            rootDir = File("/sdcard"),
            isPrimary = true,
            isRemovable = false,
            totalBytes = 64000000000L,
            freeBytes = 20000000000L
        )

        val sdCardVolume = StorageVolumeItem(
            id = "sdcard_1",
            name = "SanDisk SD Card",
            rootDir = File("/storage/1234-5678"),
            isPrimary = false,
            isRemovable = true,
            totalBytes = 128000000000L,
            freeBytes = 80000000000L
        )

        val dummyEmpty1 = EmptyDirectoryItem(
            file = File("/sdcard/Download/temp1"),
            path = "/sdcard/Download/temp1",
            name = "temp1",
            parentPath = "/sdcard/Download",
            lastModified = 1000L
        )
        val dummyEmpty2 = EmptyDirectoryItem(
            file = File("/sdcard/Download/temp2"),
            path = "/sdcard/Download/temp2",
            name = "temp2",
            parentPath = "/sdcard/Download",
            lastModified = 2000L
        )

        val dummyFiles = (1..60).map { i ->
            TopFileItem(
                file = File("/sdcard/file_$i.mp4"),
                name = "file_$i.mp4",
                path = "/sdcard/file_$i.mp4",
                sizeBytes = (100 - i) * 1000000L,
                formattedSize = "${100 - i} MB",
                extension = if (i % 2 == 0) "mp4" else "apk",
                category = if (i % 2 == 0) FileCategory.MEDIA else FileCategory.APK,
                lastModified = 3000L
            )
        }

        val fakeRepo = object : FileScannerRepository {
            override suspend fun scanStorage(volume: StorageVolumeItem, maxFiles: Int): Result<ScanResult> {
                return Result.success(
                    ScanResult(
                        volume = volume,
                        overview = StorageOverview(
                            totalSpaceBytes = volume.totalBytes,
                            freeSpaceBytes = volume.freeBytes,
                            usedSpaceBytes = volume.usedBytes,
                            usedPercentage = 50f,
                            formattedTotal = "Total",
                            formattedFree = "Free",
                            formattedUsed = "Used"
                        ),
                        emptyDirectories = listOf(dummyEmpty1, dummyEmpty2),
                        topFiles = dummyFiles,
                        totalScannedFiles = 200,
                        totalScannedDirectories = 40
                    )
                )
            }

            override suspend fun deleteEmptyDirectories(directories: List<EmptyDirectoryItem>): Result<Int> {
                return Result.success(directories.size)
            }

            override suspend fun deleteSingleDirectory(dir: File): Result<Boolean> {
                return Result.success(true)
            }

            override suspend fun deleteSingleFile(file: File): Result<Boolean> {
                return Result.success(true)
            }
        }

        val viewModel = StorageAnalyzerViewModel(repositoryFactory = { fakeRepo })
        val fakeContext = object : ContextWrapper(null) {}

        // Test scan on internal storage
        viewModel.startScan(fakeContext, internalVolume)
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.uiState.value
        assertEquals(2, state.emptyDirectories.size)
        assertEquals(60, state.allTopFiles.size)
        // Default limit is 20
        assertEquals(20, state.displayedTopFiles.size)

        // Test Large File Threshold Changes: Top 20 -> Top 50 -> Top 100
        viewModel.setMaxFilesLimit(50)
        assertEquals(50, viewModel.uiState.value.displayedTopFiles.size)

        viewModel.setMaxFilesLimit(100)
        assertEquals(60, viewModel.uiState.value.displayedTopFiles.size) // Total available is 60

        // Test Category Filter
        viewModel.setCategoryFilter(FileCategory.APK)
        val apkCount = viewModel.uiState.value.displayedTopFiles.size
        assertTrue(apkCount > 0)
        assertTrue(viewModel.uiState.value.displayedTopFiles.all { it.category == FileCategory.APK })

        // Test SD Card volume switching
        viewModel.selectVolume(sdCardVolume, fakeContext)
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        assertEquals("sdcard_1", state.selectedVolume?.id)
        assertEquals("SanDisk SD Card", state.selectedVolume?.name)

        // Test Granular Empty Folder Selection & Deletion
        viewModel.toggleSelectEmptyDir(dummyEmpty1.path)
        assertTrue(viewModel.uiState.value.selectedEmptyDirPaths.contains(dummyEmpty1.path))
        assertFalse(viewModel.uiState.value.selectedEmptyDirPaths.contains(dummyEmpty2.path))

        viewModel.deleteSelectedEmptyFolders(fakeContext)
        testDispatcher.scheduler.advanceUntilIdle()

        val afterDeleteState = viewModel.uiState.value
        assertEquals(1, afterDeleteState.emptyDirectories.size)
        assertEquals(dummyEmpty2.path, afterDeleteState.emptyDirectories.first().path)
        assertEquals(0, afterDeleteState.selectedEmptyDirPaths.size)
    }
}
