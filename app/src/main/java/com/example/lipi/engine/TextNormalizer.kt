package com.example.lipi.engine

import com.example.lipi.data.LipiDictionary

/**
 * Text Normalizer for Lipi:
 * 1. Whitespace & Unicode cleaning (removes zero-width chars, non-breaking spaces, excess whitespace).
 * 2. URL, email, @mention, and #hashtag preservation patterns.
 * 3. Chat elongation normalization (collapses stretched characters in texting: "valoooo" -> "valo", "onnnnek" -> "onek").
 * 4. Punctuation & Bengali Dari (।) normalization (replaces '|' with '।', removes space before punctuation).
 * 5. Bengali grammar & orthography normalization (negative particle 'না' separation from verbs: 'করবনা' -> 'করব না').
 */
object TextNormalizer {

    // URL regex (matches standalone URLs or www links)
    val URL_REGEX = Regex("""^(?:https?|ftp)://[^\s]+$|^www\.[^\s]+$""", RegexOption.IGNORE_CASE)

    // Email regex
    val EMAIL_REGEX = Regex("""^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$""")

    // Mention & Hashtag regex
    val MENTION_REGEX = Regex("""^@[a-zA-Z0-9_]+$""")
    val HASHTAG_REGEX = Regex("""^#[a-zA-Z0-9_\p{L}]+$""")

    // Repeated characters: 3 or more of any character
    private val REPEATED_CHARS_REGEX = Regex("""(.)\1{2,}""")

    // Common Bengali verbs concatenated with negative particle "না" that should be separated
    private val BENGALI_VERB_NA_PAIRS = listOf(
        "করবনা" to "করব না",
        "করবোনা" to "করব না",
        "করবেননা" to "করবেন না",
        "করিসনা" to "করিস না",
        "কোরোনা" to "করো না",
        "করোনা" to "করো না",
        "যাবনা" to "যাব না",
        "যাবোনা" to "যাব না",
        "যাবেননা" to "যাবেন না",
        "যেওনা" to "যেও না",
        "হবনা" to "হব না",
        "হবেনা" to "হবে না",
        "হবেননা" to "হবেন না",
        "পারবনা" to "পারব না",
        "পারবেননা" to "পারবেন না",
        "খাবনা" to "খাব না",
        "খাবেননা" to "খাবেন না",
        "খেয়োনা" to "খেও না",
        "বলবনা" to "বলব না",
        "বলবেননা" to "বলবেন না",
        "দেখবনা" to "দেখব না",
        "দেখবেননা" to "দেখবেন না",
        "শুনবনা" to "শুনব না",
        "শুনবেননা" to "শুনবেন না",
        "আসবনা" to "আসব না",
        "আসবেননা" to "আসবেন না",
        "লিখবনা" to "লিখব না",
        "পড়বনা" to "পড়ব না",
        "নেবনা" to "নেব না",
        "দেবেননা" to "দেবেন না",
        "দিবনা" to "দিব না",
        "দিবেননা" to "দিবেন না"
    )

    /**
     * Pre-normalizes input text: cleans invisible unicode chars, standardizes whitespace,
     * cleans up redundant spaces before punctuation, and normalizes Bengali Dari.
     */
    fun preNormalize(text: String): String {
        if (text.isBlank()) return text

        var result = text
            // Replace non-breaking spaces and zero-width spaces
            .replace('\u00A0', ' ')
            .replace('\u202F', ' ')
            .replace("\u200B", "")
            .replace("\uFEFF", "")

        // Collapse multiple spaces/tabs into single space per line
        result = result.replace(Regex("""[^\S\r\n]+"""), " ")

        // Fix spaces before punctuation: "kemon acho ?" -> "kemon acho?"
        result = result.replace(Regex("""\s+([?!,;:])"""), "$1")

        // Replace ASCII pipe '|' used as Dari
        result = result.replace(Regex("""\s*\|"""), "।")
        result = result.replace(Regex("""\s*।"""), "।")

        return result.trim()
    }

