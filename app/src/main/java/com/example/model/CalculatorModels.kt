package com.example.model

enum class CategoryKey(
    val id: String,
    val label: String,
    val icon: String,
    val colorHex: String,
    val description: String
) {
    ISLAMIC("islamic", "العبادات والقرآن", "🕌", "#10B981", "مواقيت الصلاة، اتجاه القبلة، الأذكار والمسبحة والقرآن وحاسبة الزكاة"),
    FINANCE("finance", "المال والأسعار والحاسبات", "💰", "#F59E0B", "الآلة الحاسبة، محول العملات والذهب والأسعار الفورية ومؤشرات الاقتصاد والخصومات والقروض"),
    DATE_TIME("dates", "الوقت والتواريخ", "📅", "#C084FC", "التوقيت العالمي، حاسبة العمر وفروق التواريخ ومؤقت العد التنازلي"),
    HEALTH("health", "الصحة واللياقة", "❤️", "#EF4444", "حساب كتلة وسعرات الجسم ومتابعة فترات الدورة الشهرية والإباضة"),
    UTILITIES("utilities", "أدوات عملية ومساعدة", "🔧", "#64748B", "تفقيط الأرقام وكتابتها بالحروف، حساب المعدل التراكمي ومحول البرمجة سداسي عشري وحاسبات الوقود للسيارات")
}

