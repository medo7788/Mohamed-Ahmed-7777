package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.CalcKey
import com.example.ui.components.ToolScreenScaffold
import com.example.ui.theme.AppIcons
import com.example.ui.theme.CustomThemeColors

private data class NewsArticle(
    val id: String,
    val title: String,
    val source: String,
    val category: String,
    val timeAgo: String,
    val summary: String,
    val url: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsAndWebBrowserScreen(colors: CustomThemeColors) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var activeTab by remember { mutableStateOf(0) } // 0: News Feed, 1: Browser
    var selectedNewsCategory by remember { mutableStateOf("الكل") }

    // Browser state
    var currentUrl by remember { mutableStateOf("https://www.aljazeera.net") }
    var inputUrl by remember { mutableStateOf("https://www.aljazeera.net") }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isLoadingWeb by remember { mutableStateOf(false) }

    val newsCategories = listOf("الكل", "اقتصاد ومال", "تكنولوجيا", "عالمي", "إسلاميات", "رياضة")

    val mockArticles = remember {
        listOf(
            NewsArticle(
                id = "1",
                title = "ارتفاع أسعار الذهب والأسواق المالية العالمية تترقب قرار أسعار الفائدة",
                source = "الجزيرة نت - الاقتصاد",
                category = "اقتصاد ومال",
                timeAgo = "منذ 15 دقيقة",
                summary = "شهدت أسواق الذهب والعملات الأجنبية تقلبات ملحوظة اليوم بالتزامن مع صدور بيانات التضخم ومؤشرات النمو الاقتصادية الأخيرة.",
                url = "https://www.aljazeera.net/ebusiness"
            ),
            NewsArticle(
                id = "2",
                title = "إطلاق أحدث نماذج الذكاء الاصطناعي التوليدي بقدرات استثنائية في التحليل الكودي",
                source = "عالم التقنية",
                category = "تكنولوجيا",
                timeAgo = "منذ 40 دقيقة",
                summary = "أعلنت شركات التقنية الكبرى عن جيل جديد من نماذج المعالجة اللغوية التي تسهل عمليات تطوير البرمجيات وحل المسائل المتقدمة.",
                url = "https://www.techdir.org"
            ),
            NewsArticle(
                id = "3",
                title = "تطورات الطقس والمناخ: تحذيرات من موجات حرارية وتوصيات بالسلامة العامة",
                source = "طقس العرب",
                category = "عالمي",
                timeAgo = "منذ ساعة",
                summary = "أصدرت هيئات الأرصاد الجوية بيانات تفصيلية حول درجات الحرارة المتوقعة خلال الأيام القادمة وتأثيرها على الحركة والملاحة.",
                url = "https://www.arabiaweather.com"
            ),
            NewsArticle(
                id = "4",
                title = "مواقيت الصلاة والأدعية المستحبة في أيام الشهر الفضيل والمناسبات المباركة",
                source = "الإسلام سؤال وجواب",
                category = "إسلاميات",
                timeAgo = "منذ ساعتين",
                summary = "توجيهات إيمانية وفتاوى شرعية حول تنظيم الأوقات والمحافظة على أذكار الصباح والمساء وأداء العبادات بنظام.",
                url = "https://islamqa.info"
            ),
            NewsArticle(
                id = "5",
                title = "ملخص مباريات وتصفيات البطولات القارية والدوريات العالمية الكبرى",
                source = "في الجول",
                category = "رياضة",
                timeAgo = "منذ 3 ساعات",
                summary = "استعراض تفصيلي لأهم أهداف ونتائج مباريات اليوم في مختلف الملاعب العربية والأوروبية.",
                url = "https://www.filgoal.com"
            )
        )
    }

    val filteredNews = mockArticles.filter {
        selectedNewsCategory == "الكل" || it.category == selectedNewsCategory
    }

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.NEWS_BROWSER),
        title = "تصفح الأخبار والإنترنت",
        subtitle = "متابعة أحدث المستجدات اليومية والتصفح السريع والآمن داخل التطبيق",
        isScrollable = false
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp)
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Tab Header
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = Color(0xFF0F172A),
                    contentColor = Color(0xFF00FFCC)
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Newspaper, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("الأخبار المباشرة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("متصفح الإنترنت", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (activeTab == 0) {
                    // TAB 0: NEWS FEED
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Category Filters
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            newsCategories.forEach { cat ->
                                val sel = selectedNewsCategory == cat
                                FilterChip(
                                    selected = sel,
                                    onClick = { selectedNewsCategory = cat },
                                    label = { Text(cat, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF00FFCC).copy(alpha = 0.25f),
                                        selectedLabelColor = Color(0xFF00FFCC),
                                        containerColor = Color(0xFF1E293B),
                                        labelColor = Color(0xFF94A3B8)
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(filteredNews, key = { it.id }) { article ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    color = Color(0xFF1E293B).copy(alpha = 0.90f),
                                    border = BorderStroke(1.dp, Color(0xFF334155)),
                                    shadowElevation = 6.dp
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF00FFCC).copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = article.category,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF00FFCC),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }

                                            Text(
                                                text = "${article.source} • ${article.timeAgo}",
                                                fontSize = 10.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = article.title,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            lineHeight = 22.sp
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = article.summary,
                                            fontSize = 12.sp,
                                            color = Color(0xFFCBD5E1),
                                            lineHeight = 18.sp
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Button(
                                                onClick = {
                                                    currentUrl = article.url
                                                    inputUrl = article.url
                                                    activeTab = 1
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC), contentColor = Color.Black),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("تصفح المقال بالكامل", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }

                                            IconButton(
                                                onClick = {
                                                    val shareIntent = Intent().apply {
                                                        action = Intent.ACTION_SEND
                                                        putExtra(Intent.EXTRA_TEXT, "${article.title}\n\n${article.url}")
                                                        type = "text/plain"
                                                    }
                                                    context.startActivity(Intent.createChooser(shareIntent, "مشاركة الخبر"))
                                                },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // TAB 1: IN-APP BROWSER
                    Column(modifier = Modifier.fillMaxSize()) {
                        // URL Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = inputUrl,
                                onValueChange = { inputUrl = it },
                                placeholder = { Text("أدخل رابط الموقع...", fontSize = 11.sp, color = Color(0xFF64748B)) },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp)) },
                                trailingIcon = {
                                    if (inputUrl.isNotEmpty()) {
                                        IconButton(onClick = { inputUrl = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "مسح", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00FFCC),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedContainerColor = Color(0xFF1E293B),
                                    unfocusedContainerColor = Color(0xFF0F172A),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Button(
                                onClick = {
                                    var formatted = inputUrl.trim()
                                    if (!formatted.startsWith("http://") && !formatted.startsWith("https://")) {
                                        formatted = "https://$formatted"
                                    }
                                    currentUrl = formatted
                                    webViewInstance?.loadUrl(formatted)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC), contentColor = Color.Black),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(46.dp)
                            ) {
                                Icon(Icons.Default.ArrowForward, contentDescription = "ذهاب", modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Navigation Controls Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = { if (webViewInstance?.canGoBack() == true) webViewInstance?.goBack() },
                                    enabled = canGoBack,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "خلف", tint = if (canGoBack) Color.White else Color(0xFF475569))
                                }

                                IconButton(
                                    onClick = { if (webViewInstance?.canGoForward() == true) webViewInstance?.goForward() },
                                    enabled = canGoForward,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ArrowForward, contentDescription = "أمام", tint = if (canGoForward) Color.White else Color(0xFF475569))
                                }

                                IconButton(
                                    onClick = { webViewInstance?.reload() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = Color(0xFF00FFCC))
                                }

                                IconButton(
                                    onClick = {
                                        currentUrl = "https://www.google.com"
                                        inputUrl = "https://www.google.com"
                                        webViewInstance?.loadUrl("https://www.google.com")
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Home, contentDescription = "الرئيسية", tint = Color.White)
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(currentUrl))
                                        Toast.makeText(context, "تم نسخ الرابط", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "نسخ الرابط", tint = Color(0xFF94A3B8))
                                }

                                IconButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.OpenInBrowser, contentDescription = "متصفح خارجي", tint = Color(0xFFFFB703))
                                }
                            }
                        }

                        if (isLoadingWeb) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp),
                                color = Color(0xFF00FFCC),
                                trackColor = Color(0xFF1E293B)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(2.dp))
                        }

                        // WebView Frame
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                                .background(Color.White)
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = true
                                        webViewClient = object : WebViewClient() {
                                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                                isLoadingWeb = true
                                                url?.let {
                                                    inputUrl = it
                                                    currentUrl = it
                                                }
                                            }

                                            override fun onPageFinished(view: WebView?, url: String?) {
                                                isLoadingWeb = false
                                                canGoBack = view?.canGoBack() == true
                                                canGoForward = view?.canGoForward() == true
                                                url?.let {
                                                    inputUrl = it
                                                    currentUrl = it
                                                }
                                            }
                                        }
                                        loadUrl(currentUrl)
                                        webViewInstance = this
                                    }
                                },
                                update = { webView ->
                                    webViewInstance = webView
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}
