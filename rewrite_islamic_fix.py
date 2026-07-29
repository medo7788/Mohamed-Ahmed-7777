import re

with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "r") as f:
    content = f.read()

if "import kotlin.math.atan2" not in content:
    content = content.replace("import kotlin.math.sin\n", "import kotlin.math.sin\nimport kotlin.math.atan2\n")

prayer_times_replacement = """    val dynamicTimes = remember(lat, lng) {
        if (lat != null && lng != null) {
            IslamicData.calculatePrayerTimes(lat!!, lng!!, 3.0) // fallback offset
        } else {
            IslamicData.getDynamicPrayerTimesForCity(IslamicData.cities.first())
        }
    }"""
content = content.replace("""    val dynamicTimes = remember(lat, lng) {
        if (lat != null && lng != null) {
            IslamicData.getDynamicPrayerTimesForLocation(lat!!, lng!!)
        } else {
            IslamicData.getDynamicPrayerTimesForCity(IslamicData.egyptCities.first())
        }
    }""", prayer_times_replacement)

with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "w") as f:
    f.write(content)
