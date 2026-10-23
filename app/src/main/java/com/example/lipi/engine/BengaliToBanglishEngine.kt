package com.example.lipi.engine

import com.example.lipi.data.ConversionResult
import com.example.lipi.data.LipiDictionary
import com.example.lipi.data.LipiMode
import com.example.lipi.data.ProcessedToken
import com.example.lipi.data.TokenType

/**
 * Ultra-lightweight, high-performance Bengali to Banglish (বাংলা → বাংলিশ) Reverse Engine.
 *
 * Architecture & Performance:
 * 1. Fast substring scan before regex matching to avoid unnecessary allocations.
 * 2. Curated reverse lexicon with preferred colloquial spellings (ami, bhalo, kemon, acho, etc.).
 * 3. Accurate Bengali phonological rules (consonants, conjuncts, matras, suppressed word-final inherent vowels).
 * 4. Zero memory bloat and minimal CPU cycles: uses StringBuilder and cached character maps.
 */
object BengaliToBanglishEngine {

    // Bengali independent vowels
    private val INITIAL_VOWELS = mapOf(
        'অ' to "o", 'আ' to "a", 'ই' to "i", 'ঈ' to "i", 'উ' to "u",
        'ঊ' to "u", 'ঋ' to "ri", 'এ' to "e", 'ঐ' to "oi", 'ও' to "o", 'ঔ' to "ou"
    )

    // Bengali vowel matras (কার-চিহ্ন)
    private val VOWEL_MATRAS = mapOf(
        'া' to "a", 'ি' to "i", 'ী' to "i", 'ু' to "u", 'ূ' to "u",
        'ৃ' to "ri", 'ে' to "e", 'ৈ' to "oi", 'ো' to "o", 'ৌ' to "ou"
    )

    // Bengali consonants
    private val CONSONANTS = mapOf(
        'ক' to "k", 'খ' to "kh", 'গ' to "g", 'ঘ' to "gh", 'ঙ' to "ng",
        'চ' to "ch", 'ছ' to "chh", 'জ' to "j", 'ঝ' to "jh", 'ঞ' to "n",
        'ট' to "t", 'ঠ' to "th", 'ড' to "d", 'ঢ' to "dh", 'ণ' to "n",
        'ত' to "t", 'থ' to "th", 'দ' to "d", 'ধ' to "dh", 'ন' to "n",
        'প' to "p", 'ফ' to "f", 'ব' to "b", 'ভ' to "bh", 'ম' to "m",
        'য' to "j", 'র' to "r", 'ল' to "l", 'শ' to "sh", 'ষ' to "sh",
        'স' to "s", 'হ' to "h", 'ড়' to "r", 'ঢ়' to "rh", 'য়' to "y",
        'ৎ' to "t", 'ং' to "ng", 'ঃ' to "h"
    )

    // Bengali digits to English digits
    private val BENGALI_TO_ENG_DIGITS = mapOf(
        '০' to '0', '১' to '1', '২' to '2', '৩' to '3', '৪' to '4',
        '৫' to '5', '৬' to '6', '৭' to '7', '৮' to '8', '৯' to '9'
    )

    // Common Bengali noun & verb suffixes for stem detachment
    private val REVERSE_SUFFIXES = listOf(
        "গুলোতে" to "gulote", "গুলোর" to "gulor", "গুলো" to "gulo",
        "গুলিতে" to "gulite", "গুলির" to "gulir", "গুলি" to "guli",
        "দেরকে" to "derke", "দের" to "der", "ের" to "er", "কে" to "ke",
        "রা" to "ra", "তে" to "te", "টা" to "ta", "টি" to "ti", "টার" to "tar", "টির" to "tir",
        "টুকু" to "tuku", "খানা" to "khana", "খানি" to "khani",
        "েই" to "ei", "ছেন" to "chen", "ছিলেন" to "chilen", "ছিলাম" to "chilam"
    )

    // Fast token splitter for Bengali & mixed content
    private val REVERSE_TOKEN_REGEX = Regex(
        """([\u0980-\u09FF]+)|([a-zA-Z0-9]+)|([^\p{L}\p{M}\d\s]+)|(\s+)"""
    )

