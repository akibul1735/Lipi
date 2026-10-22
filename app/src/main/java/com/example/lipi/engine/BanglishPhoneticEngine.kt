package com.example.lipi.engine

import com.example.lipi.data.LipiDictionary

/**
 * Robust Avro-compatible phonetic transliteration engine for Banglish -> Bengali script.
 * Converts arbitrary phonetic Bengali written in English letters.
 */
object BanglishPhoneticEngine {

    // Multi-character consonant patterns sorted by length descending
    private val CONJUNCT_PATTERNS = listOf(
        // 4 chars
        "kkhy" to "ক্ষ্য",
        "cchh" to "চ্ছ",
        "shth" to "ষ্ঠ",
        "shch" to "শ্চ",
        "shpr" to "ষ্প্র",
        // 3 chars
        "kkh" to "ক্ষ",
        "cch" to "চ্ছ",
        "jjh" to "জ্ঝ",
        "tth" to "ত্থ",
        "ddh" to "দ্ধ",
        "nth" to "ন্থ",
        "ndh" to "ন্ধ",
        "mph" to "ম্ফ",
        "mbh" to "ম্ভ",
        "ngk" to "ঙ্ক",
        "ngg" to "ঙ্গ",
        "shk" to "ষ্ক",
        "shp" to "ষ্প",
        "sht" to "ষ্ট",
        "sth" to "স্থ",
        "spr" to "স্প্র",
        "str" to "স্ত্র",
        "bhr" to "ভ্র",
        "dhr" to "ধ্র",
        "ghr" to "ঘ্র",
        "khr" to "খ্র",
        "phr" to "ফ্র",
        "thr" to "থ্র",
        // 2 chars
        "kk" to "ক্ক", "kt" to "ক্ত", "kh" to "খ",
        "gg" to "জ্ঞ", "gd" to "গ্দ", "gh" to "ঘ",
        "ch" to "চ", "jh" to "ঝ",
        "tt" to "ত্ত", "tn" to "ত্ন", "tm" to "ত্ম", "tr" to "ত্র", "th" to "থ",
        "dd" to "দ্দ", "dm" to "দ্ম", "dh" to "ধ",
        "nt" to "ন্ত", "nd" to "ন্দ", "nn" to "ন্ন",
        "pt" to "প্ত", "pn" to "প্ন", "pp" to "প্প", "pr" to "প্র", "ph" to "ফ",
        "bd" to "ব্ধ", "bb" to "ব্ব", "br" to "ব্র", "bh" to "ভ",
        "mn" to "ম্ন", "mp" to "ম্প", "mb" to "ম্ব", "mm" to "ম্ম", "mr" to "ম্র",
        "ll" to "ল্ল", "lp" to "ল্প", "lk" to "ল্ক",
        "sh" to "শ", "ss" to "ষ", "sp" to "স্প", "sk" to "স্ক", "st" to "স্ত", "sn" to "স্ন", "sm" to "স্ম",
        "kr" to "ক্র", "gr" to "গ্র", "jr" to "জ্র", "dr" to "দ্র", "nr" to "ন্র", "sr" to "স্র", "hr" to "হ্র",
        "ng" to "ং"
    )

    // Single consonants
    private val SINGLE_CONSONANTS = mapOf(
        'k' to "ক", 'g' to "গ", 'c' to "চ", 'j' to "জ", 't' to "ত",
        'd' to "দ", 'n' to "ন", 'p' to "প", 'f' to "ফ", 'b' to "ব",
        'v' to "ভ", 'm' to "ম", 'z' to "জ", 'r' to "র", 'l' to "ল",
        's' to "স", 'h' to "হ", 'y' to "য়", 'w' to "ও"
    )

    // Capital letter nuances (e.g. T -> ট, D -> ড, R -> ড়, N -> ণ)
    private val CASE_CONSONANTS = mapOf(
        'T' to "ট", 'D' to "ড", 'R' to "ড়", 'N' to "ণ", 'Y' to "য়"
    )

    // Independent initial vowels
    private val INITIAL_VOWELS = listOf(
        "aee" to "ঐ", "aou" to "ঔ", "oi" to "ঐ", "ou" to "ঔ",
        "aa" to "আ", "ee" to "ঈ", "oo" to "ঊ",
        "a" to "আ", "i" to "ই", "u" to "উ", "e" to "এ", "o" to "অ"
    )

    // Dependent vowel signs (কার / Matras) after consonants
    private val VOWEL_MATRAS = listOf(
        "aee" to "ৈ", "aou" to "ৌ", "oi" to "ৈ", "ou" to "ৌ",
        "aa" to "া", "ee" to "ী", "oo" to "ূ",
        "a" to "া", "i" to "ি", "u" to "ু", "e" to "ে", "o" to "ো"
    )

