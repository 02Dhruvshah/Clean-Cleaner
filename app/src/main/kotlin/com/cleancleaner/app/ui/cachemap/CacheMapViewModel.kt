package com.cleancleaner.app.ui.cachemap

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cleancleaner.app.data.model.AppStorageItem
import com.cleancleaner.app.data.model.DeviceStorageBreakdown
import com.cleancleaner.app.data.model.RunningProcessItem
import com.cleancleaner.app.data.repository.CacheRepository
import com.cleancleaner.app.data.repository.CacheRepositoryImpl
import com.cleancleaner.app.data.repository.ProcessInspectorRepository
import com.cleancleaner.app.data.repository.ProcessInspectorRepositoryImpl
import com.cleancleaner.app.data.util.FormatUtils
import com.cleancleaner.app.data.util.PermissionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class VisualizationMode {
    DONUT_RING,
    TREEMAP
}

data class CacheMapUiState(
    val isLoading: Boolean = false,
    val hasUsagePermission: Boolean = false,
    val apps: List<AppStorageItem> = emptyList(),
    val selectedApp: AppStorageItem? = null,
    val totalCacheBytes: Long = 0L,
    val totalCacheFormatted: String = "0 B",
    val storageBreakdown: DeviceStorageBreakdown = DeviceStorageBreakdown(),
    val errorMessage: String? = null,
    val visualizationMode: VisualizationMode = VisualizationMode.DONUT_RING,
    val showRunningProcessesDialog: Boolean = false,
    val isLoadingProcesses: Boolean = false,
    val runningProcesses: List<RunningProcessItem> = emptyList()
)

class CacheMapViewModel(
    private val repositoryFactory: (Context) -> CacheRepository = { context -> CacheRepositoryImpl(context) },
    private val processRepositoryFactory: (Context) -> ProcessInspectorRepository = { context -> ProcessInspectorRepositoryImpl(context) }
) : ViewModel() {

    private val _uiState = MutableStateFlow(CacheMapUiState())
    val uiState: StateFlow<CacheMapUiState> = _uiState.asStateFlow()

    fun checkPermissionAndLoad(context: Context, forceReload: Boolean = false) {
        val hasPermission = PermissionUtils.hasUsageStatsPermission(context)
        _uiState.update { it.copy(hasUsagePermission = hasPermission) }

        if (!hasPermission) {
            return
        }

        if (_uiState.value.apps.isNotEmpty() && !forceReload) {
            return
        }

        loadCacheStats(context)
    }

    fun loadCacheStats(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val repository = repositoryFactory(context)
            val result = repository.getAppStorageStats()

            result.fold(
                onSuccess = { scanResult ->
                    val appsList = scanResult.apps
                    val totalCache = appsList.sumOf { it.cacheBytes }
                    val formattedTotal = FormatUtils.formatBytes(context, totalCache)

                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            apps = appsList,
                            selectedApp = current.selectedApp?.let { existing ->
                                appsList.find { it.packageName == existing.packageName }
                            }, // Leave null if none selected so donut center shows Total Cache
                            totalCacheBytes = totalCache,
                            totalCacheFormatted = formattedTotal,
                            storageBreakdown = scanResult.breakdown,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.localizedMessage ?: "Failed to inspect storage stats"
                        )
                    }
                }
            )
        }
    }

    fun selectApp(app: AppStorageItem?) {
        _uiState.update { it.copy(selectedApp = app) }
    }

    fun setVisualizationMode(mode: VisualizationMode) {
        _uiState.update { it.copy(visualizationMode = mode) }
    }

    fun openRunningProcesses(context: Context) {
        _uiState.update { it.copy(showRunningProcessesDialog = true, isLoadingProcesses = true) }
        loadRunningProcesses(context)
    }

    fun closeRunningProcesses() {
        _uiState.update { it.copy(showRunningProcessesDialog = false) }
    }

    fun loadRunningProcesses(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingProcesses = true) }
            val repo = processRepositoryFactory(context)
            val result = repo.getRunningProcesses()
            result.fold(
                onSuccess = { procs ->
                    _uiState.update { it.copy(isLoadingProcesses = false, runningProcesses = procs) }
                },
                onFailure = {
                    _uiState.update { it.copy(isLoadingProcesses = false) }
                }
            )
        }
    }
}
