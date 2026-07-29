with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "r") as f:
    lines = f.readlines()

start = -1
for i, line in enumerate(lines):
    if line.startswith("fun stripBismillahIfPresent("):
        start = i
        break

if start != -1:
    end = start
    braces = 0
    for i in range(start, len(lines)):
        braces += lines[i].count("{") - lines[i].count("}")
        if braces == 0:
            end = i
            break
    
    new_func = """fun stripBismillahIfPresent(ayahText: String): String {
    val text = ayahText.trim()
    val bismillahVariations = listOf(
        "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
        "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
        "بسم الله الرحمن الرحيم",
        "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ",
        "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَـٰنِ ٱلرَّحِیمِ"
    )
    for (b in bismillahVariations) {
        if (text.startsWith(b)) {
            return text.substring(b.length).trim()
        }
    }
    
    val diacriticsRegex = Regex("[\\\\u0610-\\\\u061A\\\\u064B-\\\\u065F\\\\u06D6-\\\\u06ED\\\\u0670\\\\u0640]")
    val words = text.split(Regex("\\\\s+"))
    if (words.size >= 4) {
        val expectedBase = "بسم الله الرحمن الرحيم"
        val actualBase = words.take(4).joinToString(" ")
            .replace(diacriticsRegex, "")
            .replace("ٱ", "ا")
            .replace("ی", "ي")
            .replace("ى", "ي")
        
        if (actualBase == expectedBase) {
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
    print("Replaced stripBismillahIfPresent")
else:
    print("Function not found")
