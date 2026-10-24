package com.example.lipi.engine

import com.example.lipi.data.ConversionResult
import com.example.lipi.data.LipiDictionary
import com.example.lipi.data.LipiMode
import com.example.lipi.data.ProcessedToken
import com.example.lipi.data.TokenType
import com.example.lipi.data.TransliterationStyle

/**
 * Intelligent Mixed-Script Tokenizer & Pipeline.
 *
 * Implements the recommended runtime order:
 * 1. Longest phrase match -> 2. Exact word lookup -> 3. Normalization & suffix check -> 4. Phonetic fallback.
 * Preserves punctuation, emojis, numbers, URLs, unknown tokens, and existing Bengali text.
 *
 * Supports user-selected transliteration style:
 * 1. ALL_BENGALI: 100% text in Bengali (common English loanwords converted: app -> অ্যাপ, 5pm -> বিকাল পাঁচটায়, etc.)
 * 2. PRESERVE_ENGLISH: Preserves technical and borrowed English terms in Latin script (zoom, meeting, link, pdf).
 */
object MixedScriptTokenizer {

    private val BENGALI_CHAR_RANGE = Regex("""[\u0980-\u09FF]""")
    private val NUMBER_REGEX = Regex("""^\d+$""")
    private val TIME_REGEX = Regex("""\b(\d{1,2}(?::\d{2})?)\s*(am|pm)\b""", RegexOption.IGNORE_CASE)

