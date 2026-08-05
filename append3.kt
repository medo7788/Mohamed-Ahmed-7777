@Composable
private fun LocationDisabledStateView(
    onEnableLocationClick: () -> Unit,
    onRequestPermissionClick: () -> Unit,
    onSelectManualCityClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF080A0F)), contentAlignment = Alignment.Center) {
        androidx.compose.material3.Button(onClick = onRequestPermissionClick) { Text("تفعيل الموقع") }
    }
}

@Composable
private fun ErrorStateView(
    errorMessage: String,
    shakeTrigger: Boolean,
    onRetryClick: () -> Unit,
    onSelectManualCityClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF080A0F)), contentAlignment = Alignment.Center) {
        androidx.compose.material3.Button(onClick = onRetryClick) { Text("إعادة المحاولة") }
    }
}
