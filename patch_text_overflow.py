with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "r") as f:
    content = f.read()

# Add TextOverflow import if missing
if "import androidx.compose.ui.text.style.TextOverflow" not in content:
    content = content.replace("import androidx.compose.ui.text.style.TextAlign", "import androidx.compose.ui.text.style.TextAlign\nimport androidx.compose.ui.text.style.TextOverflow")

content = content.replace(
    'Text("📍 المدينة الحالية: $currentDisplayLocation", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.text)',
    'Text("📍 المدينة الحالية: $currentDisplayLocation", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)'
)

# And also for Qibla screen if it exists there:
content = content.replace(
    'Text("📍 المدينة الحالية: $currentDisplayLocation", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.text)',
    'Text("📍 المدينة الحالية: $currentDisplayLocation", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)'
)

with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "w") as f:
    f.write(content)


with open("app/src/main/java/com/example/ui/screens/EconomicAndWeatherScreens.kt", "r") as f:
    content = f.read()

if "import androidx.compose.ui.text.style.TextOverflow" not in content:
    content = content.replace("import androidx.compose.ui.text.style.TextAlign", "import androidx.compose.ui.text.style.TextAlign\nimport androidx.compose.ui.text.style.TextOverflow")

content = content.replace(
    'Text(selectedCity.nameAr, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.text)',
    'Text(selectedCity.nameAr, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))'
)

with open("app/src/main/java/com/example/ui/screens/EconomicAndWeatherScreens.kt", "w") as f:
    f.write(content)
