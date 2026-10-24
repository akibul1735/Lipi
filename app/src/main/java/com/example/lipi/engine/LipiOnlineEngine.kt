package com.example.lipi.engine

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import com.example.lipi.data.OnlineEngineMode
import com.example.lipi.data.LipiMode
import com.example.lipi.data.TransliterationStyle

/**
 * Online Cloud & AI Transliteration Engine for Lipi.
 *
 * Provides:
 * 1. Google Input Tools Cloud API: Fast, free online transliteration specifically optimized
 *    for complex Banglish sentences, slang, and phonetic ambiguities.
 * 2. Gemini AI Engine: Contextual neural translation and spell correction for informal chat,
 *    idioms, and conversational nuances.
 * 3. Graceful offline fallback: Never blocks or crashes when offline.
 */
object LipiOnlineEngine {

    enum class Provider(val id: String, val displayName: String, val subtitle: String) {
        GOOGLE_INPUT_TOOLS(
            "google_input_tools",
            "Google ক্লাউড ট্রান্সলিটারেশন",
            "উচ্চ নির্ভুলতা ও দ্রুততম ক্লাউড ইঞ্জিন (কোনো এপিআই কী ছাড়াই কার্যকর)"
        ),
        GEMINI_AI(
            "gemini_ai",
            "Gemini AI স্মার্ট ইঞ্জিন",
            "উন্নত কনটেক্সট-সচেতন এআই — বাংলিশ স্ল্যাং ও বাক্যার্থের সর্বোচ্চ শুদ্ধতা"
        )
    }

    data class GeminiModelInfo(
        val modelId: String,
        val displayName: String,
        val badge: String,
        val description: String,
        val groupLabel: String = "Gemini 3.x (সর্বাধুনিক প্রজন্ম)",
        val isRecommended: Boolean = false,
        val isLiveFetched: Boolean = false
    )

    /**
     * Determines which generation/group a Gemini model belongs to:
     * - Latest Group: Gemini 3.x (3.8 Flash, 3.8 Flash-Lite, 3.8 Pro, 3.0 Flash)
     * - Previous Group 1: Gemini 2.5 (2.5 Flash, 2.5 Flash-Lite, 2.5 Pro)
     * - Previous Group 2: Gemini 2.0 (2.0 Flash, 2.0 Flash-Lite, 2.0 Pro)
     * - Previous Group 3: Gemini 1.5 (1.5 Flash, 1.5 Flash-8B, 1.5 Pro)
     */
    fun determineModelGroup(modelId: String): String {
        val id = modelId.lowercase()
        return when {
            id.contains("3.8") || id.contains("3.0") || id.contains("3.") -> "Gemini 3.x (সর্বাধুনিক প্রজন্ম — Flash & Flash-Lite)"
            id.contains("2.5") -> "Gemini 2.5 (পূর্ববর্তী গ্রুপ ১)"
            id.contains("2.0") || id.contains("2.") -> "Gemini 2.0 (পূর্ববর্তী গ্রুপ ২)"
            id.contains("1.5") || id.contains("1.0") || id.contains("1.") -> "Gemini 1.5 (পূর্ববর্তী গ্রুপ ৩)"
            else -> "অন্যান্য Gemini মডেল"
        }
    }

    /**
     * Determines whether a Gemini model is suitable for Lipi (text chat, transliteration, high speed).
     * Excludes embeddings, audio, vision, robotics, and experimental tuning models.
     */
    fun isModelSuitableForLipi(modelId: String): Boolean {
        val id = modelId.lowercase()
        if (!id.contains("gemini")) return false
        val unsuitableKeywords = listOf(
            "embedding", "imagen", "veo", "vision", "audio", "voice",
            "robotics", "computer-use", "aqa", "code-instruct", "medlm",
            "custom", "tuning", "test", "internal", "tts", "asr"
        )
        if (unsuitableKeywords.any { id.contains(it) }) return false
        return id.contains("flash") || id.contains("lite") || id.contains("pro") || id.contains("8b") || id.contains("gemini-")
    }

