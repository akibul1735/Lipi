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

    private val _onlineEngineMode = MutableStateFlow(
        try {
            val saved = prefs.getString("online_engine_mode", OnlineEngineMode.OFFLINE_ONLY.name)
            OnlineEngineMode.valueOf(saved ?: OnlineEngineMode.OFFLINE_ONLY.name)
        } catch (e: Exception) {
            OnlineEngineMode.OFFLINE_ONLY
        }
    )
    val onlineEngineMode: StateFlow<OnlineEngineMode> = _onlineEngineMode.asStateFlow()

    private val _geminiApiKey = MutableStateFlow(
        prefs.getString("custom_gemini_api_key", "") ?: ""
    )
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _geminiModel = MutableStateFlow(
        prefs.getString("custom_gemini_model", "gemini-3.8-flash") ?: "gemini-3.8-flash"
    )
    val geminiModel: StateFlow<String> = _geminiModel.asStateFlow()

    private val _geminiModelFallback1 = MutableStateFlow(
        prefs.getString("custom_gemini_model_fallback_1", "gemini-3.8-flash-lite") ?: "gemini-3.8-flash-lite"
    )
    val geminiModelFallback1: StateFlow<String> = _geminiModelFallback1.asStateFlow()

    private val _geminiModelFallback2 = MutableStateFlow(
        prefs.getString("custom_gemini_model_fallback_2", "gemini-2.5-flash") ?: "gemini-2.5-flash"
    )
    val geminiModelFallback2: StateFlow<String> = _geminiModelFallback2.asStateFlow()

    private val _fetchedGeminiModels = MutableStateFlow<List<com.example.lipi.engine.LipiOnlineEngine.GeminiModelInfo>>(
        loadCachedGeminiModels()
    )
    val fetchedGeminiModels: StateFlow<List<com.example.lipi.engine.LipiOnlineEngine.GeminiModelInfo>> = _fetchedGeminiModels.asStateFlow()

    private val _customWordbook = MutableStateFlow<List<WordbookEntry>>(loadCustomWordbook())
    val customWordbook: StateFlow<List<WordbookEntry>> = _customWordbook.asStateFlow()

    private val _recentConversions = MutableStateFlow<List<ConversionResult>>(emptyList())
    val recentConversions: StateFlow<List<ConversionResult>> = _recentConversions.asStateFlow()

    fun setOnlineEngineMode(mode: OnlineEngineMode) {
        prefs.edit().putString("online_engine_mode", mode.name).apply()
        _onlineEngineMode.value = mode
    }

    fun setGeminiApiKey(key: String) {
        prefs.edit().putString("custom_gemini_api_key", key.trim()).apply()
        _geminiApiKey.value = key.trim()
    }

    fun setGeminiModel(model: String) {
        val cleanModel = model.trim().ifEmpty { "gemini-3.8-flash" }
        prefs.edit().putString("custom_gemini_model", cleanModel).apply()
        _geminiModel.value = cleanModel
    }

    fun setGeminiModelFallback1(model: String) {
        val cleanModel = model.trim().ifEmpty { "gemini-3.8-flash-lite" }
        prefs.edit().putString("custom_gemini_model_fallback_1", cleanModel).apply()
        _geminiModelFallback1.value = cleanModel
    }

    fun setGeminiModelFallback2(model: String) {
        val cleanModel = model.trim().ifEmpty { "gemini-2.5-flash" }
        prefs.edit().putString("custom_gemini_model_fallback_2", cleanModel).apply()
        _geminiModelFallback2.value = cleanModel
    }

    /**
     * Returns the ordered 3-tier Gemini model chain:
     * [Primary Model, Fallback Model 1, Fallback Model 2]
     */
    fun getGeminiModelsChain(): List<String> {
        val m1 = _geminiModel.value.trim().ifEmpty { "gemini-3.8-flash" }
        val m2 = _geminiModelFallback1.value.trim().ifEmpty { "gemini-3.8-flash-lite" }
        val m3 = _geminiModelFallback2.value.trim().ifEmpty { "gemini-2.5-flash" }
        val list = mutableListOf(m1)
        if (!list.contains(m2)) list.add(m2)
        if (!list.contains(m3)) list.add(m3)
        return list
    }

    fun setFetchedGeminiModels(models: List<com.example.lipi.engine.LipiOnlineEngine.GeminiModelInfo>) {
        _fetchedGeminiModels.value = models
        try {
            val jsonArray = JSONArray()
            for (m in models) {
                val obj = JSONObject().apply {
                    put("modelId", m.modelId)
                    put("displayName", m.displayName)
                    put("badge", m.badge)
                    put("description", m.description)
                    put("isRecommended", m.isRecommended)
                    put("isLiveFetched", m.isLiveFetched)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString("cached_gemini_models_json", jsonArray.toString()).apply()
        } catch (_: Exception) {}
    }

    private fun loadCachedGeminiModels(): List<com.example.lipi.engine.LipiOnlineEngine.GeminiModelInfo> {
        val saved = prefs.getString("cached_gemini_models_json", null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(saved)
            val list = mutableListOf<com.example.lipi.engine.LipiOnlineEngine.GeminiModelInfo>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    com.example.lipi.engine.LipiOnlineEngine.GeminiModelInfo(
                        modelId = obj.getString("modelId"),
                        displayName = obj.getString("displayName"),
                        badge = obj.optString("badge", "অনলাইন"),
                        description = obj.optString("description", ""),
                        isRecommended = obj.optBoolean("isRecommended", false),
                        isLiveFetched = obj.optBoolean("isLiveFetched", true)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

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
        val cleanBanglish = banglish.trim().lowercase()
        val cleanBengali = bengali.trim()
        val current = _customWordbook.value
        val existingIndex = current.indexOfFirst { it.banglish.equals(cleanBanglish, ignoreCase = true) }
        val updated = if (existingIndex >= 0) {
            val list = current.toMutableList()
            list[existingIndex] = list[existingIndex].copy(bengali = cleanBengali, category = category)
            list
        } else {
            current + WordbookEntry(
                banglish = cleanBanglish,
                bengali = cleanBengali,
                category = category
            )
        }
        saveCustomWordbook(updated)
    }

    /**
     * Automatically caches / learns word mappings from Gemini transliteration results (e.g. 'clg' -> 'কলেজ').
     * If an input word (like 'clg') becomes a Bengali word (like 'কলেজ') by Gemini,
     * it automatically saves it into the custom wordbook so future offline processing immediately has that mapping.
     * Returns the list of newly learned word pairs.
     */
    fun learnWordMappingFromGemini(inputText: String, geminiOutput: String): List<Pair<String, String>> {
        val inClean = inputText.trim()
        val outClean = geminiOutput.trim()
        if (inClean.isBlank() || outClean.isBlank()) return emptyList()

        val learned = mutableListOf<Pair<String, String>>()
        val inTokens = inClean.split(Regex("\\s+")).filter { it.isNotBlank() }
        val outTokens = outClean.split(Regex("\\s+")).filter { it.isNotBlank() }

        // Case 1: Single-word or slang input (e.g. "clg" -> "কলেজ", "varsity" -> "ভার্সিটি", "dept" -> "ডিপার্টমেন্ট")
        if (inTokens.size == 1 && outTokens.size == 1) {
            val banglish = inTokens[0].filter { it.isLetter() }.lowercase()
            val bengali = outTokens[0].trim(',', '.', '?', '!', '।', ';', ':', '"', '\'', '‘', '’', '“', '”')
            if (banglish.length >= 2 && bengali.any { it in '\u0980'..'\u09FF' }) {
                addWordbookEntry(banglish, bengali, "Gemini এআই লার্নিং")
                learned.add(Pair(banglish, bengali))
            }
        } else if (inTokens.size == outTokens.size && inTokens.size in 2..15) {
            // Case 2: 1-to-1 word correspondence in sentences (e.g. "ajke clg jabo" -> "আজকে কলেজ যাবো")
            for (i in inTokens.indices) {
                val rawIn = inTokens[i].filter { it.isLetter() }.lowercase()
                val rawOut = outTokens[i].trim(',', '.', '?', '!', '।', ';', ':', '"', '\'', '‘', '’', '“', '”')
                if (rawIn.length >= 2 && rawOut.any { it in '\u0980'..'\u09FF' }) {
                    val existing = _customWordbook.value.find { it.banglish.equals(rawIn, ignoreCase = true) }
                    if (existing == null || existing.bengali != rawOut) {
                        addWordbookEntry(rawIn, rawOut, "Gemini এআই লার্নিং")
                        learned.add(Pair(rawIn, rawOut))
                    }
                }
            }
        }
        return learned
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

    /**
     * Injects the curated extended chat dataset into the user's custom wordbook
     */
    fun loadTrainedChatDataset(): ImportResult {
        val baseMap = _customWordbook.value.associateBy { it.banglish.lowercase() }.toMutableMap()
        var addedCount = 0
        for ((banglish, bengali) in LipiDictionary.EXTENDED_TRAINED_CHAT_DATA) {
            val key = banglish.lowercase()
            if (!baseMap.containsKey(key)) {
                baseMap[key] = WordbookEntry(
                    banglish = key,
                    bengali = bengali,
                    category = "ট্রেইন ডাটা"
                )
                addedCount++
            }
        }
        val updated = baseMap.values.toList()
        saveCustomWordbook(updated)
        return ImportResult(
            addedCount = addedCount,
            skippedCount = LipiDictionary.EXTENDED_TRAINED_CHAT_DATA.size - addedCount,
            totalEntries = updated.size
        )
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
