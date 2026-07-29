package com.example.model

enum class CategoryKey(
    val id: String,
    val label: String,
    val icon: String,
    val colorHex: String,
    val description: String
) {
    FEATURED("featured", "المميزة", "⭐", "#8B5CF6", "أدوات ذكية وأسعار حية"),
    ISLAMIC("islamic", "إسلامية", "🕌", "#059669", "الصلاة والقرآن والأذكار"),
    CALC("calc", "الحاسبات والعملات", "🧮", "#3B82F6", "حساب ذهب وعملات ووحدات"),
    FINANCE("finance", "المالية والأعمال", "💼", "#F59E0B", "خصم وقروض وضرائب"),
    DATES("dates", "التواريخ والمناسبات", "📅", "#EC4899", "وقت وتاريخ ومناسبات"),
    HEALTH("health", "الصحة", "❤️", "#EF4444", "كتلة الجسم والدورة"),
    VEHICLE("vehicle", "السيارات والوقود", "🚗", "#6366F1", "استهلاك وكفاءة السيارة"),
    UTILITY("utility", "أدوات متنوعة", "🔧", "#64748B", "أدوات مساعدة أخرى")
}

enum class CalcKey(
    val id: String,
    val title: String,
    val icon: String,
    val category: CategoryKey,
    val badge: String? = null,
    val keywords: List<String>
) {
    HOME("home", "الرئيسية", "🏠", CategoryKey.FEATURED, null, listOf("رئيسية", "home", "لوحة", "شاشة")),

    // ⭐ Featured
    AI("ai", "المساعد الذكي", "✨", CategoryKey.FEATURED, "AI", listOf("ذكاء", "ai", "chat", "دردشة", "مساعد")),
    LIVE_PRICES("live-prices", "الأسعار الحية", "📊", CategoryKey.FEATURED, "LIVE", listOf("أسعار", "ذهب", "فضة", "عملات", "نفط")),
    ECONOMIC_INDICATORS("economic-indicators", "مؤشرات الاقتصاد والخبير", "📈", CategoryKey.FEATURED, "AI", listOf("اقتصاد", "مؤشرات", "بورصة", "تضخم", "نمو", "استثمار", "تحليل")),
    WEATHER("weather", "تحليل الطقس والمناخ", "🌤️", CategoryKey.FEATURED, "NEW", listOf("طقس", "حرارة", "مناخ", "مطرة", "رياح", "توقعات")),

    // 🕌 Islamic
    PRAYER("prayer", "مواقيت الصلاة", "🕌", CategoryKey.ISLAMIC, null, listOf("صلاة", "أذان", "فجر", "ظهر", "عصر", "مغرب", "عشاء")),
    QIBLA("qibla", "اتجاه القبلة", "🕋", CategoryKey.ISLAMIC, "NEW", listOf("قبلة", "كعبة", "بوصلة", "اتجاه")),
    ADHKAR("adhkar", "أذكار الصباح والمساء", "📿", CategoryKey.ISLAMIC, "NEW", listOf("أذكار", "ذكر", "صباح", "مساء")),
    TASBIH("tasbih", "المسبحة الرقمية", "🕊️", CategoryKey.ISLAMIC, "NEW", listOf("تسبيح", "مسبحة", "سبحان")),
    QURAN("quran", "القرآن الكريم", "📖", CategoryKey.ISLAMIC, "NEW", listOf("قرآن", "سورة", "آية", "مصحف")),
    ZAKAT("zakat", "حاسبة الزكاة", "💎", CategoryKey.ISLAMIC, null, listOf("زكاة", "نصاب", "مال", "ذهب")),

    // 🧮 Calculators & Currencies
    BASIC("basic", "الآلة الحاسبة", "🧮", CategoryKey.CALC, null, listOf("حساب", "رياضيات", "علمية")),
    CURRENCY("currency", "محول العملات", "💱", CategoryKey.CALC, null, listOf("عملة", "دولار", "يورو", "جنيه", "ريال")),
    GOLD("gold", "حاسبة الذهب", "🥇", CategoryKey.CALC, "HOT", listOf("ذهب", "فضة", "عيار", "قيراط")),
    UNIT("unit", "محول الوحدات", "📏", CategoryKey.CALC, null, listOf("وحدات", "كم", "ميل", "كيلو", "جرام")),

    // 💼 Finance
    DISCOUNT("discount", "الخصم", "🏷️", CategoryKey.FINANCE, null, listOf("خصم", "تخفيض")),
    LOAN("loan", "القروض", "🏦", CategoryKey.FINANCE, null, listOf("قرض", "قسط", "فائدة")),
    SAVINGS("savings", "التوفير والفوائد", "💰", CategoryKey.FINANCE, null, listOf("ادخار", "توفير", "فائدة")),
    SALES_TAX("sales-tax", "ضريبة المبيعات", "🧾", CategoryKey.FINANCE, null, listOf("ضريبة", "vat")),
    TIP("tip", "البقشيش", "💵", CategoryKey.FINANCE, null, listOf("بقشيش", "فاتورة")),
    PERCENT("percent", "النسبة المئوية", "%", CategoryKey.FINANCE, null, listOf("نسبة", "مئوية")),
    UNIT_PRICE("unit-price", "سعر الوحدة", "🛒", CategoryKey.FINANCE, null, listOf("سعر", "مقارنة", "منتجات")),

    // 📅 Dates & Time
    WORLD_TIME("world-time", "التوقيت العالمي", "🌍", CategoryKey.DATES, null, listOf("وقت", "ساعة", "مدن")),
    DATE("date", "حاسبة التاريخ", "📅", CategoryKey.DATES, null, listOf("تاريخ", "فرق", "أيام")),
    AGE("age", "حاسبة العمر", "🎂", CategoryKey.DATES, null, listOf("عمر", "ميلاد", "سن")),
    COUNTDOWN("countdown", "العد التنازلي", "⏰", CategoryKey.DATES, null, listOf("مناسبة", "عد", "مؤقت")),

    // 🏥 Health
    HEALTH("health", "الصحة (BMI/BMR)", "❤️", CategoryKey.HEALTH, null, listOf("كتلة", "وزن", "طول", "سعرات", "bmi")),
    OVULATION("ovulation", "الإباضة والدورة", "🌸", CategoryKey.HEALTH, null, listOf("إباضة", "دورة", "خصوبة")),

    // 🚗 Vehicle
    FUEL_COST("fuel-cost", "تكلفة الوقود", "⛽", CategoryKey.VEHICLE, null, listOf("وقود", "بنزين", "سفر")),
    FUEL_EFF("fuel-eff", "كفاءة الوقود", "🚗", CategoryKey.VEHICLE, null, listOf("كفاءة", "سيارة", "استهلاك")),

    // 🔧 Utility
    NUM_WORDS("num-words", "تفقيط الأرقام", "✍️", CategoryKey.UTILITY, null, listOf("تفقيط", "كلمات", "حروف", "شيك")),
    GPA("gpa", "المعدل التراكمي", "🎓", CategoryKey.UTILITY, null, listOf("gpa", "معدل", "دراسة", "جامعة")),
    HEX("hex", "محول سداسي عشري", "#", CategoryKey.UTILITY, null, listOf("hex", "ثنائي", "عشري", "binary"))
}
