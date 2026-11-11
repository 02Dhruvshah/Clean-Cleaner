package com.cleancleaner.app.data.util

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import androidx.core.content.ContextCompat
import com.cleancleaner.app.data.model.StorageVolumeItem
import java.io.File

object StorageVolumeUtils {

    /**
     * Dynamically detects all available storage volumes (Internal Shared Storage and mounted SD Cards).
     */
    fun getStorageVolumes(context: Context): List<StorageVolumeItem> {
        val volumeList = mutableListOf<StorageVolumeItem>()
        val seenRoots = mutableSetOf<String>()

        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager

        if (storageManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val volumes = storageManager.storageVolumes
            for (volume in volumes) {
                val rootDir: File? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    volume.directory
                } else {
                    // Pre-Android 11 fallback via reflection or external files dirs
                    try {
                        val getPathMethod = volume.javaClass.getMethod("getPath")
                        val path = getPathMethod.invoke(volume) as? String
                        path?.let { File(it) }
                    } catch (e: Exception) {
                        null
                    }
                }

                if (rootDir != null && rootDir.exists() && rootDir.canRead()) {
                    val canonicalPath = try { rootDir.canonicalPath } catch (e: Exception) { rootDir.absolutePath }
                    if (seenRoots.add(canonicalPath)) {
                        val isPrimary = volume.isPrimary
                        val isRemovable = volume.isRemovable
                        val name = volume.getDescription(context)
                            ?: if (isPrimary) "Internal Storage" else "SD Card"

                        val totalBytes = rootDir.totalSpace
                        val freeBytes = rootDir.freeSpace

                        volumeList.add(
                            StorageVolumeItem(
                                id = volume.uuid ?: (if (isPrimary) "internal" else canonicalPath),
                                name = name,
                                rootDir = rootDir,
                                isPrimary = isPrimary,
                                isRemovable = isRemovable,
                                totalBytes = totalBytes,
                                freeBytes = freeBytes,
                                formattedTotal = FormatUtils.formatBytes(context, totalBytes),
                                formattedFree = FormatUtils.formatBytes(context, freeBytes),
                                formattedUsed = FormatUtils.formatBytes(context, (totalBytes - freeBytes).coerceAtLeast(0L))
                            )
                        )
                    }
                }
            }
        }

        // Secondary fallback using ContextCompat.getExternalFilesDirs
        if (volumeList.isEmpty()) {
            val externalFilesDirs = ContextCompat.getExternalFilesDirs(context, null)
            for ((index, dir) in externalFilesDirs.withIndex()) {
                if (dir != null) {
                    val path = dir.absolutePath
                    val androidIndex = path.indexOf("/Android/data")
                    val rootPath = if (androidIndex != -1) path.substring(0, androidIndex) else path
                    val rootFile = File(rootPath)

                    if (rootFile.exists() && rootFile.canRead()) {
                        val canonical = try { rootFile.canonicalPath } catch (e: Exception) { rootFile.absolutePath }
                        if (seenRoots.add(canonical)) {
                            val isPrimary = index == 0
                            val totalBytes = rootFile.totalSpace
                            val freeBytes = rootFile.freeSpace

                            volumeList.add(
                                StorageVolumeItem(
                                    id = if (isPrimary) "internal" else "sdcard_$index",
                                    name = if (isPrimary) "Internal Storage" else "SD Card $index",
                                    rootDir = rootFile,
                                    isPrimary = isPrimary,
                                    isRemovable = !isPrimary,
                                    totalBytes = totalBytes,
                                    freeBytes = freeBytes,
                                    formattedTotal = FormatUtils.formatBytes(context, totalBytes),
                                    formattedFree = FormatUtils.formatBytes(context, freeBytes),
                                    formattedUsed = FormatUtils.formatBytes(context, (totalBytes - freeBytes).coerceAtLeast(0L))
                                )
                            )
                        }
                    }
                }
            }
        }

        // Ultimate fallback to primary external storage directory
        if (volumeList.isEmpty()) {
            val defaultRoot = Environment.getExternalStorageDirectory()
            val totalBytes = defaultRoot.totalSpace
            val freeBytes = defaultRoot.freeSpace
            volumeList.add(
                StorageVolumeItem(
                    id = "internal",
                    name = "Internal Storage",
                    rootDir = defaultRoot,
                    isPrimary = true,
                    isRemovable = false,
                    totalBytes = totalBytes,
                    freeBytes = freeBytes,
                    formattedTotal = FormatUtils.formatBytes(context, totalBytes),
                    formattedFree = FormatUtils.formatBytes(context, freeBytes),
                    formattedUsed = FormatUtils.formatBytes(context, (totalBytes - freeBytes).coerceAtLeast(0L))
                )
            )
        }

        // Always put Primary Internal Storage first
        return volumeList.sortedWith(compareByDescending<StorageVolumeItem> { it.isPrimary }.thenBy { it.name })
    }
}

