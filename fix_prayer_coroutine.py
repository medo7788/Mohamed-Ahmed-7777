import re

with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "r") as f:
    content = f.read()

# Remove duplicate import
content = content.replace("import kotlinx.coroutines.Dispatchers\nimport kotlinx.coroutines.withContext\nimport com.google.android.gms.location.LocationServices", "import kotlinx.coroutines.withContext\nimport com.google.android.gms.location.LocationServices")
content = content.replace("import kotlinx.coroutines.Dispatchers\nimport kotlinx.coroutines.Dispatchers", "import kotlinx.coroutines.Dispatchers")

# Add coroutineScope to PrayerTimesScreen
old_func_start = """@Composable
fun PrayerTimesScreen(colors: CustomThemeColors) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var selectedCityIndex by remember { mutableStateOf(0) }"""

new_func_start = """@Composable
fun PrayerTimesScreen(colors: CustomThemeColors) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var selectedCityIndex by remember { mutableStateOf(0) }"""

if old_func_start in content:
    content = content.replace(old_func_start, new_func_start)
    print("Added coroutineScope")
else:
    print("Could not find old_func_start")

with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "w") as f:
    f.write(content)
