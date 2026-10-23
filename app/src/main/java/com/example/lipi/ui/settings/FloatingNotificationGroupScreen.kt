package com.example.lipi.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.lipi.ui.LipiViewModel
import com.example.ui.theme.PadmaTealPrimary
import com.example.ui.theme.TerracottaSecondary

@Composable
fun FloatingNotificationGroupScreen(
    viewModel: LipiViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val isFloatingEnabled by viewModel.isFloatingServiceEnabled.collectAsState()
    val isPersistentNotificationEnabled by viewModel.isPersistentNotificationEnabled.collectAsState()
    val overlayGranted by viewModel.overlayPermissionGranted.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sub-screen Header with Back navigation
        SettingsSubScreenHeader(
            title = "ভাসমান সহকারী ও নোটিফিকেশন",
            subtitle = "স্ক্রিনের উপর ভাসমান বল ও স্ট্যাটাস বার সেটিংস",
            icon = Icons.Default.TouchApp,
            onBack = onBack
        )

        // Overlay Permission & Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (overlayGranted)
                    MaterialTheme.colorScheme.surface
                else
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (overlayGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (overlayGranted) PadmaTealPrimary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (overlayGranted) "অন্যান্য অ্যাপের উপর প্রদর্শন সক্রিয়" else "অনুমতি প্রয়োজন (Overlay)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (overlayGranted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = if (overlayGranted)
                                "ভাসমান 'A ⇄ অ' বল হোয়াটসঅ্যাপ, মেসেঞ্জার সহ যেকোনো অ্যাপের উপর ভেসে থাকবে।"
                            else
                                "ভাসমান বল প্রদর্শনের জন্য Android সিস্টেমে এই অনুমতি প্রদান করা আবশ্যক।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!overlayGranted) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("grant_overlay_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("অনুমতি প্রদান করুন (Allow Display Over Apps)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Toggles Card: Floating Assistant and Status Bar Notification
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
                    text = "সহকারী চালু বা বন্ধের বিকল্প",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Floating Assistant Switch
                SettingsSwitchRow(
                    title = "ভাসমান 'A ⇄ অ' সহকারী বল চালু রাখুন",
                    description = "হোয়াটসঅ্যাপ, মেসেঞ্জার বা যেকোনো অ্যাপের ওপর একটি ছোট গোলাকার 'A ⇄ অ' বাটন ভাসবে। ট্যাপ করলেই অনুবাদ উইন্ডো খুলবে।",
                    checked = isFloatingEnabled,
                    icon = Icons.Default.TouchApp,
                    testTag = "floating_master_switch",
                    onCheckedChange = { checked ->
                        if (checked && !overlayGranted) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                            Toast.makeText(context, "প্রথমে অন্যান্য অ্যাপের উপর প্রদর্শনের অনুমতি প্রদান করুন", Toast.LENGTH_LONG).show()
                        } else {
                            viewModel.setFloatingServiceEnabled(checked)
                            val serviceIntent = Intent(context, LipiOverlayService::class.java).apply {
                                action = if (checked) LipiOverlayService.ACTION_START_ASSISTANT else LipiOverlayService.ACTION_DISMISS
                            }
                            try {
                                if (checked) {
                                    androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
                                } else {
                                    context.startService(serviceIntent)
                                }
                                Toast.makeText(context, if (checked) "ভাসমান 'A ⇄ অ' অনুবাদক বল চালু হয়েছে!" else "ভাসমান বল লুকানো হয়েছে", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                )

                // Persistent Notification Bar Switch
                SettingsSwitchRow(
                    title = "স্ট্যাটাস বারে দ্রুত এক্সেস নোটিফিকেশন বার",
                    description = "মোবাইলের ওপরের নোটিফিকেশন ড্রয়ারে সার্বক্ষণিক একটি অ্যাকশন বার থাকবে, যেখান থেকে যেকোনো সময় ১-ট্যাপে ক্লিপবোর্ড থেকে অনুবাদ করা যাবে।",
                    checked = isPersistentNotificationEnabled,
                    icon = Icons.Default.Notifications,
                    testTag = "persistent_notification_switch",
                    onCheckedChange = { checked ->
                        viewModel.setPersistentNotificationEnabled(checked)
                        val serviceIntent = Intent(context, LipiOverlayService::class.java).apply {
                            action = LipiOverlayService.ACTION_UPDATE_NOTIFICATION
                        }
                        try {
                            androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
                            Toast.makeText(context, if (checked) "কুইক এক্সেস নোটিফিকেশন বার চালু হয়েছে!" else "নোটিফিকেশন বার বন্ধ করা হয়েছে", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                )
            }
        }

        // 3-Step Simple Copy & Translate Guide Card
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
    }
}
