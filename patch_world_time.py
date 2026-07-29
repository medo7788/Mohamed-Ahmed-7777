import re

with open("app/src/main/java/com/example/ui/screens/DateHealthScreens.kt", "r") as f:
    content = f.read()

old_cities = """    val worldCities = listOf(
        Triple("مكة المكرمة 🕋", "Asia/Riyadh", "GMT+3"),
        Triple("القاهرة 🇪🇬", "Africa/Cairo", "GMT+3"),
        Triple("الرياض 🇸🇦", "Asia/Riyadh", "GMT+3"),
        Triple("دبي 🇦🇪", "Asia/Dubai", "GMT+4"),
        Triple("الكويت 🇰🇼", "Asia/Kuwait", "GMT+3"),
        Triple("الدوحة 🇶🇦", "Asia/Qatar", "GMT+3"),
        Triple("عمان 🇯🇴", "Asia/Amman", "GMT+3"),
        Triple("بغداد 🇮🇶", "Asia/Baghdad", "GMT+3"),
        Triple("لندن 🇬🇧", "Europe/London", "GMT+0"),
        Triple("باريس 🇫🇷", "Europe/Paris", "GMT+1"),
        Triple("نيويورك 🇺🇸", "America/New_York", "GMT-4"),
        Triple("طوكيو 🇯🇵", "Asia/Tokyo", "GMT+9"),
        Triple("سيدني 🇦🇺", "Australia/Sydney", "GMT+10")
    )"""

new_cities = """    var searchQuery by remember { mutableStateOf("") }
    val allWorldCities = remember {
        val list = TimeZone.getAvailableIDs().filter { it.contains("/") }.mapNotNull { tzId ->
            val tz = TimeZone.getTimeZone(tzId)
            val name = tzId.substringAfterLast("/").replace("_", " ")
            val gmtOffset = tz.rawOffset / (1000 * 60 * 60f)
            val gmtStr = if (gmtOffset >= 0) "GMT+${gmtOffset.toInt()}" else "GMT${gmtOffset.toInt()}"
            if (name.length > 2) Triple(name, tzId, gmtStr) else null
        }.sortedBy { it.first }.distinctBy { it.first }
        
        // Add some popular ones at the top mapped to arabic
        val arabicPopular = listOf(
            Triple("مكة المكرمة 🕋", "Asia/Riyadh", "GMT+3"),
            Triple("القاهرة 🇪🇬", "Africa/Cairo", "GMT+3"),
            Triple("الرياض 🇸🇦", "Asia/Riyadh", "GMT+3"),
            Triple("دبي 🇦🇪", "Asia/Dubai", "GMT+4"),
            Triple("الكويت 🇰🇼", "Asia/Kuwait", "GMT+3"),
            Triple("الدوحة 🇶🇦", "Asia/Qatar", "GMT+3"),
            Triple("عمان 🇯🇴", "Asia/Amman", "GMT+3"),
            Triple("بغداد 🇮🇶", "Asia/Baghdad", "GMT+3"),
            Triple("دمشق 🇸🇾", "Asia/Damascus", "GMT+3"),
            Triple("بيروت 🇱🇧", "Asia/Beirut", "GMT+2"),
            Triple("القدس 🇵🇸", "Asia/Jerusalem", "GMT+3"),
            Triple("الخرطوم 🇸🇩", "Africa/Khartoum", "GMT+2"),
            Triple("صنعاء 🇾🇪", "Asia/Aden", "GMT+3"),
            Triple("مسقط 🇴🇲", "Asia/Muscat", "GMT+4"),
            Triple("المنامة 🇧🇭", "Asia/Bahrain", "GMT+3"),
            Triple("طرابلس 🇱🇾", "Africa/Tripoli", "GMT+2"),
            Triple("تونس 🇹🇳", "Africa/Tunis", "GMT+1"),
            Triple("الجزائر 🇩🇿", "Africa/Algiers", "GMT+1"),
            Triple("الرباط 🇲🇦", "Africa/Casablanca", "GMT+1"),
            Triple("نواكشوط 🇲🇷", "Africa/Nouakchott", "GMT+0"),
            Triple("لندن 🇬🇧", "Europe/London", "GMT+0"),
            Triple("باريس 🇫🇷", "Europe/Paris", "GMT+1"),
            Triple("نيويورك 🇺🇸", "America/New_York", "GMT-4"),
            Triple("طوكيو 🇯🇵", "Asia/Tokyo", "GMT+9"),
            Triple("سيدني 🇦🇺", "Australia/Sydney", "GMT+10"),
            Triple("موسكو 🇷🇺", "Europe/Moscow", "GMT+3"),
            Triple("بكين 🇨🇳", "Asia/Shanghai", "GMT+8")
        )
        (arabicPopular + list).distinctBy { it.second }
    }
    
    val filteredCities = allWorldCities.filter { 
        it.first.contains(searchQuery, ignoreCase = true) || it.second.contains(searchQuery, ignoreCase = true)
    }"""

old_ui = """    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBg)
            .padding(12.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
        ) {
            items(worldCities) { (city, tzId, label) ->"""

new_ui = """    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBg)
            .padding(12.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("بحث عن دولة أو مدينة...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.accent,
                focusedLabelColor = colors.accent,
                unfocusedBorderColor = colors.accent.copy(alpha = 0.5f),
                unfocusedLabelColor = colors.textMuted
            ),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
        ) {
            items(filteredCities) { (city, tzId, label) ->"""

if old_cities in content and old_ui in content:
    content = content.replace(old_cities, new_cities).replace(old_ui, new_ui)
    with open("app/src/main/java/com/example/ui/screens/DateHealthScreens.kt", "w") as f:
        f.write(content)
    print("Replaced world time logic in DateHealthScreens")
else:
    print("Could not find old_blocks exactly in DateHealthScreens")

