package com.example.data

import android.content.Context
import com.example.data.GeminiRepository

data class CountryEconomicData(
    val code: String,
    val nameAr: String,
    val flag: String,
    val currency: String,
    val gdpGrowth: String, // e.g. "+3.8%"
    val inflationRate: String, // e.g. "27.5%"
    val interestRate: String, // e.g. "27.25%"
    val unemployment: String, // e.g. "6.7%"
    val stockExchangeName: String, // e.g. "EGX 30"
    val stockExchangeValue: String, // e.g. "29,850.4"
    val stockExchangeChange: String, // e.g. "+1.2%"
    val debtToGdp: String, // e.g. "88%"
    val centralBankReserves: String, // e.g. "$46.1B"
    val mainExport: String, // e.g. "الغاز الطبيعي، النفط، الخدمات"
    val economicSummaryAr: String
)

object EconomicRepository {

    val countries = listOf(
        CountryEconomicData(
            code = "EG",
            nameAr = "مصر",
            flag = "🇪🇬",
            currency = "EGP",
            gdpGrowth = "+3.8%",
            inflationRate = "27.1%",
            interestRate = "27.25%",
            unemployment = "6.7%",
            stockExchangeName = "مؤشر البورصة (EGX30)",
            stockExchangeValue = "29,480.2",
            stockExchangeChange = "+0.85%",
            debtToGdp = "89.2%",
            centralBankReserves = "$46.4B",
            mainExport = "الغاز، المحاصيل، المنسوجات، قناة السويس",
            economicSummaryAr = "يمر الاقتصاد المصري بمرحلة إحياء وهيكلة عقب تحرير سعر الصرف، مع نمو صافي الاحتياطي الأجنبي وتدفق الاستثمارات المباشرة في رأس الحكمة والقطاعات السياحية والعقارية."
        ),
        CountryEconomicData(
            code = "SA",
            nameAr = "المملكة العربية السعودية",
            flag = "🇸🇦",
            currency = "SAR",
            gdpGrowth = "+4.4%",
            inflationRate = "1.6%",
            interestRate = "5.50%",
            unemployment = "7.6%",
            stockExchangeName = "مؤشر تداول (TASI)",
            stockExchangeValue = "11,850.6",
            stockExchangeChange = "+0.42%",
            debtToGdp = "26.2%",
            centralBankReserves = "$452.0B",
            mainExport = "النفط، البتروكيماويات، التعدين",
            economicSummaryAr = "اقتصاد صلب مدعوم برؤية 2030 وتوسع الأنشطة غير النفطية. معدل تضخم هو الأدنى إقليمياً واستثمارات هائلة في مشاريع المشاريع الكبرى والتقنية والمناطق اللوجستية."
        ),
        CountryEconomicData(
            code = "AE",
            nameAr = "الإمارات العربية المتحدة",
            flag = "🇦🇪",
            currency = "AED",
            gdpGrowth = "+3.9%",
            inflationRate = "2.1%",
            interestRate = "4.90%",
            unemployment = "2.8%",
            stockExchangeName = "سوق دبي المالي (DFM)",
            stockExchangeValue = "4,520.1",
            stockExchangeChange = "+1.12%",
            debtToGdp = "30.0%",
            centralBankReserves = "$168.5B",
            mainExport = "الخدمات المالية، العقارات، التجارة، النفط",
            economicSummaryAr = "مركز مالي وتجاري عالمي فريد. تعتمد الدولة على التنويع الاقتصادي القوي والتكنولوجيا وسوق عقاري وتجاري هو الأكثر نشاطاً في الشرق الأوسط."
        ),
        CountryEconomicData(
            code = "KW",
            nameAr = "الكويت",
            flag = "🇰🇼",
            currency = "KWD",
            gdpGrowth = "+2.8%",
            inflationRate = "2.8%",
            interestRate = "4.25%",
            unemployment = "2.1%",
            stockExchangeName = "بورصة الكويت (الأول)",
            stockExchangeValue = "7,840.3",
            stockExchangeChange = "-0.15%",
            debtToGdp = "12.5%",
            centralBankReserves = "$48.2B",
            mainExport = "النفط الخام، المكررات النفطية",
            economicSummaryAr = "مصدّر رئيسي للطاقة مع أطول فائض مالي واحتياطي أجيال قادمة يناهز 800 مليار دولار، مع توجه لتطوير المشروعات التنموية ومشاريع البنية التحتية."
        ),
        CountryEconomicData(
            code = "QA",
            nameAr = "قطر",
            flag = "🇶🇦",
            currency = "QAR",
            gdpGrowth = "+2.5%",
            inflationRate = "1.2%",
            interestRate = "5.25%",
            unemployment = "0.1%",
            stockExchangeName = "بورصة قطر (QSI)",
            stockExchangeValue = "10,120.8",
            stockExchangeChange = "+0.30%",
            debtToGdp = "41.5%",
            centralBankReserves = "$67.3B",
            mainExport = "الغاز الطبيعي المسال (LNG)، البتروكيماويات",
            economicSummaryAr = "أحد أكبر مصدري الغاز المسال في العالم. تتمتع الدولة بأعلى دخل فردي واستثمارات ضخمة في توسعة حقل الشمال للغاز للرفع القوي لإيرادات الدولة."
        ),
        CountryEconomicData(
            code = "JO",
            nameAr = "الأردن",
            flag = "🇯🇴",
            currency = "JOD",
            gdpGrowth = "+2.6%",
            inflationRate = "1.8%",
            interestRate = "7.50%",
            unemployment = "21.4%",
            stockExchangeName = "بورصة عمان (ASE)",
            stockExchangeValue = "2,410.5",
            stockExchangeChange = "+0.10%",
            debtToGdp = "88.8%",
            centralBankReserves = "$19.1B",
            mainExport = "الفوسفات، البوتاس، الأدوية، السياحة",
            economicSummaryAr = "اقتصاد متماسك يدعمه القطاع المصرفي المستقر والصادرات الفوسفاتية والتحويلات الخارجية، مع مساع لخفض نسب البطالة وتسهيل الاستثمار الأجنبي."
        ),
        CountryEconomicData(
            code = "MA",
            nameAr = "المغرب",
            flag = "🇲🇦",
            currency = "MAD",
            gdpGrowth = "+3.2%",
            inflationRate = "1.3%",
            interestRate = "2.75%",
            unemployment = "13.0%",
            stockExchangeName = "بورصة الدار البيضاء (MASI)",
            stockExchangeValue = "13,650.0",
            stockExchangeChange = "+0.68%",
            debtToGdp = "69.5%",
            centralBankReserves = "$36.8B",
            mainExport = "سيارات، الفوسفات ومشتقاته، الفلاحة، السياحة",
            economicSummaryAr = "تحول صناعي ملحوظ خاصة في قطاع السيارات والطائرات والطاقات المتجددة، مع استعدادات تنظيمية كبرى لكأس العالم 2030 تنعش قطاع الإنشاءات."
        ),
        CountryEconomicData(
            code = "US",
            nameAr = "الولايات المتحدة الأمريكية",
            flag = "🇺🇸",
            currency = "USD",
            gdpGrowth = "+2.8%",
            inflationRate = "2.6%",
            interestRate = "5.00%",
            unemployment = "4.1%",
            stockExchangeName = "مؤشر ستاندرد آند بورز (S&P 500)",
            stockExchangeValue = "5,580.2",
            stockExchangeChange = "+0.78%",
            debtToGdp = "122.3%",
            centralBankReserves = "$248.0B",
            mainExport = "التقنية، التمويل، الطاقة، الأسلحة، الطيران",
            economicSummaryAr = "أكبر اقتصاد في العالم ومحرك الأسواق العالمية. سوق عمل قوي وقوة تقنية هائلة مع مراقبة وثيقة لسياسات الفيدرالي وتوجهات أسعار الفائدة."
        ),
        CountryEconomicData(
            code = "TR",
            nameAr = "تركيا",
            flag = "🇹🇷",
            currency = "TRY",
            gdpGrowth = "+3.1%",
            inflationRate = "61.8%",
            interestRate = "50.00%",
            unemployment = "8.8%",
            stockExchangeName = "بورصة إسطنبول (BIST 100)",
            stockExchangeValue = "10,890.4",
            stockExchangeChange = "+1.45%",
            debtToGdp = "31.2%",
            centralBankReserves = "$148.2B",
            mainExport = "السيارات، المنسوجات، الأغذية، السياحة",
            economicSummaryAr = "برنامج اقتصادي تقليدي جديد يهدف لكبح التضخم وإعادة جذب رؤوس الأموال الأجنبية عبر رفع الفائدة وتنمية الصادرات والسياحة."
        )
    )

