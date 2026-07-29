with open("app/src/main/java/com/example/ui/screens/EconomicAndWeatherScreens.kt", "r") as f:
    content = f.read()

content = content.replace("import androidx.compose.foundation.lazy.items\nimport androidx.compose.foundation.lazy.itemsIndexed\npackage com.example.ui.screens", "package com.example.ui.screens\nimport androidx.compose.foundation.lazy.items\nimport androidx.compose.foundation.lazy.itemsIndexed")

with open("app/src/main/java/com/example/ui/screens/EconomicAndWeatherScreens.kt", "w") as f:
    f.write(content)
