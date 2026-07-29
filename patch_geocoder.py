with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "r") as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    if line.strip() == "import com.google.android.gms.location.LocationServices":
        new_lines.append(line)
        new_lines.append("import android.location.Geocoder\n")
        new_lines.append("import java.util.Locale\n")
        new_lines.append("import kotlinx.coroutines.Dispatchers\n")
        new_lines.append("import kotlinx.coroutines.withContext\n")
    else:
        new_lines.append(line)

with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "w") as f:
    f.writelines(new_lines)
