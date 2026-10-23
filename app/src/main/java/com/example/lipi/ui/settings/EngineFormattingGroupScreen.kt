package com.example.lipi.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lipi.data.OnlineEngineMode
import com.example.lipi.data.TransliterationStyle
import com.example.lipi.ui.LipiViewModel
import com.example.ui.theme.PadmaTealPrimary

@Composable
fun EngineFormattingGroupScreen(
    viewModel: LipiViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val transliterationStyle by viewModel.transliterationStyle.collectAsState()
    val onlineEngineMode by viewModel.onlineEngineMode.collectAsState()
    val geminiApiKey by viewModel.geminiApiKey.collectAsState()
    val isAutoClipboardEnabled by viewModel.isAutoClipboardMonitoringEnabled.collectAsState()
    val convertDigits by viewModel.convertDigitsToBengali.collectAsState()

    var showStyleBottomSheet by remember { mutableStateOf(false) }
    var showOnlineEngineBottomSheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sub-screen Header with Back navigation
        SettingsSubScreenHeader(
            title = "রূপান্তর ইঞ্জিন ও ফরম্যাটিং",
            subtitle = "অনলাইন ক্লাউড/এআই ও ভাষার আচরণ নির্ধারণ",
            icon = Icons.Default.Translate,
            onBack = onBack
        )

        // Value Selection Section (Tappable rows that open Bottom Sheets)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "অনুবাদ ইঞ্জিন ও ভাষা স্টাইল নির্বাচন",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Transliteration Style Row (Click opens Bottom Sheet)
                SettingsClickableRow(
                    title = "বাংলিশ থেকে বাংলা রূপান্তর স্টাইল",
                    subtitle = "ইংরেজি শব্দ (zoom, app) এবং সময় (5pm) বাংলায় অনুবাদ বা ইংরেজি রাখার নিয়ম",
                    currentValue = when (transliterationStyle) {
                        TransliterationStyle.ALL_BENGALI -> "সকল টেক্সট বাংলায় (১০০% বাংলা)"
                        TransliterationStyle.PRESERVE_ENGLISH -> "কিছু ইংরেজি শব্দ preserve রাখা"
                    },
                    icon = Icons.Default.Language,
                    badgeText = "ট্যাপ করে বদলান",
                    onClick = { showStyleBottomSheet = true }
                )

                // Online Engine Mode Row (Click opens Bottom Sheet)
                SettingsClickableRow(
                    title = "অনলাইন ক্লাউড ও এআই ইঞ্জিন",
                    subtitle = "জটিল বা আধুনিক বাংলিশ স্ল্যাং রূপান্তরের জন্য ক্লাউড বা এআই সহায়িকা",
                    currentValue = when (onlineEngineMode) {
                        OnlineEngineMode.OFFLINE_ONLY -> "অফলাইন ইঞ্জিন (০ms ল্যাটেন্সি)"
                        OnlineEngineMode.ONLINE_CLOUD -> "Google ক্লাউড ট্রান্সলিটারেশন API"
                        OnlineEngineMode.GEMINI_AI -> "Gemini AI স্মার্ট ইঞ্জিন"
                    },
                    icon = Icons.Default.Public,
                    badgeText = when (onlineEngineMode) {
                        OnlineEngineMode.OFFLINE_ONLY -> "অফলাইন"
                        OnlineEngineMode.ONLINE_CLOUD -> "ক্লাউড API"
                        OnlineEngineMode.GEMINI_AI -> "AI"
                    },
                    onClick = { showOnlineEngineBottomSheet = true }
                )
            }
        }

        // Smart Automation & Formatting Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "স্মার্ট অটোমেশন ও সংখ্যা রূপান্তর",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Auto Clipboard Monitoring
                SettingsSwitchRow(
                    title = "স্বয়ংক্রিয় ক্লিপবোর্ড নজরদারি (Auto Monitor)",
                    description = "যেকোনো অ্যাপে লেখা কপি করলেই স্বয়ংক্রিয়ভাবে বাংলা অনুবাদ তৈরি হবে ও ভাসমান বলের পাশে প্রিভিউ কার্ড দেখাবে।",
                    checked = isAutoClipboardEnabled,
                    testTag = "auto_clipboard_switch",
                    onCheckedChange = { viewModel.setAutoClipboardMonitoring(it) }
                )

                // Digits to Bengali Digits
                SettingsSwitchRow(
                    title = "ইংরেজি সংখ্যাকে বাংলা সংখ্যায় রূপান্তর",
                    description = "মেসেজের মধ্যে থাকা সংখ্যাগুলো বাংলায় পরিবর্তিত হবে (যেমন: 123 → ১২৩)।",
                    checked = convertDigits,
                    testTag = "convert_digits_switch",
                    onCheckedChange = { viewModel.setConvertDigits(it) }
                )
            }
        }

        // Lipi Version 5 Library & Normalization Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = PadmaTealPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "লিপি v5 এক্সপ্যান্ডেড লাইব্রেরি ও টেক্সট নরমালাইজার",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "~২,৯৭০টি শব্দ ও বাক্যাংশ অন্তর্নির্মিত রয়েছে",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "• চ্যাটে টানা অক্ষরের স্বাভাবিকীকরণ (যেমন: 'valoooo' → 'ভালো', 'amiiii' → 'আমি')\n" +
                            "• বাংলা না-বোধক অব্যয় পৃথকীকরণ (যেমন: 'করবনা' → 'করব না')\n" +
                            "• যতিচিহ্ন ও দাঁড়ি অটো স্পেসিং\n" +
                            "• বাংলা একাডেমি প্রমিত বানান সংশোধন (যেমন: 'পোষ্ট' → 'পোস্ট', 'আবিস্কার' → 'আবিষ্কার')",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }

    // Modal Bottom Sheet for Transliteration Style
    if (showStyleBottomSheet) {
        TransliterationStyleBottomSheet(
            currentStyle = transliterationStyle,
            onStyleSelected = {
                viewModel.setTransliterationStyle(it)
                Toast.makeText(context, "স্টাইল আপডেট হয়েছে!", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showStyleBottomSheet = false }
        )
    }

    // Modal Bottom Sheet for Online Engine
    if (showOnlineEngineBottomSheet) {
        OnlineEngineBottomSheet(
            currentMode = onlineEngineMode,
            currentApiKey = geminiApiKey,
            onModeSelected = {
                viewModel.setOnlineEngineMode(it)
                Toast.makeText(context, "ইঞ্জিন আপডেট হয়েছে!", Toast.LENGTH_SHORT).show()
            },
            onApiKeySaved = {
                viewModel.setGeminiApiKey(it)
                Toast.makeText(context, "API Key সংরক্ষিত হয়েছে!", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showOnlineEngineBottomSheet = false }
        )
    }
}
