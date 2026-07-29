import urllib.request
import json
import re

url = "https://api.alquran.cloud/v1/surah/2"
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
try:
    with urllib.request.urlopen(req) as response:
        data = json.loads(response.read().decode())
        ayah = data['data']['ayahs'][0]['text']
        print("EXACT AYAH 1 TEXT:", repr(ayah))
        print("Length:", len(ayah))
        
        bismillah1 = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"
        print("Starts with bismillah1?", ayah.startswith(bismillah1))
        
        words = ayah.split(" ")
        print("Split spaces:", repr(words[:5]))
        
except Exception as e:
    print("Error:", e)
