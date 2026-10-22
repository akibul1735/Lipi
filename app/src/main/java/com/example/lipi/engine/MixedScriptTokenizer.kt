package com.example.lipi.engine

import com.example.lipi.data.ConversionResult
import com.example.lipi.data.LipiDictionary
import com.example.lipi.data.LipiMode
import com.example.lipi.data.ProcessedToken
import com.example.lipi.data.TokenType

/**
 * Intelligent Mixed-Script Tokenizer & Pipeline.
 * Separates Banglish, Bengali script, English technical vocabulary, numbers, and emojis.
 * Distinguishes genuine Banglish from standard English messages (e.g. WhatsApp system notifications).
 */
object MixedScriptTokenizer {

    private val BENGALI_CHAR_RANGE = Regex("""[\u0980-\u09FF]""")
    private val LATIN_WORD_REGEX = Regex("""^[a-zA-Z]+$""")
    private val NUMBER_REGEX = Regex("""^\d+$""")
    private val TOKEN_SPLIT_REGEX = Regex("""([\p{L}\p{M}]+)|(\d+)|([^\p{L}\p{M}\d\s]+)|(\s+)""")

    // Distinctive English function and grammar words that identify English messages (e.g. WhatsApp notifications)
    private val ENGLISH_GRAMMAR_AND_SYSTEM_WORDS = setOf(
        "your", "yours", "you", "with", "without", "changed", "change", "tap", "learn",
        "more", "security", "code", "the", "this", "that", "these", "those", "from",
        "for", "have", "has", "had", "been", "was", "were", "are", "is", "and", "but",
        "not", "deleted", "encrypted", "encryption", "calling", "missed", "notification",
        "notifications", "privacy", "terms", "policy", "welcome", "congratulations",
        "yesterday", "tomorrow", "account", "settings", "message", "messages", "support"
    )

    /**
     * Determines whether a given text is predominantly standard English.
     * Prevents English notifications (e.g. "Your security code with Akash changed. Tap to learn more.")
     * from being mangled into phonetic gibberish.
     */
    fun isPredominantlyEnglish(text: String): Boolean {
        if (text.isBlank()) return false
        val words = text.lowercase().split(Regex("""\s+""")).filter { it.isNotBlank() }
        if (words.isEmpty()) return false

        var grammarHits = 0
        var banglishHits = 0

        for (w in words) {
            val cleaned = w.filter { it.isLetter() }
            if (cleaned.isBlank()) continue

            if (ENGLISH_GRAMMAR_AND_SYSTEM_WORDS.contains(cleaned)) {
                grammarHits++
            }
            if (LipiDictionary.BANGLISH_LEXICON.containsKey(cleaned) && !ENGLISH_GRAMMAR_AND_SYSTEM_WORDS.contains(cleaned)) {
                banglishHits++
            }
        }

        return (grammarHits >= 2 && grammarHits > banglishHits) || (grammarHits >= 1 && banglishHits == 0 && words.size >= 3)
    }

