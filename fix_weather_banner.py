import re

with open("app/src/main/java/com/example/ui/screens/EconomicAndWeatherScreens.kt", "r") as f:
    content = f.read()

content = re.sub(
    r'Row\(verticalAlignment = Alignment\.CenterVertically\) \{\s*Text\(selectedCity\.icon',
    r'Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {\n                    Text(selectedCity.icon',
    content
)

content = re.sub(
    r'Column \{\s*Text\("📍 \$\{selectedCity\.nameAr\}"',
    r'Column(modifier = Modifier.weight(1f)) {\n                        Text("📍 ${selectedCity.nameAr}"',
    content
)

with open("app/src/main/java/com/example/ui/screens/EconomicAndWeatherScreens.kt", "w") as f:
    f.write(content)
