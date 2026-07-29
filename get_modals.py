with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

start = content.find("fun ThemeSelectorModal")
print(content[start:])
