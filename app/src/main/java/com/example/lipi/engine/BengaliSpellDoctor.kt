package com.example.lipi.engine

import com.example.lipi.data.LipiDictionary
import com.example.lipi.data.ProcessedToken
import com.example.lipi.data.TokenType

/**
 * Bengali Spelling Doctor & Orthography Validator.
 * Handles pure Bengali text, corrects common misspellings (e.g. "ভাল" -> "ভালো", "অসুস্হ" -> "অসুস্থ"),
 * and provides educational notes on Bengali grammatical and diacritical rules.
 */
object BengaliSpellDoctor {

    data class SpellCheckResult(
        val originalText: String,
        val correctedText: String,
        val correctionsCount: Int,
        val tokenDetails: List<ProcessedToken>
    )

    /**
     * Check and correct Bengali spelling for given input.
     */
    fun checkAndCorrect(bengaliText: String): SpellCheckResult {
        if (bengaliText.isBlank()) {
            return SpellCheckResult(bengaliText, bengaliText, 0, emptyList())
        }

        // Tokenize by word boundary, preserving punctuation
        val wordRegex = Regex("""([\p{L}\p{M}]+)|([^\p{L}\p{M}\s]+)|(\s+)""")
        val matches = wordRegex.findAll(bengaliText)

        val outputBuilder = StringBuilder()
        val tokens = mutableListOf<ProcessedToken>()
        var correctionCount = 0

        for (match in matches) {
            val tokenStr = match.value
            val isWord = match.groups[1] != null

            if (!isWord) {
                // Whitespace or punctuation
                outputBuilder.append(tokenStr)
                tokens.add(ProcessedToken(tokenStr, tokenStr, TokenType.PUNCTUATION_EMOJI))
                continue
            }

            // Check if this Bengali word has a registered spelling correction rule
            val rule = LipiDictionary.SPELLING_MAP[tokenStr]
            if (rule != null) {
                correctionCount++
                outputBuilder.append(rule.correct)
                tokens.add(
                    ProcessedToken(
                        original = tokenStr,
                        converted = rule.correct,
                        type = TokenType.BENGALI,
                        correctionNote = rule.reason
                    )
                )
            } else {
                // Check if word contains known common suffix typos
                val adjusted = checkPatternCorrections(tokenStr)
                if (adjusted != null) {
                    correctionCount++
                    outputBuilder.append(adjusted.first)
                    tokens.add(
                        ProcessedToken(
                            original = tokenStr,
                            converted = adjusted.first,
                            type = TokenType.BENGALI,
                            correctionNote = adjusted.second
                        )
                    )
                } else {
                    outputBuilder.append(tokenStr)
                    tokens.add(ProcessedToken(tokenStr, tokenStr, TokenType.BENGALI))
                }
            }
        }

        return SpellCheckResult(
            originalText = bengaliText,
            correctedText = outputBuilder.toString(),
            correctionsCount = correctionCount,
            tokenDetails = tokens
        )
    }

    private fun checkPatternCorrections(word: String): Pair<String, String>? {
        // 1. Common missing o-kar in common verbs (e.g. "করছিস" -> "করছিস", "বলছিস" -> "বলছিস", "করছিল" -> "করছিল")
        if (word == "ভাল") {
            return Pair("ভালো", "বিশেষণে ও-কার যুক্ত 'ভালো' প্রমিত রূপ।")
        }
        if (word == "কোরোনা") {
            return Pair("করো না", "'না' অব্যয়টি ক্রিয়াপদের পর পৃথকভাবে বসে।")
        }
        if (word == "যাবনা") {
            return Pair("যাব না", "না-সূচক অব্যয় ক্রিয়াপদের সাথে পৃথক বসে।")
        }
        if (word == "হবনা") {
            return Pair("হব না", "'না' আলাদা বসবে।")
        }
        if (word == "করবনা") {
            return Pair("করব না", "'না' আলাদা বসবে।")
        }
        // ন/ণ বিধান rule: post র্, ঋ, বা ষ্
        if (word == "ধরণ") {
            return Pair("ধরন", "'ধরণ' নয়, পদ্ধতি অর্থে প্রমিত বানান 'ধরন'।")
        }
        if (word == "পোষ্ট") {
            return Pair("পোস্ট", "বিদেশি শব্দে মূর্ধন্য 'ষ' বর্জিত হয়ে দন্ত্য 'স' (পোস্ট) হয়।")
        }
        if (word == "মাষ্টার") {
            return Pair("মাস্টার", "ইংরেজি বা বিদেশি শব্দে 'ষ্ট' এর পরিবর্তে 'স্ট' হবে।")
        }
        if (word == "রেজিষ্ট্রেশন") {
            return Pair("রেজিস্ট্রেশন", "বিদেশি শব্দে 'ষ্ট' এর বদলে 'স্ট'।")
        }
        if (word == "দাবী") {
            return Pair("দাবি", "বাংলা একাডেমি প্রমিত নিয়মে অতৎসম শব্দে হ্রস্ব ই-কার 'দাবি'।")
        }
        if (word == "বাড়ি") {
            return null // correct
        }
        if (word == "বাড়ি") {
            return Pair("বাড়ি", "বাংলা একাডেমি বর্গের ড-এ বিন্দু ড়।")
        }
        return null
    }
}
