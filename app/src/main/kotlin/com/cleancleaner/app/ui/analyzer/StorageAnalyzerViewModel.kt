package com.cleancleaner.app.ui.analyzer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cleancleaner.app.data.model.EmptyDirectoryItem
import com.cleancleaner.app.data.model.FileCategory
import com.cleancleaner.app.data.model.StorageOverview
import com.cleancleaner.app.data.model.StorageVolumeItem
import com.cleancleaner.app.data.model.TopFileItem
import com.cleancleaner.app.data.repository.FileScannerRepository
import com.cleancleaner.app.data.repository.FileScannerRepositoryImpl
import com.cleancleaner.app.data.util.PermissionUtils
import com.cleancleaner.app.data.util.StorageVolumeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StorageAnalyzerUiState(
    val isScanning: Boolean = false,
    val hasStoragePermission: Boolean = false,
    val availableVolumes: List<StorageVolumeItem> = emptyList(),
    val selectedVolume: StorageVolumeItem? = null,
    val overview: StorageOverview? = null,
    val emptyDirectories: List<EmptyDirectoryItem> = emptyList(),
    val selectedEmptyDirPaths: Set<String> = emptySet(),
    val allTopFiles: List<TopFileItem> = emptyList(),
    val displayedTopFiles: List<TopFileItem> = emptyList(),
    val maxFilesLimit: Int = 20, // 20, 50, 100
    val selectedCategoryFilter: FileCategory? = null, // null for All
    val isDeletingEmptyDirs: Boolean = false,
    val deletedCountMessage: String? = null,
    val errorMessage: String? = null,
    val totalScannedFiles: Int = 0,
    val totalScannedDirs: Int = 0
)

