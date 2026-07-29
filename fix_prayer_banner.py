import re

with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "r") as f:
    content = f.read()

old_banner = """                        Text(city.nameAr, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.text)
                        Text(city.countryAr, fontSize = 11.sp, color = colors.textMuted)"""

new_banner = """                        Text(city.nameAr, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (city.countryAr.isNotBlank()) {
                            Text(city.countryAr, fontSize = 11.sp, color = colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }"""

if old_banner in content:
    content = content.replace(old_banner, new_banner)
    with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "w") as f:
        f.write(content)
    print("Fixed Prayer Location Banner")
else:
    print("Could not find old_banner")

