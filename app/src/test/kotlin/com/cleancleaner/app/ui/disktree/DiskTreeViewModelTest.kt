package com.cleancleaner.app.ui.disktree

import android.content.ContextWrapper
import com.cleancleaner.app.data.model.DirectoryNode
import com.cleancleaner.app.data.model.FileCategory
import com.cleancleaner.app.data.model.StorageVolumeItem
import com.cleancleaner.app.data.repository.DiskTreeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class DiskTreeViewModelTest {

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
        val viewModel = DiskTreeViewModel()
        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertTrue(state.breadcrumbs.isEmpty())
    }

    @Test
    fun testNavigateToDirectoryAndUp() = runTest {
        val rootDir = File("/storage/emulated/0")
        val moviesDir = File("/storage/emulated/0/Movies")

        val rootNode = DirectoryNode(
            file = rootDir,
            name = "0",
            path = rootDir.absolutePath,
            sizeBytes = 5000000000L,
            formattedSize = "5 GB",
            fileCount = 100,
            subfolderCount = 10,
            dominantCategory = FileCategory.MEDIA,
            children = listOf(
                DirectoryNode(
                    file = moviesDir,
                    name = "Movies",
                    path = moviesDir.absolutePath,
                    sizeBytes = 2000000000L,
                    formattedSize = "2 GB",
                    fileCount = 5,
                    subfolderCount = 1,
                    dominantCategory = FileCategory.MEDIA
                )
            )
        )

        val fakeRepo = object : DiskTreeRepository {
            override suspend fun loadDirectoryNode(dir: File): Result<DirectoryNode> {
                return Result.success(rootNode)
            }
        }

        val viewModel = DiskTreeViewModel(repositoryFactory = { fakeRepo })
        val fakeContext = object : ContextWrapper(null) {}

        viewModel.navigateToDirectory(rootDir, fakeContext)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertNotNull(state.currentNode)
        assertEquals(rootDir.absolutePath, state.currentDirectory?.absolutePath)
        assertEquals(1, state.breadcrumbs.size)
        assertEquals(1, state.currentNode?.children?.size)
    }
}

