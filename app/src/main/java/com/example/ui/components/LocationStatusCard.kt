package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppIcons
import com.example.ui.theme.CustomThemeColors
import kotlin.math.roundToInt

enum class LocationCardState { IDLE, LOADING, SUCCESS, PERMISSION_DENIED, DISABLED, ERROR }

/**
 * Professional "detecting your location" card, matching the pattern used by major
 * weather / maps / prayer-time apps: a live status line, a spinning locate icon while
 * searching, the resolved place name + GPS accuracy once found, and a clear one-tap
 * recovery action when permission is missing or GPS is off — instead of a silent
 * failure or a raw stack trace.
 */
@Composable
fun LocationStatusCard(
    colors: CustomThemeColors,
    state: LocationCardState,
    placeName: String? = null,
    accuracyMeters: Float? = null,
    onRequestPermission: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onRetry: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "locating")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation"
    )

    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (icon, iconColor) = when (state) {
                LocationCardState.LOADING -> AppIcons.LocationSearching to colors.accent
                LocationCardState.SUCCESS -> AppIcons.Location to Color4(0xFF10B981)
                LocationCardState.PERMISSION_DENIED, LocationCardState.DISABLED -> AppIcons.LocationOff to Color4(0xFFEF4444)
                LocationCardState.ERROR -> AppIcons.Warning to Color4(0xFFF59E0B)
                LocationCardState.IDLE -> AppIcons.Location to colors.textMuted
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier
                        .size(20.dp)
                        .then(if (state == LocationCardState.LOADING) Modifier.rotate(rotation) else Modifier)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (state) {
                        LocationCardState.LOADING -> "جارِ تحديد موقعك الحالي..."
                        LocationCardState.SUCCESS -> placeName ?: "تم تحديد الموقع"
                        LocationCardState.PERMISSION_DENIED -> "إذن الموقع غير مفعّل"
                        LocationCardState.DISABLED -> "خدمة الموقع (GPS) مطفأة"
                        LocationCardState.ERROR -> "تعذّر تحديد الموقع"
                        LocationCardState.IDLE -> "الموقع غير محدد"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.text
                )
                val subtitle = when (state) {
                    LocationCardState.SUCCESS -> accuracyMeters?.let { "دقة التحديد: ±${it.roundToInt()} متر" }
                    LocationCardState.PERMISSION_DENIED -> "اسمح للتطبيق بالوصول لموقعك لعرض بيانات دقيقة"
                    LocationCardState.DISABLED -> "فعّل خدمة الموقع من إعدادات الجهاز"
                    LocationCardState.ERROR -> "تحقق من اتصال الإنترنت وحاول مرة أخرى"
                    else -> null
                }
                if (subtitle != null) {
                    Text(subtitle, fontSize = 11.sp, color = colors.textMuted)
                }
            }

            when (state) {
                LocationCardState.PERMISSION_DENIED -> TextButton(onClick = onRequestPermission) {
                    Text("سماح", color = colors.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                LocationCardState.DISABLED -> TextButton(onClick = onOpenLocationSettings) {
                    Text("الإعدادات", color = colors.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                LocationCardState.ERROR -> IconButton(onClick = onRetry) {
                    Icon(AppIcons.Refresh, contentDescription = "إعادة المحاولة", tint = colors.accent)
                }
                else -> {}
            }
        }
    }
}

// small local helper so this file has no extra import surprises for callers copy-pasting it
private fun Color4(value: Long) = androidx.compose.ui.graphics.Color(value)
