with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "r") as f:
    content = f.read()

content = content.replace(
    "// Bismillah Banner for non-Tawbah surahs\n        if (surah.number != 9) {",
    "// Bismillah Banner for non-Tawbah and non-Fatiha surahs\n        if (surah.number != 9 && surah.number != 1) {"
)

with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "w") as f:
    f.write(content)
