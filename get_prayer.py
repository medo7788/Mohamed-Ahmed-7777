with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "r") as f:
    content = f.read()

start = content.find("fun PrayerTimesScreen(colors: CustomThemeColors)")
end = content.find("fun QiblaDirectionScreen(", start)
print(content[start:end])
