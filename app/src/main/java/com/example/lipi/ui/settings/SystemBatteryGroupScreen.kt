package com.example.lipi.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lipi.ui.LipiViewModel
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.PadmaTealPrimary
import com.example.ui.theme.TerracottaSecondary

@Composable
fun SystemBatteryGroupScreen(
    viewModel: LipiViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val overlayGranted by viewModel.overlayPermissionGranted.collectAsState()

    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
    val isIgnoringBatteryOptimizations = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
        } else true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Navigation Header
        SettingsSubScreenHeader(
            title = SettingsGroup.SYSTEM_BATTERY.titleBn,
            subtitle = SettingsGroup.SYSTEM_BATTERY.subtitleBn,
            icon = SettingsGroup.SYSTEM_BATTERY.icon,
            onBack = onBack
        )

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

                PermissionStatusItem(
                    title = "ভাসমান উইন্ডো অনুমতি (Display over other apps)",
                    description = "যেকোনো চ্যাট বা মেসেজিং অ্যাপের ওপর লিপির ভাসমান বাটন ও প্রিভিউ উইন্ডো দেখানোর জন্য আবশ্যক।",
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
                            text = "কোনো সংবেদনশীল অ্যাক্সেসিবিলিটি বা গোপনীয় পারমিশন ছাড়াই নিরাপদ কপি-পেস্টের মাধ্যমে লিপি কাজ করে। গুগল প্লে স্টোরে ১০০% পলিসি অনুসারী।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Battery Optimization & Long-running Service Protection Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isIgnoringBatteryOptimizations)
                    MaterialTheme.colorScheme.surface
                else
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isIgnoringBatteryOptimizations) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isIgnoringBatteryOptimizations) PadmaTealPrimary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "সার্বক্ষণিক নোটিফিকেশন ও ব্যাকগ্রাউন্ড রক্ষা",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isIgnoringBatteryOptimizations)
                        "✓ ব্যাটারি অপটিমাইজেশন বন্ধ রয়েছে। লিপিকে ব্যাকগ্রাউন্ডে অ্যান্ড্রয়েড সিস্টেম বা OEM সেভার কিল করবে না এবং নোটিফিকেশন বার সার্বক্ষণিক দৃশ্যমান থাকবে।"
                    else
                        "কিছু ডিভাইসে (Xiaomi, Samsung, Oppo, Vivo, Realme) কিছুক্ষণ পর সিস্টেম ব্যাকগ্রাউন্ড সার্ভিস ও নোটিফিকেশন বন্ধ করে দেয়। নোটিফিকেশন ও অনুবাদ বল সর্বদা চালু রাখতে ব্যাটারি অপটিমাইজেশন বন্ধ (Unrestricted) করুন।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!isIgnoringBatteryOptimizations) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val intent = Intent().apply {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                                } else {
                                    action = Settings.ACTION_SETTINGS
                                }
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "সেটিংস খোলা সম্ভব হয়নি", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PadmaTealPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ব্যাটারি আনরেস্ট্রিকটেড (Unrestricted) করুন", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Privacy & Offline Security Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PadmaTealPrimary.copy(alpha = 0.08f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun PermissionStatusItem(
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
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.testTag("overlay_permission_action_btn")
        ) {
            Text(if (isGranted) "যাচাই করুন" else "অনুমতি দিন", fontSize = 12.sp)
        }
    }
}
