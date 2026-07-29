kotlin_code = """
fun stripBismillahIfPresent(ayahText: String): String {
    var text = ayahText.trim()
    val bismillahVariations = listOf(
        "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
        "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
        "بسم الله الرحمن الرحيم",
        "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ"
    )
    for (b in bismillahVariations) {
        if (text.startsWith(b)) {
            text = text.substring(b.length).trim()
            // If it starts with space or some other characters, remove them
            return text
        }
    }
    // Fallback regex approach
    val diacriticsRegex = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u06D6-\\u06ED\\u0670]")
    val words = text.split(Regex("\\\\s+"))
    if (words.size >= 4) {
        val expectedBase = listOf("بسم", "الله", "الرحمن", "الرحيم")
        val actualBase = words.take(4).map { 
            diacriticsRegex.replace(it, "").replace("ٱ", "ا") 
        }
        if (actualBase == expectedBase) {
            return words.drop(4).joinToString(" ").trim()
        }
    }
    return text
}
"""
print(kotlin_code)
