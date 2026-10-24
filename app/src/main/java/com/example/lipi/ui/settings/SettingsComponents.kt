package com.example.lipi.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lipi.data.OnlineEngineMode
import com.example.lipi.data.TransliterationStyle
import com.example.lipi.engine.LipiOnlineEngine
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.PadmaTealPrimary
import kotlinx.coroutines.launch

/**
 * Settings Group Enum defining the 4 core hierarchical sections of Lipi settings.
 */
enum class SettingsGroup(
    val titleBn: String,
    val subtitleBn: String,
    val icon: ImageVector,
    val defaultBadge: String
) {
    FLOATING_NOTIFICATION(
        titleBn = "ভাসমান সহকারী ও নোটিফিকেশন",
        subtitleBn = "স্ক্রিনের উপর ভাসমান বাটন, অটো ক্লিপবোর্ড প্রিভিউ ও কুইক স্ট্যাটাস বার",
        icon = Icons.Default.TouchApp,
        defaultBadge = "অ্যাসিস্ট্যান্ট"
    ),
    ENGINE_FORMATTING(
        titleBn = "রূপান্তর ইঞ্জিন ও ফরম্যাটিং",
        subtitleBn = "অনলাইন ক্লাউড/এআই এপিআই, ট্রান্সলিটারেশন স্টাইল ও বাংলা সংখ্যা আচরণ",
        icon = Icons.Default.Translate,
        defaultBadge = "v5 + Cloud"
    ),
    WORDBOOK_DATA(
        titleBn = "কাস্টম শব্দকোষ ও ট্রেইন ডাটা",
        subtitleBn = "ব্যক্তিগত ডিকশনারি, ১০০+ চ্যাট ট্রেইন ডাটা, ব্যাকআপ ও ফাইল ইমপোর্ট/এক্সপোর্ট",
        icon = Icons.AutoMirrored.Filled.PlaylistAdd,
        defaultBadge = "শব্দকোষ"
    ),
    SYSTEM_BATTERY(
        titleBn = "সিস্টেম অনুমতি ও ব্যাটারি সুরক্ষা",
        subtitleBn = "অন্যান্য অ্যাপের উপর প্রদর্শন (ওভারলে) ও আনরেস্ট্রিক্টেড ব্যাকগ্রাউন্ড সার্ভিস",
        icon = Icons.Default.Lock,
        defaultBadge = "সুরক্ষিত"
    )
}

/**
 * Main Settings Screen Group Item Card with icon, title, description, badge and right chevron.
 */
