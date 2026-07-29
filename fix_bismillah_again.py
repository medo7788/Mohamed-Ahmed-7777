with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "r") as f:
    lines = f.readlines()

start = -1
for i, line in enumerate(lines):
    if line.startswith("fun stripBismillahIfPresent("):
        start = i
        break

end = start
braces = 0
for i in range(start, len(lines)):
    braces += lines[i].count("{") - lines[i].count("}")
    if braces == 0:
        end = i
        break

new_func = """fun stripBismillahIfPresent(ayahText: String): String {
    var text = ayahText.trim()
    val knownPrefixes = listOf(
        "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَـٰنِ ٱلرَّحِیمِ",
        "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
        "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
        "بسم الله الرحمن الرحيم",
        "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ"
    )
    for (prefix in knownPrefixes) {
        if (text.startsWith(prefix)) {
            text = text.substring(prefix.length).trim()
            // Some apis might have weird invisible chars or an extra space, trim again
            return text
        }
    }
    
    // Fallback: If it starts with bismi, remove up to 4 words.
    // We will do a generic diacritic strip to check.
    val diacriticRegex = Regex("[\\\\p{Mn}\\\\p{Me}\\\\u0640]+")
    val cleanText = text.replace(diacriticRegex, "")
        .replace("ٱ", "ا")
        .replace("ی", "ي")
        .replace("ى", "ي")
        
    if (cleanText.startsWith("بسم الله الرحمن الرحيم")) {
        // Find the index of the 4th space in the original string? It's risky.
        // Let's just find the first character of the next word.
        val words = text.split(Regex("\\\\s+"))
        if (words.size > 4) {
            return words.drop(4).joinToString(" ").trim()
        }
    }
    return text
}
"""

with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "w") as f:
    f.writelines(lines[:start])
    f.write(new_func)
    f.writelines(lines[end+1:])
