import re
with open("app/src/main/java/com/example/ui/screens/EconomicAndWeatherScreens.kt", "r") as f:
    content = f.read()

start = content.find("fun WeatherScreen(colors: CustomThemeColors)")
end = content.find("fun EconomicIndicatorsScreen(", start)
with open("weather_orig.txt", "w") as f2:
    f2.write(content[start:end])