    fun getCountryByCode(code: String): CountryEconomicData {
        return countries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: countries[0]
    }

    suspend fun getAIEconomicReport(context: Context? = null, country: CountryEconomicData, query: String? = null): String {
        val prompt = if (query.isNullOrBlank()) {
            """
            أنت مستشار اقتصادي وخبير مالي دولي رفيع المستوى.
            قدم تحليلاً شاملاً ودقيقاً للاقتصاد في دولة: ${country.nameAr} (${country.flag}) بناءً على البيانات التالية:
            - معدل نمو الناتج المحلي: ${country.gdpGrowth}
            - نسبة التضخم: ${country.inflationRate}
            - سعر الفائدة البنكي: ${country.interestRate}
            - نسبة البطالة: ${country.unemployment}
            - سوق الأسهم (${country.stockExchangeName}): ${country.stockExchangeValue} (${country.stockExchangeChange})
            - نسبة الدين إلى الناتج المحلي: ${country.debtToGdp}
            - صافي الاحتياطي الأجنبي: ${country.centralBankReserves}
            - أهم الصادرات والأنشطة: ${country.mainExport}

            يرجى تنظيم الإجابة في أجزاء واضحة بالشكل التالي:
            1. 📊 **ملخص وضع السوق واقتصاد الدولة الحالي**
            2. ⚠️ **المخاطر والتحديات الرئيسية (التضخم والفائدة)**
            3. 💡 **فرص الاستثمار والنصائح المالية للمواطنين والمستثمرين**
            4. 🔮 **التوقعات والنظرة المستقبلية للأسواق**

            اجعل الأسلوب مهنياً، ممتعاً، ومباشراً باللغة العربية.
            """.trimIndent()
        } else {
            """
            أنت خبير ومستشار اقتصادي ومالي.
            السؤال أو الاستفسار المالي من المستخدم حول اقتصاد ${country.nameAr} (${country.flag}):
            "$query"

            معطيات أرقام الدولة الحالية:
            تضخم: ${country.inflationRate} | فائدة: ${country.interestRate} | نمو: ${country.gdpGrowth} | بورصة: ${country.stockExchangeValue}

            أجب بطريقة دقيقة، مستندة إلى التحليل المالي السليم، وقدم إرشادات عملية وواضحة.
            """.trimIndent()
        }

        return GeminiRepository.generateContent(context, prompt)
    }
}
