import re

with open("app/src/main/java/com/example/ui/screens/EconomicAndWeatherScreens.kt", "r") as f:
    content = f.read()

# Make sure Geocoder is imported
if "import android.location.Geocoder" not in content:
    content = content.replace("import android.location.LocationManager", "import android.location.LocationManager\nimport android.location.Geocoder\nimport java.util.Locale\nimport kotlinx.coroutines.Dispatchers\nimport kotlinx.coroutines.withContext\nimport kotlinx.coroutines.launch")

old_block_1 = """                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (lastKnown != null) {
                    selectedCity = WeatherCity("موقعي الحالي", "GPS", lastKnown.latitude, lastKnown.longitude, "📍")
                }"""

new_block_1 = """                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (lastKnown != null) {
                    coroutineScope.launch {
                        var locName = "موقعي الحالي"
                        try {
                            val geocoder = Geocoder(context, Locale("ar"))
                            val addresses = withContext(Dispatchers.IO) { geocoder.getFromLocation(lastKnown.latitude, lastKnown.longitude, 1) }
                            if (!addresses.isNullOrEmpty()) {
                                val address = addresses[0]
                                val parts = listOfNotNull(address.countryName, address.adminArea, address.locality ?: address.subAdminArea)
                                if (parts.isNotEmpty()) locName = parts.joinToString("، ")
                            }
                        } catch (e: Exception) {}
                        selectedCity = WeatherCity(locName, "GPS", lastKnown.latitude, lastKnown.longitude, "📍")
                    }
                }"""

old_block_2 = """                                val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                                val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                                if (loc != null) {
                                    selectedCity = WeatherCity("موقعي الحالي", "GPS", loc.latitude, loc.longitude, "📍")
                                }"""

new_block_2 = """                                val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                                val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                                if (loc != null) {
                                    coroutineScope.launch {
                                        var locName = "موقعي الحالي"
                                        try {
                                            val geocoder = Geocoder(context, Locale("ar"))
                                            val addresses = withContext(Dispatchers.IO) { geocoder.getFromLocation(loc.latitude, loc.longitude, 1) }
                                            if (!addresses.isNullOrEmpty()) {
                                                val address = addresses[0]
                                                val parts = listOfNotNull(address.countryName, address.adminArea, address.locality ?: address.subAdminArea)
                                                if (parts.isNotEmpty()) locName = parts.joinToString("، ")
                                            }
                                        } catch (e: Exception) {}
                                        selectedCity = WeatherCity(locName, "GPS", loc.latitude, loc.longitude, "📍")
                                    }
                                }"""

if old_block_1 in content and old_block_2 in content:
    content = content.replace(old_block_1, new_block_1).replace(old_block_2, new_block_2)
    with open("app/src/main/java/com/example/ui/screens/EconomicAndWeatherScreens.kt", "w") as f:
        f.write(content)
    print("Replaced GPS location fetching in WeatherScreen")
else:
    print("Could not find old_blocks exactly in WeatherScreen")

