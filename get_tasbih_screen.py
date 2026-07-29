with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "r") as f:
    content = f.read()

start_idx = content.find("fun TasbihScreen(colors: CustomThemeColors) {")
end_idx = content.find("@Composable\nfun QuranScreen(", start_idx)
print(content[start_idx:end_idx])
