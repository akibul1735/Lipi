package com.example.lipi.engine

import com.example.lipi.data.ConversionResult
import com.example.lipi.data.LipiDictionary
import com.example.lipi.data.LipiMode
import com.example.lipi.data.ProcessedToken
import com.example.lipi.data.TokenType
import com.example.lipi.data.TransliterationStyle

/**
 * Intelligent Mixed-Script Tokenizer & Pipeline.
 * Supports user-selected transliteration style:
 * 1. ALL_BENGALI: 100% text in Bengali (common English loanwords converted: app -> অ্যাপ, 5pm -> বিকাল পাঁচটায়, etc.)
 * 2. PRESERVE_ENGLISH: Preserves technical and borrowed English terms in Latin script (zoom, meeting, link, pdf).
 */
object MixedScriptTokenizer {

    private val BENGALI_CHAR_RANGE = Regex("""[\u0980-\u09FF]""")
    private val LATIN_WORD_REGEX = Regex("""^[a-zA-Z]+$""")
    private val NUMBER_REGEX = Regex("""^\d+$""")
    private val TOKEN_SPLIT_REGEX = Regex("""([\p{L}\p{M}]+)|(\d+)|([^\p{L}\p{M}\d\s]+)|(\s+)""")
    private val TIME_REGEX = Regex("""\b(\d{1,2}(?::\d{2})?)\s*(am|pm)\b""", RegexOption.IGNORE_CASE)

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
     * Process any input text according to specified mode and transliteration style.
     */
    fun process(
        inputText: String,
        mode: LipiMode = LipiMode.SMART_AUTO,
        convertDigitsToBengali: Boolean = false,
        customWordbook: Map<String, String> = emptyMap(),
        transliterationStyle: TransliterationStyle = TransliterationStyle.ALL_BENGALI
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

        // If text is predominantly English and in SMART_AUTO mode with PRESERVE_ENGLISH style, preserve it intact!
        if (mode == LipiMode.SMART_AUTO && transliterationStyle == TransliterationStyle.PRESERVE_ENGLISH && isPredominantlyEnglish(inputText)) {
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

        // When ALL_BENGALI style is active, pre-convert colloquial times like "5pm" -> "বিকাল পাঁচটায়"
        val preprocessedText = if (transliterationStyle == TransliterationStyle.ALL_BENGALI) {
            TIME_REGEX.replace(inputText) { matchResult ->
                LipiDictionary.formatBengaliTime(matchResult.value) ?: matchResult.value
            }
        } else {
            inputText
        }

        val tokens = mutableListOf<ProcessedToken>()
        val outputBuilder = StringBuilder()
        var spellingCorrections = 0

        val matches = TOKEN_SPLIT_REGEX.findAll(preprocessedText)

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
                    val finalNumber = if (convertDigitsToBengali || transliterationStyle == TransliterationStyle.ALL_BENGALI) {
                        rawToken.map { LipiDictionary.DIGIT_MAP[it] ?: it }.joinToString("")
                    } else {
                        rawToken
                    }
                    outputBuilder.append(finalNumber)
                    tokens.add(ProcessedToken(rawToken, finalNumber, TokenType.NUMBER))
                }

                // 3. Bengali Script Word (contains Unicode 0980-09FF)
                BENGALI_CHAR_RANGE.containsMatchIn(rawToken) -> {
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

                    // User wordbook takes highest priority
                    val customWord = customWordbook[lower]

                    if (customWord != null) {
                        outputBuilder.append(customWord)
                        tokens.add(ProcessedToken(rawToken, customWord, TokenType.BANGLISH))
                    } else if (transliterationStyle == TransliterationStyle.ALL_BENGALI) {
                        // Option 1: ALL TEXT IN BENGALI (nothing in English script)
                        // Check if it's a known common English loanword (e.g. app -> অ্যাপ, zoom -> জুম, meeting -> মিটিং)
                        val loanword = LipiDictionary.COMMON_ENGLISH_LOANWORDS_TO_BENGALI[lower]

                        val resultBengali = when {
                            loanword != null -> loanword
                            LipiDictionary.BANGLISH_LEXICON.containsKey(lower) -> LipiDictionary.BANGLISH_LEXICON[lower]!!
                            else -> BanglishPhoneticEngine.transliterateWord(rawToken, customWordbook)
                        }

                        outputBuilder.append(resultBengali)
                        tokens.add(ProcessedToken(rawToken, resultBengali, TokenType.BANGLISH))
                    } else {
                        // Option 2: PRESERVE_ENGLISH (keep common English/tech terms in Latin script)
                        val isPreserved = LipiDictionary.PRESERVED_ENGLISH_WORDS.contains(lower) ||
                                (LipiDictionary.STANDARD_ENGLISH_WORDS.contains(lower) && !LipiDictionary.BANGLISH_LEXICON.containsKey(lower))

                        if (isPreserved && mode != LipiMode.BANGLISH_TO_BENGALI) {
                            outputBuilder.append(rawToken)
                            tokens.add(ProcessedToken(rawToken, rawToken, TokenType.ENGLISH_TECH))
                        } else {
                            val transliterated = BanglishPhoneticEngine.transliterateWord(rawToken, customWordbook)
                            outputBuilder.append(transliterated)
                            tokens.add(ProcessedToken(rawToken, transliterated, TokenType.BANGLISH))
                        }
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
     */
    fun hasBanglishContent(text: String): Boolean {
        if (text.isBlank() || text.length < 2) return false
        if (isPredominantlyEnglish(text)) return false

        val words = text.lowercase().split(Regex("""\s+""")).filter { it.isNotBlank() }
        var banglishHits = 0
        for (w in words) {
            val cleaned = w.filter { it.isLetter() }
            if (LipiDictionary.BANGLISH_LEXICON.containsKey(cleaned) || LipiDictionary.COMMON_ENGLISH_LOANWORDS_TO_BENGALI.containsKey(cleaned)) {
                banglishHits++
            }
        }
        return banglishHits >= 1
    }
}
