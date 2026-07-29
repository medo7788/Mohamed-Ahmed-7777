with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "r") as f:
    content = f.read()

content = content.replace("import kotlinx.coroutines.Dispatchers\nimport kotlinx.coroutines.Dispatchers", "import kotlinx.coroutines.Dispatchers")

old_func_start = """@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTimesScreen(colors: CustomThemeColors) {
    val context = LocalContext.current"""

new_func_start = """@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTimesScreen(colors: CustomThemeColors) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current"""

if old_func_start in content:
    content = content.replace(old_func_start, new_func_start)
    print("Added coroutineScope")

with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "w") as f:
    f.write(content)
