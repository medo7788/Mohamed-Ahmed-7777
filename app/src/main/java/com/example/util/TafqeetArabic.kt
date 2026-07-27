package com.example.util

import java.math.BigDecimal

object TafqeetArabic {

    private val ones = arrayOf(
        "", "واحد", "اثنان", "ثلاثة", "أربعة", "خمسة", "ستة", "سبعة", "ثمانية", "تسعة", "عشرة",
        "أحد عشر", "إثنا عشر", "ثلاثة عشر", "أربعة عشر", "خمسة عشر", "ستة عشر", "سبعة عشر", "ثمانية عشر", "تسعة عشر"
    )

    private val tens = arrayOf(
        "", "", "عشرون", "ثلاثون", "أربعون", "خمسون", "ستون", "سبعون", "ثمانون", "تسعون"
    )

    private val hundreds = arrayOf(
        "", "مائة", "مائتان", "ثلاثمائة", "أربعمائة", "خمسماءة", "ستمائة", "سبعمائة", "ثمانمائة", "تسعمائة"
    )

    fun convertToWords(number: Double, currency: String = "EGP"): String {
        if (number == 0.0) return "صفر"

        val big = BigDecimal.valueOf(number)
        val integerPart = big.toLong()
        val decimalPart = ((number - integerPart) * 100).toInt()

        val currencyUnits = when (currency) {
            "SAR" -> Pair("ريال سعودي", "هللة")
            "AED" -> Pair("درهم إماراتي", "فلس")
            "KWD" -> Pair("دينار كويتي", "فلس")
            "USD" -> Pair("دولار أمريكي", "سنت")
            "EUR" -> Pair("يورو", "سنت")
            else -> Pair("جنيه مصري", "قرش")
        }

        var result = convertGroup(integerPart) + " " + currencyUnits.first

        if (decimalPart > 0) {
            result += " و " + convertGroup(decimalPart.toLong()) + " " + currencyUnits.second
        }

        return "فقط " + result.trim() + " لا غير"
    }

    private fun convertGroup(n: Long): String {
        if (n == 0L) return ""
        if (n < 20L) return ones[n.toInt()]
        if (n < 100L) {
            val unit = (n % 10).toInt()
            val ten = (n / 10).toInt()
            return if (unit == 0) tens[ten] else "${ones[unit]} و${tens[ten]}"
        }
        if (n < 1000L) {
            val hundred = (n / 100).toInt()
            val remainder = n % 100
            return if (remainder == 0L) hundreds[hundred] else "${hundreds[hundred]} و${convertGroup(remainder)}"
        }
        if (n < 1000000L) {
            val thousands = n / 1000
            val remainder = n % 1000
            val thousandStr = when (thousands) {
                1L -> "ألف"
                2L -> "ألفان"
                in 3L..10L -> "${ones[thousands.toInt()]} آلاف"
                else -> "${convertGroup(thousands)} ألفاً"
            }
            return if (remainder == 0L) thousandStr else "$thousandStr و${convertGroup(remainder)}"
        }
        if (n < 1000000000L) {
            val millions = n / 1000000
            val remainder = n % 1000000
            val millionStr = when (millions) {
                1L -> "مليون"
                2L -> "مليونان"
                in 3L..10L -> "${ones[millions.toInt()]} ملايين"
                else -> "${convertGroup(millions)} مليوناً"
            }
            return if (remainder == 0L) millionStr else "$millionStr و${convertGroup(remainder)}"
        }

        return n.toString()
    }
}
