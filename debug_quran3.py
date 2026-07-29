import re
ayah = "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَـٰنِ ٱلرَّحِیمِ الۤمۤ"
words = re.split(r'\s+', ayah.strip())
first4 = " ".join(words[:4])
clean = re.sub(r'[\u0610-\u061A\u064B-\u065F\u06D6-\u06ED\u0670\u0640]', '', first4)
clean = clean.replace("ٱ", "ا").replace("ی", "ي").replace("ى", "ي")
print("Cleaned:", repr(clean))
if clean == "بسم الله الرحمن الرحيم":
    print("MATCH!")
