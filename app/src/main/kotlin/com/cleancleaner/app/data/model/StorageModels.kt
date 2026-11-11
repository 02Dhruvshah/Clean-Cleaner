package com.cleancleaner.app.data.model

import android.graphics.drawable.Drawable
import java.io.File

enum class FileCategory(val displayName: String) {
    MEDIA("Videos"),
    AUDIO("Audio"),
    IMAGES("Images"),
    DOCUMENTS("Documents"),
    ARCHIVE("Archives"),
    APK("APKs"),
    DOWNLOADS("Downloads"),
    OTHER("Other")
}

data class StorageVolumeItem(
    val id: String,
    val name: String,
    val rootDir: File,
    val isPrimary: Boolean,
    val isRemovable: Boolean,
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long = (totalBytes - freeBytes).coerceAtLeast(0L),
    val formattedTotal: String = "",
    val formattedFree: String = "",
    val formattedUsed: String = ""
)

data class EmptyDirectoryItem(
    val file: File,
    val path: String,
    val name: String,
    val parentPath: String,
    val lastModified: Long
)

data class TopFileItem(
    val file: File,
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val formattedSize: String,
    val extension: String,
    val category: FileCategory,
    val lastModified: Long
)

data class StorageOverview(
    val totalSpaceBytes: Long,
    val freeSpaceBytes: Long,
    val usedSpaceBytes: Long,
    val usedPercentage: Float,
    val formattedTotal: String,
    val formattedFree: String,
    val formattedUsed: String
)

data class DeviceStorageBreakdown(
    val totalBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val totalAppsCacheBytes: Long = 0L,
    val totalAppsCodeAndDataBytes: Long = 0L,
    val systemAndOtherBytes: Long = 0L,
    val formattedTotal: String = "0 B",
    val formattedFree: String = "0 B",
    val formattedCache: String = "0 B",
    val formattedAppsFootprint: String = "0 B",
    val formattedSystemOther: String = "0 B"
)

data class DirectoryNode(
    val file: File,
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val formattedSize: String,
    val fileCount: Int,
    val subfolderCount: Int,
    val dominantCategory: FileCategory = FileCategory.OTHER,
    val children: List<DirectoryNode> = emptyList()
)

data class RunningProcessItem(
    val packageName: String,
    val appLabel: String,
    val icon: Drawable? = null,
    val pid: Int,
    val processName: String,
    val memoryBytes: Long,
    val memoryFormatted: String,
    val importanceLabel: String,
    val isForeground: Boolean
)