    val SUITABLE_GEMINI_MODELS = listOf(
        // Group: Gemini 3.x (Latest Generation)
        GeminiModelInfo(
            modelId = "gemini-3.8-flash",
            displayName = "Gemini 3.8 Flash",
            badge = "সর্বাধুনিক ৩.৮ ফ্ল্যাশ",
            description = "নতুন ৩.৮ সংস্করণ — অতি দ্রুত ও বুদ্ধিমান (অনলাইন সমস্যায় স্বয়ংক্রিয় অফলাইন ব্যাকআপ)",
            groupLabel = "Gemini 3.x (সর্বাধুনিক প্রজন্ম — Flash & Flash-Lite)",
            isRecommended = true
        ),
        GeminiModelInfo(
            modelId = "gemini-3.8-flash-lite",
            displayName = "Gemini 3.8 Flash-Lite",
            badge = "সর্বাধুনিক ৩.৮ লাইট",
            description = "সর্বাধুনিক ৩.৮ ফ্ল্যাশ-লাইট — আল্ট্রা লো-ল্যাটেন্সি দ্রুততম রেসপন্স ও সাশ্রয়ী কোটা ব্যবহার",
            groupLabel = "Gemini 3.x (সর্বাধুনিক প্রজন্ম — Flash & Flash-Lite)",
            isRecommended = true
        ),
        GeminiModelInfo(
            modelId = "gemini-3.8-pro",
            displayName = "Gemini 3.8 Pro",
            badge = "সর্বাধুনিক ৩.৮ প্রো",
            description = "জটিল বাক্য, ব্যাকরণগত শুদ্ধতা ও গভীর অর্থ বিশ্লেষণের জন্য শক্তিশালী ৩.৮ প্রো সংস্করণ",
            groupLabel = "Gemini 3.x (সর্বাধুনিক প্রজন্ম — Flash & Flash-Lite)",
            isRecommended = false
        ),
        GeminiModelInfo(
            modelId = "gemini-3.0-flash",
            displayName = "Gemini 3.0 Flash",
            badge = "৩.০ ফ্ল্যাশ",
            description = "দ্রুত ও বুদ্ধিমান ৩.০ ফ্ল্যাশ মডেল",
            groupLabel = "Gemini 3.x (সর্বাধুনিক প্রজন্ম — Flash & Flash-Lite)",
            isRecommended = false
        ),

        // Group: Gemini 2.5 (Previous Group 1)
        GeminiModelInfo(
            modelId = "gemini-2.5-flash",
            displayName = "Gemini 2.5 Flash",
            badge = "২.৫ ফ্ল্যাশ",
            description = "লিপির জন্য অত্যন্ত নির্ভরযোগ্য ও দ্রুততম মডেল — কোনো ডাউনটাইম ছাড়া কার্যকর",
            groupLabel = "Gemini 2.5 (পূর্ববর্তী গ্রুপ ১)",
            isRecommended = false
        ),
        GeminiModelInfo(
            modelId = "gemini-2.5-flash-lite",
            displayName = "Gemini 2.5 Flash-Lite",
            badge = "২.৫ ফ্ল্যাশ লাইট",
            description = "হালকা ও অত্যন্ত দ্রুত ২.৫ সংস্করণ",
            groupLabel = "Gemini 2.5 (পূর্ববর্তী গ্রুপ ১)",
            isRecommended = false
        ),
        GeminiModelInfo(
            modelId = "gemini-2.5-pro",
            displayName = "Gemini 2.5 Pro",
            badge = "২.৫ প্রো কোয়ালিটি",
            description = "উচ্চ নির্ভুলতা ও বিস্তারিত ভাষাগত বিশ্লেষণ",
            groupLabel = "Gemini 2.5 (পূর্ববর্তী গ্রুপ ১)",
            isRecommended = false
        ),

        // Group: Gemini 2.0 (Previous Group 2)
        GeminiModelInfo(
            modelId = "gemini-2.0-flash",
            displayName = "Gemini 2.0 Flash",
            badge = "২.০ ফ্ল্যাশ",
            description = "আল্ট্রা লো-ল্যাটেন্সি দ্রুততম রেসপন্স টাইম ও নির্ভরযোগ্য পারফরম্যান্স",
            groupLabel = "Gemini 2.0 (পূর্ববর্তী গ্রুপ ২)",
            isRecommended = false
        ),
        GeminiModelInfo(
            modelId = "gemini-2.0-flash-lite",
            displayName = "Gemini 2.0 Flash-Lite",
            badge = "২.০ লাইট",
            description = "দ্রুততম রেসপন্স ও অতি হালকা ক্লাউড রিসোর্স ব্যবহার",
            groupLabel = "Gemini 2.0 (পূর্ববর্তী গ্রুপ ২)",
            isRecommended = false
        ),

        // Group: Gemini 1.5 (Previous Group 3)
        GeminiModelInfo(
            modelId = "gemini-1.5-flash",
            displayName = "Gemini 1.5 Flash",
            badge = "১.৫ ফ্ল্যাশ",
            description = "জনপ্রিয় ও বিশ্বস্ত ক্লাসিক স্ট্যান্ডার্ড সংস্করণ",
            groupLabel = "Gemini 1.5 (পূর্ববর্তী গ্রুপ ৩)",
            isRecommended = false
        ),
        GeminiModelInfo(
            modelId = "gemini-1.5-flash-8b",
            displayName = "Gemini 1.5 Flash-8B",
            badge = "১.৫ ফ্ল্যাশ ৮বি",
            description = "অত্যন্ত হালকা ৮বি প্যারামিটার মডেল",
            groupLabel = "Gemini 1.5 (পূর্ববর্তী গ্রুপ ৩)",
            isRecommended = false
        ),
        GeminiModelInfo(
            modelId = "gemini-1.5-pro",
            displayName = "Gemini 1.5 Pro",
            badge = "১.৫ প্রো",
            description = "উচ্চ প্রিসিশন সমৃদ্ধ ক্লাসিক প্রো মডেল",
            groupLabel = "Gemini 1.5 (পূর্ববর্তী গ্রুপ ৩)",
            isRecommended = false
        )
    )

    val AVAILABLE_GEMINI_MODELS = SUITABLE_GEMINI_MODELS

    /**
     * Dynamically fetches the latest Gemini models available for the user's API key from Google Generative AI API.
     */
    suspend fun fetchAvailableModels(customApiKey: String = ""): Result<List<GeminiModelInfo>> = withContext(Dispatchers.IO) {
        val effectiveApiKey = when {
            customApiKey.isNotBlank() -> customApiKey.trim()
            try { BuildConfig.GEMINI_API_KEY.isNotBlank() } catch (_: Exception) { false } -> BuildConfig.GEMINI_API_KEY
            else -> ""
        }

        if (effectiveApiKey.isBlank() || effectiveApiKey == "your_api_key_here") {
            return@withContext Result.failure(Exception("মডেল ফেচ করতে বৈধ API Key প্রয়োজন।"))
        }

        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models?key=$effectiveApiKey"
            val url = URL(endpoint)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 10000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; LipiApp)")

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
                val responseStr = reader.use { it.readText() }
                connection.disconnect()

