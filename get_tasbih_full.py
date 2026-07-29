import re

with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "r") as f:
    content = f.read()

start_idx = content.find("@OptIn(ExperimentalMaterial3Api::class)\nfun TasbihScreen(colors: CustomThemeColors) {")
if start_idx == -1:
    start_idx = content.find("fun TasbihScreen(colors: CustomThemeColors) {")
end_idx = content.find("@Composable\nfun QuranScreen(", start_idx)
print(content[start_idx:start_idx+100])
print(start_idx, end_idx)
