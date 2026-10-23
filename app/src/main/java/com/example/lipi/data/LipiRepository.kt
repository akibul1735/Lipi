package com.example.lipi.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class LipiRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("lipi_preferences", Context.MODE_PRIVATE)

    private val _isFloatingServiceEnabled = MutableStateFlow(
        prefs.getBoolean("floating_enabled", true)
    )
    val isFloatingServiceEnabled: StateFlow<Boolean> = _isFloatingServiceEnabled.asStateFlow()

    private val _isPersistentNotificationEnabled = MutableStateFlow(
        prefs.getBoolean("persistent_notification_enabled", true)
    )
    val isPersistentNotificationEnabled: StateFlow<Boolean> = _isPersistentNotificationEnabled.asStateFlow()

    private val _convertDigitsToBengali = MutableStateFlow(
        prefs.getBoolean("convert_digits", false)
    )
    val convertDigitsToBengali: StateFlow<Boolean> = _convertDigitsToBengali.asStateFlow()

    private val _isAutoClipboardMonitoringEnabled = MutableStateFlow(
        prefs.getBoolean("auto_clipboard_monitoring", true)
    )
    val isAutoClipboardMonitoringEnabled: StateFlow<Boolean> = _isAutoClipboardMonitoringEnabled.asStateFlow()

    private val _transliterationStyle = MutableStateFlow(
        try {
            val saved = prefs.getString("transliteration_style", TransliterationStyle.ALL_BENGALI.name)
            TransliterationStyle.valueOf(saved ?: TransliterationStyle.ALL_BENGALI.name)
        } catch (e: Exception) {
            TransliterationStyle.ALL_BENGALI
        }
    )
    val transliterationStyle: StateFlow<TransliterationStyle> = _transliterationStyle.asStateFlow()

    private val _customWordbook = MutableStateFlow<List<WordbookEntry>>(loadCustomWordbook())
    val customWordbook: StateFlow<List<WordbookEntry>> = _customWordbook.asStateFlow()

    private val _recentConversions = MutableStateFlow<List<ConversionResult>>(emptyList())
    val recentConversions: StateFlow<List<ConversionResult>> = _recentConversions.asStateFlow()

    fun setPersistentNotificationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("persistent_notification_enabled", enabled).apply()
        _isPersistentNotificationEnabled.value = enabled
    }

    fun setTransliterationStyle(style: TransliterationStyle) {
        prefs.edit().putString("transliteration_style", style.name).apply()
        _transliterationStyle.value = style
    }

    fun setAutoClipboardMonitoringEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("auto_clipboard_monitoring", enabled).apply()
        _isAutoClipboardMonitoringEnabled.value = enabled
    }

    fun setFloatingServiceEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("floating_enabled", enabled).apply()
        _isFloatingServiceEnabled.value = enabled
    }

    fun setConvertDigitsToBengali(enabled: Boolean) {
        prefs.edit().putBoolean("convert_digits", enabled).apply()
        _convertDigitsToBengali.value = enabled
    }

    private fun loadCustomWordbook(): List<WordbookEntry> {
        val saved = prefs.getString("custom_wordbook_json", null)
        val defaultEntries = listOf(
            WordbookEntry(banglish = "ki obosta", bengali = "কী অবস্থা", category = "কথোপকথন"),
            WordbookEntry(banglish = "pera nai", bengali = "প্যারা নাই", category = "কথোপকথন"),
            WordbookEntry(banglish = "chill koro", bengali = "চিল করো", category = "কথোপকথন")
        )
        if (saved == null) return defaultEntries
        return try {
            val jsonArray = JSONArray(saved)
            val list = mutableListOf<WordbookEntry>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    WordbookEntry(
                        id = obj.getString("id"),
                        banglish = obj.getString("banglish"),
                        bengali = obj.getString("bengali"),
                        category = obj.optString("category", "কাস্টম")
                    )
                )
            }
            list
        } catch (e: Exception) {
            defaultEntries
        }
    }

    fun addWordbookEntry(banglish: String, bengali: String, category: String = "কাস্টম") {
        val newEntry = WordbookEntry(
            banglish = banglish.trim().lowercase(),
            bengali = bengali.trim(),
            category = category
        )
        val updated = _customWordbook.value + newEntry
        saveCustomWordbook(updated)
    }

    fun removeWordbookEntry(id: String) {
        val updated = _customWordbook.value.filter { it.id != id }
        saveCustomWordbook(updated)
    }

    private fun saveCustomWordbook(entries: List<WordbookEntry>) {
        val jsonArray = JSONArray()
        entries.forEach { entry ->
            val obj = JSONObject().apply {
                put("id", entry.id)
                put("banglish", entry.banglish)
                put("bengali", entry.bengali)
                put("category", entry.category)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString("custom_wordbook_json", jsonArray.toString()).apply()
        _customWordbook.value = entries
    }

    /**
     * Imports custom data from raw text (file or paste).
     * Supports formats:
     * - banglish=bengali
     * - banglish,bengali
     * - banglish:bengali
     * - banglish \t bengali
     * - JSON array: [{"banglish": "...", "bengali": "..."}, ...]
     */
    fun bulkImport(rawContent: String, overwrite: Boolean = false): ImportResult {
        if (rawContent.isBlank()) {
            return ImportResult(0, 0, _customWordbook.value.size, "ফাইল খালি বা কোনো তথ্য পাওয়া যায়নি")
        }

        val trimmed = rawContent.trim()
        val parsedEntries = mutableListOf<Pair<String, String>>()
        var skippedCount = 0

        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            // JSON format
            try {
                val jsonArray = JSONArray(trimmed)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.optJSONObject(i)
                    if (obj != null) {
                        val banglish = obj.optString("banglish", "").trim().lowercase()
                        val bengali = obj.optString("bengali", "").trim()
                        if (banglish.isNotEmpty() && bengali.isNotEmpty()) {
                            parsedEntries.add(banglish to bengali)
                        } else {
                            skippedCount++
                        }
                    }
                }
            } catch (e: Exception) {
                return ImportResult(0, 0, _customWordbook.value.size, "JSON ফাইল পার্স করতে ত্রুটি: ${e.message}")
            }
        } else {
            // Line-by-line format
            val lines = trimmed.lines()
            for (line in lines) {
                val lineTrimmed = line.trim()
                if (lineTrimmed.isEmpty() || lineTrimmed.startsWith("#") || lineTrimmed.startsWith("//")) {
                    continue // Comment or empty line
                }

                // Detect delimiter (=, ,, :, or \t)
                val parts = when {
                    lineTrimmed.contains("=") -> lineTrimmed.split("=", limit = 2)
                    lineTrimmed.contains("\t") -> lineTrimmed.split("\t", limit = 2)
                    lineTrimmed.contains(":") -> lineTrimmed.split(":", limit = 2)
                    lineTrimmed.contains(",") -> lineTrimmed.split(",", limit = 2)
                    else -> null
                }

                if (parts != null && parts.size == 2) {
                    val key = parts[0].trim().lowercase()
                    val value = parts[1].trim()
                    if (key.isNotEmpty() && value.isNotEmpty()) {
                        parsedEntries.add(key to value)
                    } else {
                        skippedCount++
                    }
                } else {
                    skippedCount++
                }
            }
        }

        if (parsedEntries.isEmpty()) {
            return ImportResult(0, skippedCount, _customWordbook.value.size, "সঠিক ফরম্যাটের কোনো শব্দ পাওয়া যায়নি। ফাইল ফরম্যাট নির্দেশিকা চেক করুন।")
        }

        // Merge or overwrite
        val baseMap = if (overwrite) {
            mutableMapOf<String, WordbookEntry>()
        } else {
            _customWordbook.value.associateBy { it.banglish.lowercase() }.toMutableMap()
        }

        var addedCount = 0
        for ((banglish, bengali) in parsedEntries) {
            val existing = baseMap[banglish]
            if (existing != null) {
                baseMap[banglish] = existing.copy(bengali = bengali)
            } else {
                baseMap[banglish] = WordbookEntry(
                    banglish = banglish,
                    bengali = bengali,
                    category = "ইমপোর্ট"
                )
            }
            addedCount++
        }

        val updatedList = baseMap.values.toList()
        saveCustomWordbook(updatedList)

        return ImportResult(
            addedCount = addedCount,
            skippedCount = skippedCount,
            totalEntries = updatedList.size
        )
    }

    /**
     * Exports custom wordbook as a formatted text file content
     */
    fun exportCustomDataAsText(): String {
        val entries = _customWordbook.value
        val sb = StringBuilder()
        sb.append("# ==========================================\n")
        sb.append("# Lipi Custom Dictionary Export (লিপি শব্দকোষ)\n")
        sb.append("# মোট শব্দ সংখ্যা: ${entries.size}\n")
        sb.append("# ফরম্যাট: banglish=বাংলা\n")
        sb.append("# ==========================================\n\n")

        entries.forEach { entry ->
            sb.append("${entry.banglish}=${entry.bengali}\n")
        }
        return sb.toString()
    }

    /**
     * Exports custom wordbook as formatted JSON
     */
    fun exportCustomDataAsJson(): String {
        val entries = _customWordbook.value
        val jsonArray = JSONArray()
        entries.forEach { entry ->
            val obj = JSONObject().apply {
                put("banglish", entry.banglish)
                put("bengali", entry.bengali)
                put("category", entry.category)
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString(2)
    }

    fun clearCustomWordbook() {
        saveCustomWordbook(emptyList())
    }

    fun resetToDefaultWordbook() {
        val defaultEntries = listOf(
            WordbookEntry(banglish = "ki obosta", bengali = "কী অবস্থা", category = "কথোপকথন"),
            WordbookEntry(banglish = "pera nai", bengali = "প্যারা নাই", category = "কথোপকথন"),
            WordbookEntry(banglish = "chill koro", bengali = "চিল করো", category = "কথোপকথন"),
            WordbookEntry(banglish = "dhonnobad", bengali = "ধন্যবাদ", category = "সাধারণ"),
            WordbookEntry(banglish = "shagotom", bengali = "স্বাগতম", category = "সাধারণ")
        )
        saveCustomWordbook(defaultEntries)
    }

    fun addRecentConversion(result: ConversionResult) {
        val current = _recentConversions.value.toMutableList()
        current.add(0, result)
        if (current.size > 20) {
            _recentConversions.value = current.take(20)
        } else {
            _recentConversions.value = current
        }
    }
}