    /**
     * Post-normalizes the converted output text: cleans punctuation spacing,
     * normalizes Dari, and separates Bengali negative verb forms ("করবনা" -> "করব না").
     */
    fun postNormalizeBengali(text: String): String {
        if (text.isBlank()) return text

        var result = text

        // Normalize spaces before punctuation
        result = result.replace(Regex("""\s+([?!,;:।])"""), "$1")

        // Ensure single space after punctuation (unless at end or followed by newline/bracket/quote)
        result = result.replace(Regex("""([।?!,;])([^\s\d।?!,;:\"'\)\]\}])"""), "$1 $2")

        // For colon, only add space if not part of a URL protocol (like https:// or http://)
        result = result.replace(Regex("""(:)(?!\/\/|\d|\s)([^\s\d।?!,;:\"'\)\]\}])"""), "$1 $2")

        // Bengali verb + "না" separation (fast guard to save CPU cycles)
        if (result.contains("না")) {
            for ((joined, separated) in BENGALI_VERB_NA_PAIRS) {
                if (result.contains(joined)) {
                    result = result.replace(Regex("""\b$joined\b"""), separated)
                }
            }
        }

        return result
    }

    /**
     * Checks if a word has elongated / repeated characters (e.g. "onnnnek", "valoooo", "koiiii")
     * and generates candidates that match against the custom wordbook or built-in lexicon.
     */
    fun normalizeElongatedWord(
        word: String,
        customWordbook: Map<String, String>,
        builtInLexicon: Map<String, String>
    ): String? {
        val lower = word.lowercase()
        // If exact match already exists, no elongation fix needed
        if (customWordbook.containsKey(lower) || builtInLexicon.containsKey(lower)) {
            return null
        }

        // Try reducing consecutive repeating letters (3+ down to 1 or 2)
        if (REPEATED_CHARS_REGEX.containsMatchIn(lower)) {
            // Candidate 1: reduce all 3+ runs to 1
            val candidate1 = lower.replace(REPEATED_CHARS_REGEX) { it.value.substring(0, 1) }
            if (customWordbook.containsKey(candidate1) || builtInLexicon.containsKey(candidate1)) {
                return candidate1
            }

            // Candidate 2: reduce all 3+ runs to 2 (e.g., "acchaaa" -> "accha", "shooob" -> "shob" / "sob")
            val candidate2 = lower.replace(REPEATED_CHARS_REGEX) { it.value.substring(0, 2) }
            if (customWordbook.containsKey(candidate2) || builtInLexicon.containsKey(candidate2)) {
                return candidate2
            }
        }

        // Check common trailing vowel elongation (e.g., "valooo" -> "valo", "amiii" -> "ami", "bhaaaai" -> "bhai")
        val trailingVowelsMatch = Regex("""([aeiou])\1+$""").find(lower)
        if (trailingVowelsMatch != null) {
            val vowel = trailingVowelsMatch.groupValues[1]
            // Try single trailing vowel
            val trimmed1 = lower.substring(0, trailingVowelsMatch.range.first) + vowel
            if (customWordbook.containsKey(trimmed1) || builtInLexicon.containsKey(trimmed1)) {
                return trimmed1
            }
            // Try double trailing vowel (e.g. "aa" in "bhaa")
            val trimmed2 = lower.substring(0, trailingVowelsMatch.range.first) + vowel + vowel
            if (customWordbook.containsKey(trimmed2) || builtInLexicon.containsKey(trimmed2)) {
                return trimmed2
            }
        }

        return null
    }

    /**
     * Checks if a token is a protected URL, email, @mention, or #hashtag
     */
    fun isProtectedToken(token: String): Boolean {
        if (token.startsWith("http://") || token.startsWith("https://") || token.startsWith("www.") || token.startsWith("ftp://")) {
            return true
        }
        return URL_REGEX.matches(token) ||
                EMAIL_REGEX.matches(token) ||
                MENTION_REGEX.matches(token) ||
                HASHTAG_REGEX.matches(token)
    }
}
