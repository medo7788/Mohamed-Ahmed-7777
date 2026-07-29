import re

with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "r") as f:
    content = f.read()

# Let's find "📍 المدينة الحالية"
if 'Text("📍 المدينة الحالية:' in content:
    content = re.sub(
        r'Text\("📍 المدينة الحالية: \$currentDisplayLocation"[^\)]+\)',
        'Text("📍 $currentDisplayLocation", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)',
        content
    )
    content = content.replace(
        'Text("الإحداثيات: ${city.lat}, ${city.lng}", fontSize = 11.sp, color = colors.textMuted)',
        ''
    )

with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "w") as f:
    f.write(content)


with open("app/src/main/java/com/example/ui/screens/EconomicAndWeatherScreens.kt", "r") as f:
    content = f.read()

content = content.replace(
    'Text(selectedCity.nameAr, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))',
    'Text("📍 ${selectedCity.nameAr}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))'
)
with open("app/src/main/java/com/example/ui/screens/EconomicAndWeatherScreens.kt", "w") as f:
    f.write(content)