@Composable
fun SettingsGroupCard(
    group: SettingsGroup,
    badgeText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("settings_group_${group.name.lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PadmaTealPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = group.icon,
                    contentDescription = group.titleBn,
                    tint = PadmaTealPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = group.titleBn,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = PadmaTealPrimary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = PadmaTealPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = group.subtitleBn,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "খুলুন",
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Top bar header for Group Details screens with back button, icon and title.
 */
@Composable
fun SettingsSubScreenHeader(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("settings_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "ফিরে যান",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PadmaTealPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PadmaTealPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

/**
 * Clickable row in settings detail page that triggers a selection dialog/bottom sheet.
 */
@Composable
fun SettingsClickableRow(
    title: String,
    subtitle: String,
    currentValue: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    badgeText: String? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("settings_clickable_${title.take(8)}"),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PadmaTealPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (badgeText != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = PadmaTealPrimary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = PadmaTealPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ) {
                    Text(
                        text = "বর্তমান মান: $currentValue",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "পরিবর্তন করুন",
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * Modern switch toggle row for on/off settings.
 */
@Composable
fun SettingsSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    testTag: String = ""
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (checked) PadmaTealPrimary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = PadmaTealPrimary
                )
            )
        }
    }
}

/**
 * Value Selection Modal Bottom Sheet for Transliteration Style.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransliterationStyleBottomSheet(
    currentStyle: TransliterationStyle,
    onStyleSelected: (TransliterationStyle) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "ট্রান্সলিটারেশন স্টাইল নির্বাচন",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "অনুবাদ করার সময় ইংরেজি শব্দ ও সময় প্রদর্শনের আচরণ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            // Option 1: ALL_BENGALI
            val isOption1Selected = currentStyle == TransliterationStyle.ALL_BENGALI
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        onStyleSelected(TransliterationStyle.ALL_BENGALI)
                        onDismiss()
                    }
                    .testTag("style_all_bengali_option"),
                color = if (isOption1Selected) PadmaTealPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(
                    if (isOption1Selected) 1.5.dp else 1.dp,
                    if (isOption1Selected) PadmaTealPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .border(2.dp, if (isOption1Selected) PadmaTealPrimary else MaterialTheme.colorScheme.outline, CircleShape)
                            .background(if (isOption1Selected) PadmaTealPrimary else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isOption1Selected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "১. সকল টেক্সট বাংলায়",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isOption1Selected) PadmaTealPrimary else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = PadmaTealPrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "প্রস্তাবিত",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = PadmaTealPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "কোন কিছুই ইংরেজিতে নয়। ইংরেজি হলেও বাংলাতে বহুল প্রচলিত শব্দগুলো খাঁটি বাংলায় দেখাবে (যেমন: app → অ্যাপ, 5pm → বিকাল পাঁচটায়, zoom → জুম, meeting → মিটিং)।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 19.sp
                        )
                    }
                }
            }

            // Option 2: PRESERVE_ENGLISH
            val isOption2Selected = currentStyle == TransliterationStyle.PRESERVE_ENGLISH
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        onStyleSelected(TransliterationStyle.PRESERVE_ENGLISH)
                        onDismiss()
                    }
                    .testTag("style_preserve_english_option"),
                color = if (isOption2Selected) PadmaTealPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(
                    if (isOption2Selected) 1.5.dp else 1.dp,
                    if (isOption2Selected) PadmaTealPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .border(2.dp, if (isOption2Selected) PadmaTealPrimary else MaterialTheme.colorScheme.outline, CircleShape)
                            .background(if (isOption2Selected) PadmaTealPrimary else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isOption2Selected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "২. কিছু ইংরেজি শব্দ preserve রাখা",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isOption2Selected) PadmaTealPrimary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "প্রচলিত প্রযুক্তি ও অফিস শব্দ মূল ইংরেজি হরফে অক্ষত থাকবে (যেমন: zoom, meeting, link, pdf, call ইত্যাদি)।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 19.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Value Selection Modal Bottom Sheet for Online / Cloud Engine Mode and Gemini Models.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineEngineBottomSheet(
    currentMode: OnlineEngineMode,
    currentApiKey: String,
    currentModel: String = "gemini-3.8-flash",
    fetchedModels: List<LipiOnlineEngine.GeminiModelInfo> = emptyList(),
    isFetchingModels: Boolean = false,
    fetchFeedback: String? = null,
    onFetchModels: (String) -> Unit = {},
    onModeSelected: (OnlineEngineMode) -> Unit,
    onApiKeySaved: (String) -> Unit,
    onModelSelected: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var apiKeyInput by remember(currentApiKey) { mutableStateOf(currentApiKey) }
    var selectedModel by remember(currentModel) { mutableStateOf(currentModel) }
    var customModelInput by remember { mutableStateOf("") }
    
    // Auto-fetch latest models when sheet opens if API key is present
    androidx.compose.runtime.LaunchedEffect(apiKeyInput) {
        if (apiKeyInput.isNotBlank() && fetchedModels.isEmpty()) {
            onFetchModels(apiKeyInput)
        }
    }

    val allAvailableModels = remember(fetchedModels) {
        if (fetchedModels.isEmpty()) {
            LipiOnlineEngine.AVAILABLE_GEMINI_MODELS
        } else {
            // Deduplicate: prioritize fetched models, then append missing defaults
            val fetchedIds = fetchedModels.map { it.modelId }.toSet()
            val missingDefaults = LipiOnlineEngine.AVAILABLE_GEMINI_MODELS.filter { it.modelId !in fetchedIds }
            fetchedModels + missingDefaults
        }
    }

    var isCustomModelSelected by remember(selectedModel, allAvailableModels) {
        mutableStateOf(allAvailableModels.none { it.modelId == selectedModel })
    }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testResultType by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column {
                Text(
                    text = "অনলাইন ও ক্লাউড ইঞ্জিন নির্বাচন",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "জটিল বা আধুনিক বাংলিশ চ্যাট রূপান্তরের জন্য ইঞ্জিন নির্বাচন করুন",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            // Option 1: Offline
            val isOffline = currentMode == OnlineEngineMode.OFFLINE_ONLY
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onModeSelected(OnlineEngineMode.OFFLINE_ONLY) },
                color = if (isOffline) PadmaTealPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(if (isOffline) 1.5.dp else 1.dp, if (isOffline) PadmaTealPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .border(2.dp, if (isOffline) PadmaTealPrimary else MaterialTheme.colorScheme.outline, CircleShape)
                            .background(if (isOffline) PadmaTealPrimary else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isOffline) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "১. অফলাইন ইঞ্জিন (ডিফল্ট - সবচেয়ে দ্রুত)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isOffline) PadmaTealPrimary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "০ মিলি-সেকেন্ড ল্যাটেন্সি। কোনো ইন্টারনেট ছাড়াই অভ্যন্তরীণ সমৃদ্ধ শব্দকোষ ও ফোনেটিক অ্যালগরিদমে রূপান্তর।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Option 2: Google Input Tools Cloud API
            val isCloud = currentMode == OnlineEngineMode.ONLINE_CLOUD
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onModeSelected(OnlineEngineMode.ONLINE_CLOUD) },
                color = if (isCloud) PadmaTealPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(if (isCloud) 1.5.dp else 1.dp, if (isCloud) PadmaTealPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .border(2.dp, if (isCloud) PadmaTealPrimary else MaterialTheme.colorScheme.outline, CircleShape)
                            .background(if (isCloud) PadmaTealPrimary else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCloud) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "২. Google ক্লাউড ট্রান্সলিটারেশন API",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isCloud) PadmaTealPrimary else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(shape = RoundedCornerShape(4.dp), color = PadmaTealPrimary.copy(alpha = 0.15f)) {
                                Text("ফ্রি ও দ্রুত", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = PadmaTealPrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        Text(
                            text = "ক্লাউড এপিআই — কোনো কী ছাড়াই সরাসরি কার্যকর। জটিল বাক্য ও চলিত মেসেজ রূপান্তরে অত্যন্ত নির্ভুল।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Option 3: Gemini AI
            val isGemini = currentMode == OnlineEngineMode.GEMINI_AI
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onModeSelected(OnlineEngineMode.GEMINI_AI) },
                color = if (isGemini) PadmaTealPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(if (isGemini) 1.5.dp else 1.dp, if (isGemini) PadmaTealPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .border(2.dp, if (isGemini) PadmaTealPrimary else MaterialTheme.colorScheme.outline, CircleShape)
                                .background(if (isGemini) PadmaTealPrimary else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isGemini) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "৩. Gemini AI স্মার্ট ইঞ্জিন",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isGemini) PadmaTealPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) {
                                    Text("Neural AI", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            Text(
                                text = "উন্নত এআই কনটেক্সট মডেল — চলিত ভাষা, আধুনিক স্ল্যাং ও বানানের শতভাগ শুদ্ধ রূপান্তর।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isGemini) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                        // API Key Input
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Gemini API Key:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            OutlinedTextField(
                                value = apiKeyInput,
                                onValueChange = { apiKeyInput = it },
                                label = { Text("API Key (Google AI Studio)") },
                                placeholder = { Text("AIzaSy...") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                trailingIcon = {
                                    Button(
                                        onClick = { onApiKeySaved(apiKeyInput) },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.padding(end = 4.dp)
                                    ) {
                                        Text("সেভ", fontSize = 12.sp)
                                    }
                                }
                            )
                            Text(
                                text = "Google AI Studio থেকে বিনামূল্যে নিজস্ব Gemini API Key তৈরি করে ব্যবহার করতে পারেন।",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                        // Model Selection
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = PadmaTealPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Gemini মডেল নির্বাচন:",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // Auto-Fetch Button
                                OutlinedButton(
                                    onClick = { onFetchModels(apiKeyInput) },
                                    enabled = !isFetchingModels && apiKeyInput.isNotBlank(),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    if (isFetchingModels) {
                                        androidx.compose.material3.CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = PadmaTealPrimary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("ফেচ হচ্ছে...", fontSize = 11.sp)
                                    } else {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("মডেল রিফ্রেশ", fontSize = 11.sp)
                                    }
                                }
                            }

                            Text(
                                text = "গুগলের সর্বশেষ ৩.৮ সহ সব মডেলের তালিকা অটো-ফেচ করা হয়। পছন্দমতো মডেল বেছে নিন:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PadmaTealPrimary.copy(alpha = 0.08f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "💡 পরামর্শ: ৩.৮ খুব নতুন হওয়ায় গুগলে মাঝে মাঝে অতিরিক্ত চাপ (503 High Demand) দেখা দেয়। লিপি স্বয়ংক্রিয়ভাবে ব্যাকআপ মডেলে (Gemini 2.5 Flash) রূপান্তর করে টাইপিং চালু রাখে।",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = PadmaTealPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }

                            if (fetchFeedback != null) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (fetchFeedback.contains("সফল")) PadmaTealPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = fetchFeedback,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (fetchFeedback.contains("সফল")) PadmaTealPrimary else MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Dynamic / Available Models List
                            allAvailableModels.forEach { modelInfo ->
                                val isSelected = !isCustomModelSelected && selectedModel == modelInfo.modelId
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            isCustomModelSelected = false
                                            selectedModel = modelInfo.modelId
                                            onModelSelected(modelInfo.modelId)
                                        },
                                    color = if (isSelected) PadmaTealPrimary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(
                                        if (isSelected) 1.5.dp else 1.dp,
                                        if (isSelected) PadmaTealPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 2.dp)
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .border(2.dp, if (isSelected) PadmaTealPrimary else MaterialTheme.colorScheme.outline, CircleShape)
                                                .background(if (isSelected) PadmaTealPrimary else Color.Transparent),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = modelInfo.displayName,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) PadmaTealPrimary else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    if (modelInfo.isLiveFetched) {
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Surface(
                                                            shape = RoundedCornerShape(3.dp),
                                                            color = Color(0xFF388E3C).copy(alpha = 0.15f)
                                                        ) {
                                                            Text(
                                                                text = "লাইভ API",
                                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                                color = Color(0xFF2E7D32),
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (modelInfo.isRecommended) PadmaTealPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                                ) {
                                                    Text(
                                                        text = modelInfo.badge,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                        color = if (modelInfo.isRecommended) PadmaTealPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "ID: ${modelInfo.modelId}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = modelInfo.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // Custom Model Option
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        isCustomModelSelected = true
                                    },
                                color = if (isCustomModelSelected) PadmaTealPrimary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    if (isCustomModelSelected) 1.5.dp else 1.dp,
                                    if (isCustomModelSelected) PadmaTealPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .border(2.dp, if (isCustomModelSelected) PadmaTealPrimary else MaterialTheme.colorScheme.outline, CircleShape)
                                                .background(if (isCustomModelSelected) PadmaTealPrimary else Color.Transparent),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isCustomModelSelected) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "কাস্টম মডেল নাম (Custom Model Entry)",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCustomModelSelected) PadmaTealPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "ভবিষ্যতে আসা নতুন মডেল বা এক্সপেরিমেন্টাল মডেল আইডি টাইপ করুন",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (isCustomModelSelected) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = customModelInput.ifEmpty { if (allAvailableModels.none { it.modelId == selectedModel }) selectedModel else "" },
                                            onValueChange = { customModelInput = it },
                                            label = { Text("মডেল আইডি লিখুন") },
                                            placeholder = { Text("যেমন: gemini-2.0-flash-lite") },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            singleLine = true,
                                            trailingIcon = {
                                                Button(
                                                    onClick = {
                                                        val clean = customModelInput.trim()
                                                        if (clean.isNotEmpty()) {
                                                            selectedModel = clean
                                                            onModelSelected(clean)
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.padding(end = 4.dp)
                                                ) {
                                                    Text("প্রয়োগ", fontSize = 11.sp)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Connection Test
            OutlinedButton(
                onClick = {
                    scope.launch {
                        isTesting = true
                        testResult = "টেস্ট করা হচ্ছে..."
                        testResultType = 0
                        if (currentMode == OnlineEngineMode.GEMINI_AI) {
                            val res = LipiOnlineEngine.testGeminiConnection(
                                customApiKey = apiKeyInput,
                                model = selectedModel
                            )
                            isTesting = false
                            testResult = res.message
                            testResultType = when {
                                res.isHighDemandFallback -> 2
                                res.isSuccess -> 1
                                else -> 3
                            }
                        } else {
                            val res = LipiOnlineEngine.transliterateOnline(
                                inputText = "Khawya suru korli naki abr??",
                                provider = LipiOnlineEngine.Provider.GOOGLE_INPUT_TOOLS
                            )
                            isTesting = false
                            res.onSuccess {
                                testResult = "সফল! রূপান্তর: $it\nগুগল ক্লাউড ইঞ্জিন সম্পূর্ণ সচল।"
                                testResultType = 1
                            }.onFailure {
                                testResult = "ত্রুটি: ${it.message ?: "কানেকশন ব্যাহত হয়েছে"}"
                                testResultType = 3
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isTesting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("যাচাই করা হচ্ছে...")
                } else {
                    Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp), tint = PadmaTealPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (currentMode == OnlineEngineMode.GEMINI_AI) {
                            "অনলাইন কানেকশন টেস্ট ($selectedModel)"
                        } else {
                            "অনলাইন কানেকশন টেস্ট করুন (Test API)"
                        }
                    )
                }
            }

            if (testResult != null) {
                val bgColor = when (testResultType) {
                    1 -> PadmaTealPrimary.copy(alpha = 0.12f)
                    2 -> Color(0xFFF57C00).copy(alpha = 0.12f)
                    3 -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
                val borderColor = when (testResultType) {
                    1 -> PadmaTealPrimary.copy(alpha = 0.4f)
                    2 -> Color(0xFFF57C00).copy(alpha = 0.4f)
                    3 -> MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                    else -> Color.Transparent
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = bgColor,
                    border = BorderStroke(1.dp, borderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = testResult!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = when (testResultType) {
                            1 -> PadmaTealPrimary
                            2 -> Color(0xFFE65100)
                            3 -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

/**
 * 3-Step Guide Step Row Component.
 */
@Composable
fun GuideStepRow(
    icon: ImageVector,
    stepNumber: String,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(PadmaTealPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = PadmaTealPrimary
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}
