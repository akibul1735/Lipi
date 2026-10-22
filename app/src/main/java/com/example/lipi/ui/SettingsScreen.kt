package com.example.lipi.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lipi.service.LipiOverlayService
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.PadmaTealPrimary
import com.example.ui.theme.TerracottaSecondary

@Composable
fun SettingsScreen(
    viewModel: LipiViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val isFloatingEnabled by viewModel.isFloatingServiceEnabled.collectAsState()
    val overlayGranted by viewModel.overlayPermissionGranted.collectAsState()
    val convertDigits by viewModel.convertDigitsToBengali.collectAsState()
    val customWordbook by viewModel.customWordbook.collectAsState()

    var showAddWordDialog by remember { mutableStateOf(false) }
    var newBanglish by remember { mutableStateOf("") }
    var newBengali by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.checkSystemPermissions(context)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Master Floating Switch Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isFloatingEnabled)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ভাসমান 'ল' বল (কপি সহকারী)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isFloatingEnabled)
                                "সক্রিয়: স্ক্রিনে ভাসমান 'ল' বোতাম চালু আছে। যেকোনো অ্যাপে মেসেজ কপি করে 'ল' বল চাপলেই তাৎক্ষণিক বাংলা দেখা যাবে।"
                            else
                                "নিষ্ক্রিয়: ভাসমান উইন্ডো বন্ধ রয়েছে।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = isFloatingEnabled,
                        onCheckedChange = { enable ->
                            if (enable && !Settings.canDrawOverlays(context)) {
                                Toast.makeText(context, "অনুগ্রহ করে প্রথমে ভাসমান উইন্ডো (Display over other apps) অন করুন", Toast.LENGTH_LONG).show()
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                try { context.startActivity(intent) } catch (e: Exception) { /* fallback */ }
                            } else {
                                viewModel.setFloatingServiceEnabled(enable)
                                val serviceIntent = Intent(context, LipiOverlayService::class.java).apply {
                                    action = if (enable) LipiOverlayService.ACTION_START_ASSISTANT else LipiOverlayService.ACTION_DISMISS
                                }
                                try { context.startService(serviceIntent) } catch (e: Exception) { e.printStackTrace() }
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PadmaTealPrimary),
                        modifier = Modifier.testTag("floating_master_switch")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Direct One-Tap Floating Ball Launcher Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (!Settings.canDrawOverlays(context)) {
                                Toast.makeText(context, "ভাসমান উইন্ডো অনুমতি প্রয়োজন। সেটিংস থেকে 'Display over other apps' অন করুন।", Toast.LENGTH_LONG).show()
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                try { context.startActivity(intent) } catch (e: Exception) { /* fallback */ }
                            } else {
                                viewModel.setFloatingServiceEnabled(true)
                                val intent = Intent(context, LipiOverlayService::class.java).apply {
                                    action = LipiOverlayService.ACTION_START_ASSISTANT
                                }
                                try {
                                    context.startService(intent)
                                    Toast.makeText(context, "ভাসমান 'ল' বল স্ক্রিনে দৃশ্যমান হয়েছে!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "সার্ভিস শুরু করা যায়নি: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("launch_floating_ball_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = PadmaTealPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("স্ক্রিনে 'ল' বল দেখান", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.setFloatingServiceEnabled(false)
                            val intent = Intent(context, LipiOverlayService::class.java).apply {
                                action = LipiOverlayService.ACTION_DISMISS
                            }
                            try {
                                context.startService(intent)
                                Toast.makeText(context, "ভাসমান বল লুকানো হয়েছে", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) { /* fallback */ }
                        },
                        modifier = Modifier.testTag("hide_floating_ball_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("লুকান")
                    }
                }
            }
        }

        // 3-Step Simple Copy & Translate Guide
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "সহজ ব্যবহারের ৩টি ধাপ (কপি ও অনুবাদ)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                GuideStepRow(
                    icon = Icons.Default.ContentCopy,
                    stepNumber = "১",
                    title = "যেকোনো অ্যাপে বাংলিশ কপি করুন",
                    description = "হোয়াটসঅ্যাপ, মেসেঞ্জার, ফেসবুক বা ব্রাউজারে যেকোনো মেসেজ বা লেখা সিলেক্ট করে 'Copy' করুন।"
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.surfaceVariant)

                GuideStepRow(
                    icon = Icons.Default.TouchApp,
                    stepNumber = "২",
                    title = "ভাসমান 'ল' বোতামে চাপুন",
                    description = "স্ক্রিনের পাশে থাকা ভাসমান 'ল' বলে ট্যাপ করুন (অথবা লিপি অ্যাপে এসে 'পেস্ট করুন' চাপুন)।"
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.surfaceVariant)

                GuideStepRow(
                    icon = Icons.Default.Translate,
                    stepNumber = "৩",
                    title = "তাৎক্ষণিক বাংলা দেখুন ও কপি করুন",
                    description = "পপআপে শতভাগ প্রমিত বাংলা রূপান্তর দেখতে পাবেন। 'কপি করুন' চাপলেই আপনার ক্লিপবোর্ডে বাংলা লেখা রেডি!"
                )
            }
        }

        // Overlay Permission Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "সিস্টেম অনুমতি ও নিরাপত্তা",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))

                PermissionStatusRow(
                    title = "ভাসমান উইন্ডো অনুমতি (Display over other apps)",
                    description = "হোয়াটসঅ্যাপ বা মেসেঞ্জারের ওপর 'ল' ব্যাজ ভাসানোর জন্য প্রয়োজন।",
                    isGranted = overlayGranted,
                    onActionClick = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "অনুমতি সেটিংস খোলা যাচ্ছে না", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Safe Mode",
                        tint = Color(0xFF00A896),
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "প্লে প্রোটেক্ট নিরাপদ কপি মোড",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "কোনো সংবেদনশীল পারমিশন ছাড়াই নিরাপদ ওভারলে ও কপি-পেস্টের মাধ্যমে কাজ করে। গুগল প্লে প্রোটেক্টে কোনো ব্লক নেই।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // User-Requested Transliteration Options:
        // 1. সকল টেক্সট বাংলায় (কোন কিছুই ইংরেজিতে নয়, সেসব শব্দ ইংরেজি হলেও বাংলাতে প্রচলিত, সেসবও বাংলাতে দেখাবে: app - অ্যাপ, 5pm - বিকাল পাঁচটায়, ইত্যাদি)
        // 2. কিছু ইংরেজি শব্দ preserve রাখা।
        val transliterationStyle by viewModel.transliterationStyle.collectAsState()

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "বাংলিশ থেকে বাংলা রূপান্তর অপশন",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "অনুবাদ করার সময় ইংরেজি শব্দ ও সময় কীভাবে প্রদর্শিত হবে তা নির্বাচন করুন:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Option 1
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { viewModel.setTransliterationStyle(com.example.lipi.data.TransliterationStyle.ALL_BENGALI) }
                        .testTag("transliteration_style_all_bengali"),
                    color = if (transliterationStyle == com.example.lipi.data.TransliterationStyle.ALL_BENGALI)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (transliterationStyle == com.example.lipi.data.TransliterationStyle.ALL_BENGALI) MaterialTheme.colorScheme.primary else Color.Transparent
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = transliterationStyle == com.example.lipi.data.TransliterationStyle.ALL_BENGALI,
                            onClick = { viewModel.setTransliterationStyle(com.example.lipi.data.TransliterationStyle.ALL_BENGALI) },
                            colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = PadmaTealPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "১। সকল টেক্সট বাংলায় (১০০% খাঁটি বাংলা)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "কোন কিছুই ইংরেজিতে নয়। যেসব শব্দ ইংরেজি হলেও বাংলাতে প্রচলিত, সেসবও বাংলাতে দেখাবে (যেমন: app → অ্যাপ, 5pm → বিকাল পাঁচটায়, meeting → মিটিং, link → লিংক ইত্যাদি)।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Option 2
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { viewModel.setTransliterationStyle(com.example.lipi.data.TransliterationStyle.PRESERVE_ENGLISH) }
                        .testTag("transliteration_style_preserve_english"),
                    color = if (transliterationStyle == com.example.lipi.data.TransliterationStyle.PRESERVE_ENGLISH)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (transliterationStyle == com.example.lipi.data.TransliterationStyle.PRESERVE_ENGLISH) MaterialTheme.colorScheme.primary else Color.Transparent
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = transliterationStyle == com.example.lipi.data.TransliterationStyle.PRESERVE_ENGLISH,
                            onClick = { viewModel.setTransliterationStyle(com.example.lipi.data.TransliterationStyle.PRESERVE_ENGLISH) },
                            colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = PadmaTealPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "২। কিছু ইংরেজি শব্দ preserve রাখা",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "প্রচলিত প্রযুক্তি ও অফিস শব্দ মূল ইংরেজি হরফে অক্ষত থাকবে (যেমন: zoom, meeting, link, pdf, call ইত্যাদি)।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        // Additional Formatting & Automation Preferences
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "স্মার্ট অটোমেশন ও ফরম্যাটিং",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                val isAutoClipboardEnabled by viewModel.isAutoClipboardMonitoringEnabled.collectAsState()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "স্বয়ংক্রিয় ক্লিপবোর্ড নজরদারি (Auto Monitor)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "যেকোনো অ্যাপে লেখা কপি করলেই স্বয়ংক্রিয়ভাবে বাংলা অনুবাদ তৈরি হবে ও ভাসমান বলের পাশে প্রিভিউ কার্ড দেখাবে।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isAutoClipboardEnabled,
                        onCheckedChange = { viewModel.setAutoClipboardMonitoring(it) },
                        modifier = Modifier.testTag("auto_clipboard_switch")
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ইংরেজি সংখ্যাকে বাংলা সংখ্যায় রূপান্তর",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "যেমন: 123 → ১২৩",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = convertDigits,
                        onCheckedChange = { viewModel.setConvertDigits(it) },
                        modifier = Modifier.testTag("convert_digits_switch")
                    )
                }
            }
        }

        // Custom User Wordbook
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ব্যবহারকারীর ব্যক্তিগত শব্দকোষ",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "আপনার পছন্দের বাংলিশ শব্দের নির্দিষ্ট অনুবাদ সংরক্ষণ করুন",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { showAddWordDialog = !showAddWordDialog },
                        modifier = Modifier.testTag("add_word_button")
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "শব্দ যুক্ত করুন",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (showAddWordDialog) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newBanglish,
                            onValueChange = { newBanglish = it },
                            label = { Text("বাংলিশ শব্দ (যেমন: moner moto)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newBengali,
                            onValueChange = { newBengali = it },
                            label = { Text("বাংলা রূপ (যেমন: মনের মতো)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                if (newBanglish.isNotBlank() && newBengali.isNotBlank()) {
                                    viewModel.addCustomWord(newBanglish, newBengali)
                                    newBanglish = ""
                                    newBengali = ""
                                    showAddWordDialog = false
                                    Toast.makeText(context, "শব্দ যুক্ত হয়েছে!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.align(Alignment.End),
                            colors = ButtonDefaults.buttonColors(containerColor = TerracottaSecondary)
                        ) {
                            Text("সংরক্ষণ করুন")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                customWordbook.forEach { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${entry.banglish} → ${entry.bengali}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "ব্যক্তিগত পছন্দ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        IconButton(onClick = { viewModel.removeCustomWord(entry.id) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "মুছুন",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Privacy & Offline Security Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PadmaTealPrimary.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = PadmaTealPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "গোপনীয়তা ও অফলাইন সুরক্ষা",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = PadmaTealPrimary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• সমস্ত টেক্সট প্রসেসিং ১০০% আপনার ডিভাইসে অফলাইনে সম্পন্ন হয়।\n• লিপি কোনো মেসেজ সার্ভারে পাঠায় না এবং কোনো কথোপকথন ব্যাকগ্রাউন্ডে রেকর্ড করে না।\n• মূল বার্তা কখনোই স্বয়ংক্রিয়ভাবে পরিবর্তন বা পাঠানো হয় না; নিয়ন্ত্রণ সর্বদা আপনার হাতে।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
fun GuideStepRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    stepNumber: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(PadmaTealPrimary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun PermissionStatusRow(
    title: String,
    description: String,
    isGranted: Boolean,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isGranted) CyanAccent else TerracottaSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        OutlinedButton(
            onClick = onActionClick,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(if (isGranted) "যাচাই করুন" else "অনুমতি দিন", fontSize = 12.sp)
        }
    }
}
