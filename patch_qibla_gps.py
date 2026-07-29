import re

with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "r") as f:
    content = f.read()

old_block = """    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            try {
                fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            customLat = location.latitude
                            customLng = location.longitude
                            locationName = "موقعي الحالي"
                        }
                    }
            } catch (e: SecurityException) { }
        }
    }

    LaunchedEffect(Unit) {"""

new_block = """    val coroutineScope = rememberCoroutineScope()
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            try {
                fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
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
                                locationName = locName
                            }
                        }
                    }
            } catch (e: SecurityException) { }
        }
    }

    LaunchedEffect(Unit) {"""

if old_block in content:
    content = content.replace(old_block, new_block)
    with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "w") as f:
        f.write(content)
    print("Replaced Qibla location fetcher")
else:
    print("Could not find old_block exactly in Qibla")