    /**
     * Converts full Bengali/mixed text into natural Banglish.
     */
    fun process(
        inputText: String,
        customWordbook: Map<String, String> = emptyMap()
    ): ConversionResult {
        if (inputText.isBlank()) {
            return ConversionResult(inputText, inputText, emptyList(), LipiMode.BENGALI_TO_BANGLISH, 0)
        }

        // Pre-normalize whitespace and punctuation spacing
        var text = TextNormalizer.preNormalize(inputText)

        // 1. Multi-word Bengali phrase matching (longest first)
        for ((bengaliPhrase, banglishPhrase) in LipiDictionary.REVERSE_PHRASES) {
            if (text.contains(bengaliPhrase)) {
                text = text.replace(bengaliPhrase, banglishPhrase)
            }
        }

        val tokens = mutableListOf<ProcessedToken>()
        val outputBuilder = StringBuilder()

        val matches = REVERSE_TOKEN_REGEX.findAll(text)
        for (match in matches) {
            val rawToken = match.value

            when {
                // Whitespace
                rawToken.all { it.isWhitespace() } -> {
                    outputBuilder.append(rawToken)
                    tokens.add(ProcessedToken(rawToken, rawToken, TokenType.PUNCTUATION_EMOJI))
                }

                // Bengali numerals
                rawToken.all { it in '০'..'৯' } -> {
                    val engDigits = rawToken.map { BENGALI_TO_ENG_DIGITS[it] ?: it }.joinToString("")
                    outputBuilder.append(engDigits)
                    tokens.add(ProcessedToken(rawToken, engDigits, TokenType.NUMBER))
                }

                // Bengali script word
                rawToken.any { it in '\u0980'..'\u09FF' } -> {
                    val banglish = transliterateBengaliWord(rawToken, customWordbook)
                    outputBuilder.append(banglish)
                    tokens.add(ProcessedToken(rawToken, banglish, TokenType.BENGALI))
                }

                // Dari replacement
                rawToken == "।" -> {
                    outputBuilder.append(".")
                    tokens.add(ProcessedToken(rawToken, ".", TokenType.PUNCTUATION_EMOJI))
                }

                // Already English / Punctuation / Emoji
                else -> {
                    outputBuilder.append(rawToken)
                    val type = if (rawToken.any { it.isLetter() }) TokenType.ENGLISH_TECH else TokenType.PUNCTUATION_EMOJI
                    tokens.add(ProcessedToken(rawToken, rawToken, type))
                }
            }
        }

        val finalResult = outputBuilder.toString()
        return ConversionResult(
            originalText = inputText,
            convertedText = finalResult,
            tokens = tokens,
            modeUsed = LipiMode.BENGALI_TO_BANGLISH,
            spellingCorrectionsCount = 0
        )
    }

    /**
     * Transliterates an individual Bengali word into Banglish.
     */
    fun transliterateBengaliWord(
        word: String,
        customWordbook: Map<String, String> = emptyMap()
    ): String {
        val trimmed = word.trim()
        if (trimmed.isEmpty()) return trimmed

        // 1. Custom reverse wordbook (if user defined banglish -> bengali, match reverse)
        for ((banglish, bengali) in customWordbook) {
            if (bengali.equals(trimmed, ignoreCase = true)) {
                return banglish
            }
        }

        // 2. High-speed curated reverse lexicon lookup
        LipiDictionary.BENGALI_TO_BANGLISH_MAP[trimmed]?.let { return it }

        // 3. Stem + Suffix detachment
        for ((suffixBn, suffixEn) in REVERSE_SUFFIXES) {
            if (trimmed.length > suffixBn.length + 1 && trimmed.endsWith(suffixBn)) {
                val stem = trimmed.dropLast(suffixBn.length)
                val stemBanglish = LipiDictionary.BENGALI_TO_BANGLISH_MAP[stem]
                if (stemBanglish != null) {
                    return stemBanglish + suffixEn
                }
            }
        }

        // 4. Syllable & Character-level phonetic state machine
        return parseBengaliPhonetics(trimmed)
    }

