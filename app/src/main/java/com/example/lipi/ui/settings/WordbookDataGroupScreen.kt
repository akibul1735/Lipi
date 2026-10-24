package com.example.lipi.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lipi.data.WordbookEntry
import com.example.lipi.ui.LipiViewModel
import com.example.ui.theme.PadmaTealPrimary
import com.example.ui.theme.TerracottaSecondary

@Composable
fun WordbookDataGroupScreen(
    viewModel: LipiViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customWordbook by viewModel.customWordbook.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var isSingleEntryExpanded by remember { mutableStateOf(true) }
    var banglishInput by remember { mutableStateOf("") }
    var bengaliInput by remember { mutableStateOf("") }

    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showInstructionsDialog by remember { mutableStateOf(false) }

    // Filtered words memoized
    val filteredWords = remember(searchQuery, customWordbook) {
        if (searchQuery.isBlank()) customWordbook
        else customWordbook.filter {
            it.banglish.contains(searchQuery, ignoreCase = true) || it.bengali.contains(searchQuery)
        }
    }

    // File picker for import
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val content = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                val result = viewModel.bulkImportWords(content, overwrite = false)
                if (result.errorMessage != null) {
                    Toast.makeText(context, result.errorMessage, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(
                        context,
                        "সফলভাবে ${result.addedCount}টি শব্দ যুক্ত হয়েছে! (মোট: ${result.totalEntries}টি)",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "ফাইল পড়তে সমস্যা হয়েছে: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // Sub-screen Header with Back navigation
        item(key = "header") {
            SettingsSubScreenHeader(
                title = "কাস্টম শব্দকোষ ও ট্রেইন ডাটা",
                subtitle = "ব্যক্তিগত ডিকশনারি, ব্যাকআপ ও ফাইল আমদানি/রপ্তানি",
                icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                onBack = onBack
            )
        }

        // Summary & 1-Tap Load Training Data Card
        item(key = "management_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "শব্দকোষ ব্যবস্থাপনা",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "মোট ${customWordbook.size}টি শব্দ সংরক্ষিত (অনুবাদে সর্বোচ্চ অগ্রাধিকার পাবে)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = { showInstructionsDialog = true }) {
                            Icon(
                                Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = "ফরম্যাট নির্দেশিকা",
                                tint = PadmaTealPrimary
                            )
                        }
                    }

                    // 1-Tap Load Extended Chat Training Data
                    OutlinedButton(
                        onClick = {
                            val result = viewModel.loadTrainedChatDataset()
                            Toast.makeText(
                                context,
                                "${result.addedCount}টি নতুন চ্যাট শব্দ (যেমন: suru→শুরু, abr→আবার, naki→নাকি ইত্যাদি) শব্দকোষে সফলভাবে যুক্ত হয়েছে!",
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("load_extended_chat_data_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PadmaTealPrimary),
                        border = BorderStroke(1.2.dp, PadmaTealPrimary.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = PadmaTealPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("অতিরিক্ত চ্যাট ট্রেইন ডাটা লোড করুন (১০০+ নতুন শব্দ)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Action Buttons Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { isSingleEntryExpanded = !isSingleEntryExpanded },
                            modifier = Modifier
                                .weight(1.15f)
                                .testTag("add_single_word_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSingleEntryExpanded) PadmaTealPrimary else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = if (isSingleEntryExpanded) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                "নতুন শব্দ",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                color = if (isSingleEntryExpanded) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedButton(
                            onClick = { showImportDialog = true },
                            modifier = Modifier.weight(1f).testTag("bulk_import_button"),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                "ইমপোর্ট",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        OutlinedButton(
                            onClick = { showExportDialog = true },
                            modifier = Modifier.weight(1f).testTag("export_data_button"),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                "এক্সপোর্ট",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Single Entry Input Drawer
                    AnimatedVisibility(visible = isSingleEntryExpanded) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, PadmaTealPrimary.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("single_data_entry_card")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "একটি নির্দিষ্ট বাংলিশ ও তার বাংলা অনুবাদ যুক্ত করুন:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = banglishInput,
                                    onValueChange = { banglishInput = it },
                                    label = { Text("বাংলিশ শব্দ / বাক্য") },
                                    placeholder = { Text("যেমন: kemon acho") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("single_data_entry_banglish_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = bengaliInput,
                                    onValueChange = { bengaliInput = it },
                                    label = { Text("বাংলা অনুবাদ") },
                                    placeholder = { Text("যেমন: কেমন আছো") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        if (banglishInput.isNotBlank() && bengaliInput.isNotBlank()) {
                                            viewModel.addCustomWord(banglishInput, bengaliInput)
                                            Toast.makeText(context, "'${banglishInput.trim()}' শব্দকোষে যুক্ত হয়েছে!", Toast.LENGTH_SHORT).show()
                                            banglishInput = ""
                                            bengaliInput = ""
                                            isSingleEntryExpanded = false
                                        } else {
                                            Toast.makeText(context, "বাংলিশ ও বাংলা উভয় ঘর পূরণ করুন", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("save_single_data_entry_button"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("শব্দকোষে সেভ করুন", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Reset and Clear Options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showResetConfirmDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ডিফল্ট রিস্টোর", fontSize = 11.sp, maxLines = 1, softWrap = false)
                        }

                        OutlinedButton(
                            onClick = { showClearConfirmDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("সব মুছুন", fontSize = 11.sp, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }

        // Custom Words List with Search Header
        item(key = "search_header") {
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
                        Text(
                            text = "সংরক্ষিত শব্দ তালিকা",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PadmaTealPrimary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${filteredWords.size}টি শব্দ",
                                style = MaterialTheme.typography.labelSmall,
                                color = PadmaTealPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("শব্দ খুঁজুন...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "মুছুন")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }
            }
        }

        if (filteredWords.isEmpty()) {
            item(key = "empty_words_notice") {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) "কোনো কাস্টম শব্দ যুক্ত করা হয়নি।" else "খুঁজে পাওয়া যায়নি।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(
                items = filteredWords,
                key = { it.id }
            ) { entry ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.banglish,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "→ ${entry.bengali}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PadmaTealPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = {
                                viewModel.deleteCustomWord(entry.id)
                                Toast.makeText(context, "'${entry.banglish}' মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "মুছুন",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Bulk Import Dialog
    if (showImportDialog) {
        var rawText by remember { mutableStateOf("") }
        var overwrite by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("শব্দকোষ আমদানি (Bulk Import)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "ফাইল নির্বাচন করুন বা সরাসরি টেক্সট পেস্ট করুন:", style = MaterialTheme.typography.bodySmall)

                    OutlinedButton(
                        onClick = {
                            showImportDialog = false
                            filePickerLauncher.launch("*/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ফাইল নির্বাচন করুন (.txt, .json, .csv)")
                    }

                    OutlinedTextField(
                        value = rawText,
                        onValueChange = { rawText = it },
                        placeholder = { Text("ফরম্যাট: banglish=বাংলা\nযেমন:\namar=আমার\ntomar=তোমার") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = overwrite, onCheckedChange = { overwrite = it })
                        Text("পূর্বের শব্দগুলো মুছে প্রতিস্থাপন করুন", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val res = viewModel.bulkImportWords(rawText, overwrite)
                        showImportDialog = false
                        Toast.makeText(context, "${res.addedCount}টি শব্দ যুক্ত হয়েছে!", Toast.LENGTH_SHORT).show()
                    }
                ) { Text("ইমপোর্ট করুন") }
            },
            dismissButton = { OutlinedButton(onClick = { showImportDialog = false }) { Text("বাতিল") } }
        )
    }

    // Export Dialog
    if (showExportDialog) {
        var exportFormat by remember { mutableStateOf("txt") }

        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("শব্দকোষ রপ্তানি (Export Backup)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("মোট ${customWordbook.size}টি শব্দ ব্যাকআপ হিসেবে সংরক্ষণ বা শেয়ার করতে পারেন:")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { exportFormat = "txt" },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (exportFormat == "txt") PadmaTealPrimary else MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text("টেক্সট (.txt)", fontSize = 12.sp, maxLines = 1, softWrap = false)
                        }
                        Button(
                            onClick = { exportFormat = "json" },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (exportFormat == "json") PadmaTealPrimary else MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text("JSON (.json)", fontSize = 12.sp, maxLines = 1, softWrap = false)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val data = if (exportFormat == "txt") viewModel.exportWordsAsText() else viewModel.exportWordsAsJson()
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Lipi Wordbook Export", data))
                        showExportDialog = false
                        Toast.makeText(context, "ক্লিপবোর্ডে কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                    }
                ) { Text("ক্লিপবোর্ডে কপি করুন") }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        val data = if (exportFormat == "txt") viewModel.exportWordsAsText() else viewModel.exportWordsAsJson()
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, data)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "শব্দকোষ ব্যাকআপ শেয়ার"))
                        showExportDialog = false
                    }
                ) { Text("শেয়ার করুন") }
            }
        )
    }

    // Reset Confirm Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("ডিফল্ট শব্দকোষে ফিরবেন?") },
            text = { Text("আপনার তৈরি করা অতিরিক্ত শব্দগুলো মুছে ডিফল্ট মৌলিক শব্দকোষ সেট করা হবে।") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetWordsToDefault()
                        showResetConfirmDialog = false
                        Toast.makeText(context, "ডিফল্ট শব্দকোষ সেট করা হয়েছে", Toast.LENGTH_SHORT).show()
                    }
                ) { Text("হ্যাঁ, রিস্টোর করুন") }
            },
            dismissButton = { OutlinedButton(onClick = { showResetConfirmDialog = false }) { Text("বাতিল") } }
        )
    }

    // Clear Confirm Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("সব শব্দ মুছে ফেলবেন?") },
            text = { Text("ব্যক্তিগত শব্দকোষের সমস্ত এন্ট্রি সম্পূর্ণ মুছে যাবে।") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllCustomWords()
                        showClearConfirmDialog = false
                        Toast.makeText(context, "সমস্ত শব্দকোষ খালি করা হয়েছে", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("মুছে ফেলুন") }
            },
            dismissButton = { OutlinedButton(onClick = { showClearConfirmDialog = false }) { Text("বাতিল") } }
        )
    }

    // Instructions Dialog
    if (showInstructionsDialog) {
        AlertDialog(
            onDismissRequest = { showInstructionsDialog = false },
            title = { Text("শব্দকোষ ফরম্যাট নির্দেশিকা", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("সমর্থিত ফরম্যাটসমূহ:", fontWeight = FontWeight.SemiBold)
                    Text("১. সমান চিহ্ন: banglish=বাংলা (যেমন: kmn=কেমন)\n২. কমা চিহ্ন: banglish,বাংলা\n৩. কোলন চিহ্ন: banglish:বাংলা\n৪. JSON অ্যারে: [{\"banglish\":\"...\", \"bengali\":\"...\"}]")
                }
            },
            confirmButton = { Button(onClick = { showInstructionsDialog = false }) { Text("বুঝেছি") } }
        )
    }
}