enum class CalcKey(
    val id: String,
    val title: String,
    val icon: String,
    val category: CategoryKey,
    val badge: String? = null,
    val keywords: List<String>
) {
    HOME("home", "الرئيسية", "🏠", CategoryKey.UTILITIES, null, listOf("رئيسية", "home", "لوحة", "شاشة")),

    // AI & Weather (integrated directly on home but kept in enum)
    AI("ai", "المساعد الذكي", "✨", CategoryKey.UTILITIES, "AI", listOf("ذكاء", "ai", "chat", "دردشة", "مساعد")),
    WEATHER("weather", "تحليل الطقس والمناخ", "🌤️", CategoryKey.UTILITIES, null, listOf("طقس", "حرارة", "مناخ", "مطرة", "رياح", "توقعات")),

    // 🕌 Islamic
    PRAYER("prayer", "مواقيت الصلاة", "🕌", CategoryKey.ISLAMIC, null, listOf("صلاة", "أذان", "فجر", "ظهر", "عصر", "مغرب", "عشاء")),
    QIBLA("qibla", "اتجاه القبلة", "🕋", CategoryKey.ISLAMIC, null, listOf("قبلة", "كعبة", "بوصلة", "اتجاه")),
    ADHKAR("adhkar", "أذكار الصباح والمساء", "📿", CategoryKey.ISLAMIC, null, listOf("أذكار", "ذكر", "صباح", "مساء")),
    TASBIH("tasbih", "المسبحة الرقمية", "🕊️", CategoryKey.ISLAMIC, null, listOf("تسبيح", "مسبحة", "سبحان")),
    QURAN("quran", "القرآن الكريم", "📖", CategoryKey.ISLAMIC, null, listOf("قرآن", "سورة", "آية", "مصحف")),
    ZAKAT("zakat", "حاسبة الزكاة", "💎", CategoryKey.ISLAMIC, null, listOf("زكاة", "نصاب", "مال", "ذهب")),
    ADHAN_SETTINGS("adhan-settings", "إعدادات الأذان", "🔔", CategoryKey.ISLAMIC, null, listOf("أذان", "إشعارات", "صوت", "اهتزاز", "تنبيه")),

    // 💰 Calculators, Money & Currencies
    BASIC("basic", "الآلة الحاسبة", "🧮", CategoryKey.FINANCE, null, listOf("حساب", "رياضيات", "علمية")),
    CURRENCY("currency", "محول العملات", "💱", CategoryKey.FINANCE, null, listOf("عملة", "دولار", "يورو", "جنيه", "ريال")),
    GOLD("gold", "حاسبة الذهب", "🥇", CategoryKey.FINANCE, null, listOf("ذهب", "فضة", "عيار", "قيراط")),
    UNIT("unit", "محول الوحدات", "📏", CategoryKey.FINANCE, null, listOf("وحدات", "كم", "ميل", "كيلو", "جرام")),
    LIVE_PRICES("live-prices", "الأسعار الحية", "📊", CategoryKey.FINANCE, "LIVE", listOf("أسعار", "ذهب", "فضة", "عملات", "نفط")),
    ECONOMIC_INDICATORS("economic-indicators", "مؤشرات الاقتصاد والخبير", "📈", CategoryKey.FINANCE, "AI", listOf("اقتصاد", "مؤشرات", "بورصة", "تضخم", "نمو", "استثمار", "تحليل")),
    DISCOUNT("discount", "الخصم والتخفيض", "🏷️", CategoryKey.FINANCE, null, listOf("خصم", "تخفيض")),
    LOAN("loan", "حاسبة القروض", "🏦", CategoryKey.FINANCE, null, listOf("قرض", "قسط", "فائدة")),
    SAVINGS("savings", "التوفير والفوائد", "💰", CategoryKey.FINANCE, null, listOf("ادخار", "توفير", "فائدة")),
    SALES_TAX("sales-tax", "ضريبة المبيعات", "🧾", CategoryKey.FINANCE, null, listOf("ضريبة", "vat")),
    TIP("tip", "حاسبة البقشيش", "💵", CategoryKey.FINANCE, null, listOf("بقشيش", "فاتورة")),
    PERCENT("percent", "النسبة المئوية", "%", CategoryKey.FINANCE, null, listOf("نسبة", "مئوية")),
    UNIT_PRICE("unit-price", "سعر الوحدة", "🛒", CategoryKey.FINANCE, null, listOf("سعر", "مقارنة", "منتجات")),

    // 📅 Dates & Time
    WORLD_TIME("world-time", "التوقيت العالمي", "🌍", CategoryKey.DATE_TIME, null, listOf("وقت", "ساعة", "مدن")),
    DATE("date", "حاسبة التاريخ", "📅", CategoryKey.DATE_TIME, null, listOf("تاريخ", "فرق", "أيام")),
    AGE("age", "حاسبة العمر", "🎂", CategoryKey.DATE_TIME, null, listOf("عمر", "ميلاد", "سن")),
    COUNTDOWN("countdown", "العد التنازلي", "⏰", CategoryKey.DATE_TIME, null, listOf("مناسبة", "عد", "مؤقت")),

    // ❤️ Health & Fitness
    HEALTH("health", "الصحة (BMI/BMR)", "❤️", CategoryKey.HEALTH, null, listOf("كتلة", "وزن", "طول", "سعرات", "bmi")),
    OVULATION("ovulation", "الإباضة والدورة", "🌸", CategoryKey.HEALTH, null, listOf("إباضة", "دورة", "خصوبة")),

    // 🔧 Utilities & Practical Tools
    FUEL_COST("fuel-cost", "تكلفة الوقود", "⛽", CategoryKey.UTILITIES, null, listOf("وقود", "بنزين", "سفر")),
    FUEL_EFF("fuel-eff", "كفاءة الوقود", "🚗", CategoryKey.UTILITIES, null, listOf("كفاءة", "سيارة", "استهلاك")),
    NUM_WORDS("num-words", "تفقيط الأرقام", "✍️", CategoryKey.UTILITIES, null, listOf("تفقيط", "كلمات", "حروف", "شيك")),
    GPA("gpa", "المعدل التراكمي", "🎓", CategoryKey.UTILITIES, null, listOf("gpa", "معدل", "دراسة", "جامعة")),
    HEX("hex", "محول سداسي عشري", "#", CategoryKey.UTILITIES, null, listOf("hex", "ثنائي", "عشري", "binary"))
}
