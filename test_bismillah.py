import re

def strip_bismillah(text):
    # Remove all diacritics and small characters
    text_clean = re.sub(r'[\u0610-\u061A\u064B-\u065F\u06D6-\u06ED\u0670]', '', text)
    # Also replace Alef Wasla with Alef, etc if needed, but let's just do a substring match
    if "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ" in text or "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ" in text:
        return True
    
    # Let's check with regex matching the exact prefixes from quran api
    prefixes = [
        "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ ",
        "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ ",
        "بسم الله الرحمن الرحيم "
    ]
    for p in prefixes:
        if text.startswith(p):
            return text[len(p):]
            
    # Or more robustly:
    # Bismillah length in characters usually around 38-40.
    return text

print(strip_bismillah("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ ٱلْحَمْدُ لِلَّهِ ٱلَّذِى خَلَقَ"))
