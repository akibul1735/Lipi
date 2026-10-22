package com.example.lipi.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import com.example.lipi.service.LipiOverlayService
import com.example.ui.theme.PadmaTealPrimary
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lipi.data.ConversionResult
import com.example.lipi.data.LipiMode
import com.example.lipi.data.TokenType
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.PadmaTealPrimary
import com.example.ui.theme.TerracottaSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudioScreen(
    viewModel: LipiViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val inputText by viewModel.inputText.collectAsState()
    val selectedMode by viewModel.selectedMode.collectAsState()
    val currentResult by viewModel.currentResult.collectAsState()
    val scrollState = rememberScrollState()

    val samplePhrases = listOf(
        "Ami valo asi",
        "Tumi kemon acho?",
        "আমি ভালো আছি, তুমি কেমন আছো?",
        "কালকে সকালে মিটিং আছে",
        "Ajke ki korba?",
        "অসুস্হ মানুষটি হাসপাতালে ভর্তি",
        "Meeting er zoom link pathao please"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Floating Ball Quick Action Card
        val isFloatingEnabled by viewModel.isFloatingServiceEnabled.collectAsState()
        val overlayGranted by viewModel.overlayPermissionGranted.collectAsState()

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isFloatingEnabled && overlayGranted)
                    PadmaTealPrimary.copy(alpha = 0.15f)
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF07232B), Color(0xFF0F5161))
                                ),
                                CircleShape
                            )
                            .border(1.5.dp, CyanAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "A⇄অ",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp
                            )
                            Text(
                                "LIPI",
                                color = CyanAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 7.sp
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "ভাসমান 'A ⇄ অ' অনুবাদক বল",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isFloatingEnabled && overlayGranted) "স্ক্রিনে সক্রিয় আছে" else "অন্য অ্যাপের ওপর ব্যবহারের জন্য চালু করুন",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Switch(
                        checked = isFloatingEnabled && overlayGranted,
                        onCheckedChange = { checked ->
                            if (checked && !Settings.canDrawOverlays(context)) {
                                Toast.makeText(context, "অনুমতি দিন: 'Display over other apps' অন করুন", Toast.LENGTH_LONG).show()
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                try { context.startActivity(intent) } catch (e: Exception) { /* fallback */ }
                            } else {
                                viewModel.setFloatingServiceEnabled(checked)
                                val serviceIntent = Intent(context, LipiOverlayService::class.java).apply {
                                    action = if (checked) LipiOverlayService.ACTION_START_ASSISTANT else LipiOverlayService.ACTION_DISMISS
                                }
                                try {
                                    context.startService(serviceIntent)
                                    Toast.makeText(context, if (checked) "ভাসমান 'A ⇄ অ' অনুবাদক বল চালু হয়েছে!" else "ভাসমান বল লুকানো হয়েছে", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        },
                        modifier = Modifier.testTag("floating_overlay_toggle_switch")
                    )
                }
            }
        }

        // Mode Selector Card
        ModeSelectorCard(
            selectedMode = selectedMode,
            onModeSelect = { viewModel.setMode(it) }
        )

        // Preset Samples Row
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "দ্রুত টেস্ট উদাহরণ:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                samplePhrases.forEach { sample ->
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.updateInput(sample) }
                            .testTag("sample_chip_${sample.take(6)}"),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        tonalElevation = 1.dp
                    ) {
                        Text(
                            text = sample,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Input Box Area
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (selectedMode) {
                            LipiMode.BENGALI_TO_BANGLISH -> "বাংলা টেক্সট লিখুন (বাংলা → বাংলিশ):"
                            LipiMode.SPELL_DOCTOR -> "বাংলা টেক্সট লিখুন (বানান ডাক্তার):"
                            else -> "বাংলিশ / মিশ্র টেক্সট লিখুন (বাংলিশ → বাংলা):"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Prominent Paste Button with Label
                        OutlinedButton(
                            onClick = {
                                try {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = if (clipboard.hasPrimaryClip()) {
                                        clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
                                    } else null

                                    if (!clip.isNullOrBlank()) {
                                        viewModel.updateInput(clip)
                                        Toast.makeText(context, "লেখা পেস্ট করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "ক্লিপবোর্ডে কোনো লেখা পাওয়া যায়নি। আগে কোনো মেসেজ কপি করুন।", Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "পেস্ট ব্যর্থ হয়েছে: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("paste_button"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentPaste,
                                contentDescription = "পেস্ট করুন",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("পেস্ট করুন", fontSize = 12.sp)
                        }

                        if (inputText.isNotBlank()) {
                            IconButton(
                                onClick = { viewModel.updateInput("") },
                                modifier = Modifier.size(36.dp).testTag("clear_button")
                            ) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "মুছুন",
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { viewModel.updateInput(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .testTag("workbench_input_field"),
                    placeholder = {
                        Text(
                            text = when (selectedMode) {
                                LipiMode.BENGALI_TO_BANGLISH -> "যেমন: আমি ভালো আছি, তুমি কেমন আছো? কালকে দেখা হবে..."
                                LipiMode.SPELL_DOCTOR -> "যেমন: আমি ভাল আছি, কিন্তু তিনি উপস্থিত ছিলেন না..."
                                else -> "যেমন: Ami valo asi, tumi kemon acho? Ajke 5pm e call dao..."
                            }
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${inputText.length} অক্ষর",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = when (selectedMode) {
                            LipiMode.BENGALI_TO_BANGLISH -> "বাংলা → বাংলিশ রূপান্তর সক্রিয়"
                            LipiMode.SPELL_DOCTOR -> "বাংলা বানান ডাক্তার সক্রিয়"
                            else -> "তাৎক্ষণিক রূপান্তর"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Quick Language Swap / Reverse Action Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { viewModel.swapLanguages() }
                .testTag("swap_languages_button"),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "বাংলা ⇄ বাংলিশ বিপরীত রূপান্তর",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedMode == LipiMode.BENGALI_TO_BANGLISH)
                        "বর্তমান মোড: বাংলা → বাংলিশ (ট্যাপ করে বাংলিশ → বাংলা করুন)"
                    else
                        "বাংলা ⇄ বাংলিশ বিপরীত রূপান্তর (Reverse Swap)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Output Result Card
        OutputResultCard(
            result = currentResult,
            onCopy = {
                if (currentResult.convertedText.isNotBlank()) {
                    try {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Lipi Result", currentResult.convertedText)
                        clipboard.setPrimaryClip(clip)
                        val copyToast = if (currentResult.modeUsed == LipiMode.BENGALI_TO_BANGLISH) "বাংলিশ ফলাফল সফলভাবে কপি হয়েছে!" else "বাংলা অনুবাদ সফলভাবে কপি হয়েছে!"
                        Toast.makeText(context, copyToast, Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "কপি ব্যর্থ হয়েছে: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val emptyToast = if (currentResult.modeUsed == LipiMode.BENGALI_TO_BANGLISH) "কপি করার মতো কোনো বাংলিশ লেখা নেই।" else "কপি করার মতো কোনো বাংলা লেখা নেই। আগে কিছু লিখুন বা পেস্ট করুন।"
                    Toast.makeText(context, emptyToast, Toast.LENGTH_LONG).show()
                }
            },
            onShare = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, currentResult.convertedText)
                }
                context.startActivity(Intent.createChooser(intent, "শেয়ার করুন"))
            },
            onSaveWord = { original, converted ->
                viewModel.addCustomWord(original, converted)
                Toast.makeText(context, "'$original' শব্দকোষে যুক্ত হয়েছে!", Toast.LENGTH_SHORT).show()
            }
        )

        // Token Breakdown Section
        TokenBreakdownCard(result = currentResult)
    }
}

@Composable
fun ModeSelectorCard(
    selectedMode: LipiMode,
    onModeSelect: (LipiMode) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "কাজের ধরন নির্বাচন করুন:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LipiMode.values().forEach { mode ->
                    val isSelected = selectedMode == mode
                    FilterChip(
                        selected = isSelected,
                        onClick = { onModeSelect(mode) },
                        label = {
                            Text(
                                text = mode.titleBn,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = when (mode) {
                                    LipiMode.BANGLISH_TO_BENGALI -> Icons.Default.Language
                                    LipiMode.BENGALI_TO_BANGLISH -> Icons.Default.SwapHoriz
                                    LipiMode.SPELL_DOCTOR -> Icons.Default.Spellcheck
                                    LipiMode.SMART_AUTO -> Icons.Default.AutoAwesome
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("mode_${mode.name}")
                    )
                }
            }
        }
    }
}

@Composable
fun OutputResultCard(
    result: ConversionResult,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onSaveWord: (String, String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                when (result.modeUsed) {
                                    LipiMode.BENGALI_TO_BANGLISH -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    LipiMode.SPELL_DOCTOR -> TerracottaSecondary.copy(alpha = 0.2f)
                                    else -> CyanAccent.copy(alpha = 0.2f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (result.modeUsed) {
                                LipiMode.BENGALI_TO_BANGLISH -> Icons.Default.SwapHoriz
                                LipiMode.SPELL_DOCTOR -> Icons.Default.HealthAndSafety
                                else -> Icons.Default.AutoAwesome
                            },
                            contentDescription = null,
                            tint = when (result.modeUsed) {
                                LipiMode.BENGALI_TO_BANGLISH -> MaterialTheme.colorScheme.primary
                                LipiMode.SPELL_DOCTOR -> TerracottaSecondary
                                else -> CyanAccent
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (result.modeUsed) {
                            LipiMode.BENGALI_TO_BANGLISH -> "বাংলিশ রূপান্তর ফলাফল"
                            LipiMode.SPELL_DOCTOR -> "বানান পরীক্ষিত বাংলা স্ক্রিপ্ট"
                            else -> "বাংলা রূপান্তর ফলাফল"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (result.spellingCorrectionsCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = TerracottaSecondary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${result.spellingCorrectionsCount}টি বানান সংশোধন",
                            style = MaterialTheme.typography.labelSmall,
                            color = TerracottaSecondary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Large Bengali Display
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                )
            ) {
                Text(
                    text = if (result.convertedText.isBlank()) "কোনো ফলাফল নেই..." else result.convertedText,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        lineHeight = 32.sp,
                        letterSpacing = 0.sp
                    ),
                    color = if (result.convertedText.isBlank())
                        MaterialTheme.colorScheme.outline
                    else
                        MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(16.dp)
                        .testTag("converted_output_text")
                )
            }

            // Spelling doctor notes if available
            val correctedTokens = result.tokens.filter { it.correctionNote != null }
            if (correctedTokens.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(TerracottaSecondary.copy(alpha = 0.08f))
                        .border(1.dp, TerracottaSecondary.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "বানান ডাক্তারের পর্যবেক্ষণ:",
                        style = MaterialTheme.typography.labelMedium,
                        color = TerracottaSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    correctedTokens.forEach { token ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                text = "• ",
                                color = TerracottaSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "'${token.original}' → '${token.converted}': ${token.correctionNote}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onCopy,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("copy_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "কপি করুন",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("কপি করুন")
                }

                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("share_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "শেয়ার করুন",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("শেয়ার")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TokenBreakdownCard(result: ConversionResult) {
    if (result.tokens.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "মিশ্র-স্ক্রিপ্ট টোকেন বিশ্লেষণ (Token Analysis)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "লিপি স্বয়ংক্রিয়ভাবে বাংলিশ, বিশুদ্ধ বাংলা, ইংরেজি প্রযুক্তি শব্দ, সংখ্যা ও ইমোজি পৃথকভাবে শনাক্ত করেছে:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                result.tokens.filter { it.original.isNotBlank() }.forEach { token ->
                    val (tagColor, typeLabel) = when (token.type) {
                        TokenType.BANGLISH -> Pair(MaterialTheme.colorScheme.primary, "বাংলিশ")
                        TokenType.BENGALI -> Pair(CyanAccent, "বাংলা")
                        TokenType.ENGLISH_TECH -> Pair(MaterialTheme.colorScheme.outline, "ইংরেজি শব্দ")
                        TokenType.NUMBER -> Pair(TerracottaSecondary, "সংখ্যা")
                        TokenType.PUNCTUATION_EMOJI -> Pair(Color.Gray, "চিহ্ন/ইমোজি")
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = tagColor.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, tagColor.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = token.original,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = tagColor
                            )
                            if (token.original != token.converted) {
                                Text(
                                    text = " → ${token.converted}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "[$typeLabel]",
                                style = MaterialTheme.typography.labelSmall,
                                color = tagColor.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}
