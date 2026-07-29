import urllib.request
import json

url = "https://api.alquran.cloud/v1/surah/2"
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
try:
    with urllib.request.urlopen(req) as response:
        data = json.loads(response.read().decode())
        ayah = data['data']['ayahs'][0]['text']
        print(repr(ayah))
except Exception as e:
    print("Error:", e)
