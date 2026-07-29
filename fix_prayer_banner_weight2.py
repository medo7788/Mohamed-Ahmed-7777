import re

with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "r") as f:
    content = f.read()

content = re.sub(
    r'Row\(verticalAlignment = Alignment\.CenterVertically\) \{\s*Text\("📍"',
    r'Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {\n                    Text("📍"',
    content
)

content = re.sub(
    r'Column \{\s*Text\(city\.nameAr',
    r'Column(modifier = Modifier.weight(1f)) {\n                        Text(city.nameAr',
    content
)

with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "w") as f:
    f.write(content)
