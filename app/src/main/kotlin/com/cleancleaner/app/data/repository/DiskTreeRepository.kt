package com.cleancleaner.app.data.repository

import android.content.Context
import com.cleancleaner.app.data.model.DirectoryNode
import com.cleancleaner.app.data.model.FileCategory
import com.cleancleaner.app.data.util.FormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

interface DiskTreeRepository {
    suspend fun loadDirectoryNode(dir: File): Result<DirectoryNode>
}

class DiskTreeRepositoryImpl(
    private val context: Context
) : DiskTreeRepository {

    override suspend fun loadDirectoryNode(dir: File): Result<DirectoryNode> = withContext(Dispatchers.IO) {
        runCatching {
            if (!dir.exists() || !dir.isDirectory) {
                throw IllegalArgumentException("Invalid directory: ${dir.absolutePath}")
            }

            val children = dir.listFiles() ?: emptyArray()
            val childNodes = mutableListOf<DirectoryNode>()
            var totalDirSize = 0L
            var totalFiles = 0
            var totalFolders = 0
            val categoryCounts = mutableMapOf<FileCategory, Long>()

            for (child in children) {
                if (child.isDirectory) {
                    totalFolders++
                    // Shallow recursion (1 depth) for fast responsive UI
                    val (folderSize, fCount, subCount, dominantCat) = calculateFolderStats(child)
                    totalDirSize += folderSize
                    totalFiles += fCount
                    totalFolders += subCount

                    childNodes.add(
                        DirectoryNode(
                            file = child,
                            name = child.name,
                            path = child.absolutePath,
                            sizeBytes = folderSize,
                            formattedSize = FormatUtils.formatBytes(context, folderSize),
                            fileCount = fCount,
                            subfolderCount = subCount,
                            dominantCategory = dominantCat,
                            children = emptyList()
                        )
                    )
                } else if (child.isFile) {
                    totalFiles++
                    val len = child.length()
                    totalDirSize += len
                    val cat = getCategory(child)
                    categoryCounts[cat] = (categoryCounts[cat] ?: 0L) + len

                    childNodes.add(
                        DirectoryNode(
                            file = child,
                            name = child.name,
                            path = child.absolutePath,
                            sizeBytes = len,
                            formattedSize = FormatUtils.formatBytes(context, len),
                            fileCount = 1,
                            subfolderCount = 0,
                            dominantCategory = cat,
                            children = emptyList()
                        )
                    )
                }
            }

            val sortedChildren = childNodes.sortedByDescending { it.sizeBytes }
            val dominant = categoryCounts.maxByOrNull { it.value }?.key ?: FileCategory.OTHER

            DirectoryNode(
                file = dir,
                name = dir.name.ifEmpty { dir.absolutePath },
                path = dir.absolutePath,
                sizeBytes = totalDirSize,
                formattedSize = FormatUtils.formatBytes(context, totalDirSize),
                fileCount = totalFiles,
                subfolderCount = totalFolders,
                dominantCategory = dominant,
                children = sortedChildren
            )
        }
    }

    private fun calculateFolderStats(dir: File, maxDepth: Int = 3): FolderStat {
        var size = 0L
        var files = 0
        var folders = 0
        val catMap = mutableMapOf<FileCategory, Long>()

        fun walk(current: File, depth: Int) {
            if (depth > maxDepth) return
            val list = try { current.listFiles() } catch (e: Exception) { null } ?: return
            for (f in list) {
                if (f.isDirectory) {
                    folders++
                    walk(f, depth + 1)
                } else if (f.isFile) {
                    files++
                    val len = f.length()
                    size += len
                    val cat = getCategory(f)
                    catMap[cat] = (catMap[cat] ?: 0L) + len
                }
            }
        }

        walk(dir, 1)
        val dominant = catMap.maxByOrNull { it.value }?.key ?: FileCategory.OTHER
        return FolderStat(size, files, folders, dominant)
    }

    private data class FolderStat(
        val size: Long,
        val files: Int,
        val folders: Int,
        val dominantCategory: FileCategory
    )

    private fun getCategory(file: File): FileCategory {
        val ext = file.extension.lowercase()
        return when {
            ext in listOf("apk") -> FileCategory.APK
            ext in listOf("mp4", "mkv", "mov", "avi", "3gp", "webm") -> FileCategory.MEDIA
            ext in listOf("mp3", "flac", "wav", "m4a", "ogg", "aac") -> FileCategory.AUDIO
            ext in listOf("jpg", "jpeg", "png", "heic", "webp", "gif") -> FileCategory.IMAGES
            ext in listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt") -> FileCategory.DOCUMENTS
            ext in listOf("zip", "rar", "7z", "tar", "gz") -> FileCategory.ARCHIVE
            else -> FileCategory.OTHER
        }
    }
}