                val rootObj = JSONObject(responseStr)
                val modelsArray = rootObj.optJSONArray("models")
                if (modelsArray != null) {
                    val modelList = mutableListOf<GeminiModelInfo>()
                    for (i in 0 until modelsArray.length()) {
                        val obj = modelsArray.getJSONObject(i)
                        val name = obj.optString("name", "") // e.g. "models/gemini-3.8-flash"
                        val displayName = obj.optString("displayName", "")
                        val description = obj.optString("description", "")
                        val methods = obj.optJSONArray("supportedGenerationMethods")

                        var supportsGenerateContent = false
                        if (methods != null) {
                            for (m in 0 until methods.length()) {
                                if (methods.getString(m) == "generateContent") {
                                    supportsGenerateContent = true
                                    break
                                }
                            }
                        }

                        // Filter only models that support generateContent and are suitable for Lipi
                        if (supportsGenerateContent && isModelSuitableForLipi(name)) {
                            val cleanId = name.removePrefix("models/")
                            if (isModelSuitableForLipi(cleanId)) {
                                val group = determineModelGroup(cleanId)
                                val badge = when {
                                    cleanId.contains("3.8") && cleanId.contains("lite") -> "সর্বাধুনিক ৩.৮ লাইট"
                                    cleanId.contains("3.8") && cleanId.contains("flash") -> "সর্বাধুনিক ৩.৮ ফ্ল্যাশ"
                                    cleanId.contains("3.8") -> "সর্বাধুনিক ৩.৮"
                                    cleanId.contains("3.0") -> "নতুন ৩.০"
                                    cleanId.contains("2.5") && cleanId.contains("lite") -> "২.৫ ফ্ল্যাশ লাইট"
                                    cleanId.contains("2.5") -> "আধুনিক ২.৫"
                                    cleanId.contains("2.0") && cleanId.contains("lite") -> "২.০ ফ্ল্যাশ লাইট"
                                    cleanId.contains("2.0") -> "স্টেবল ২.০"
                                    cleanId.contains("1.5") && cleanId.contains("8b") -> "১.৫ ৮বি"
                                    cleanId.contains("1.5") -> "ক্লাসিক ১.৫"
                                    cleanId.contains("lite") -> "ফ্ল্যাশ লাইট"
                                    cleanId.contains("flash") -> "দ্রুতগতির ফ্ল্যাশ"
                                    cleanId.contains("pro") -> "প্রো কোয়ালিটি"
                                    else -> "এআই মডেল"
                                }
                                val isRec = cleanId == "gemini-3.8-flash" || cleanId == "gemini-3.8-flash-lite"
                                modelList.add(
                                    GeminiModelInfo(
                                        modelId = cleanId,
                                        displayName = displayName.ifBlank { cleanId },
                                        badge = badge,
                                        description = description.ifBlank { "Google Generative AI Gemini মডেল" },
                                        groupLabel = group,
                                        isRecommended = isRec,
                                        isLiveFetched = true
                                    )
                                )
                            }
                        }
                    }

                    // Sort models smartly: recommended and latest versions (3.8 > 3.0 > 2.5 > 2.0 > 1.5) on top
                    modelList.sortWith(
                        compareByDescending<GeminiModelInfo> { it.isRecommended }
                            .thenByDescending { extractModelScore(it.modelId) }
                    )

                    if (modelList.isNotEmpty()) {
                        return@withContext Result.success(modelList)
                    }
                }
                Result.failure(Exception("কোনো জেনারেটিভ মডেল পাওয়া যায়নি।"))
            } else {
                val errorStream = connection.errorStream
                val errorMsg = if (errorStream != null) {
                    BufferedReader(InputStreamReader(errorStream, "UTF-8")).use { it.readText() }
                } else "HTTP $responseCode"
                connection.disconnect()
                Result.failure(Exception("মডেল ফেচিং ত্রুটি: $errorMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractModelScore(modelId: String): Int {
        var score = 0
        if (modelId.contains("3.8")) score += 3800
        else if (modelId.contains("3.0")) score += 3000
        else if (modelId.contains("2.5")) score += 2500
        else if (modelId.contains("2.0")) score += 2000
        else if (modelId.contains("1.5")) score += 1500

        if (modelId.contains("flash-lite") || modelId.contains("flash_lite") || modelId.contains("lite")) score += 48
        else if (modelId.contains("flash")) score += 50
        else if (modelId.contains("pro")) score += 40
        else if (modelId.contains("8b")) score += 35
        if (modelId.contains("exp") || modelId.contains("preview")) score -= 5
        return score
    }

    enum class ModelHealthState {
        NOT_CHECKED,
        CHECKING,
        ACTIVE,
        RATE_LIMITED,
        HIGH_DEMAND,
        ERROR
    }

    data class ModelHealthStatus(
        val modelId: String,
        val roleLabel: String,
        val state: ModelHealthState = ModelHealthState.NOT_CHECKED,
        val latencyMs: Long = 0,
        val convertedSample: String = "",
        val message: String = ""
    )

    data class GeminiTransliterationOutput(
        val convertedText: String,
        val modelUsed: String,
        val wasFallbackUsed: Boolean = false,
        val fallbackNote: String? = null
    )

    /**
     * Translates or transliterates Banglish to authentic Bengali using online cloud/AI services.
     */
    suspend fun transliterateOnline(
        inputText: String,
        provider: Provider = Provider.GOOGLE_INPUT_TOOLS,
        customGeminiApiKey: String = "",
        geminiModel: String = "gemini-3.8-flash",
        geminiModelsChain: List<String> = emptyList(),
        mode: LipiMode = LipiMode.BANGLISH_TO_BENGALI
    ): Result<String> = withContext(Dispatchers.IO) {
        val trimmed = inputText.trim()
        if (trimmed.isEmpty()) return@withContext Result.success(inputText)

        try {
            when (provider) {
                Provider.GOOGLE_INPUT_TOOLS -> transliterateWithGoogleInputTools(trimmed)
                Provider.GEMINI_AI -> {
                    val geminiRes = transliterateWithGemini(
                        text = trimmed,
                        customApiKey = customGeminiApiKey,
                        model = geminiModel,
                        modelsChain = geminiModelsChain,
                        mode = mode
                    )
                    if (geminiRes.isSuccess) {
                        Result.success(geminiRes.getOrThrow().convertedText)
                    } else {
                        Result.failure(geminiRes.exceptionOrNull() ?: Exception("Gemini রূপান্তর ব্যর্থ"))
                    }
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    data class SmartTransliterationResult(
        val originalText: String,
        val convertedText: String,
        val engineUsed: OnlineEngineMode,
        val isFallbackToOffline: Boolean = false,
        val feedbackMessage: String? = null,
        val modelUsed: String? = null
    )

    /**
     * Executes transliteration according to user's selected engine:
     * - If Gemini AI is selected: Uses Gemini AI with multi-model fallback chain (Model 1 -> Model 2 -> Model 3).
     *   If all configured models rate-limit or fail, automatically falls back to authentic offline phonetic logic.
     * - If Google Input Tools is selected: Uses Google Input Tools first, with offline fallback.
     * - If Offline Only: Uses offline data directly.
     */
    suspend fun transliterateSmart(
        inputText: String,
        mode: LipiMode = LipiMode.BANGLISH_TO_BENGALI,
        enginePreference: OnlineEngineMode,
        customGeminiApiKey: String = "",
        geminiModel: String = "gemini-3.8-flash",
        geminiModelsChain: List<String> = emptyList(),
        customWordbook: Map<String, String> = emptyMap(),
        convertDigitsToBengali: Boolean = false,
        transliterationStyle: TransliterationStyle = TransliterationStyle.ALL_BENGALI
    ): SmartTransliterationResult = withContext(Dispatchers.IO) {
        val trimmed = inputText.trim()
        if (trimmed.isEmpty()) {
            return@withContext SmartTransliterationResult(
                originalText = inputText,
                convertedText = inputText,
                engineUsed = OnlineEngineMode.OFFLINE_ONLY
            )
        }

        fun runOffline(): String {
            return MixedScriptTokenizer.process(
                inputText = trimmed,
                mode = mode,
                convertDigitsToBengali = convertDigitsToBengali,
                customWordbook = customWordbook,
                transliterationStyle = transliterationStyle
            ).convertedText
        }

        return@withContext when (enginePreference) {
            OnlineEngineMode.OFFLINE_ONLY -> {
                SmartTransliterationResult(
                    originalText = trimmed,
                    convertedText = runOffline(),
                    engineUsed = OnlineEngineMode.OFFLINE_ONLY,
                    isFallbackToOffline = false
                )
            }
            OnlineEngineMode.GEMINI_AI -> {
                val geminiRes = transliterateWithGemini(
                    text = trimmed,
                    customApiKey = customGeminiApiKey,
                    model = geminiModel,
                    modelsChain = geminiModelsChain,
                    mode = mode
                )
                if (geminiRes.isSuccess) {
                    val out = geminiRes.getOrThrow()
                    val feedback = if (out.wasFallbackUsed) {
                        "বিকল্প মডেল (${out.modelUsed}) সফল!"
                    } else {
                        "Gemini AI (${out.modelUsed}) সফল!"
                    }
                    SmartTransliterationResult(
                        originalText = trimmed,
                        convertedText = out.convertedText,
                        engineUsed = OnlineEngineMode.GEMINI_AI,
                        isFallbackToOffline = false,
                        feedbackMessage = feedback,
                        modelUsed = out.modelUsed
                    )
                } else {
                    val exc = geminiRes.exceptionOrNull()
                    if (exc is kotlinx.coroutines.CancellationException) {
                        throw exc
                    }
                    // Seamless fallback to offline dictionary and phonetic engine
                    val offlineText = runOffline()
                    val errorDetail = exc?.message ?: "ইন্টারনেট/এআই সংযোগ পাওয়া যায়নি"
                    SmartTransliterationResult(
                        originalText = trimmed,
                        convertedText = offlineText,
                        engineUsed = OnlineEngineMode.OFFLINE_ONLY,
                        isFallbackToOffline = true,
                        feedbackMessage = "অফলাইন লজিক ব্যবহৃত ($errorDetail)"
                    )
                }
            }
            OnlineEngineMode.ONLINE_CLOUD -> {
                val googleRes = transliterateOnline(
                    inputText = trimmed,
                    provider = Provider.GOOGLE_INPUT_TOOLS
                )
                if (googleRes.isSuccess) {
                    SmartTransliterationResult(
                        originalText = trimmed,
                        convertedText = googleRes.getOrThrow(),
                        engineUsed = OnlineEngineMode.ONLINE_CLOUD,
                        isFallbackToOffline = false,
                        feedbackMessage = "Google ক্লাউড ট্রান্সলিটারেশন সফল!"
                    )
                } else {
                    // Seamless fallback to offline dictionary and phonetic engine
                    val offlineText = runOffline()
                    SmartTransliterationResult(
                        originalText = trimmed,
                        convertedText = offlineText,
                        engineUsed = OnlineEngineMode.OFFLINE_ONLY,
                        isFallbackToOffline = true,
                        feedbackMessage = "ইন্টারনেট না থাকায় অফলাইন ডেটাবেস দিয়ে রূপান্তর সম্পন্ন হয়েছে।"
                    )
                }
            }
        }
    }

    /**
     * Google Input Tools online transliteration endpoint.
     */
    private fun transliterateWithGoogleInputTools(text: String): Result<String> {
        return try {
            val encodedText = URLEncoder.encode(text, "UTF-8")
            val endpoint = "https://inputtools.google.com/request?text=$encodedText&itc=bn-t-i0-und&num=1&cp=0&cs=1&ie=utf-8&oe=utf-8&app=demopage"
            val url = URL(endpoint)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 7000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; LipiApp)")

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
                val responseStr = reader.use { it.readText() }
                connection.disconnect()

                val jsonArray = JSONArray(responseStr)
                val status = jsonArray.optString(0, "")
                if (status == "SUCCESS") {
                    val chunksArray = jsonArray.optJSONArray(1)
                    if (chunksArray != null && chunksArray.length() > 0) {
                        val resultBuilder = StringBuilder()
                        for (i in 0 until chunksArray.length()) {
                            val chunkItem = chunksArray.optJSONArray(i)
                            if (chunkItem != null) {
                                val originalChunk = chunkItem.optString(0, "")
                                val candidates = chunkItem.optJSONArray(1)
                                if (candidates != null && candidates.length() > 0) {
                                    resultBuilder.append(candidates.optString(0, originalChunk))
                                } else {
                                    resultBuilder.append(originalChunk)
                                }
                            }
                        }
                        val converted = resultBuilder.toString()
                        if (converted.isNotBlank()) {
                            return Result.success(TextNormalizer.postNormalizeBengali(converted))
                        }
                    }
                }
                Result.failure(Exception("অনলাইন এপিআই থেকে কাঙ্ক্ষিত উত্তর পাওয়া যায়নি"))
            } else {
                connection.disconnect()
                Result.failure(Exception("সার্ভার ত্রুটি: HTTP $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    data class GeminiTestResult(
        val isSuccess: Boolean,
        val isHighDemandFallback: Boolean = false,
        val modelUsed: String = "",
        val convertedText: String = "",
        val message: String = ""
    )

    private data class GeminiExecutionResult(
        val isSuccess: Boolean,
        val text: String = "",
        val httpCode: Int = 0,
        val rawErrorMessage: String = "",
        val isHighDemand: Boolean = false,
        val isRateLimit: Boolean = false,
        val isAuthError: Boolean = false
    )

    /**
     * Determines the optimal stable fallback model if the primary model suffers from 503 High Demand or 429 Rate Limit.
     */
    fun getFallbackModel(currentModel: String): String {
        val clean = currentModel.trim()
        return when {
            clean.contains("3.8") && clean.contains("pro") -> "gemini-2.5-pro"
            clean.contains("3.8") -> "gemini-2.5-flash"
            clean.contains("3.0") -> "gemini-2.5-flash"
            clean == "gemini-2.5-flash" -> "gemini-2.0-flash"
            clean == "gemini-2.5-pro" -> "gemini-2.5-flash"
            clean != "gemini-2.5-flash" -> "gemini-2.5-flash"
            else -> "gemini-2.0-flash"
        }
    }

    /**
     * Parses Google Gemini API error payloads into clear, human-readable Bengali descriptions.
     */
    fun parseGeminiErrorMessage(rawError: String, responseCode: Int, model: String): String {
        try {
            val trimmed = rawError.trim()
            if (trimmed.startsWith("{")) {
                val json = JSONObject(trimmed)
                val errorObj = json.optJSONObject("error")
                if (errorObj != null) {
                    val code = errorObj.optInt("code", responseCode)
                    val msg = errorObj.optString("message", "")
                    val status = errorObj.optString("status", "")

                    return when {
                        code == 503 || status == "UNAVAILABLE" || msg.contains("high demand", ignoreCase = true) -> {
                            "গুগল সার্ভার সাময়িক ব্যস্ত (503 High Demand): নির্বাচিত '$model' মডেলে এই মুহূর্তে বিশ্বব্যাপী অতিরিক্ত ট্রাফিকের চাপ রয়েছে। অনুগ্রহ করে কিছুক্ষণ পর চেষ্টা করুন অথবা স্থিতিশীল Gemini 2.5 Flash মডেল বেছে নিন।"
                        }
                        code == 429 || status == "RESOURCE_EXHAUSTED" || msg.contains("quota", ignoreCase = true) -> {
                            "এপিআই কোটা বা রেট লিমিট অতিক্রম করেছে (429 Rate Limit)। অনুগ্রহ করে কিছুক্ষণ অপেক্ষা করে চেষ্টা করুন।"
                        }
                        code == 403 || status == "PERMISSION_DENIED" || msg.contains("API key", ignoreCase = true) -> {
                            "API Key সংক্রান্ত ত্রুটি (403): আপনার Gemini API কী সঠিক ও সক্রিয় কিনা যাচাই করুন।"
                        }
                        code == 404 || status == "NOT_FOUND" -> {
                            "মডেলটি পাওয়া যায়নি (404 Not Found): '$model' মডেলটি বর্তমান API সংস্করণে অনুপস্থিত। তালিকা রিফ্রেশ করে অন্য কোনো সক্রিয় মডেল বেছে নিন।"
                        }
                        else -> {
                            "গুগল এআই ত্রুটি ($code): ${msg.ifBlank { trimmed }}"
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        return when {
            responseCode == 503 || rawError.contains("high demand", ignoreCase = true) || rawError.contains("UNAVAILABLE", ignoreCase = true) -> {
                "গুগল সার্ভার সাময়িক ব্যস্ত (503 High Demand): '$model' মডেলে সাময়িক উচ্চ চাপ রয়েছে।"
            }
            responseCode == 429 -> {
                "এপিআই কোটা বা রেট লিমিট অতিক্রম করেছে (429 Rate Limit)। কিছুক্ষণ পর চেষ্টা করুন।"
            }
            rawError.isNotBlank() && !rawError.startsWith("{") -> {
                rawError
            }
            else -> {
                "এআই রেসপন্স ত্রুটি: HTTP $responseCode"
            }
        }
    }

    private fun executeSingleGeminiCall(
        text: String,
        effectiveApiKey: String,
        model: String,
        mode: LipiMode = LipiMode.BANGLISH_TO_BENGALI
    ): GeminiExecutionResult {
        return try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$effectiveApiKey"
            val url = URL(endpoint)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 8000
            connection.readTimeout = 12000
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.doOutput = true

            val systemInstruction = if (mode == LipiMode.BENGALI_TO_BANGLISH) {
                "You are Lipi, an expert Bengali to Romanized Banglish transliteration engine. " +
                        "Convert the following Bengali text into clean, casual, natural phonetic Banglish script as commonly typed in chat messages (e.g., 'শুরু' -> 'suru', 'নাকি' -> 'naki', 'আবার' -> 'abar', 'করলি' -> 'korli'). " +
                        "Preserve emojis, punctuation, and question marks exactly. Output ONLY the converted Banglish text with no explanations or preamble."
            } else {
                "You are Lipi, an expert Banglish to Bengali transliteration engine. " +
                        "Convert the following casual Banglish text into authentic, natural conversational Bengali script. " +
                        "Fix common phonetic typing mistakes and slang (e.g., 'suru' -> 'শুরু', 'naki' -> 'নাকি', 'abr' -> 'আবার', 'korli' -> 'করলি'). " +
                        "Preserve emojis, punctuation, and question marks exactly. Output ONLY the converted Bengali text with no explanations or preamble."
            }

            val jsonBody = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            val part = JSONObject().apply {
                                put("text", "$systemInstruction\n\nInput text:\n$text")
                            }
                            put(part)
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use {
                it.write(jsonBody.toString())
                it.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
                val responseStr = reader.use { it.readText() }
                connection.disconnect()

                val rootObj = JSONObject(responseStr)
                val candidates = rootObj.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val generatedText = parts.getJSONObject(0).optString("text", "").trim()
                        if (generatedText.isNotBlank()) {
                            return GeminiExecutionResult(
                                isSuccess = true,
                                text = TextNormalizer.postNormalizeBengali(generatedText)
                            )
                        }
                    }
                }
                GeminiExecutionResult(
                    isSuccess = false,
                    rawErrorMessage = "এআই থেকে কোনো উত্তর পাওয়া যায়নি",
                    httpCode = 200
                )
            } else {
                val errorStream = connection.errorStream
                val errorMsg = if (errorStream != null) {
                    BufferedReader(InputStreamReader(errorStream, "UTF-8")).use { it.readText() }
                } else "HTTP $responseCode"
                connection.disconnect()

                val isHighDemand = responseCode == 503 ||
                        errorMsg.contains("high demand", ignoreCase = true) ||
                        errorMsg.contains("UNAVAILABLE", ignoreCase = true)

                val isRateLimit = responseCode == 429 ||
                        errorMsg.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
                        errorMsg.contains("quota", ignoreCase = true)

                val isAuthError = responseCode == 400 || responseCode == 403 ||
                        errorMsg.contains("API_KEY_INVALID", ignoreCase = true) ||
                        errorMsg.contains("PERMISSION_DENIED", ignoreCase = true)

                GeminiExecutionResult(
                    isSuccess = false,
                    httpCode = responseCode,
                    rawErrorMessage = errorMsg,
                    isHighDemand = isHighDemand,
                    isRateLimit = isRateLimit,
                    isAuthError = isAuthError
                )
            }
        } catch (e: Exception) {
            GeminiExecutionResult(
                isSuccess = false,
                httpCode = 0,
                rawErrorMessage = e.message ?: "কানেকশন ব্যাহত হয়েছে"
            )
        }
    }

    /**
     * Gemini AI contextual Banglish-to-Bengali translation with multi-model fallback chain.
     * Tries Model 1 -> Model 2 -> Model 3 sequentially if rate-limit (429), 503 high demand or errors happen.
     */
    private fun transliterateWithGemini(
        text: String,
        customApiKey: String,
        model: String = "gemini-3.8-flash",
        modelsChain: List<String> = emptyList(),
        mode: LipiMode = LipiMode.BANGLISH_TO_BENGALI
    ): Result<GeminiTransliterationOutput> {
        val effectiveApiKey = when {
            customApiKey.isNotBlank() -> customApiKey.trim()
            try { BuildConfig.GEMINI_API_KEY.isNotBlank() } catch (_: Exception) { false } -> BuildConfig.GEMINI_API_KEY
            else -> ""
        }

        if (effectiveApiKey.isBlank() || effectiveApiKey == "your_api_key_here") {
            return Result.failure(Exception("Gemini API Key পাওয়া যায়নি। সেটিংসে আপনার API Key যুক্ত করুন অথবা Google ক্লাউড ইঞ্জিন ব্যবহার করুন।"))
        }

        val chain = mutableListOf<String>()
        if (modelsChain.isNotEmpty()) {
            modelsChain.forEach { m ->
                val clean = m.trim()
                if (clean.isNotEmpty() && !chain.contains(clean)) chain.add(clean)
            }
        }
        if (chain.isEmpty()) {
            val chosen = model.trim().ifEmpty { "gemini-3.8-flash" }
            chain.add(chosen)
            val fb = getFallbackModel(chosen)
            if (!chain.contains(fb)) chain.add(fb)
        }

        var lastErrorMsg = ""
        var lastHttpCode = 0
        var lastModelTried = ""
        val errorLogs = mutableListOf<String>()

        for ((index, currentModel) in chain.withIndex()) {
            lastModelTried = currentModel
            val callResult = executeSingleGeminiCall(text, effectiveApiKey, currentModel, mode)
            if (callResult.isSuccess) {
                val wasFallback = index > 0
                val note = if (wasFallback) "মডেল ১ ব্যস্ত বা ত্রুটিযুক্ত থাকায় বিকল্প মডেল ($currentModel) ব্যবহৃত হয়েছে" else null
                return Result.success(
                    GeminiTransliterationOutput(
                        convertedText = callResult.text,
                        modelUsed = currentModel,
                        wasFallbackUsed = wasFallback,
                        fallbackNote = note
                    )
                )
            }
            lastErrorMsg = callResult.rawErrorMessage
            lastHttpCode = callResult.httpCode
            val shortErr = when {
                callResult.isRateLimit -> "429 Rate Limit"
                callResult.isHighDemand -> "503 High Demand"
                callResult.httpCode > 0 -> "HTTP ${callResult.httpCode}"
                else -> callResult.rawErrorMessage.take(30)
            }
            errorLogs.add("$currentModel ($shortErr)")
        }

        val cleanError = parseGeminiErrorMessage(lastErrorMsg, lastHttpCode, lastModelTried)
        val combinedError = if (chain.size > 1) {
            "$cleanError\n(ফলব্যাক চেইনে চেষ্টা করা মডেল: ${errorLogs.joinToString(", ")})"
        } else {
            cleanError
        }
        return Result.failure(Exception(combinedError))
    }

    /**
     * Checks whether a specific Gemini model is operational, measures latency, and returns clear diagnostics.
     */
    suspend fun checkModelHealth(
        customApiKey: String,
        modelId: String,
        roleLabel: String = ""
    ): ModelHealthStatus = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val effectiveApiKey = when {
            customApiKey.isNotBlank() -> customApiKey.trim()
            try { BuildConfig.GEMINI_API_KEY.isNotBlank() } catch (_: Exception) { false } -> BuildConfig.GEMINI_API_KEY
            else -> ""
        }

        if (effectiveApiKey.isBlank() || effectiveApiKey == "your_api_key_here") {
            return@withContext ModelHealthStatus(
                modelId = modelId,
                roleLabel = roleLabel,
                state = ModelHealthState.ERROR,
                message = "API Key দেওয়া হয়নি। অনুগ্রহ করে আপনার Gemini API Key যুক্ত করুন।"
            )
        }

        val testText = "Khawya suru korli naki abr??"
        val callRes = executeSingleGeminiCall(testText, effectiveApiKey, modelId.trim())
        val latency = System.currentTimeMillis() - startTime

        if (callRes.isSuccess) {
            ModelHealthStatus(
                modelId = modelId,
                roleLabel = roleLabel,
                state = ModelHealthState.ACTIVE,
                latencyMs = latency,
                convertedSample = callRes.text,
                message = "✓ সচল (${latency}ms) — রূপান্তর: \"${callRes.text}\""
            )
        } else if (callRes.isRateLimit) {
            ModelHealthStatus(
                modelId = modelId,
                roleLabel = roleLabel,
                state = ModelHealthState.RATE_LIMITED,
                latencyMs = latency,
                message = "⚠️ কোটা/রেট লিমিট শেষ (429 Rate Limit)"
            )
        } else if (callRes.isHighDemand) {
            ModelHealthStatus(
                modelId = modelId,
                roleLabel = roleLabel,
                state = ModelHealthState.HIGH_DEMAND,
                latencyMs = latency,
                message = "⚠️ গুগল সার্ভার ব্যস্ত (503 High Demand)"
            )
        } else {
            val cleanErr = parseGeminiErrorMessage(callRes.rawErrorMessage, callRes.httpCode, modelId)
            ModelHealthStatus(
                modelId = modelId,
                roleLabel = roleLabel,
                state = ModelHealthState.ERROR,
                latencyMs = latency,
                message = cleanErr
            )
        }
    }

    /**
     * Diagnostic test for Gemini API connectivity with intelligent fallback reporting.
     */
    suspend fun testGeminiConnection(
        customApiKey: String,
        model: String,
        sampleText: String = "Khawya suru korli naki abr??"
    ): GeminiTestResult = withContext(Dispatchers.IO) {
        val effectiveApiKey = when {
            customApiKey.isNotBlank() -> customApiKey.trim()
            try { BuildConfig.GEMINI_API_KEY.isNotBlank() } catch (_: Exception) { false } -> BuildConfig.GEMINI_API_KEY
            else -> ""
        }

        if (effectiveApiKey.isBlank() || effectiveApiKey == "your_api_key_here") {
            return@withContext GeminiTestResult(
                isSuccess = false,
                message = "Gemini API Key প্রদান করা হয়নি। অনুগ্রহ করে সেটিংসে আপনার নিজস্ব Gemini API Key লিখুন।"
            )
        }

        val chosenModel = model.trim().ifEmpty { "gemini-3.8-flash" }
        val primaryResult = executeSingleGeminiCall(sampleText, effectiveApiKey, chosenModel)

        if (primaryResult.isSuccess) {
            return@withContext GeminiTestResult(
                isSuccess = true,
                modelUsed = chosenModel,
                convertedText = primaryResult.text,
                message = "সফল! [$chosenModel]\nরূপান্তর: ${primaryResult.text}\nলিপির সাথে মডেলটির সংযোগ সম্পূর্ণ সক্রিয়।"
            )
        }

        // If primary model experienced 503 High Demand or 429 Rate Limit
        if (primaryResult.isHighDemand || primaryResult.isRateLimit) {
            val fallbackModel = getFallbackModel(chosenModel)
            val fallbackResult = executeSingleGeminiCall(sampleText, effectiveApiKey, fallbackModel)

            if (fallbackResult.isSuccess) {
                return@withContext GeminiTestResult(
                    isSuccess = true,
                    isHighDemandFallback = true,
                    modelUsed = fallbackModel,
                    convertedText = fallbackResult.text,
                    message = "⚠️ নির্বাচিত মডেলে উচ্চ চাপ (503 High Demand):\n" +
                            "গুগলের '$chosenModel' সার্ভারে এই মুহূর্তে বিশ্বব্যাপী সাময়িক অতিরিক্ত চাপ রয়েছে।\n\n" +
                            "✅ কিন্তু আপনার API Key সম্পূর্ণ কার্যকর ও সক্রিয়!\n" +
                            "স্বয়ংক্রিয় ব্যাকআপ মডেল ($fallbackModel) দিয়ে সফলভাবে রূপান্তর হয়েছে:\n" +
                            "\"${fallbackResult.text}\"\n\n" +
                            "💡 পরামর্শ: নিরবচ্ছিন্ন সেবার জন্য আপনি তালিকা থেকে সরাসরি '$fallbackModel' বেছে নিতে পারেন, অথবা এই মডেল রাখলেও লিপি ব্যাকআপ মডেল দিয়ে টাইপিং চালু রাখবে।"
                )
            } else {
                val fallbackError = parseGeminiErrorMessage(fallbackResult.rawErrorMessage, fallbackResult.httpCode, fallbackModel)
                return@withContext GeminiTestResult(
                    isSuccess = false,
                    message = "গুগল সার্ভার সাময়িক ব্যস্ত (503 High Demand):\n" +
                            "'$chosenModel' এবং ব্যাকআপ মডেল উভয়টিতেই গুগলের সার্ভারে চাপ রয়েছে। অনুগ্রহ করে কিছুক্ষণ পর চেষ্টা করুন।"
                )
            }
        }

        // Other error
        val cleanError = parseGeminiErrorMessage(primaryResult.rawErrorMessage, primaryResult.httpCode, chosenModel)
        GeminiTestResult(
            isSuccess = false,
            message = cleanError
        )
    }

    data class UnknownWordsLoadResult(
        val wordMappings: Map<String, String>,
        val modelUsed: String = "",
        val rawResponse: String = ""
    )

    /**
     * Translates ONLY the unknown/missing Banglish words using Gemini AI, in the context of the sentence.
     * Returns the exact (banglish -> authentic Bengali) word mappings.
     */
    suspend fun fetchUnknownWordsFromGemini(
        words: List<String>,
        contextSentence: String,
        customApiKey: String = "",
        model: String = "gemini-3.8-flash",
        modelsChain: List<String> = emptyList()
    ): Result<UnknownWordsLoadResult> = withContext(Dispatchers.IO) {
        if (words.isEmpty()) {
            return@withContext Result.success(UnknownWordsLoadResult(emptyMap()))
        }

        val effectiveApiKey = when {
            customApiKey.isNotBlank() -> customApiKey.trim()
            try { BuildConfig.GEMINI_API_KEY.isNotBlank() } catch (_: Exception) { false } -> BuildConfig.GEMINI_API_KEY
            else -> ""
        }

        if (effectiveApiKey.isBlank() || effectiveApiKey == "your_api_key_here") {
            return@withContext Result.failure(Exception("Gemini API Key পাওয়া যায়নি। সেটিংসে API Key দিন।"))
        }

        val wordsListStr = words.distinct().joinToString("\n") { "- $it" }
        val prompt = "You are Lipi, an expert Banglish to Bengali transliteration and dictionary engine.\n" +
                "Translate the following unknown Banglish word(s) into authentic, natural conversational Bengali script.\n" +
                "Full sentence context: \"$contextSentence\"\n\n" +
                "Words to convert:\n$wordsListStr\n\n" +
                "Provide the translation for each word. Respond ONLY with valid JSON in this exact structure, with no markdown code blocks or other text:\n" +
                "[\n" +
                words.distinct().take(2).joinToString(",\n") { "  {\"banglish\": \"$it\", \"bengali\": \"...\"}" } + "\n" +
                "]"

        val chain = mutableListOf<String>()
        if (modelsChain.isNotEmpty()) {
            modelsChain.forEach { m ->
                val clean = m.trim()
                if (clean.isNotEmpty() && !chain.contains(clean)) chain.add(clean)
            }
        }
        if (chain.isEmpty()) {
            val chosen = model.trim().ifEmpty { "gemini-3.8-flash" }
            chain.add(chosen)
            val fb = getFallbackModel(chosen)
            if (!chain.contains(fb)) chain.add(fb)
        }

        var lastError = ""
        for (currentModel in chain) {
            val callResult = executeSingleGeminiCall(
                text = prompt,
                effectiveApiKey = effectiveApiKey,
                model = currentModel,
                mode = LipiMode.BANGLISH_TO_BENGALI
            )
            if (callResult.isSuccess) {
                val mappings = parseWordMappings(callResult.text, words)
                if (mappings.isNotEmpty()) {
                    return@withContext Result.success(
                        UnknownWordsLoadResult(
                            wordMappings = mappings,
                            modelUsed = currentModel,
                            rawResponse = callResult.text
                        )
                    )
                }
            } else {
                lastError = callResult.rawErrorMessage
            }
        }

        Result.failure(Exception("Gemini থেকে নতুন শব্দ লোড করা যায়নি: $lastError"))
    }

    fun parseWordMappings(rawText: String, requestedWords: List<String>): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val trimmed = rawText.trim()

        // 1. Regex JSON extraction (zero runtime dependency, works identically on Android device and host JVM test runner)
        val jsonPattern = Regex(""""banglish"\s*:\s*"([^"]+)"\s*,\s*"bengali"\s*:\s*"([^"]+)"""", RegexOption.IGNORE_CASE)
        val jsonPatternReverse = Regex(""""bengali"\s*:\s*"([^"]+)"\s*,\s*"banglish"\s*:\s*"([^"]+)"""", RegexOption.IGNORE_CASE)

        for (match in jsonPattern.findAll(trimmed)) {
            val banglish = match.groupValues[1].lowercase().trim()
            val bengali = match.groupValues[2].trim()
            if (banglish.isNotBlank() && bengali.any { it in '\u0980'..'\u09FF' }) {
                map[banglish] = bengali
            }
        }
        for (match in jsonPatternReverse.findAll(trimmed)) {
            val bengali = match.groupValues[1].trim()
            val banglish = match.groupValues[2].lowercase().trim()
            if (banglish.isNotBlank() && bengali.any { it in '\u0980'..'\u09FF' }) {
                map[banglish] = bengali
            }
        }

        if (map.isNotEmpty()) return map

        // 2. Try org.json parsing as backup
        try {
            val jsonClean = trimmed
                .replace(Regex("""^```(?:json)?\s*""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""\s*```$"""), "")
                .trim()

            if (jsonClean.startsWith("[") && jsonClean.endsWith("]")) {
                val array = JSONArray(jsonClean)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i)
                    if (obj != null) {
                        val banglish = obj.optString("banglish", "").lowercase().trim()
                        val bengali = obj.optString("bengali", "").trim()
                        if (banglish.isNotBlank() && bengali.any { it in '\u0980'..'\u09FF' }) {
                            map[banglish] = bengali
                        }
                    }
                }
            } else if (jsonClean.startsWith("{") && jsonClean.endsWith("}")) {
                val obj = JSONObject(jsonClean)
                val array = obj.optJSONArray("mappings") ?: obj.optJSONArray("words")
                if (array != null) {
                    for (i in 0 until array.length()) {
                        val item = array.optJSONObject(i)
                        if (item != null) {
                            val banglish = item.optString("banglish", "").lowercase().trim()
                            val bengali = item.optString("bengali", "").trim()
                            if (banglish.isNotBlank() && bengali.any { it in '\u0980'..'\u09FF' }) {
                                map[banglish] = bengali
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        if (map.isNotEmpty()) return map

        // 2. Line by line parsing: "clg=কলেজ", "clg: কলেজ", "clg -> কলেজ"
        val lines = trimmed.lines()
        for (line in lines) {
            val l = line.trim().removePrefix("-").removePrefix("*").trim()
            val delimiter = when {
                l.contains("=") -> "="
                l.contains("->") -> "->"
                l.contains(":") -> ":"
                l.contains("\t") -> "\t"
                else -> null
            }
            if (delimiter != null) {
                val parts = l.split(delimiter, limit = 2)
                if (parts.size == 2) {
                    val banglish = parts[0].trim().filter { it.isLetter() }.lowercase()
                    val bengali = parts[1].trim(',', '.', '?', '!', '।', ';', ':', '"', '\'', '‘', '’', '“', '”', ' ')
                    if (banglish.isNotBlank() && bengali.any { it in '\u0980'..'\u09FF' }) {
                        map[banglish] = bengali
                    }
                }
            }
        }

        // 3. Single word request fallback: e.g. "কলেজ"
        if (map.isEmpty() && requestedWords.size == 1) {
            val singleWord = requestedWords[0].lowercase().filter { it.isLetter() }
            val firstBengaliToken = trimmed.split(Regex("""\s+""")).firstOrNull { it.any { c -> c in '\u0980'..'\u09FF' } }
            if (!firstBengaliToken.isNullOrBlank()) {
                val cleanBn = firstBengaliToken.trim(',', '.', '?', '!', '।', ';', ':', '"', '\'', '‘', '’', '“', '”', ' ')
                map[singleWord] = cleanBn
            }
        }

        return map
    }
}