    // Token splitter:
    // 1. URLs, Emails, @mentions, #hashtags
    // 2. Alphanumeric words containing at least one Latin letter (e.g. 2day, 2moro, b4, ami, sim1)
    // 3. Bengali words and signs
    // 4. Pure digits
    // 5. Punctuation, symbols, and emojis
    // 6. Whitespace
    private val TOKEN_SPLIT_REGEX = Regex(
        """(?:https?://[^\s]+|www\.[^\s]+|[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}|@[a-zA-Z0-9_]+|#[a-zA-Z0-9_\p{L}]+)|([a-zA-Z0-9]*[a-zA-Z][a-zA-Z0-9]*)|([\u0980-\u09FF\p{M}]+)|(\d+)|([^\p{L}\p{M}\d\s]+)|(\s+)"""
    )

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
            if (LipiDictionary.findWord(cleaned) != null && !ENGLISH_GRAMMAR_AND_SYSTEM_WORDS.contains(cleaned)) {
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

        // Handle reverse mode: BENGALI_TO_BANGLISH (বাংলা → বাংলিশ)
        if (mode == LipiMode.BENGALI_TO_BANGLISH) {
            return BengaliToBanglishEngine.process(
                inputText = inputText,
                customWordbook = customWordbook
            )
        }

        // Handle pure SPELL_DOCTOR mode
        if (mode == LipiMode.SPELL_DOCTOR) {
            val doctorResult = BengaliSpellDoctor.checkAndCorrect(inputText)
            val normalizedOutput = TextNormalizer.postNormalizeBengali(doctorResult.correctedText)
            return ConversionResult(
                originalText = inputText,
                convertedText = normalizedOutput,
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
                    type = if (v.any { it.isLetter() }) TokenType.ENGLISH_TECH else TokenType.PUNCTUATION_EMOJI
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

        // Step 0: Pre-normalization (whitespace, invisible chars, and pipe Dari)
        var stageText = TextNormalizer.preNormalize(inputText)

        // When ALL_BENGALI style is active, pre-convert colloquial times like "5pm" -> "বিকাল পাঁচটায়"
        if (transliterationStyle == TransliterationStyle.ALL_BENGALI) {
            stageText = TIME_REGEX.replace(stageText) { matchResult ->
                LipiDictionary.formatBengaliTime(matchResult.value) ?: matchResult.value
            }
        }

        // Step 1: Longest Multi-word Phrase Matching (only if input contains spaces)
        if (stageText.contains(' ')) {
            stageText = LipiDictionary.replacePhrases(stageText)

            // Custom user wordbook phrases
            val customPhrases = if (customWordbook.isNotEmpty()) {
                customWordbook.entries
                    .filter { it.key.contains(" ") }
                    .sortedByDescending { it.key.length }
            } else emptyList()

            for (entry in customPhrases) {
                val phrase = entry.key
                val bengaliReplacement = entry.value
                val spaceIndex = phrase.indexOf(' ')
                val firstWord = if (spaceIndex > 0) phrase.substring(0, spaceIndex) else phrase
                if (!stageText.contains(firstWord, ignoreCase = true)) continue

                val words = phrase.trim().split(Regex("""\s+""")).filter { it.isNotBlank() }
                if (words.size >= 2) {
                    val phrasePattern = Regex("""(?i)\b""" + words.joinToString("""\s+""") { Regex.escape(it) } + """\b""")
                    if (phrasePattern.containsMatchIn(stageText)) {
                        stageText = phrasePattern.replace(stageText, bengaliReplacement)
                    }
                }
            }

            // Pre-compiled built-in phrases (zero runtime regex compilation overhead)
            for (compiled in LipiDictionary.COMPILED_BANGLISH_PHRASES) {
                if (!stageText.contains(compiled.firstWord, ignoreCase = true)) continue
                if (compiled.regex.containsMatchIn(stageText)) {
                    stageText = compiled.regex.replace(stageText, compiled.replacement)
                }
            }
        }

        val tokens = mutableListOf<ProcessedToken>()
        val outputBuilder = StringBuilder()
        var spellingCorrections = 0

        val matches = TOKEN_SPLIT_REGEX.findAll(stageText)

        for (match in matches) {
            val rawToken = match.value

            when {
                // 1. Whitespace
                rawToken.all { it.isWhitespace() } -> {
                    outputBuilder.append(rawToken)
                    tokens.add(ProcessedToken(rawToken, rawToken, TokenType.PUNCTUATION_EMOJI))
                }

                // 2. Protected URLs, Emails, @mentions, and #hashtags
                TextNormalizer.isProtectedToken(rawToken) -> {
                    outputBuilder.append(rawToken)
                    tokens.add(ProcessedToken(rawToken, rawToken, TokenType.ENGLISH_TECH))
                }

                // 3. Bengali Script Word or Phrase (contains Unicode 0980-09FF)
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

                // 4. Pure Numbers (e.g. "5", "2026")
                NUMBER_REGEX.matches(rawToken) -> {
                    val finalNumber = if (convertDigitsToBengali || transliterationStyle == TransliterationStyle.ALL_BENGALI) {
                        rawToken.map { LipiDictionary.DIGIT_MAP[it] ?: it }.joinToString("")
                    } else {
                        rawToken
                    }
                    outputBuilder.append(finalNumber)
                    tokens.add(ProcessedToken(rawToken, finalNumber, TokenType.NUMBER))
                }

                // 5. Latin / Alphanumeric Word containing letters (e.g. "ami", "2day", "2moro", "b4", "app")
                rawToken.any { it.isLetter() } -> {
                    val lower = rawToken.lowercase()
                    val v5OrLexiconWord = LipiDictionary.findWord(lower)

                    // Step 2a: Exact Custom Wordbook match
                    val customWord = customWordbook[lower]

                    if (customWord != null) {
                        outputBuilder.append(customWord)
                        tokens.add(ProcessedToken(rawToken, customWord, TokenType.BANGLISH))
                    } else if (transliterationStyle == TransliterationStyle.PRESERVE_ENGLISH &&
                        (LipiDictionary.PRESERVED_ENGLISH_WORDS.contains(lower) ||
                                (LipiDictionary.STANDARD_ENGLISH_WORDS.contains(lower) && v5OrLexiconWord == null)) &&
                        mode != LipiMode.BANGLISH_TO_BENGALI
                    ) {
                        // Preserved English term in Latin
                        outputBuilder.append(rawToken)
                        tokens.add(ProcessedToken(rawToken, rawToken, TokenType.ENGLISH_TECH))
                    } else if (transliterationStyle == TransliterationStyle.ALL_BENGALI && LipiDictionary.COMMON_ENGLISH_LOANWORDS_TO_BENGALI.containsKey(lower)) {
                        // English loanword transliterated to authentic Bengali
                        val loanword = LipiDictionary.COMMON_ENGLISH_LOANWORDS_TO_BENGALI[lower]!!
                        outputBuilder.append(loanword)
                        tokens.add(ProcessedToken(rawToken, loanword, TokenType.BANGLISH))
                    } else if (v5OrLexiconWord != null) {
                        // Exact built-in lexicon match or V5 expanded library
                        outputBuilder.append(v5OrLexiconWord)
                        tokens.add(ProcessedToken(rawToken, v5OrLexiconWord, TokenType.BANGLISH))
                    } else {
                        // Step 3: Text Normalization Rules (Chat Elongation & Suffix morphology)
                        val elongationWord = LipiDictionary.findWordWithElongation(lower)
                        val morphWord = if (elongationWord == null) LipiDictionary.findWordWithMorphology(lower) else null
                        val elongatedCandidate = if (morphWord == null && elongationWord == null) {
                            TextNormalizer.normalizeElongatedWord(lower, customWordbook, LipiDictionary.BANGLISH_LEXICON)
                        } else null
                        val resolvedWord = elongationWord ?: morphWord ?: when {
                            elongatedCandidate != null -> {
                                customWordbook[elongatedCandidate] ?: LipiDictionary.findWord(elongatedCandidate)
                            }
                            else -> null
                        }

                        if (resolvedWord != null) {
                            outputBuilder.append(resolvedWord)
                            tokens.add(ProcessedToken(rawToken, resolvedWord, TokenType.BANGLISH))
                        } else {
                            // Step 4: Phonetic state machine fallback
                            val transliterated = BanglishPhoneticEngine.transliterateWord(rawToken, customWordbook)
                            outputBuilder.append(transliterated)
                            tokens.add(ProcessedToken(rawToken, transliterated, TokenType.BANGLISH))
                        }
                    }
                }

                // 6. Punctuation, Emojis, or Symbols
                else -> {
                    val normalizedPunct = if (rawToken == "|" || rawToken == "/") "।" else rawToken
                    outputBuilder.append(normalizedPunct)
                    tokens.add(ProcessedToken(rawToken, normalizedPunct, TokenType.PUNCTUATION_EMOJI))
                }
            }
        }

        val postNormalizedText = TextNormalizer.postNormalizeBengali(outputBuilder.toString())

        return ConversionResult(
            originalText = inputText,
            convertedText = postNormalizedText,
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
            if (LipiDictionary.findWord(cleaned) != null || LipiDictionary.COMMON_ENGLISH_LOANWORDS_TO_BENGALI.containsKey(cleaned)) {
                banglishHits++
            }
        }
        return banglishHits >= 1
    }

    /**
     * Checks whether a specific Latin/Banglish word token is already recognized in the wordbank
     * (custom wordbook, V5 library, built-in lexicon, morphology, elongation, loanwords, or preserved English).
     */
    fun isWordInWordbank(
        token: String,
        customWordbook: Map<String, String>,
        mode: LipiMode = LipiMode.BANGLISH_TO_BENGALI,
        transliterationStyle: TransliterationStyle = TransliterationStyle.ALL_BENGALI
    ): Boolean {
        val lower = token.lowercase().trim()
        if (lower.isBlank()) return true
        val cleanLetters = lower.filter { it.isLetter() }
        if (cleanLetters.isBlank()) return true

        // 1. Exact Custom Wordbook match
        if (customWordbook.containsKey(lower) || customWordbook.containsKey(cleanLetters)) return true

        // 2. Preserved or standard English words
        if (transliterationStyle == TransliterationStyle.PRESERVE_ENGLISH &&
            (LipiDictionary.PRESERVED_ENGLISH_WORDS.contains(lower) ||
             (LipiDictionary.STANDARD_ENGLISH_WORDS.contains(lower) && LipiDictionary.findWord(lower) == null)) &&
            mode != LipiMode.BANGLISH_TO_BENGALI
        ) {
            return true
        }

        // 3. English loanword to Bengali
        if (LipiDictionary.COMMON_ENGLISH_LOANWORDS_TO_BENGALI.containsKey(lower) ||
            LipiDictionary.COMMON_ENGLISH_LOANWORDS_TO_BENGALI.containsKey(cleanLetters)
        ) {
            return true
        }

        // 4. Exact built-in lexicon match or V5 expanded library
        if (LipiDictionary.findWord(lower) != null || LipiDictionary.findWord(cleanLetters) != null) {
            return true
        }

        // 5. Elongation or morphology rules
        if (LipiDictionary.findWordWithElongation(lower) != null || LipiDictionary.findWordWithMorphology(lower) != null) {
            return true
        }

        val elongatedCandidate = TextNormalizer.normalizeElongatedWord(lower, customWordbook, LipiDictionary.BANGLISH_LEXICON)
        if (elongatedCandidate != null) {
            if (customWordbook.containsKey(elongatedCandidate) || LipiDictionary.findWord(elongatedCandidate) != null) {
                return true
            }
        }

        // 6. Time formatting (e.g. 5pm)
        if (LipiDictionary.formatBengaliTime(lower) != null) {
            return true
        }

        return false
    }

    /**
     * Identifies all Latin/Banglish word tokens in the input text that are NOT present in the wordbank.
     * If this list is empty, ALL input words are already in the wordbank and no Gemini call is needed.
     */
    fun identifyUnknownWords(
        inputText: String,
        customWordbook: Map<String, String>,
        mode: LipiMode = LipiMode.BANGLISH_TO_BENGALI,
        transliterationStyle: TransliterationStyle = TransliterationStyle.ALL_BENGALI
    ): List<String> {
        if (inputText.isBlank()) return emptyList()
        if (mode == LipiMode.SPELL_DOCTOR) return emptyList()

        if (mode == LipiMode.BENGALI_TO_BANGLISH) {
            // For Bengali to Banglish, find Bengali words not in BENGALI_TO_BANGLISH_MAP or customWordbook
            val matches = TOKEN_SPLIT_REGEX.findAll(inputText)
            val unknowns = mutableListOf<String>()
            for (m in matches) {
                val t = m.value.trim()
                if (t.any { it in '\u0980'..'\u09FF' }) {
                    val cleanBn = t.trim(',', '.', '?', '!', '।', ';', ':', '"', '\'', '‘', '’', '“', '”', ' ')
                    if (cleanBn.length >= 2 && !customWordbook.values.contains(cleanBn) && !LipiDictionary.BENGALI_TO_BANGLISH_MAP.containsKey(cleanBn)) {
                        unknowns.add(cleanBn)
                    }
                }
            }
            return unknowns.distinct()
        }

        if (mode == LipiMode.SMART_AUTO && transliterationStyle == TransliterationStyle.PRESERVE_ENGLISH && isPredominantlyEnglish(inputText)) {
            return emptyList()
        }

        // Pre-normalize and check phrases
        var stageText = TextNormalizer.preNormalize(inputText)
        if (transliterationStyle == TransliterationStyle.ALL_BENGALI) {
            stageText = TIME_REGEX.replace(stageText) { matchResult ->
                LipiDictionary.formatBengaliTime(matchResult.value) ?: matchResult.value
            }
        }
        if (stageText.contains(' ')) {
            stageText = LipiDictionary.replacePhrases(stageText)
        }

        val unknownWords = mutableListOf<String>()
        val matches = TOKEN_SPLIT_REGEX.findAll(stageText)
        for (match in matches) {
            val rawToken = match.value
            if (rawToken.any { it.isLetter() } && !BENGALI_CHAR_RANGE.containsMatchIn(rawToken) && !TextNormalizer.isProtectedToken(rawToken)) {
                val cleanWord = rawToken.filter { it.isLetter() }.lowercase()
                if (cleanWord.length >= 2) {
                    if (!isWordInWordbank(cleanWord, customWordbook, mode, transliterationStyle)) {
                        unknownWords.add(cleanWord)
                    }
                }
            }
        }
        return unknownWords.distinct()
    }
}