class StorageAnalyzerViewModel(
    private val repositoryFactory: (Context) -> FileScannerRepository = { context -> FileScannerRepositoryImpl(context) }
) : ViewModel() {

    private val _uiState = MutableStateFlow(StorageAnalyzerUiState())
    val uiState: StateFlow<StorageAnalyzerUiState> = _uiState.asStateFlow()

    fun checkPermissionAndScan(context: Context, forceReload: Boolean = false) {
        val hasPermission = PermissionUtils.hasStoragePermission(context)
        _uiState.update { it.copy(hasStoragePermission = hasPermission) }

        if (!hasPermission) {
            return
        }

        // Detect volumes
        val volumes = StorageVolumeUtils.getStorageVolumes(context)
        val currentSelected = _uiState.value.selectedVolume
        val selected = if (currentSelected != null && volumes.any { it.id == currentSelected.id }) {
            volumes.first { it.id == currentSelected.id }
        } else {
            volumes.firstOrNull()
        }

        _uiState.update {
            it.copy(
                availableVolumes = volumes,
                selectedVolume = selected
            )
        }

        if (_uiState.value.overview != null && !forceReload) {
            return
        }

        selected?.let { startScan(context, it) }
    }

    fun selectVolume(volume: StorageVolumeItem, context: Context) {
        if (_uiState.value.selectedVolume?.id == volume.id) return
        _uiState.update {
            it.copy(
                selectedVolume = volume,
                overview = null,
                emptyDirectories = emptyList(),
                selectedEmptyDirPaths = emptySet(),
                allTopFiles = emptyList(),
                displayedTopFiles = emptyList()
            )
        }
        startScan(context, volume)
    }

    fun startScan(context: Context, volume: StorageVolumeItem? = _uiState.value.selectedVolume) {
        val vol = volume ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, errorMessage = null, deletedCountMessage = null) }

            val repository = repositoryFactory(context)
            val result = repository.scanStorage(vol, maxFiles = 100)

            result.fold(
                onSuccess = { scanResult ->
                    _uiState.update { current ->
                        val filtered = filterFiles(scanResult.topFiles, current.selectedCategoryFilter, current.maxFilesLimit)
                        current.copy(
                            isScanning = false,
                            overview = scanResult.overview,
                            emptyDirectories = scanResult.emptyDirectories,
                            selectedEmptyDirPaths = emptySet(),
                            allTopFiles = scanResult.topFiles,
                            displayedTopFiles = filtered,
                            totalScannedFiles = scanResult.totalScannedFiles,
                            totalScannedDirs = scanResult.totalScannedDirectories,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            errorMessage = error.localizedMessage ?: "Failed to scan storage"
                        )
                    }
                }
            )
        }
    }

    fun setMaxFilesLimit(limit: Int) {
        _uiState.update { current ->
            current.copy(
                maxFilesLimit = limit,
                displayedTopFiles = filterFiles(current.allTopFiles, current.selectedCategoryFilter, limit)
            )
        }
    }

    fun setCategoryFilter(category: FileCategory?) {
        _uiState.update { current ->
            current.copy(
                selectedCategoryFilter = category,
                displayedTopFiles = filterFiles(current.allTopFiles, category, current.maxFilesLimit)
            )
        }
    }

    private fun filterFiles(allFiles: List<TopFileItem>, category: FileCategory?, limit: Int): List<TopFileItem> {
        val byCat = if (category == null) allFiles else allFiles.filter { it.category == category }
        return byCat.take(limit)
    }

    // Granular selection of empty directories
    fun toggleSelectEmptyDir(path: String) {
        _uiState.update { current ->
            val set = current.selectedEmptyDirPaths.toMutableSet()
            if (set.contains(path)) set.remove(path) else set.add(path)
            current.copy(selectedEmptyDirPaths = set)
        }
    }

    fun selectAllEmptyDirs() {
        _uiState.update { current ->
            current.copy(selectedEmptyDirPaths = current.emptyDirectories.map { it.path }.toSet())
        }
    }

    fun clearSelectedEmptyDirs() {
        _uiState.update { current ->
            current.copy(selectedEmptyDirPaths = emptySet())
        }
    }

    fun deleteSelectedEmptyFolders(context: Context) {
        val selectedPaths = _uiState.value.selectedEmptyDirPaths
        if (selectedPaths.isEmpty()) return

        val dirsToDelete = _uiState.value.emptyDirectories.filter { selectedPaths.contains(it.path) }
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingEmptyDirs = true) }
            val repository = repositoryFactory(context)
            val result = repository.deleteEmptyDirectories(dirsToDelete)

            result.fold(
                onSuccess = { count ->
                    _uiState.update { current ->
                        val remaining = current.emptyDirectories.filterNot { selectedPaths.contains(it.path) }
                        current.copy(
                            isDeletingEmptyDirs = false,
                            emptyDirectories = remaining,
                            selectedEmptyDirPaths = emptySet(),
                            deletedCountMessage = "Successfully removed $count empty folders"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isDeletingEmptyDirs = false,
                            errorMessage = "Error deleting folders: ${error.localizedMessage}"
                        )
                    }
                }
            )
        }
    }

    fun deleteSingleEmptyFolder(context: Context, item: EmptyDirectoryItem) {
        viewModelScope.launch {
            val repository = repositoryFactory(context)
            val result = repository.deleteSingleDirectory(item.file)
            result.fold(
                onSuccess = { success ->
                    if (success) {
                        _uiState.update { current ->
                            current.copy(
                                emptyDirectories = current.emptyDirectories.filter { it.path != item.path },
                                selectedEmptyDirPaths = current.selectedEmptyDirPaths - item.path,
                                deletedCountMessage = "Removed ${item.name}"
                            )
                        }
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(errorMessage = "Could not delete folder") }
                }
            )
        }
    }

    fun deleteEmptyFolders(context: Context) {
        val currentDirs = _uiState.value.emptyDirectories
        if (currentDirs.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingEmptyDirs = true) }
            val repository = repositoryFactory(context)
            val result = repository.deleteEmptyDirectories(currentDirs)

            result.fold(
                onSuccess = { count ->
                    _uiState.update {
                        it.copy(
                            isDeletingEmptyDirs = false,
                            emptyDirectories = emptyList(),
                            selectedEmptyDirPaths = emptySet(),
                            deletedCountMessage = "Successfully removed $count empty folders"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isDeletingEmptyDirs = false,
                            errorMessage = "Error deleting empty folders: ${error.localizedMessage}"
                        )
                    }
                }
            )
        }
    }

    fun deleteSingleFile(context: Context, fileItem: TopFileItem) {
        viewModelScope.launch {
            val repository = repositoryFactory(context)
            val result = repository.deleteSingleFile(fileItem.file)
            result.fold(
                onSuccess = { success ->
                    if (success) {
                        _uiState.update { current ->
                            val updatedAll = current.allTopFiles.filter { it.path != fileItem.path }
                            val updatedDisplayed = filterFiles(updatedAll, current.selectedCategoryFilter, current.maxFilesLimit)
                            current.copy(
                                allTopFiles = updatedAll,
                                displayedTopFiles = updatedDisplayed,
                                deletedCountMessage = "Deleted ${fileItem.name}"
                            )
                        }
                    }
                },
                onFailure = {
                    _uiState.update { current ->
                        current.copy(errorMessage = "Could not delete file")
                    }
                }
            )
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(deletedCountMessage = null, errorMessage = null) }
    }
}
