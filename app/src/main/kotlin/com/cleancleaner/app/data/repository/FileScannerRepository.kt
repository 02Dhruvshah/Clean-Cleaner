package com.cleancleaner.app.data.repository

import android.content.Context
import android.os.Environment
import com.cleancleaner.app.data.model.EmptyDirectoryItem
import com.cleancleaner.app.data.model.FileCategory
import com.cleancleaner.app.data.model.StorageOverview
import com.cleancleaner.app.data.model.StorageVolumeItem
import com.cleancleaner.app.data.model.TopFileItem
import com.cleancleaner.app.data.util.FormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.PriorityQueue

data class ScanResult(
    val volume: StorageVolumeItem,
    val overview: StorageOverview,
    val emptyDirectories: List<EmptyDirectoryItem>,
    val topFiles: List<TopFileItem>,
    val totalScannedFiles: Int,
    val totalScannedDirectories: Int
)

interface FileScannerRepository {
    suspend fun scanStorage(volume: StorageVolumeItem, maxFiles: Int = 100): Result<ScanResult>
    suspend fun deleteEmptyDirectories(directories: List<EmptyDirectoryItem>): Result<Int>
    suspend fun deleteSingleDirectory(dir: File): Result<Boolean>
    suspend fun deleteSingleFile(file: File): Result<Boolean>
}

class FileScannerRepositoryImpl(
    private val context: Context
) : FileScannerRepository {

    override suspend fun scanStorage(volume: StorageVolumeItem, maxFiles: Int): Result<ScanResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val root = volume.rootDir
                if (!root.exists() || !root.canRead()) {
                    throw IllegalStateException("Selected storage volume ${volume.name} is not readable")
                }

                val totalSpace = root.totalSpace
                val freeSpace = root.freeSpace
                val usedSpace = (totalSpace - freeSpace).coerceAtLeast(0L)
                val usedPercentage = if (totalSpace > 0L) {
                    ((usedSpace.toDouble() / totalSpace.toDouble()) * 100.0).toFloat()
                } else 0f

                val overview = StorageOverview(
                    totalSpaceBytes = totalSpace,
                    freeSpaceBytes = freeSpace,
                    usedSpaceBytes = usedSpace,
                    usedPercentage = usedPercentage,
                    formattedTotal = FormatUtils.formatBytes(context, totalSpace),
                    formattedFree = FormatUtils.formatBytes(context, freeSpace),
                    formattedUsed = FormatUtils.formatBytes(context, usedSpace)
                )

                val emptyDirsList = mutableListOf<EmptyDirectoryItem>()
                var fileCount = 0
                var dirCount = 0

                val maxCapacity = maxFiles.coerceIn(20, 200)
                val topFilesQueue = PriorityQueue<TopFileItem>(maxCapacity, compareBy { it.sizeBytes })

                fun inspectDirectory(dir: File): Boolean {
                    dirCount++
                    val children = try {
                        dir.listFiles()
                    } catch (e: SecurityException) {
                        null
                    } catch (e: Exception) {
                        null
                    }

                    if (children == null) {
                        return false
                    }

                    var hasRealFiles = false
                    var hasNonEmptySubdir = false

                    for (child in children) {
                        if (child.isDirectory) {
                            val name = child.name
                            val path = child.absolutePath
                            // Skip system restricted paths on Android
                            if (name.equals("data", ignoreCase = true) && path.contains("/Android/")) {
                                hasNonEmptySubdir = true
                                continue
                            }
                            if (name.equals("obb", ignoreCase = true) && path.contains("/Android/")) {
                                hasNonEmptySubdir = true
                                continue
                            }

                            val isChildEmpty = inspectDirectory(child)
                            if (!isChildEmpty) {
                                hasNonEmptySubdir = true
                            }
                        } else if (child.isFile) {
                            hasRealFiles = true
                            fileCount++
                            val length = child.length()
                            if (length > 0L) {
                                val ext = child.extension.lowercase()
                                val category = determineCategory(child, ext)
                                val item = TopFileItem(
                                    file = child,
                                    name = child.name,
                                    path = child.absolutePath,
                                    sizeBytes = length,
                                    formattedSize = FormatUtils.formatBytes(context, length),
                                    extension = ext,
                                    category = category,
                                    lastModified = child.lastModified()
                                )

                                if (topFilesQueue.size < maxCapacity) {
                                    topFilesQueue.offer(item)
                                } else if (item.sizeBytes > (topFilesQueue.peek()?.sizeBytes ?: 0L)) {
                                    topFilesQueue.poll()
                                    topFilesQueue.offer(item)
                                }
                            }
                        }
                    }

                    val isEmpty = !hasRealFiles && !hasNonEmptySubdir
                    if (isEmpty && dir != root) {
                        emptyDirsList.add(
                            EmptyDirectoryItem(
                                file = dir,
                                path = dir.absolutePath,
                                name = dir.name,
                                parentPath = dir.parent ?: "",
                                lastModified = dir.lastModified()
                            )
                        )
                    }

                    return isEmpty
                }

                inspectDirectory(root)

                val sortedTopFiles = topFilesQueue.sortedByDescending { it.sizeBytes }
                val sortedEmptyDirs = emptyDirsList.sortedByDescending { it.path.length }

                ScanResult(
                    volume = volume,
                    overview = overview,
                    emptyDirectories = sortedEmptyDirs,
                    topFiles = sortedTopFiles,
                    totalScannedFiles = fileCount,
                    totalScannedDirectories = dirCount
                )
            }
        }

    override suspend fun deleteEmptyDirectories(directories: List<EmptyDirectoryItem>): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                var deletedCount = 0
                val sorted = directories.sortedByDescending { it.path.length }
                for (item in sorted) {
                    try {
                        val file = item.file
                        if (file.exists() && file.isDirectory) {
                            val contents = file.list()
                            if (contents == null || contents.isEmpty()) {
                                if (file.delete()) {
                                    deletedCount++
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Continue
                    }
                }
                deletedCount
            }
        }

    override suspend fun deleteSingleDirectory(dir: File): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            if (dir.exists() && dir.isDirectory) {
                dir.delete()
            } else false
        }
    }

    override suspend fun deleteSingleFile(file: File): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            if (file.exists() && file.isFile) {
                file.delete()
            } else false
        }
    }

    private fun determineCategory(file: File, ext: String): FileCategory {
        val path = file.absolutePath.lowercase()
        return when {
            ext in listOf("apk") -> FileCategory.APK
            ext in listOf("mp4", "mkv", "mov", "avi", "3gp", "webm") -> FileCategory.MEDIA
            ext in listOf("mp3", "flac", "wav", "m4a", "ogg", "aac") -> FileCategory.AUDIO
            ext in listOf("jpg", "jpeg", "png", "heic", "webp", "gif", "bmp") -> FileCategory.IMAGES
            ext in listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "epub") -> FileCategory.DOCUMENTS
            ext in listOf("zip", "rar", "7z", "tar", "gz") -> FileCategory.ARCHIVE
            path.contains("/download") -> FileCategory.DOWNLOADS
            else -> FileCategory.OTHER
        }
    }
}
