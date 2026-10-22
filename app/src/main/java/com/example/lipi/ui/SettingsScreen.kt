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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
    var newCategory by remember { mutableStateOf("সাধারণ") }

    var showImportDialog by remember { mutableStateOf(false) }
    var importTextContent by remember { mutableStateOf("") }
    var importOverwrite by remember { mutableStateOf(false) }

    var showExportDialog by remember { mutableStateOf(false) }
    var showInstructionsDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var wordSearchQuery by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val content = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                if (content.isNotBlank()) {
                    val result = viewModel.bulkImportWords(content, overwrite = importOverwrite)
                    if (result.errorMessage != null) {
                        Toast.makeText(context, "ত্রুটি: ${result.errorMessage}", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "সফলভাবে ${result.addedCount}টি শব্দ আমদানি করা হয়েছে! (মোট: ${result.totalEntries}টি)", Toast.LENGTH_LONG).show()
                        showImportDialog = false
                    }
                } else {
                    Toast.makeText(context, "ফাইলটি খালি ছিল", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "ফাইল পড়তে সমস্যা: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

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
                            text = "ভাসমান 'A ⇄ অ' সহকারী (অনুবাদ বল)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isFloatingEnabled)
                                "সক্রিয়: স্ক্রিনে ভাসমান 'A ⇄ অ' সহকারী চালু আছে। যেকোনো লেখা কপি করলেই স্বয়ংক্রিয় বাংলা প্রিভিউ ভেসে উঠবে অথবা বলে ট্যাপ করে পূর্ণ উইন্ডো খোলা যাবে।"
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
                                    Toast.makeText(context, "ভাসমান 'A ⇄ অ' সহকারী স্ক্রিনে দৃশ্যমান হয়েছে!", Toast.LENGTH_SHORT).show()
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
                        Text("স্ক্রিনে সহকারী দেখান", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.setFloatingServiceEnabled(false)
                            val intent = Intent(context, LipiOverlayService::class.java).apply {
                                action = LipiOverlayService.ACTION_DISMISS
                            }
                            try {
                                context.startService(intent)
                                Toast.makeText(context, "ভাসমান সহকারী লুকানো হয়েছে", Toast.LENGTH_SHORT).show()
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
                    title = "স্বয়ংক্রিয় প্রিভিউ কার্ড বা 'A ⇄ অ' বলে চাপুন",
                    description = "কপি করার সাথে সাথে পাশে তাৎক্ষণিক বাংলা প্রিভিউ ভেসে উঠবে (অথবা 'A ⇄ অ' বলে ট্যাপ করুন)।"
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

        // Custom User Wordbook & Data Management (Single Entry, Bulk File Import, Export, Format Guide)
        val filteredWordbook = customWordbook.filter {
            wordSearchQuery.isBlank() ||
                    it.banglish.contains(wordSearchQuery, ignoreCase = true) ||
                    it.bengali.contains(wordSearchQuery, ignoreCase = true) ||
                    it.category.contains(wordSearchQuery, ignoreCase = true)
        }

        val sampleFormatInstruction = """# ==========================================
# লিপি কাস্টম শব্দকোষ ফাইল ফরম্যাট নির্দেশিকা
# ==========================================
# প্রতিটি লাইনে একটি করে বাংলিশ ও বাংলা শব্দ লিখুন।
# সমান (=) অথবা কমা (,) অথবা কোলন (:) দিয়ে আলাদা করুন।
# '#' অথবা '//' দিয়ে শুরু হওয়া লাইনগুলো মন্তব্য হিসেবে গণ্য হবে।

dhonnobad=ধন্যবাদ
shagotom=স্বাগতম
apni kemon achen=আপনি কেমন আছেন
valo thakben=ভালো থাকবেন
pera nai=প্যারা নাই
chill koro=চিল করো
meeting=মিটিং
zoom=জুম
office=অফিস
link=লিংক
pdf=পিডিএফ"""

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ব্যক্তিগত শব্দকোষ ও ডাটা ব্যবস্থাপনা",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "মোট ${customWordbook.size}টি শব্দ সংরক্ষিত (অনুবাদে অগ্রাধিকার পাবে)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { showInstructionsDialog = true },
                        modifier = Modifier.testTag("format_guide_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "ফরম্যাট নির্দেশিকা",
                            tint = PadmaTealPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons Bar (Single, Import, Export)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showAddWordDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_single_word_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = PadmaTealPrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("একক শব্দ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bulk_import_button"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ইমপোর্ট", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { showExportDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_data_button"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("এক্সপোর্ট", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Bar inside Custom Wordbook
                if (customWordbook.isNotEmpty()) {
                    OutlinedTextField(
                        value = wordSearchQuery,
                        onValueChange = { wordSearchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wordbook_search_input"),
                        placeholder = { Text("সংরক্ষিত শব্দ অনুসন্ধান করুন...", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (wordSearchQuery.isNotBlank()) {
                                IconButton(onClick = { wordSearchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "মুছুন", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Word List Container
                if (filteredWordbook.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (wordSearchQuery.isNotBlank())
                                "কোনো শব্দ খুঁজে পাওয়া যায়নি"
                            else
                                "কোনো কাস্টম শব্দ নেই। উপরের বোতাম চেপে যুক্ত করুন।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (filteredWordbook.size > 5) 240.dp else androidx.compose.ui.unit.Dp.Unspecified)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        filteredWordbook.forEach { entry ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = entry.banglish,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = PadmaTealPrimary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "→",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = entry.bengali,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Text(
                                            text = entry.category,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.removeCustomWord(entry.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "মুছুন",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Utilities Row: Reset to default or clear all
                if (customWordbook.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                viewModel.resetWordsToDefault()
                                Toast.makeText(context, "ডিফল্ট শব্দাবলি রিস্টোর করা হয়েছে", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ডিফল্ট শব্দ রিস্টোর", fontSize = 11.sp)
                        }

                        androidx.compose.material3.TextButton(
                            onClick = { showClearConfirmDialog = true }
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("সব মুছুন", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // DIALOG 1: Single Word Entry Dialog
        // -------------------------------------------------------------
        if (showAddWordDialog) {
            AlertDialog(
                onDismissRequest = { showAddWordDialog = false },
                title = { Text("নতুন একক শব্দ যুক্ত করুন", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "আপনার বাংলিশ লেখার সাথে কাঙ্ক্ষিত বাংলা অর্থ লিখুন:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = newBanglish,
                            onValueChange = { newBanglish = it },
                            label = { Text("বাংলিশ শব্দ / বাক্য (যেমন: pera nai)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newBengali,
                            onValueChange = { newBengali = it },
                            label = { Text("বাংলা রূপ (যেমন: প্যারা নাই)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newCategory,
                            onValueChange = { newCategory = it },
                            label = { Text("ক্যাটাগরি (ঐচ্ছিক)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newBanglish.isNotBlank() && newBengali.isNotBlank()) {
                                viewModel.addCustomWord(newBanglish, newBengali)
                                newBanglish = ""
                                newBengali = ""
                                showAddWordDialog = false
                                Toast.makeText(context, "শব্দ যুক্ত হয়েছে!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "বাংলিশ ও বাংলা দুটি ঘরই পূরণ করুন", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PadmaTealPrimary)
                    ) {
                        Text("সংরক্ষণ করুন")
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showAddWordDialog = false }) {
                        Text("বাতিল")
                    }
                }
            )
        }

        // -------------------------------------------------------------
        // DIALOG 2: Vast Import Dialog (File Picker + Bulk Paste)
        // -------------------------------------------------------------
        if (showImportDialog) {
            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, tint = PadmaTealPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("শব্দকোষ বৃহৎ ইমপোর্ট", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "টেক্সট ফাইল (.txt / .csv / .json) নির্বাচন করুন অথবা নিচে সরাসরি লেখা পেস্ট করুন:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // 1-Tap File Picker Button
                        Button(
                            onClick = {
                                filePickerLauncher.launch("*/*")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = PadmaTealPrimary.copy(alpha = 0.9f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("📁 ফাইল ম্যানেজার থেকে ফাইল নির্বাচন করুন")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f))
                            Text(
                                " অথবা সরাসরি পেস্ট করুন ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f))
                        }

                        OutlinedTextField(
                            value = importTextContent,
                            onValueChange = { importTextContent = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            placeholder = {
                                Text(
                                    "dhonnobad=ধন্যবাদ\nshagotom=স্বাগতম\npera nai=প্যারা নাই\nmeeting=মিটিং",
                                    fontSize = 12.sp
                                )
                            },
                            maxLines = 10
                        )

                        // Overwrite toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { importOverwrite = !importOverwrite }
                        ) {
                            Checkbox(
                                checked = importOverwrite,
                                onCheckedChange = { importOverwrite = it },
                                colors = CheckboxDefaults.colors(checkedColor = PadmaTealPrimary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "বিদ্যমান শব্দ প্রতিস্থাপন করুন (Overwrite)",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        // Help hint button
                        androidx.compose.material3.TextButton(
                            onClick = { showInstructionsDialog = true },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ফাইল ফরম্যাট কেমন হবে? নির্দেশিকা দেখুন", fontSize = 11.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (importTextContent.isNotBlank()) {
                                val result = viewModel.bulkImportWords(importTextContent, overwrite = importOverwrite)
                                if (result.errorMessage != null) {
                                    Toast.makeText(context, "ত্রুটি: ${result.errorMessage}", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "সফলভাবে ${result.addedCount}টি শব্দ আমদানি করা হয়েছে!", Toast.LENGTH_LONG).show()
                                    importTextContent = ""
                                    showImportDialog = false
                                }
                            } else {
                                Toast.makeText(context, "ফাইল নির্বাচন করুন অথবা টেক্সট বক্সে লিখুন", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PadmaTealPrimary)
                    ) {
                        Text("ইমপোর্ট সম্পন্ন করুন")
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showImportDialog = false }) {
                        Text("বন্ধ")
                    }
                }
            )
        }

        // -------------------------------------------------------------
        // DIALOG 3: Export Custom Data Dialog
        // -------------------------------------------------------------
        if (showExportDialog) {
            val exportText = viewModel.exportWordsAsText()
            val exportJson = viewModel.exportWordsAsJson()
            var selectedExportFormat by remember { mutableStateOf("TXT") } // TXT or JSON

            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, tint = PadmaTealPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("শব্দকোষ এক্সপোর্ট (${customWordbook.size}টি শব্দ)", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "আপনার সংরক্ষিত কাস্টম শব্দাবলি অন্য ফোনে বা ব্যাকআপ রাখার জন্য এক্সপোর্ট করুন:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Format selector
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            androidx.compose.material3.FilterChip(
                                selected = selectedExportFormat == "TXT",
                                onClick = { selectedExportFormat = "TXT" },
                                label = { Text("টেক্সট ফাইল (.txt)") }
                            )
                            androidx.compose.material3.FilterChip(
                                selected = selectedExportFormat == "JSON",
                                onClick = { selectedExportFormat = "JSON" },
                                label = { Text("JSON ফাইল (.json)") }
                            )
                        }

                        // Preview box
                        val activeContent = if (selectedExportFormat == "TXT") exportText else exportJson
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                        ) {
                            Text(
                                text = activeContent,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .verticalScroll(rememberScrollState()),
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = {
                                val content = if (selectedExportFormat == "TXT") exportText else exportJson
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val clip = ClipData.newPlainText("Lipi Wordbook Export", content)
                                clipboard?.setPrimaryClip(clip)
                                Toast.makeText(context, "ক্লিপবোর্ডে কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("কপি")
                        }

                        Button(
                            onClick = {
                                val content = if (selectedExportFormat == "TXT") exportText else exportJson
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, if (selectedExportFormat == "TXT") "lipi_custom_dictionary.txt" else "lipi_custom_dictionary.json")
                                    putExtra(Intent.EXTRA_TEXT, content)
                                }
                                context.startActivity(Intent.createChooser(intent, "লিপি শব্দকোষ ফাইল পাঠান বা সংরক্ষণ করুন"))
                                showExportDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PadmaTealPrimary)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("শেয়ার / সেভ")
                        }
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showExportDialog = false }) {
                        Text("বন্ধ")
                    }
                }
            )
        }

        // -------------------------------------------------------------
        // DIALOG 4: File Format Instructions Dialog (Detailed Guide)
        // -------------------------------------------------------------
        if (showInstructionsDialog) {
            AlertDialog(
                onDismissRequest = { showInstructionsDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = PadmaTealPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ফাইল ফরম্যাট নির্দেশিকা", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "ফাইলটি যেকোনো সাধারণ টেক্সট ফাইল (.txt বা .csv) বা JSON হতে পারে।",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "১. প্রতি লাইনে একটি করে বাংলিশ ও বাংলা জোড়া লিখুন:\n" +
                                    "   • banglish=বাংলা  (যেমন: dhonnobad=ধন্যবাদ)\n" +
                                    "   • banglish,বাংলা  (যেমন: shagotom,স্বাগতম)\n" +
                                    "   • banglish:বাংলা  (যেমন: meeting:মিটিং)\n" +
                                    "২. '#' দিয়ে শুরু হওয়া লাইনগুলো মন্তব্য হিসেবে এড়িয়ে যাওয়া হবে।\n" +
                                    "৩. ফাইলে একসাথে হাজার হাজার লাইন আমদানি করা সম্ভব।\n" +
                                    "৪. JSON ফরম্যাটে: [{\"banglish\": \"...\", \"bengali\": \"...\"}] সমর্থন করে।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )

                        Text(
                            text = "নমুনা ফাইল প্রিভিউ:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PadmaTealPrimary
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = sampleFormatInstruction,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                modifier = Modifier.padding(10.dp),
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val clip = ClipData.newPlainText("Sample Lipi Format", sampleFormatInstruction)
                            clipboard?.setPrimaryClip(clip)
                            Toast.makeText(context, "নমুনা ফরম্যাট ক্লিপবোর্ডে কপি হয়েছে!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PadmaTealPrimary)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("নমুনা কপি করুন")
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showInstructionsDialog = false }) {
                        Text("বুঝেছি")
                    }
                }
            )
        }

        // -------------------------------------------------------------
        // DIALOG 5: Clear All Confirmation Dialog
        // -------------------------------------------------------------
        if (showClearConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showClearConfirmDialog = false },
                title = { Text("সব কাস্টম শব্দ মুছতে চান?", fontWeight = FontWeight.Bold) },
                text = {
                    Text("আপনার সংরক্ষিত সকল শব্দ মুছে যাবে। প্রয়োজন হলে আগে 'এক্সপোর্ট' করে ব্যাকআপ রাখুন।")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearAllCustomWords()
                            showClearConfirmDialog = false
                            Toast.makeText(context, "সকল কাস্টম শব্দ মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("হ্যাঁ, সব মুছুন")
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showClearConfirmDialog = false }) {
                        Text("বাতিল")
                    }
                }
            )
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
