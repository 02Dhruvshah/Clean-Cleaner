package com.cleancleaner.app.data.repository

import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.cleancleaner.app.data.model.RunningProcessItem
import com.cleancleaner.app.data.util.FormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ProcessInspectorRepository {
    suspend fun getRunningProcesses(): Result<List<RunningProcessItem>>
}

class ProcessInspectorRepositoryImpl(
    private val context: Context
) : ProcessInspectorRepository {

    override suspend fun getRunningProcesses(): Result<List<RunningProcessItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                ?: return@runCatching emptyList()
            val packageManager = context.packageManager
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

            val runningProcs = activityManager.runningAppProcesses ?: emptyList()
            val items = mutableListOf<RunningProcessItem>()
            val seenPackages = mutableSetOf<String>()

            // Get recent usage stats (last 2 hours) to enrich running background apps
            val now = System.currentTimeMillis()
            val recentUsage = try {
                usageStatsManager?.queryUsageStats(
                    UsageStatsManager.INTERVAL_BEST,
                    now - (2 * 60 * 60 * 1000L),
                    now
                )?.filter { it.lastTimeUsed > 0L }?.associateBy { it.packageName } ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }

            for (proc in runningProcs) {
                val pkgName = proc.pkgList?.firstOrNull() ?: proc.processName.substringBefore(":")
                if (!seenPackages.add(pkgName)) continue

                val appInfo = try {
                    packageManager.getApplicationInfo(pkgName, 0)
                } catch (e: Exception) {
                    null
                }

                // Focus on user apps or updated system apps
                if (appInfo != null) {
                    val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val isUpdated = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                    if (isSystem && !isUpdated) continue
                }

                val label = appInfo?.let {
                    runCatching { it.loadLabel(packageManager).toString() }.getOrNull()
                } ?: pkgName.substringAfterLast(".")

                val icon = appInfo?.let {
                    runCatching { it.loadIcon(packageManager) }.getOrNull()
                }

                // Query memory footprint
                val memInfo = runCatching {
                    activityManager.getProcessMemoryInfo(intArrayOf(proc.pid))
                }.getOrNull()
                val pssKb = memInfo?.firstOrNull()?.totalPss ?: 0
                val memoryBytes = pssKb * 1024L

                val isFg = proc.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                val importanceLabel = when (proc.importance) {
                    ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND -> "Foreground"
                    ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE -> "Visible"
                    ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE -> "Background Service"
                    ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED -> "Cached in RAM"
                    else -> "Background"
                }

                items.add(
                    RunningProcessItem(
                        packageName = pkgName,
                        appLabel = label,
                        icon = icon,
                        pid = proc.pid,
                        processName = proc.processName,
                        memoryBytes = memoryBytes,
                        memoryFormatted = if (memoryBytes > 0) FormatUtils.formatBytes(context, memoryBytes) else "Active",
                        importanceLabel = importanceLabel,
                        isForeground = isFg
                    )
                )
            }

            // Include recently active apps from UsageStats if not already listed
            for ((pkg, stats) in recentUsage) {
                if (seenPackages.add(pkg)) {
                    val appInfo = try { packageManager.getApplicationInfo(pkg, 0) } catch (e: Exception) { null }
                    if (appInfo != null) {
                        val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        val isUpdated = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                        if (isSystem && !isUpdated) continue

                        val label = runCatching { appInfo.loadLabel(packageManager).toString() }.getOrDefault(pkg)
                        val icon = runCatching { appInfo.loadIcon(packageManager) }.getOrNull()

                        items.add(
                            RunningProcessItem(
                                packageName = pkg,
                                appLabel = label,
                                icon = icon,
                                pid = -1,
                                processName = pkg,
                                memoryBytes = 0L,
                                memoryFormatted = "Active Recently",
                                importanceLabel = "Recent Activity",
                                isForeground = false
                            )
                        )
                    }
                }
            }

            // Sort with Foreground & highest RAM consumers first
            items.sortedWith(
                compareByDescending<RunningProcessItem> { it.isForeground }
                    .thenByDescending { it.memoryBytes }
            )
        }
    }
}

