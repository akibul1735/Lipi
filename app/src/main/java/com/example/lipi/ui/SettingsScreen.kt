package com.example.lipi.ui

import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.lipi.ui.settings.EngineFormattingGroupScreen
import com.example.lipi.ui.settings.FloatingNotificationGroupScreen
import com.example.lipi.ui.settings.SettingsGroup
import com.example.lipi.ui.settings.SettingsGroupCard
import com.example.lipi.ui.settings.SystemBatteryGroupScreen
import com.example.lipi.ui.settings.WordbookDataGroupScreen
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.PadmaTealPrimary

/**
 * Hierarchical, page-based Settings Screen for Lipi.
 * 
 * Root Screen: Clean list of settings category cards with icons, subtitles, dynamic status badges, and trailing chevrons.
 * Sub Screens: Dedicated group details screens for each category with back-navigation.
 */
@Composable
fun SettingsScreen(
    viewModel: LipiViewModel,
    modifier: Modifier = Modifier
) {
    var activeGroup by rememberSaveable { mutableStateOf<SettingsGroup?>(null) }

    // Intercept hardware and gesture back button when inside a group details screen
    BackHandler(enabled = activeGroup != null) {
        activeGroup = null
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        when (activeGroup) {
            null -> {
                MainSettingsMenu(
                    viewModel = viewModel,
                    onOpenGroup = { selectedGroup ->
                        activeGroup = selectedGroup
                    }
                )
            }
            SettingsGroup.FLOATING_NOTIFICATION -> {
                FloatingNotificationGroupScreen(
                    viewModel = viewModel,
                    onBack = { activeGroup = null }
                )
            }
            SettingsGroup.ENGINE_FORMATTING -> {
                EngineFormattingGroupScreen(
                    viewModel = viewModel,
                    onBack = { activeGroup = null }
                )
            }
            SettingsGroup.WORDBOOK_DATA -> {
                WordbookDataGroupScreen(
                    viewModel = viewModel,
                    onBack = { activeGroup = null }
                )
            }
            SettingsGroup.SYSTEM_BATTERY -> {
                SystemBatteryGroupScreen(
                    viewModel = viewModel,
                    onBack = { activeGroup = null }
                )
            }
        }
    }
}

/**
 * Root Settings Menu showing only clean, unexpanded group cards with icons and trailing chevrons.
 */
@Composable
fun MainSettingsMenu(
    viewModel: LipiViewModel,
    onOpenGroup: (SettingsGroup) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val isFloatingEnabled by viewModel.isFloatingServiceEnabled.collectAsState()
    val isPersistentNotificationEnabled by viewModel.isPersistentNotificationEnabled.collectAsState()
    val onlineEngineMode by viewModel.onlineEngineMode.collectAsState()
    val customWordbook by viewModel.customWordbook.collectAsState()
    val overlayGranted by viewModel.overlayPermissionGranted.collectAsState()

    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
    val isIgnoringBattery = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
        } else true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // App Settings Hero Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(PadmaTealPrimary, CyanAccent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "সেটিংস",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "অ্যাপ সেটিংস",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PadmaTealPrimary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "v1.4.0",
                                style = MaterialTheme.typography.labelSmall,
                                color = PadmaTealPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "লিপির ভাসমান ফিচার, অনুবাদ ইঞ্জিন ও ব্যক্তিগত ডাটা কনফিগার করুন",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Group 1: Floating & Notifications
        SettingsGroupCard(
            group = SettingsGroup.FLOATING_NOTIFICATION,
            badgeText = if (isFloatingEnabled) "সক্রিয়" else "বন্ধ",
            onClick = { onOpenGroup(SettingsGroup.FLOATING_NOTIFICATION) }
        )

        val engineBadge = when (onlineEngineMode) {
            com.example.lipi.data.OnlineEngineMode.OFFLINE_ONLY -> "অফলাইন"
            com.example.lipi.data.OnlineEngineMode.ONLINE_CLOUD -> "Google ক্লাউড"
            com.example.lipi.data.OnlineEngineMode.GEMINI_AI -> "Gemini AI"
        }

        // Group 2: Engine & Formatting
        SettingsGroupCard(
            group = SettingsGroup.ENGINE_FORMATTING,
            badgeText = engineBadge,
            onClick = { onOpenGroup(SettingsGroup.ENGINE_FORMATTING) }
        )

        // Group 3: Wordbook & Data Management
        SettingsGroupCard(
            group = SettingsGroup.WORDBOOK_DATA,
            badgeText = "${customWordbook.size}টি শব্দ",
            onClick = { onOpenGroup(SettingsGroup.WORDBOOK_DATA) }
        )

        // Group 4: System & Battery Protection
        SettingsGroupCard(
            group = SettingsGroup.SYSTEM_BATTERY,
            badgeText = if (overlayGranted && isIgnoringBattery) "সুরক্ষিত" else "সতর্কতা",
            onClick = { onOpenGroup(SettingsGroup.SYSTEM_BATTERY) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Privacy & System Security Tip
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PadmaTealPrimary.copy(alpha = 0.06f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PadmaTealPrimary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "নিরাপত্তা",
                        tint = PadmaTealPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "১০০% নিরাপদ ও অন-ডিভাইস প্রাইভেসি",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = PadmaTealPrimary
                    )
                    Text(
                        text = "লিপি কোনো ব্যক্তিগত বার্তা বা কীস্ট্রোক রেকর্ড করে না। যেকোনো ক্যাটাগরিতে ট্যাপ করে বিস্তারিত সেটিংস পরিবর্তন করুন।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
