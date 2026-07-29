import re

with open("app/src/main/java/com/example/ui/screens/EconomicAndWeatherScreens.kt", "r") as f:
    content = f.read()

# Fix conflicting imports
lines = content.split('\n')
new_lines = []
imports_seen = set()
for line in lines:
    if line.startswith('import '):
        if line in imports_seen:
            continue
        imports_seen.add(line)
    new_lines.append(line)
content = '\n'.join(new_lines)

# Fix CityLocationInfo -> WeatherCity
content = content.replace('CityLocationInfo(locName ?: "موقعي", "My Location", "🌍", result.lat, result.lng)', 'WeatherCity(locName ?: "موقعي", "موقعي", result.lat, result.lng, "📍")')

# Fix fetchWeather -> fetchRealWeather
content = content.replace('WeatherRepository.fetchWeather(selectedCity.lat, selectedCity.lng)', 'WeatherRepository.fetchRealWeather(context, selectedCity.lat, selectedCity.lng)')

# Fix currentTemp -> tempC
content = content.replace('weather.currentTemp', 'weather.tempC')

with open("app/src/main/java/com/example/ui/screens/EconomicAndWeatherScreens.kt", "w") as f:
    f.write(content)
