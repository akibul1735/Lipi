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
        val isRecommended: Boolean = false
    )

    val AVAILABLE_GEMINI_MODELS = listOf(
        GeminiModelInfo(
            modelId = "gemini-2.5-flash",
            displayName = "Gemini 2.5 Flash",
            badge = "ডিফল্ট ও সুপারিশকৃত",
            description = "সর্বোচ্চ গতি ও আধুনিক কনটেক্সট — রিয়েলটাইম চ্যাট ও বার্তা রূপান্তরের জন্য সেরা",
            isRecommended = true
        ),
        GeminiModelInfo(
            modelId = "gemini-2.5-pro",
            displayName = "Gemini 2.5 Pro",
            badge = "উচ্চ বুদ্ধিমত্তা",
            description = "জটিল বাক্য, ব্যাকরণগত শুদ্ধতা ও গভীর অর্থ বিশ্লেষণের জন্য শক্তিশালী",
            isRecommended = false
        ),
        GeminiModelInfo(
            modelId = "gemini-2.0-flash",
            displayName = "Gemini 2.0 Flash",
            badge = "স্টেবল",
            description = "আল্ট্রা লো-ল্যাটেন্সি ও উচ্চ নির্ভরযোগ্যতা",
            isRecommended = false
        ),
        GeminiModelInfo(
            modelId = "gemini-1.5-flash",
            displayName = "Gemini 1.5 Flash",
            badge = "ক্লাসিক",
            description = "স্ট্যান্ডার্ড লাইটওয়েট সংস্করণ ও বিশ্বস্ত কার্যকারিতা",
            isRecommended = false
        )
    )

    /**
     * Translates or transliterates Banglish to authentic Bengali using online cloud/AI services.
     */
    suspend fun transliterateOnline(
        inputText: String,
        provider: Provider = Provider.GOOGLE_INPUT_TOOLS,
        customGeminiApiKey: String = "",
        geminiModel: String = "gemini-2.5-flash"
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
        model: String = "gemini-2.5-flash"
    ): Result<String> {
        val effectiveApiKey = when {
            customApiKey.isNotBlank() -> customApiKey.trim()
            try { BuildConfig.GEMINI_API_KEY.isNotBlank() } catch (_: Exception) { false } -> BuildConfig.GEMINI_API_KEY
            else -> ""
        }

        if (effectiveApiKey.isBlank() || effectiveApiKey == "your_api_key_here") {
            return Result.failure(Exception("Gemini API Key পাওয়া যায়নি। সেটিংসে আপনার API Key যুক্ত করুন অথবা Google ক্লাউড ইঞ্জিন ব্যবহার করুন।"))
        }

        val chosenModel = model.trim().ifEmpty { "gemini-2.5-flash" }

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