    /**
     * Rule-based phonetic parser for unlisted Bengali words and names.
     */
    private fun parseBengaliPhonetics(input: String): String {
        val sb = StringBuilder()
        val len = input.length
        var i = 0

        while (i < len) {
            val ch = input[i]

            // 1. Independent Vowels (অ, আ, ই, etc.)
            val indVowel = INITIAL_VOWELS[ch]
            if (indVowel != null) {
                sb.append(indVowel)
                i++
                continue
            }

            // 2. Consonants
            val cons = CONSONANTS[ch]
            if (cons != null) {
                // Check if followed by hasanta (্) indicating a conjunct or suppressed vowel
                if (i + 1 < len && input[i + 1] == '্') {
                    // Lookahead: conjunct
                    if (i + 2 < len) {
                        val nextConsChar = input[i + 2]
                        when {
                            // ক + ্ + ষ = kkh
                            ch == 'ক' && nextConsChar == 'ষ' -> {
                                sb.append("kkh")
                                i += 3
                                // Check if followed by matra
                                if (i < len && VOWEL_MATRAS.containsKey(input[i])) {
                                    sb.append(VOWEL_MATRAS[input[i]])
                                    i++
                                }
                                continue
                            }
                            // জ + ্ + ঞ = gg / jny
                            ch == 'জ' && nextConsChar == 'ঞ' -> {
                                sb.append("gg")
                                i += 3
                                if (i < len && VOWEL_MATRAS.containsKey(input[i])) {
                                    sb.append(VOWEL_MATRAS[input[i]])
                                    i++
                                }
                                continue
                            }
                            // য-ফলা (্ + য)
                            nextConsChar == 'য' -> {
                                sb.append(cons)
                                sb.append("y")
                                i += 3
                                if (i < len && VOWEL_MATRAS.containsKey(input[i])) {
                                    sb.append(VOWEL_MATRAS[input[i]])
                                    i++
                                } else {
                                    sb.append("a")
                                }
                                continue
                            }
                            // র-ফলা (্ + র)
                            nextConsChar == 'র' -> {
                                sb.append(cons)
                                sb.append("r")
                                i += 3
                                if (i < len && VOWEL_MATRAS.containsKey(input[i])) {
                                    sb.append(VOWEL_MATRAS[input[i]])
                                    i++
                                }
                                continue
                            }
                            // ব-ফলা (্ + ব)
                            nextConsChar == 'ব' -> {
                                sb.append(cons)
                                if (ch == 'স' || ch == 'শ') {
                                    sb.append("w")
                                } else {
                                    sb.append(cons) // e.g. বিশ্ব -> bishwo
                                }
                                i += 3
                                if (i < len && VOWEL_MATRAS.containsKey(input[i])) {
                                    sb.append(VOWEL_MATRAS[input[i]])
                                    i++
                                }
                                continue
                            }
                            // General consonant + consonant conjunct
                            CONSONANTS.containsKey(nextConsChar) -> {
                                sb.append(cons)
                                sb.append(CONSONANTS[nextConsChar])
                                i += 3
                                if (i < len && VOWEL_MATRAS.containsKey(input[i])) {
                                    sb.append(VOWEL_MATRAS[input[i]])
                                    i++
                                }
                                continue
                            }
                        }
                    }
                    // Isolated hasanta (suppresses vowel)
                    sb.append(cons)
                    i += 2
                    continue
                }

                // Consonant followed by vowel matra
                if (i + 1 < len && VOWEL_MATRAS.containsKey(input[i + 1])) {
                    sb.append(cons)
                    sb.append(VOWEL_MATRAS[input[i + 1]])
                    i += 2
                    continue
                }

                // Consonant with inherent vowel:
                // In Banglish texting, word-final consonant drops inherent vowel (ghor, ekhon, kolom).
                // Non-final consonant appends inherent "o" (kolom, shokto, gorom).
                sb.append(cons)
                val isLastChar = (i + 1 >= len)
                if (!isLastChar && input[i + 1] != 'ঁ' && input[i + 1] != 'ং' && input[i + 1] != 'ঃ') {
                    val nextChar = input[i + 1]
                    // If 'স' or 'শ' is followed by another consonant (e.g., 'প' in 'হাসপাতাল', 'ত' in 'রাস্তা'),
                    // it represents a consonant cluster without inherent 'o' (e.g. haspatale, not hasopatale)
                    val isClusterS = (ch == 'স' || ch == 'শ') && CONSONANTS.containsKey(nextChar)
                    if (!isClusterS) {
                        sb.append("o")
                    }
                }
                i++
                continue
            }

            // 3. Isolated vowel matra (fallback)
            val matra = VOWEL_MATRAS[ch]
            if (matra != null) {
                sb.append(matra)
                i++
                continue
            }

            // 4. Special marks
            when (ch) {
                'ঁ' -> sb.append("n")
                'ং' -> sb.append("ng")
                'ঃ' -> sb.append("h")
                'ৎ' -> sb.append("t")
                '।' -> sb.append(".")
                else -> sb.append(ch)
            }
            i++
        }

        return sb.toString()
    }
}
