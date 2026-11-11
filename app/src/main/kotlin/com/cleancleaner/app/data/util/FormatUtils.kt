package com.cleancleaner.app.data.util

import android.content.Context
import android.text.format.Formatter
import java.util.Locale

object FormatUtils {

    fun formatBytes(context: Context?, bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        return try {
            if (context != null) {
                Formatter.formatShortFileSize(context, bytes) ?: fallbackFormat(bytes)
            } else {
                fallbackFormat(bytes)
            }
        } catch (e: Exception) {
            fallbackFormat(bytes)
        }
    }

    private fun fallbackFormat(bytes: Long): String {
        if (bytes < 1024L) return "$bytes B"
        val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt().coerceIn(0, 4)
        val pre = listOf("B", "KB", "MB", "GB", "TB")[exp]
        return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, exp.toDouble()), pre)
    }
}

