package com.cleancleaner.app.ui.disktree

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cleancleaner.app.data.model.DirectoryNode
import com.cleancleaner.app.data.model.StorageVolumeItem
import com.cleancleaner.app.data.repository.DiskTreeRepository
import com.cleancleaner.app.data.repository.DiskTreeRepositoryImpl
import com.cleancleaner.app.data.util.PermissionUtils
import com.cleancleaner.app.data.util.StorageVolumeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class DiskTreeUiState(
    val isLoading: Boolean = false,
    val hasStoragePermission: Boolean = false,
    val availableVolumes: List<StorageVolumeItem> = emptyList(),
    val selectedVolume: StorageVolumeItem? = null,
    val currentDirectory: File? = null,
    val currentNode: DirectoryNode? = null,
    val selectedNode: DirectoryNode? = null,
    val breadcrumbs: List<File> = emptyList(),
    val errorMessage: String? = null
)

class DiskTreeViewModel(
    private val repositoryFactory: (Context) -> DiskTreeRepository = { context -> DiskTreeRepositoryImpl(context) }
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiskTreeUiState())
    val uiState: StateFlow<DiskTreeUiState> = _uiState.asStateFlow()

    fun checkPermissionAndLoad(context: Context, forceReload: Boolean = false) {
        val hasPermission = PermissionUtils.hasStoragePermission(context)
        _uiState.update { it.copy(hasStoragePermission = hasPermission) }

        if (!hasPermission) return

        val volumes = StorageVolumeUtils.getStorageVolumes(context)
        val selected = _uiState.value.selectedVolume ?: volumes.firstOrNull()

        _uiState.update {
            it.copy(
                availableVolumes = volumes,
                selectedVolume = selected
            )
        }

        if (_uiState.value.currentNode != null && !forceReload) return

        selected?.let {
            navigateToDirectory(it.rootDir, context)
        }
    }

    fun selectVolume(volume: StorageVolumeItem, context: Context) {
        if (_uiState.value.selectedVolume?.id == volume.id) return
        _uiState.update {
            it.copy(
                selectedVolume = volume,
                breadcrumbs = emptyList(),
                selectedNode = null,
                currentNode = null
            )
        }
        navigateToDirectory(volume.rootDir, context)
    }

    fun navigateToDirectory(dir: File, context: Context) {
        val volumeRoot = _uiState.value.selectedVolume?.rootDir ?: dir
        val crumbs = buildBreadcrumbs(volumeRoot, dir)

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    currentDirectory = dir,
                    breadcrumbs = crumbs,
                    selectedNode = null,
                    errorMessage = null
                )
            }

            val repository = repositoryFactory(context)
            val result = repository.loadDirectoryNode(dir)

            result.fold(
                onSuccess = { node ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentNode = node,
                            selectedNode = node.children.firstOrNull() ?: node,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Could not index directory"
                        )
                    }
                }
            )
        }
    }

    fun navigateUp(context: Context) {
        val current = _uiState.value.currentDirectory ?: return
        val root = _uiState.value.selectedVolume?.rootDir ?: return
        if (current.absolutePath == root.absolutePath) return

        val parent = current.parentFile ?: return
        if (parent.exists() && parent.canRead()) {
            navigateToDirectory(parent, context)
        }
    }

    fun selectNode(node: DirectoryNode) {
        _uiState.update { it.copy(selectedNode = node) }
    }

    private fun buildBreadcrumbs(root: File, current: File): List<File> {
        val list = mutableListOf<File>()
        var curr: File? = current
        val rootPath = root.absolutePath

        while (curr != null) {
            list.add(0, curr)
            if (curr.absolutePath == rootPath) break
            curr = curr.parentFile
        }
        return list
    }
}

