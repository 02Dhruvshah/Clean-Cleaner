package com.cleancleaner.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleancleaner.app.data.model.DeviceStorageBreakdown
import com.cleancleaner.app.ui.theme.ElectricViolet
import com.cleancleaner.app.ui.theme.NeonCyan

@Composable
fun GlobalStorageBar(
    breakdown: DeviceStorageBreakdown,
    modifier: Modifier = Modifier
) {
    val total = breakdown.totalBytes.coerceAtLeast(1L).toFloat()
    val systemWeight = (breakdown.systemAndOtherBytes.toFloat() / total).coerceIn(0.02f, 0.9f)
    val appCodeDataWeight = (breakdown.totalAppsCodeAndDataBytes.toFloat() / total).coerceIn(0.01f, 0.9f)
    val cacheWeight = (breakdown.totalAppsCacheBytes.toFloat() / total).coerceIn(0.01f, 0.9f)
    val freeWeight = (breakdown.freeBytes.toFloat() / total).coerceIn(0.02f, 0.9f)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DEVICE STORAGE BREAKDOWN",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${breakdown.formattedFree} free of ${breakdown.formattedTotal}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Multi-segment progress bar
            val barShape = RoundedCornerShape(8.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(barShape)
                    .background(Color(0xFF131822))
            ) {
                // 1. System / Other
                Box(
                    modifier = Modifier
                        .fillMaxWidth(systemWeight / (systemWeight + appCodeDataWeight + cacheWeight + freeWeight))
                        .height(14.dp)
                        .background(Color(0xFF475569))
                )
                // 2. App Code & Data
                Box(
                    modifier = Modifier
                        .weight(appCodeDataWeight)
                        .height(14.dp)
                        .background(ElectricViolet)
                )
                // 3. Total Apps Cache (Highlighted Neon)
                Box(
                    modifier = Modifier
                        .weight(cacheWeight)
                        .height(14.dp)
                        .background(NeonCyan)
                )
                // 4. Free Space
                Box(
                    modifier = Modifier
                        .weight(freeWeight)
                        .height(14.dp)
                        .background(Color(0xFF1E2638))
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Summary Badges & Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Neon Highlighted Total Cache Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeonCyan.copy(alpha = 0.15f))
                        .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(NeonCyan)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Total Cache: ${breakdown.formattedCache}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        )
                    }
                }

                // App Footprint Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(ElectricViolet.copy(alpha = 0.15f))
                        .border(1.dp, ElectricViolet.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ElectricViolet)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "App Footprint: ${breakdown.formattedAppsFootprint}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = ElectricViolet
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Legend indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendIndicator(color = Color(0xFF475569), label = "System / Other")
                LegendIndicator(color = ElectricViolet, label = "Code & Data")
                LegendIndicator(color = NeonCyan, label = "Cache")
                LegendIndicator(color = Color(0xFF1E2638), label = "Free")
            }
        }
    }
}

@Composable
private fun LegendIndicator(
    color: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

