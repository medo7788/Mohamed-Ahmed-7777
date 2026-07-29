package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class EconomicChatMessage(val text: String, val isUser: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EconomicAndWeatherScreen(
    onAskExpert: suspend (String, String) -> Result<String>
) {
    val countries = remember { listOf("مصر 🇪🇬", "السعودية 🇸🇦", "الإمارات 🇦🇪", "الكويت 🇰🇼", "قطر 🇶🇦") }
    var selectedCountry by remember { mutableStateOf(countries[0]) }
    var expandedCountryDropdown by remember { mutableStateOf(false) }
    
    var userQuery by remember { mutableStateOf("") }
    val chatMessages = remember { mutableStateListOf<EconomicChatMessage>() }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .imePadding()
    ) {
        // Dashboard Panel & Country Selector
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "المؤشر الاقتصادي:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Box {
                    OutlinedButton(
                        onClick = { expandedCountryDropdown = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = selectedCountry,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    DropdownMenu(
                        expanded = expandedCountryDropdown,
                        onDismissRequest = { expandedCountryDropdown = false }
                    ) {
                        countries.forEach { country ->
                            DropdownMenuItem(
                                text = { Text(country, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                onClick = {
                                    selectedCountry = country
                                    expandedCountryDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // LazyColumn Chat Stream
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(chatMessages) { message ->
                ChatBubble(message = message)
            }
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    }
                }
            }
        }

        // Chat Input Controls
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = userQuery,
                    onValueChange = { userQuery = it },
                    placeholder = { Text("اسأل الخبير الاقتصادي...") },
                    modifier = Modifier.weight(1f),
                    maxLines = 3,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )

                IconButton(
                    onClick = {
                        if (userQuery.isNotBlank() && !isLoading) {
                            val prompt = userQuery
                            chatMessages.add(EconomicChatMessage(prompt, isUser = true))
                            userQuery = ""
                            isLoading = true

                            coroutineScope.launch {
                                val result = onAskExpert(prompt, selectedCountry)
                                isLoading = false
                                result.onSuccess { responseText ->
                                    chatMessages.add(EconomicChatMessage(responseText, isUser = false))
                                }.onFailure { error ->
                                    chatMessages.add(EconomicChatMessage("حدث خطأ أثناء الاتصال: ${error.localizedMessage}", isUser = false))
                                }
                            }
                        }
                    },
                    enabled = userQuery.isNotBlank() && !isLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "إرسال",
                        tint = if (userQuery.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: EconomicChatMessage) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Surface(
            color = bgColor,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 2.dp,
                bottomEnd = if (message.isUser) 2.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Text(
                text = message.text,
                color = textColor,
                fontSize = 14.sp,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}
