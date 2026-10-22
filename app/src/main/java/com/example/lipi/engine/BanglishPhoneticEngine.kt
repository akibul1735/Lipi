package com.example.lipi.engine

import com.example.lipi.data.LipiDictionary

/**
 * Avro-style phonetic transliteration engine for Banglish -> Bengali script.
 *
 * Fixes over the previous version:
 *  - 'o' after a consonant is the inherent vowel (ekhon -> এখন, ghor -> ঘর); only a word-final
 *    'o' becomes ো (korbo -> করবো, bolo -> বলো).
 *  - "chh" -> ছ was missing; 'x' and 'q' were missing.
 *  - A capitalised first letter / ALL-CAPS word (typical in chats) is treated as normal text,
 *    so "Tumi" is no longer read as ট+ুমি. Capitals inside a word (baRi, TaKa) still pick
 *    the retroflex letters (ড়, ট ...), including "Th" -> ঠ and "Dh" -> ঢ.
 *  - Conjunct patterns that usually cross a syllable boundary (mr, hr, nr, sr, jr, bd, gd, lk)
 *    were removed, so amra -> আমরা and kalke -> কালকে.
 *  - Suffix handling covers more endings (ke, ra, der, guli, ti, ei ...).
 */
object BanglishPhoneticEngine {

    // Multi-character consonant patterns, longest first.
    private val CONJUNCT_PATTERNS = listOf(
        // 4 chars
        "kkhy" to "ক্ষ্য",
        "cchh" to "চ্ছ",
        "shth" to "ষ্ঠ",
        "shch" to "শ্চ",
        // 3 chars
        "chh" to "ছ",
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
        "str" to "স্ত্র",
        "bhr" to "ভ্র",
        "dhr" to "ধ্র",
        "ghr" to "ঘ্র",
        "khr" to "খ্র",
        "phr" to "ফ্র",
        "thr" to "থ্র",
        // 2 chars
        "kk" to "ক্ক", "kt" to "ক্ত", "kh" to "খ",
        "gg" to "জ্ঞ", "gh" to "ঘ",
        "ch" to "চ", "jh" to "ঝ",
        "tt" to "ত্ত", "tn" to "ত্ন", "tm" to "ত্ম", "tr" to "ত্র", "th" to "থ",
        "dd" to "দ্দ", "dm" to "দ্ম", "dh" to "ধ",
        "nt" to "ন্ত", "nd" to "ন্দ", "nn" to "ন্ন",
        "pt" to "প্ত", "pn" to "প্ন", "pp" to "প্প", "pr" to "প্র", "ph" to "ফ",
        "bb" to "ব্ব", "br" to "ব্র", "bh" to "ভ",
        "mn" to "ম্ন", "mp" to "ম্প", "mb" to "ম্ব", "mm" to "ম্ম",
        "ll" to "ল্ল", "lp" to "ল্প",
        "sh" to "শ", "ss" to "ষ", "sp" to "স্প", "sk" to "স্ক", "st" to "স্ত", "sn" to "স্ন", "sm" to "স্ম",
        "kr" to "ক্র", "gr" to "গ্র", "dr" to "দ্র",
        "ng" to "ং"
    )

    private val SINGLE_CONSONANTS = mapOf(
        'k' to "ক", 'g' to "গ", 'c' to "চ", 'j' to "জ", 't' to "ত",
        'd' to "দ", 'n' to "ন", 'p' to "প", 'f' to "ফ", 'b' to "ব",
        'v' to "ভ", 'm' to "ম", 'z' to "জ", 'r' to "র", 'l' to "ল",
        's' to "স", 'h' to "হ", 'y' to "য়", 'w' to "ও", 'x' to "ক্স", 'q' to "ক"
    )

    // Capital letters inside a word select the retroflex / special forms.
    private val CASE_CONSONANTS = mapOf(
        'T' to "ট", 'D' to "ড", 'R' to "ড়", 'N' to "ণ", 'Y' to "য়"
    )

    private val INITIAL_VOWELS = listOf(
        "aee" to "ঐ", "aou" to "ঔ", "oi" to "ঐ", "ou" to "ঔ",
        "aa" to "আ", "ee" to "ঈ", "oo" to "ঊ",
        "a" to "আ", "i" to "ই", "u" to "উ", "e" to "এ", "o" to "অ"
    )

    private val VOWEL_MATRAS = listOf(
        "aee" to "ৈ", "aou" to "ৌ", "oi" to "ৈ", "ou" to "ৌ",
        "aa" to "া", "ee" to "ী", "oo" to "ূ",
        "a" to "া", "i" to "ি", "u" to "ু", "e" to "ে", "o" to "ো"
    )

    private val SUFFIXES = listOf(
        "gulo" to "গুলো", "guli" to "গুলি", "der" to "দের", "er" to "ের", "ke" to "কে",
        "ra" to "রা", "te" to "তে", "ta" to "টা", "ti" to "টি", "ei" to "েই"
    )

