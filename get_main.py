with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

start = content.find("fun ModernFeatureCard(")
end = content.find("fun AppContent(", start)
if start == -1:
    print(content)
else:
    print(content[start:end])
