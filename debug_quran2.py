import re
ayah = "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَـٰنِ ٱلرَّحِیمِ الۤمۤ\n"

# Remove whitespace at start/end
text = ayah.strip()
print("Original:", repr(text))

# Split by spaces or newlines
words = re.split(r'\s+', text)
print("Words:", repr(words))

if len(words) >= 4:
    first4 = " ".join(words[:4])
    # Strip diacritics AND tatweel
    clean = re.sub(r'[\u0610-\u061A\u064B-\u065F\u06D6-\u06ED\u0670\u0640]', '', first4)
    # Replace alef wasla with normal alef
    clean = clean.replace("ٱ", "ا")
    print("Cleaned first 4:", repr(clean))
    
    if clean == "بسم الله الرحمن الرحيم":
        print("MATCH! Remaining:", repr(" ".join(words[4:])))
    else:
        print("NO MATCH")
