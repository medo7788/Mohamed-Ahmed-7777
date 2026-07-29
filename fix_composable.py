with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "r") as f:
    content = f.read()

content = content.replace("@Composable\n@OptIn(ExperimentalMaterial3Api::class)\n@Composable", "@OptIn(ExperimentalMaterial3Api::class)\n@Composable")

with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "w") as f:
    f.write(content)
