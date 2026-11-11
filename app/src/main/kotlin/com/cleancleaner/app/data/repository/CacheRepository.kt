package com.cleancleaner.app.data.repository

import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Environment
import android.os.Process
import android.os.storage.StorageManager
import com.cleancleaner.app.data.model.AppStorageItem
import com.cleancleaner.app.data.model.DeviceStorageBreakdown
import com.cleancleaner.app.data.util.FormatUtils
import com.cleancleaner.app.ui.theme.ChartPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

data class CacheScanResult(
    val apps: List<AppStorageItem>,
    val breakdown: DeviceStorageBreakdown
)

interface CacheRepository {
    suspend fun getAppStorageStats(): Result<CacheScanResult>
}

class CacheRepositoryImpl(
    private val context: Context
) : CacheRepository {

    override suspend fun getAppStorageStats(): Result<CacheScanResult> = withContext(Dispatchers.IO) {
        runCatching {
            val packageManager = context.packageManager
            val storageStatsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager
                ?: throw IllegalStateException("StorageStatsManager service is unavailable on this device")

            val myUserHandle = Process.myUserHandle()
            val installedApps = try {
                packageManager.getInstalledApplications(0)
            } catch (e: Exception) {
                emptyList<ApplicationInfo>()
            }

            // Filter for non-system / user-installed applications (or updated system apps)
            val userApps = installedApps.filter { appInfo ->
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isUpdatedSystem = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                !isSystem || isUpdatedSystem
            }

            val items = mutableListOf<AppStorageItem>()
            var totalAppsCache = 0L
            var totalAppsCodeAndData = 0L

            for (app in userApps) {
                val pkg = app.packageName
                try {
                    val stats = storageStatsManager.queryStatsForPackage(
                        StorageManager.UUID_DEFAULT,
                        pkg,
                        myUserHandle
                    )

                    val cacheBytes = stats.cacheBytes
                    val dataBytes = stats.dataBytes
                    val appBytes = stats.appBytes
                    val totalBytes = cacheBytes + dataBytes + appBytes

                    totalAppsCache += cacheBytes
                    totalAppsCodeAndData += (dataBytes + appBytes)

                    val label = runCatching { app.loadLabel(packageManager).toString() }.getOrDefault(pkg)
                    val icon = runCatching { app.loadIcon(packageManager) }.getOrNull()

                    val item = AppStorageItem(
                        packageName = pkg,
                        appLabel = label,
                        icon = icon,
                        cacheBytes = cacheBytes,
                        dataBytes = dataBytes,
                        appBytes = appBytes,
                        totalBytes = totalBytes,
                        cacheFormatted = FormatUtils.formatBytes(context, cacheBytes),
                        dataFormatted = FormatUtils.formatBytes(context, dataBytes),
                        appFormatted = FormatUtils.formatBytes(context, appBytes),
                        totalFormatted = FormatUtils.formatBytes(context, totalBytes)
                    )
                    items.add(item)
                } catch (e: SecurityException) {
                    continue
                } catch (e: PackageManager.NameNotFoundException) {
                    continue
                } catch (e: IOException) {
                    continue
                } catch (e: Exception) {
                    continue
                }
            }

            // Sort by cacheBytes descending so the heaviest cache consumers appear first
            val sorted = items.sortedByDescending { it.cacheBytes }
            val totalCache = sorted.sumOf { it.cacheBytes }

            // Assign chart colors and percentages
            val coloredApps = sorted.mapIndexed { index, appItem ->
                val percentage = if (totalCache > 0L) {
                    (appItem.cacheBytes.toFloat() / totalCache.toFloat()) * 100f
                } else 0f
                val color = ChartPalette[index % ChartPalette.size]
                appItem.copy(
                    cachePercentage = percentage,
                    color = color
                )
            }

            // Global Storage Breakdown Calculation
            val dataDir = Environment.getDataDirectory()
            val totalSpace = dataDir.totalSpace
            val freeSpace = dataDir.freeSpace
            val usedSpace = (totalSpace - freeSpace).coerceAtLeast(0L)
            val systemAndOther = (usedSpace - totalAppsCache - totalAppsCodeAndData).coerceAtLeast(0L)

            val breakdown = DeviceStorageBreakdown(
                totalBytes = totalSpace,
                freeBytes = freeSpace,
                usedBytes = usedSpace,
                totalAppsCacheBytes = totalAppsCache,
                totalAppsCodeAndDataBytes = totalAppsCodeAndData,
                systemAndOtherBytes = systemAndOther,
                formattedTotal = FormatUtils.formatBytes(context, totalSpace),
                formattedFree = FormatUtils.formatBytes(context, freeSpace),
                formattedCache = FormatUtils.formatBytes(context, totalAppsCache),
                formattedAppsFootprint = FormatUtils.formatBytes(context, totalAppsCache + totalAppsCodeAndData),
                formattedSystemOther = FormatUtils.formatBytes(context, systemAndOther)
            )

            CacheScanResult(
                apps = coloredApps,
                breakdown = breakdown
            )
        }
    }
}
