import re
with open('./app/src/main/java/com/example/data/GeminiRepository.kt', 'r', encoding='utf-8') as f:
    text = f.read()

text = text.replace('return buildKey.trim()\n        return ""', 'return buildKey.trim()\n        return "AIzaSyD3pTDbGJlv9yTn40lkDvtAl12W6pdkXJc"')

with open('./app/src/main/java/com/example/data/GeminiRepository.kt', 'w', encoding='utf-8') as f:
    f.write(text)
