with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "r") as f:
    content = f.read()

old_row = """                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📍", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(city.nameAr, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (city.countryAr.isNotBlank()) {
                            Text(city.countryAr, fontSize = 11.sp, color = colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {"""

new_row = """                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text("📍", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(city.nameAr, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (city.countryAr.isNotBlank()) {
                            Text(city.countryAr, fontSize = 11.sp, color = colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {"""

if old_row in content:
    content = content.replace(old_row, new_row)
    with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "w") as f:
        f.write(content)
    print("Fixed Prayer Location Banner Weight")
else:
    print("Could not find old_row")

