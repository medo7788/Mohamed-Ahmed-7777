with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "r") as f:
    content = f.read()

start_idx = content.find("fun PrayerTimesScreen(colors: CustomThemeColors) {")
end_idx = content.find("fun QiblaDirectionScreen(", start_idx)
print(content[start_idx:start_idx+2000])
