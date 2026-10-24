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
        val isRecommended: Boolean = false,
        val isLiveFetched: Boolean = false
    )

    val AVAILABLE_GEMINI_MODELS = listOf(
        GeminiModelInfo(
            modelId = "gemini-3.8-flash",
            displayName = "Gemini 3.8 Flash",
            badge = "সর্বাধুনিক ৩.৮ ও সুপার ফাস্ট",
            description = "সর্বাধুনিক ৩.৮ আল্ট্রা-ফাস্ট মডেল — দ্রুততম ল্যাটেন্সি ও নির্ভুল বাংলিশ চ্যাট রূপান্তরের জন্য সেরা",
            isRecommended = true
        ),
        GeminiModelInfo(
            modelId = "gemini-3.8-pro",
            displayName = "Gemini 3.8 Pro",
            badge = "সর্বোচ্চ বুদ্ধিমত্তা ৩.৮",
            description = "জটিল ব্যাকরণ, দীর্ঘ বাক্য ও গভীর বুদ্ধিমত্তাসম্পন্ন নতুন ৩.৮ ফ্ল্যাগশিপ মডেল",
            isRecommended = false
        ),
        GeminiModelInfo(
            modelId = "gemini-3.0-flash",
            displayName = "Gemini 3.0 Flash",
            badge = "নতুন ৩.০",
            description = "উচ্চ নির্ভুলতা ও দ্রুত এআই রেসপন্স",
            isRecommended = false
        ),
        GeminiModelInfo(
            modelId = "gemini-2.5-flash",
            displayName = "Gemini 2.5 Flash",
            badge = "স্টেবল ও দ্রুত",
            description = "জনপ্রিয় ২.৫ সংস্করণ — রিয়েলটাইম বার্তা রূপান্তরের জন্য নির্ভরযোগ্য",
            isRecommended = false
        ),
        GeminiModelInfo(
            modelId = "gemini-2.5-pro",
            displayName = "Gemini 2.5 Pro",
            badge = "প্রো কোয়ালিটি",
            description = "জটিল বাক্য, ব্যাকরণগত শুদ্ধতা ও গভীর অর্থ বিশ্লেষণের জন্য শক্তিশালী",
            isRecommended = false
        ),
        GeminiModelInfo(
            modelId = "gemini-2.0-flash",
            displayName = "Gemini 2.0 Flash",
            badge = "আল্ট্রা ফাস্ট",
            description = "লো-ল্যাটেন্সি ও দ্রুত উত্তর প্রদান",
            isRecommended = false
        ),
        GeminiModelInfo(
            modelId = "gemini-1.5-flash",
            displayName = "Gemini 1.5 Flash",
            badge = "ক্লাসিক",
            description = "স্ট্যান্ডার্ড সংস্করণ ও বিশ্বস্ত কার্যকারিতা",
            isRecommended = false
        )
    )

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

                        // Filter models that support content generation and belong to the gemini family
                        if (supportsGenerateContent && name.contains("gemini", ignoreCase = true)) {
                            val cleanId = name.removePrefix("models/")
                            if (!cleanId.contains("embedding") && !cleanId.contains("aqa")) {
                                val badge = when {
                                    cleanId.contains("3.8") -> "সর্বাধুনিক ৩.৮"
                                    cleanId.contains("3.0") -> "নতুন ৩.০"
                                    cleanId.contains("2.5") -> "আধুনিক ২.৫"
                                    cleanId.contains("2.0") -> "স্টেবল ২.০"
                                    cleanId.contains("flash") -> "দ্রুতগতির ফ্ল্যাশ"
                                    cleanId.contains("pro") -> "প্রো কোয়ালিটি"
                                    else -> "এআই মডেল"
                                }
                                val isRec = cleanId == "gemini-3.8-flash" || (cleanId.contains("3.8") && cleanId.contains("flash"))
                                modelList.add(
                                    GeminiModelInfo(
                                        modelId = cleanId,
                                        displayName = displayName.ifBlank { cleanId },
                                        badge = badge,
                                        description = description.ifBlank { "Google Generative AI Gemini মডেল" },
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

        if (modelId.contains("flash")) score += 50
        if (modelId.contains("pro")) score += 40
        if (modelId.contains("exp") || modelId.contains("preview")) score -= 5
        return score
    }

    /**
     * Translates or transliterates Banglish to authentic Bengali using online cloud/AI services.
     */
    suspend fun transliterateOnline(
        inputText: String,
        provider: Provider = Provider.GOOGLE_INPUT_TOOLS,
        customGeminiApiKey: String = "",
        geminiModel: String = "gemini-3.8-flash"
    ): Result<String> = withContext(Dispatchers.IO) {
        val trimmed = inputText.trim()
        if (trimmed.isEmpty()) return@withContext Result.success(inputText)

        try {
            when (provider) {
                Provider.GOOGLE_INPUT_TOOLS -> transliterateWithGoogleInputTools(trimmed)
                Provider.GEMINI_AI -> transliterateWithGemini(trimmed, customGeminiApiKey, geminiModel)
            }
        } catch (e: Exception) {
            Result.failure(e)
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

    /**
     * Gemini AI contextual Banglish-to-Bengali translation.
     */
    private fun transliterateWithGemini(
        text: String,
        customApiKey: String,
        model: String = "gemini-3.8-flash"
    ): Result<String> {
        val effectiveApiKey = when {
            customApiKey.isNotBlank() -> customApiKey.trim()
            try { BuildConfig.GEMINI_API_KEY.isNotBlank() } catch (_: Exception) { false } -> BuildConfig.GEMINI_API_KEY
            else -> ""
        }

        if (effectiveApiKey.isBlank() || effectiveApiKey == "your_api_key_here") {
            return Result.failure(Exception("Gemini API Key পাওয়া যায়নি। সেটিংসে আপনার API Key যুক্ত করুন অথবা Google ক্লাউড ইঞ্জিন ব্যবহার করুন।"))
        }

        val chosenModel = model.trim().ifEmpty { "gemini-3.8-flash" }

        return try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$chosenModel:generateContent?key=$effectiveApiKey"
            val url = URL(endpoint)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 8000
            connection.readTimeout = 12000
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.doOutput = true

            val systemInstruction = "You are Lipi, an expert Banglish to Bengali transliteration engine. " +
                    "Convert the following casual Banglish text into authentic, natural conversational Bengali script. " +
                    "Fix common phonetic typing mistakes and slang (e.g., 'suru' -> 'শুরু', 'naki' -> 'নাকি', 'abr' -> 'আবার', 'korli' -> 'করলি'). " +
                    "Preserve emojis, punctuation, and question marks exactly. Output ONLY the converted Bengali text with no explanations or preamble."

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
                            return Result.success(TextNormalizer.postNormalizeBengali(generatedText))
                        }
                    }
                }
                Result.failure(Exception("এআই থেকে কোনো উত্তর পাওয়া যায়নি"))
            } else {
                val errorStream = connection.errorStream
                val errorMsg = if (errorStream != null) {
                    BufferedReader(InputStreamReader(errorStream, "UTF-8")).use { it.readText() }
                } else "HTTP $responseCode"
                connection.disconnect()
                Result.failure(Exception("এআই রেসপন্স ত্রুটি: $errorMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
