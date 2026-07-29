with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "r") as f:
    content = f.read()

imports = [
    "import androidx.compose.material.icons.filled.Delete",
    "import androidx.compose.material.icons.filled.Add",
    "import androidx.compose.foundation.BorderStroke"
]

new_content = content
for imp in imports:
    if imp not in new_content:
        new_content = new_content.replace("import androidx.compose.material.icons.filled.Settings", f"import androidx.compose.material.icons.filled.Settings\n{imp}")

with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "w") as f:
    f.write(new_content)
