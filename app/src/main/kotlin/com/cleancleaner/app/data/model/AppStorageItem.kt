package com.cleancleaner.app.data.model

import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color

data class AppStorageItem(
    val packageName: String,
    val appLabel: String,
    val icon: Drawable? = null,
    val cacheBytes: Long = 0L,
    val dataBytes: Long = 0L,
    val appBytes: Long = 0L,
    val totalBytes: Long = cacheBytes + dataBytes + appBytes,
    val cacheFormatted: String = "",
    val dataFormatted: String = "",
    val appFormatted: String = "",
    val totalFormatted: String = "",
    val cachePercentage: Float = 0f,
    val color: Color = Color(0xFF00E5FF)
)

