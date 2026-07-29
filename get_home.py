with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "r") as f:
    content = f.read()

start = content.find("fun HomeScreen(")
end = content.find("fun CalcCard(", start)
print(content[start:end])