    /**
     * Process any input text according to specified or auto-detected mode.
     */
    fun process(
        inputText: String,
        mode: LipiMode = LipiMode.SMART_AUTO,
        convertDigitsToBengali: Boolean = false,
        customWordbook: Map<String, String> = emptyMap()
    ): ConversionResult {
        if (inputText.isBlank()) {
            return ConversionResult(inputText, inputText, emptyList(), mode, 0)
        }

        // Handle pure SPELL_DOCTOR mode
        if (mode == LipiMode.SPELL_DOCTOR) {
            val doctorResult = BengaliSpellDoctor.checkAndCorrect(inputText)
            return ConversionResult(
                originalText = inputText,
                convertedText = doctorResult.correctedText,
                tokens = doctorResult.tokenDetails,
                modeUsed = LipiMode.SPELL_DOCTOR,
                spellingCorrectionsCount = doctorResult.correctionsCount
            )
        }

        // If text is predominantly English and in SMART_AUTO mode, preserve it intact!
        if (mode == LipiMode.SMART_AUTO && isPredominantlyEnglish(inputText)) {
            val engTokens = TOKEN_SPLIT_REGEX.findAll(inputText).map { match ->
                val v = match.value
                ProcessedToken(
                    original = v,
                    converted = v,
                    type = if (LATIN_WORD_REGEX.matches(v)) TokenType.ENGLISH_TECH else TokenType.PUNCTUATION_EMOJI
                )
            }.toList()
            return ConversionResult(
                originalText = inputText,
                convertedText = inputText,
                tokens = engTokens,
                modeUsed = LipiMode.SMART_AUTO,
                spellingCorrectionsCount = 0
            )
        }

        val tokens = mutableListOf<ProcessedToken>()
        val outputBuilder = StringBuilder()
        var spellingCorrections = 0

        val matches = TOKEN_SPLIT_REGEX.findAll(inputText)

        for (match in matches) {
            val rawToken = match.value

            when {
                // 1. Whitespace
                rawToken.all { it.isWhitespace() } -> {
                    outputBuilder.append(rawToken)
                    tokens.add(ProcessedToken(rawToken, rawToken, TokenType.PUNCTUATION_EMOJI))
                }

                // 2. Pure Numbers (e.g. "5", "2026")
                NUMBER_REGEX.matches(rawToken) -> {
                    val finalNumber = if (convertDigitsToBengali) {
                        rawToken.map { LipiDictionary.DIGIT_MAP[it] ?: it }.joinToString("")
                    } else {
                        rawToken
                    }
                    outputBuilder.append(finalNumber)
                    tokens.add(ProcessedToken(rawToken, finalNumber, TokenType.NUMBER))
                }

                // 3. Bengali Script Word (contains Unicode 0980-09FF)
                BENGALI_CHAR_RANGE.containsMatchIn(rawToken) -> {
                    // Check spelling doctor for Bengali tokens
                    val spellCheck = BengaliSpellDoctor.checkAndCorrect(rawToken)
                    if (spellCheck.correctionsCount > 0) {
                        spellingCorrections += spellCheck.correctionsCount
                        outputBuilder.append(spellCheck.correctedText)
                        tokens.add(
                            ProcessedToken(
                                original = rawToken,
                                converted = spellCheck.correctedText,
                                type = TokenType.BENGALI,
                                correctionNote = spellCheck.tokenDetails.firstOrNull()?.correctionNote
                            )
                        )
                    } else {
                        outputBuilder.append(rawToken)
                        tokens.add(ProcessedToken(rawToken, rawToken, TokenType.BENGALI))
                    }
                }

                // 4. Latin / English Word
                LATIN_WORD_REGEX.matches(rawToken) -> {
                    val lower = rawToken.lowercase()

                    // Check if it's a preserved/standard English word that should stay in English
                    val isPreserved = LipiDictionary.PRESERVED_ENGLISH_WORDS.contains(lower) ||
                            (LipiDictionary.STANDARD_ENGLISH_WORDS.contains(lower) && !LipiDictionary.BANGLISH_LEXICON.containsKey(lower))

                    if (isPreserved && mode != LipiMode.BANGLISH_TO_BENGALI) {
                        outputBuilder.append(rawToken)
                        tokens.add(
                            ProcessedToken(
                                original = rawToken,
                                converted = rawToken,
                                type = TokenType.ENGLISH_TECH
                            )
                        )
                    } else {
                        // It is a Banglish token to be transliterated!
                        val transliterated = BanglishPhoneticEngine.transliterateWord(rawToken, customWordbook)
                        outputBuilder.append(transliterated)
                        tokens.add(
                            ProcessedToken(
                                original = rawToken,
                                converted = transliterated,
                                type = TokenType.BANGLISH
                            )
                        )
                    }
                }

                // 5. Punctuation, Emojis, or Symbols
                else -> {
                    outputBuilder.append(rawToken)
                    tokens.add(ProcessedToken(rawToken, rawToken, TokenType.PUNCTUATION_EMOJI))
                }
            }
        }

        return ConversionResult(
            originalText = inputText,
            convertedText = outputBuilder.toString(),
            tokens = tokens,
            modeUsed = mode,
            spellingCorrectionsCount = spellingCorrections
        )
    }

    /**
     * Determines whether a given text likely contains readable Banglish.
     * Returns false for English system messages and English chats.
     */
    fun hasBanglishContent(text: String): Boolean {
        if (text.isBlank() || text.length < 2) return false
        if (isPredominantlyEnglish(text)) return false

        val words = text.lowercase().split(Regex("""\s+""")).filter { it.isNotBlank() }
        var banglishHits = 0
        for (w in words) {
            val cleaned = w.filter { it.isLetter() }
            if (LipiDictionary.BANGLISH_LEXICON.containsKey(cleaned)) {
                banglishHits++
            }
        }
        return banglishHits >= 1
    }
}
