import re

with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "r") as f:
    content = f.read()

old_func = """    fun fetchGPSLocation() {
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        customLat = location.latitude
                        customLng = location.longitude
                        customLocationName = "موقعي الحالي"
                        saveLocationState(location.latitude, location.longitude, "موقعي الحالي", selectedCityIndex)
                    } else {
                        fusedLocationClient.lastLocation
                            .addOnSuccessListener { lastLoc ->
                                if (lastLoc != null) {
                                    customLat = lastLoc.latitude
                                    customLng = lastLoc.longitude
                                    customLocationName = "موقعي الحالي"
                                    saveLocationState(lastLoc.latitude, lastLoc.longitude, "موقعي الحالي", selectedCityIndex)
                                }
                            }
                    }
                }
        } catch (e: SecurityException) { }
    }"""

new_func = """    fun fetchGPSLocation() {
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        customLat = location.latitude
                        customLng = location.longitude
                        coroutineScope.launch {
                            var locName = "موقعي الحالي"
                            try {
                                val geocoder = Geocoder(context, Locale("ar"))
                                val addresses = withContext(Dispatchers.IO) { geocoder.getFromLocation(location.latitude, location.longitude, 1) }
                                if (!addresses.isNullOrEmpty()) {
                                    val address = addresses[0]
                                    val parts = listOfNotNull(address.countryName, address.adminArea, address.locality ?: address.subAdminArea)
                                    if (parts.isNotEmpty()) locName = parts.joinToString("، ")
                                }
                            } catch (e: Exception) {}
                            customLocationName = locName
                            saveLocationState(location.latitude, location.longitude, locName, selectedCityIndex)
                        }
                    } else {
                        fusedLocationClient.lastLocation
                            .addOnSuccessListener { lastLoc ->
                                if (lastLoc != null) {
                                    customLat = lastLoc.latitude
                                    customLng = lastLoc.longitude
                                    coroutineScope.launch {
                                        var locName = "موقعي الحالي"
                                        try {
                                            val geocoder = Geocoder(context, Locale("ar"))
                                            val addresses = withContext(Dispatchers.IO) { geocoder.getFromLocation(lastLoc.latitude, lastLoc.longitude, 1) }
                                            if (!addresses.isNullOrEmpty()) {
                                                val address = addresses[0]
                                                val parts = listOfNotNull(address.countryName, address.adminArea, address.locality ?: address.subAdminArea)
                                                if (parts.isNotEmpty()) locName = parts.joinToString("، ")
                                            }
                                        } catch (e: Exception) {}
                                        customLocationName = locName
                                        saveLocationState(lastLoc.latitude, lastLoc.longitude, locName, selectedCityIndex)
                                    }
                                }
                            }
                    }
                }
        } catch (e: SecurityException) { }
    }"""

if old_func in content:
    content = content.replace(old_func, new_func)
    with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "w") as f:
        f.write(content)
    print("Replaced fetchGPSLocation in PrayerTimesScreen")
else:
    print("Could not find old_func exactly")