    private const val VOWEL_SIGNS = "ািীুূেৈোৌ"
    private const val VOWEL_LETTERS = "অআইঈউঊএঐওঔ"

    /** Transliterate a single Banglish word to Bengali. */
    fun transliterateWord(rawWord: String, customMap: Map<String, String> = emptyMap()): String {
        if (rawWord.isBlank()) return rawWord

        val cleaned = rawWord.trim()
        val lower = cleaned.lowercase()

        // 1. User wordbook
        customMap[lower]?.let { return it }

        // 2. Built-in lexicon
        LipiDictionary.BANGLISH_LEXICON[lower]?.let { return it }

        // 3. Lexicon stem + common suffix
        checkCommonSuffix(lower)?.let { return it }

        // 4. Phonetic state machine
        return parsePhoneticLetters(normalizeCase(cleaned))
    }

    /**
     * "Tumi" (sentence start) and "KORO" (shouting) are ordinary text, not retroflex hints.
     * Mixed-case words such as "baRi" or "TaKa" are left alone.
     */
    private fun normalizeCase(word: String): String {
        if (word.length < 2) return word.lowercase()
        if (word.all { !it.isLetter() || it.isUpperCase() }) return word.lowercase()
        if (word[0].isUpperCase() && word.drop(1).none { it.isUpperCase() }) return word.lowercase()
        return word
    }

    private fun checkCommonSuffix(lower: String): String? {
        for ((suffix, bn) in SUFFIXES) {
            if (lower.length > suffix.length + 1 && lower.endsWith(suffix)) {
                val stem = LipiDictionary.BANGLISH_LEXICON[lower.dropLast(suffix.length)] ?: continue
                return attachSuffix(stem, bn)
            }
        }
        return null
    }

    private fun attachSuffix(stem: String, suffix: String): String {
        val last = stem.last()
        val endsWithVowel = VOWEL_SIGNS.indexOf(last) >= 0 || VOWEL_LETTERS.indexOf(last) >= 0
        // "ের" / "েই" after a vowel lose their leading ে (বাবা + ের -> বাবার)
        return if (suffix.startsWith("ে") && endsWithVowel) stem + suffix.substring(1) else stem + suffix
    }

    private fun parsePhoneticLetters(input: String): String {
        val result = StringBuilder()
        var i = 0
        var afterConsonant = false
        val len = input.length

        while (i < len) {
            val ch = input[i]

            // 1. Retroflex / special capitals (keeps Th -> ঠ, Dh -> ঢ)
            if (CASE_CONSONANTS.containsKey(ch)) {
                when {
                    ch == 'T' && input.startsWith("Th", i) -> { result.append("ঠ"); i += 2 }
                    ch == 'D' && input.startsWith("Dh", i) -> { result.append("ঢ"); i += 2 }
                    else -> { result.append(CASE_CONSONANTS[ch]); i += 1 }
                }
                afterConsonant = true
                continue
            }

            // 2. Multi-character consonant patterns
            var conjunct: String? = null
            var conjunctLen = 0
            for ((pat, bn) in CONJUNCT_PATTERNS) {
                if (input.startsWith(pat, i, ignoreCase = true)) {
                    conjunct = bn
                    conjunctLen = pat.length
                    break
                }
            }
            if (conjunct != null) {
                result.append(conjunct)
                i += conjunctLen
                afterConsonant = true
                continue
            }

            // 3. Vowels
            if (afterConsonant) {
                var matra: String? = null
                var vowelLen = 0
                var vowelPat = ""
                for ((vpat, m) in VOWEL_MATRAS) {
                    if (input.startsWith(vpat, i, ignoreCase = true)) {
                        matra = m
                        vowelLen = vpat.length
                        vowelPat = vpat
                        break
                    }
                }
                if (matra != null) {
                    if (vowelPat == "o") {
                        // inherent অ inside a word, ো only at the end
                        if (i + 1 == len) result.append("ো")
                    } else {
                        result.append(matra)
                    }
                    i += vowelLen
                    afterConsonant = false
                    continue
                }
            } else {
                var independent: String? = null
                var vowelLen = 0
                for ((vpat, iv) in INITIAL_VOWELS) {
                    if (input.startsWith(vpat, i, ignoreCase = true)) {
                        independent = iv
                        vowelLen = vpat.length
                        break
                    }
                }
                if (independent != null) {
                    result.append(independent)
                    i += vowelLen
                    afterConsonant = false
                    continue
                }
            }

            // 4. Single consonants
            val single = SINGLE_CONSONANTS[ch.lowercaseChar()]
            if (single != null) {
                result.append(single)
                i++
                afterConsonant = true
                continue
            }

            // 5. Anything else passes through
            result.append(ch)
            i++
            afterConsonant = false
        }

        return result.toString()
    }
}