    /**
     * Transliterate a single Banglish word to Bengali.
     */
    fun transliterateWord(rawWord: String, customMap: Map<String, String> = emptyMap()): String {
        if (rawWord.isBlank()) return rawWord

        val cleaned = rawWord.trim()
        val lower = cleaned.lowercase()

        // 1. Check custom user wordbook first
        customMap[lower]?.let { return matchCasingOrPreserve(cleaned, it) }

        // 2. Check rich dictionary lookup
        LipiDictionary.BANGLISH_LEXICON[lower]?.let { return it }

        // 3. Check common grammar suffixes (e.g. "amake", "tomader", "ami-o", "valo-i")
        val strippedSuffix = checkCommonSuffix(lower)
        if (strippedSuffix != null) {
            return strippedSuffix
        }

        // 4. Algorithmic State-Machine Transliteration
        return parsePhoneticLetters(cleaned)
    }

    private fun checkCommonSuffix(lower: String): String? {
        // e.g. "te" -> "তে", "er" -> "এর", "gulo" -> "গুলো", "ta" -> "টা"
        if (lower.endsWith("er") && lower.length > 3) {
            val stem = lower.substring(0, lower.length - 2)
            LipiDictionary.BANGLISH_LEXICON[stem]?.let { return "${it}ের" }
        }
        if (lower.endsWith("te") && lower.length > 3) {
            val stem = lower.substring(0, lower.length - 2)
            LipiDictionary.BANGLISH_LEXICON[stem]?.let { return "${it}তে" }
        }
        if (lower.endsWith("gulo") && lower.length > 5) {
            val stem = lower.substring(0, lower.length - 4)
            LipiDictionary.BANGLISH_LEXICON[stem]?.let { return "${it}গুলো" }
        }
        if (lower.endsWith("ta") && lower.length > 3) {
            val stem = lower.substring(0, lower.length - 2)
            LipiDictionary.BANGLISH_LEXICON[stem]?.let { return "${it}টা" }
        }
        return null
    }

    private fun parsePhoneticLetters(input: String): String {
        val result = StringBuilder()
        var i = 0
        var isAfterConsonant = false
        val len = input.length

        while (i < len) {
            val sub = input.substring(i)

            // 1. Try matching multi-char conjuncts
            var matchedConjunct: String? = null
            var conjunctLen = 0
            for ((pat, bn) in CONJUNCT_PATTERNS) {
                if (sub.startsWith(pat, ignoreCase = true)) {
                    matchedConjunct = bn
                    conjunctLen = pat.length
                    break
                }
            }

            if (matchedConjunct != null) {
                result.append(matchedConjunct)
                i += conjunctLen
                isAfterConsonant = true
                continue
            }

            // 2. Try case-sensitive specific consonants (T -> ট, D -> ড, R -> ড়, N -> ণ, S -> ষ)
            val charAt = input[i]
            if (CASE_CONSONANTS.containsKey(charAt)) {
                result.append(CASE_CONSONANTS[charAt])
                i++
                isAfterConsonant = true
                continue
            }

            // 3. Check for Vowel Matras (if after consonant)
            if (isAfterConsonant) {
                var matchedMatra: String? = null
                var matraLen = 0
                for ((vpat, matra) in VOWEL_MATRAS) {
                    if (sub.startsWith(vpat, ignoreCase = true)) {
                        matchedMatra = matra
                        matraLen = vpat.length
                        break
                    }
                }

                if (matchedMatra != null) {
                    // In Bengali, implicit 'o' or single 'a' at end of certain words might not need matra,
                    // but for general phonetic, 'a' -> া, 'i' -> ি etc.
                    result.append(matchedMatra)
                    i += matraLen
                    isAfterConsonant = false
                    continue
                }
            } else {
                // 4. Initial or Independent Vowels
                var matchedInitial: String? = null
                var initLen = 0
                for ((vpat, iv) in INITIAL_VOWELS) {
                    if (sub.startsWith(vpat, ignoreCase = true)) {
                        matchedInitial = iv
                        initLen = vpat.length
                        break
                    }
                }

                if (matchedInitial != null) {
                    result.append(matchedInitial)
                    i += initLen
                    isAfterConsonant = false
                    continue
                }
            }

            // 5. Single consonants
            val lowerChar = charAt.lowercaseChar()
            if (SINGLE_CONSONANTS.containsKey(lowerChar)) {
                result.append(SINGLE_CONSONANTS[lowerChar])
                i++
                isAfterConsonant = true
                continue
            }

            // 6. Unknown / punctuation / symbols
            result.append(charAt)
            i++
            isAfterConsonant = false
        }

        return result.toString()
    }

    private fun matchCasingOrPreserve(original: String, bengali: String): String {
        return bengali
    }
}
