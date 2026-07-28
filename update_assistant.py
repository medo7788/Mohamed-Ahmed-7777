import re
with open('./app/src/main/java/com/example/ui/screens/FeaturedScreens.kt', 'r', encoding='utf-8') as f:
    text = f.read()

# Make the chat messages look more professional and add long press to copy
import_gestures = "import androidx.compose.foundation.gestures.detectTapGestures\nimport androidx.compose.ui.input.pointer.pointerInput"
if "detectTapGestures" not in text:
    text = text.replace("import androidx.compose.foundation.layout.*", import_gestures + "\nimport androidx.compose.foundation.layout.*")

chat_msg_pattern = r'''Surface\(\s*color = if \(isAi\) colors\.surface2 else colors\.accent,[\s\S]*?modifier = Modifier\.widthIn\(max = 280\.dp\)[\s\S]*?\{[\s\S]*?if \(isAi\) \{[\s\S]*?Icon\([\s\S]*?\}\s*\}\s*\}\s*\}'''

replacement = '''Surface(
                        color = if (isAi) colors.surface2 else colors.accent,
                        shape = RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = if (isAi) 4.dp else 20.dp,
                            bottomEnd = if (isAi) 20.dp else 4.dp
                        ),
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = {
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(msg.text))
                                        android.widget.Toast.makeText(context, "تم نسخ الرسالة", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text(
                                text = msg.text,
                                fontSize = 15.sp,
                                lineHeight = 24.sp,
                                color = if (isAi) colors.text else Color.White
                            )
                        }
                    }'''

text = re.sub(chat_msg_pattern, replacement, text)

# Just to make sure we don't have the old selection container
text = text.replace("androidx.compose.foundation.text.selection.SelectionContainer {", "")
text = text.replace("} // End SelectionContainer", "")

with open('./app/src/main/java/com/example/ui/screens/FeaturedScreens.kt', 'w', encoding='utf-8') as f:
    f.write(text)
