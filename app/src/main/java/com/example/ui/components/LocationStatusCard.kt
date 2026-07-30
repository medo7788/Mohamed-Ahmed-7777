package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CustomThemeColors

enum class LocationCardState { IDLE, LOADING, SUCCESS, PERMISSION_DENIED, DISABLED, ERROR }

@Composable
fun LocationStatusCard(
    colors: CustomThemeColors,
    state: LocationCardState,
    placeName: String? = null,
    accuracyMeters: Float? = null,
    onRequestPermission: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(90.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon section
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (state == LocationCardState.ERROR) Color.Red.copy(alpha = 0.1f)
                        else colors.accent.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (state == LocationCardState.ERROR) Icons.Default.Error else Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = if (state == LocationCardState.ERROR) Color.Red else colors.accent,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text section
            Column(modifier = Modifier.weight(1f)) {
                when (state) {
                    LocationCardState.SUCCESS -> {
                        Text(
                            text = placeName ?: "الموقع الحالي",
                            fontSize = 18.sp,
                            color = colors.text,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (accuracyMeters != null) "دقة الموقع: ${accuracyMeters.toInt()} م" else "تم تحديد الموقع بنجاح",
                            fontSize = 12.sp,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    LocationCardState.LOADING -> {
                        Text(
                            text = "جاري تحديد الموقع...",
                            fontSize = 16.sp,
                            color = colors.text,
                            fontWeight = FontWeight.Bold
                        )
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            color = colors.accent,
                            trackColor = colors.accent.copy(alpha = 0.1f)
                        )
                    }
                    LocationCardState.ERROR, LocationCardState.PERMISSION_DENIED, LocationCardState.DISABLED -> {
                        Text(
                            text = when(state) {
                                LocationCardState.PERMISSION_DENIED -> "الأذن مرفوض"
                                LocationCardState.DISABLED -> "الموقع غير مفعل"
                                else -> "فشل في تحديد الموقع"
                            },
                            fontSize = 16.sp,
                            color = Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "اضغط للتحديث",
                            fontSize = 12.sp,
                            color = colors.textMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    else -> {
                        Text(
                            text = "تحديد الموقع",
                            fontSize = 16.sp,
                            color = colors.text,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Action button section
            if (state == LocationCardState.ERROR || state == LocationCardState.PERMISSION_DENIED || state == LocationCardState.DISABLED || state == LocationCardState.IDLE) {
                IconButton(
                    onClick = when(state) {
                        LocationCardState.PERMISSION_DENIED -> onRequestPermission
                        LocationCardState.DISABLED -> onOpenLocationSettings
                        else -> onRetry
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry",
                        tint = colors.accent
                    )
                }
            }
        }
    }
}
