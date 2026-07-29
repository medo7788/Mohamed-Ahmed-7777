import re
with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "r") as f:
    content = f.read()

start = content.find("fun PrayerTimesScreen(colors: CustomThemeColors)")
end = content.find("fun QiblaDirectionScreen(", start)
with open("prayer_orig.txt", "w") as f2:
    f2.write(content[start:end])
