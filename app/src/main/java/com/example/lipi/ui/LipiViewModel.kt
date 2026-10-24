package com.example.lipi.ui

import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lipi.data.ConversionResult
import com.example.lipi.data.LipiMode
import com.example.lipi.data.LipiRepository
import com.example.lipi.data.OnlineEngineMode
import com.example.lipi.data.ProcessedToken
import com.example.lipi.data.TokenType
import com.example.lipi.data.TransliterationStyle
import com.example.lipi.data.WordbookEntry
import com.example.lipi.engine.LipiOnlineEngine
import com.example.lipi.engine.MixedScriptTokenizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LipiViewModel(private val repository: LipiRepository) : ViewModel() {

    private val _inputText = MutableStateFlow("Khawya suru korli naki abr??")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _selectedMode = MutableStateFlow(LipiMode.SMART_AUTO)
    val selectedMode: StateFlow<LipiMode> = _selectedMode.asStateFlow()

    private val _overlayPermissionGranted = MutableStateFlow(false)
    val overlayPermissionGranted: StateFlow<Boolean> = _overlayPermissionGranted.asStateFlow()

    val isFloatingServiceEnabled: StateFlow<Boolean> = repository.isFloatingServiceEnabled
    val isPersistentNotificationEnabled: StateFlow<Boolean> = repository.isPersistentNotificationEnabled
    val isAutoClipboardMonitoringEnabled: StateFlow<Boolean> = repository.isAutoClipboardMonitoringEnabled
    val transliterationStyle: StateFlow<TransliterationStyle> = repository.transliterationStyle
    val onlineEngineMode: StateFlow<OnlineEngineMode> = repository.onlineEngineMode
    val geminiApiKey: StateFlow<String> = repository.geminiApiKey
    val geminiModel: StateFlow<String> = repository.geminiModel
    val geminiModelFallback1: StateFlow<String> = repository.geminiModelFallback1
    val geminiModelFallback2: StateFlow<String> = repository.geminiModelFallback2
    val fetchedGeminiModels: StateFlow<List<com.example.lipi.engine.LipiOnlineEngine.GeminiModelInfo>> = repository.fetchedGeminiModels

    private val _modelsHealthStatus = MutableStateFlow<Map<String, com.example.lipi.engine.LipiOnlineEngine.ModelHealthStatus>>(emptyMap())
    val modelsHealthStatus: StateFlow<Map<String, com.example.lipi.engine.LipiOnlineEngine.ModelHealthStatus>> = _modelsHealthStatus.asStateFlow()

    private val _isTestingModels = MutableStateFlow(false)
    val isTestingModels: StateFlow<Boolean> = _isTestingModels.asStateFlow()

    private val _isFetchingGeminiModels = MutableStateFlow(false)
    val isFetchingGeminiModels: StateFlow<Boolean> = _isFetchingGeminiModels.asStateFlow()

    private val _fetchModelsFeedback = MutableStateFlow<String?>(null)
    val fetchModelsFeedback: StateFlow<String?> = _fetchModelsFeedback.asStateFlow()

    init {
        // Automatically fetch latest models if an API key is available
        val existingKey = repository.geminiApiKey.value
        if (existingKey.isNotBlank()) {
            fetchLatestGeminiModels(existingKey)
        }
    }

    private val _isOnlineTranslating = MutableStateFlow(false)
    val isOnlineTranslating: StateFlow<Boolean> = _isOnlineTranslating.asStateFlow()

    private val _onlineFeedback = MutableStateFlow<String?>(null)
    val onlineFeedback: StateFlow<String?> = _onlineFeedback.asStateFlow()

    private val _onlineRefinedResult = MutableStateFlow<String?>(null)
    val onlineRefinedResult: StateFlow<String?> = _onlineRefinedResult.asStateFlow()
    val convertDigitsToBengali: StateFlow<Boolean> = repository.convertDigitsToBengali
    val customWordbook: StateFlow<List<WordbookEntry>> = repository.customWordbook
    val recentConversions: StateFlow<List<ConversionResult>> = repository.recentConversions

    // Cached map of custom wordbook so keystrokes don't re-allocate thousands of entries
    val customWordbookMap: StateFlow<Map<String, String>> = repository.customWordbook
        .map { list -> list.associate { it.banglish.lowercase() to it.bengali } }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            repository.customWordbook.value.associate { it.banglish.lowercase() to it.bengali }
        )

    // Real-time conversion result (zero map allocations per keystroke)
    val currentResult: StateFlow<ConversionResult> = combine(
        _inputText,
        _selectedMode,
        convertDigitsToBengali,
        customWordbookMap,
        transliterationStyle
    ) { text, mode, convertDigits, wordMap, style ->
        MixedScriptTokenizer.process(
            inputText = text,
            mode = mode,
            convertDigitsToBengali = convertDigits,
            customWordbook = wordMap,
            transliterationStyle = style
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MixedScriptTokenizer.process(_inputText.value, _selectedMode.value)
    )

    fun setTransliterationStyle(style: TransliterationStyle) {
        repository.setTransliterationStyle(style)
    }

    fun updateInput(text: String) {
        _inputText.value = text
        _onlineRefinedResult.value = null
        _onlineFeedback.value = null
    }

    fun setOnlineEngineMode(mode: OnlineEngineMode) {
        repository.setOnlineEngineMode(mode)
    }

    fun setGeminiApiKey(key: String) {
        repository.setGeminiApiKey(key)
        if (key.isNotBlank()) {
            fetchLatestGeminiModels(key)
        }
    }

    fun setGeminiModel(model: String) {
        repository.setGeminiModel(model)
    }

    fun fetchLatestGeminiModels(apiKey: String? = null) {
        val key = apiKey ?: repository.geminiApiKey.value
        viewModelScope.launch {
            _isFetchingGeminiModels.value = true
            _fetchModelsFeedback.value = "অনলাইন থেকে নতুন Gemini মডেল খোঁজা হচ্ছে..."
            val result = com.example.lipi.engine.LipiOnlineEngine.fetchAvailableModels(key)
            _isFetchingGeminiModels.value = false
            result.onSuccess { models ->
                repository.setFetchedGeminiModels(models)
                _fetchModelsFeedback.value = "সফল! ${models.size}টি মডেল পাওয়া গেছে (সর্বাধুনিক ৩.৮ সহ)।"
            }.onFailure { err ->
                _fetchModelsFeedback.value = "মডেল ফেচ করা যায়নি: ${err.message ?: "ইন্টারনেট কানেকশন চেক করুন"}"
            }
        }
    }

    fun setGeminiModelFallback1(model: String) {
        repository.setGeminiModelFallback1(model)
    }

    fun setGeminiModelFallback2(model: String) {
        repository.setGeminiModelFallback2(model)
    }

    fun testSingleModel(slotLabel: String, modelId: String) {
        val key = repository.geminiApiKey.value
        val current = _modelsHealthStatus.value.toMutableMap()
        current[modelId] = com.example.lipi.engine.LipiOnlineEngine.ModelHealthStatus(
            modelId = modelId,
            roleLabel = slotLabel,
            state = com.example.lipi.engine.LipiOnlineEngine.ModelHealthState.CHECKING,
            message = "যাচাই করা হচ্ছে..."
        )
        _modelsHealthStatus.value = current

        viewModelScope.launch {
            val res = com.example.lipi.engine.LipiOnlineEngine.checkModelHealth(key, modelId, slotLabel)
            val updated = _modelsHealthStatus.value.toMutableMap()
            updated[modelId] = res
            _modelsHealthStatus.value = updated
        }
    }

    fun testAllConfiguredModels() {
        val key = repository.geminiApiKey.value
        val m1 = repository.geminiModel.value
        val m2 = repository.geminiModelFallback1.value
        val m3 = repository.geminiModelFallback2.value
        val models = listOf(
            Pair("মডেল ১ (প্রধান)", m1),
            Pair("মডেল ২ (বিকল্প ১)", m2),
            Pair("মডেল ৩ (বিকল্প ২)", m3)
        )

        viewModelScope.launch {
            _isTestingModels.value = true
            val initial = _modelsHealthStatus.value.toMutableMap()
            models.forEach { (slot, mId) ->
                initial[mId] = com.example.lipi.engine.LipiOnlineEngine.ModelHealthStatus(
                    modelId = mId,
                    roleLabel = slot,
                    state = com.example.lipi.engine.LipiOnlineEngine.ModelHealthState.CHECKING,
                    message = "যাচাই করা হচ্ছে..."
                )
            }
            _modelsHealthStatus.value = initial

            models.forEach { (slot, mId) ->
                val res = com.example.lipi.engine.LipiOnlineEngine.checkModelHealth(key, mId, slot)
                val map = _modelsHealthStatus.value.toMutableMap()
                map[mId] = res
                _modelsHealthStatus.value = map
            }
            _isTestingModels.value = false
        }
    }

    fun loadTrainedChatDataset(): com.example.lipi.data.ImportResult {
        return repository.loadTrainedChatDataset()
    }

    fun clearOnlineFeedback() {
        _onlineFeedback.value = null
    }

    fun refineWithOnlineApi() {
        val currentInput = _inputText.value.trim()
        if (currentInput.isBlank()) return

        val mode = repository.onlineEngineMode.value

        viewModelScope.launch {
            _isOnlineTranslating.value = true
            _onlineFeedback.value = if (mode == OnlineEngineMode.GEMINI_AI) {
                "Gemini AI দিয়ে রূপান্তর করা হচ্ছে..."
            } else {
                "Google ক্লাউড ট্রান্সলিটারেশন করা হচ্ছে..."
            }

            val chain = repository.getGeminiModelsChain()
            val result = LipiOnlineEngine.transliterateSmart(
                inputText = currentInput,
                mode = _selectedMode.value,
                enginePreference = mode,
                customGeminiApiKey = repository.geminiApiKey.value,
                geminiModel = repository.geminiModel.value,
                geminiModelsChain = chain,
                customWordbook = customWordbookMap.value,
                convertDigitsToBengali = convertDigitsToBengali.value,
                transliterationStyle = transliterationStyle.value
            )
            _isOnlineTranslating.value = false

            _onlineRefinedResult.value = result.convertedText
            
            // Auto-cache learned words from Gemini into wordbook (e.g. 'clg' -> 'কলেজ')
            if (result.engineUsed == OnlineEngineMode.GEMINI_AI && !result.isFallbackToOffline) {
                val learned = repository.learnWordMappingFromGemini(currentInput, result.convertedText)
                if (learned.isNotEmpty()) {
                    val learnedSummary = learned.joinToString(", ") { "${it.first} → ${it.second}" }
                    _onlineFeedback.value = "${result.feedbackMessage ?: "Gemini AI সফল"} (শব্দকোষে সংরক্ষিত: $learnedSummary)"
                } else {
                    _onlineFeedback.value = result.feedbackMessage
                }
            } else {
                _onlineFeedback.value = result.feedbackMessage
            }

            val finalResult = ConversionResult(
                originalText = currentInput,
                convertedText = result.convertedText,
                tokens = listOf(ProcessedToken(currentInput, result.convertedText, TokenType.BANGLISH)),
                modeUsed = _selectedMode.value
            )
            repository.addRecentConversion(finalResult)
        }
    }

    fun setMode(mode: LipiMode) {
        _selectedMode.value = mode
    }

    /**
     * Swaps the input with the converted output and toggles between Banglish->Bengali and Bengali->Banglish mode.
     */
    fun swapLanguages() {
        val converted = currentResult.value.convertedText
        val newMode = if (_selectedMode.value == LipiMode.BENGALI_TO_BANGLISH) {
            LipiMode.BANGLISH_TO_BENGALI
        } else {
            LipiMode.BENGALI_TO_BANGLISH
        }
        _selectedMode.value = newMode
        if (converted.isNotBlank()) {
            _inputText.value = converted
        }
    }

    fun setFloatingServiceEnabled(enabled: Boolean) {
        repository.setFloatingServiceEnabled(enabled)
    }

    fun setPersistentNotificationEnabled(enabled: Boolean) {
        repository.setPersistentNotificationEnabled(enabled)
    }

    fun setAutoClipboardMonitoring(enabled: Boolean) {
        repository.setAutoClipboardMonitoringEnabled(enabled)
    }

    fun setConvertDigits(enabled: Boolean) {
        repository.setConvertDigitsToBengali(enabled)
    }

    fun addCustomWord(banglish: String, bengali: String, category: String = "কাস্টম") {
        repository.addWordbookEntry(banglish, bengali, category)
    }

    fun removeCustomWord(id: String) {
        repository.removeWordbookEntry(id)
    }

    fun deleteCustomWord(banglishOrId: String) {
        val found = repository.customWordbook.value.find {
            it.banglish.equals(banglishOrId, ignoreCase = true) || it.id == banglishOrId
        }
        if (found != null) {
            repository.removeWordbookEntry(found.id)
        } else {
            repository.removeWordbookEntry(banglishOrId)
        }
    }

    fun bulkImportWords(rawText: String, overwrite: Boolean = false): com.example.lipi.data.ImportResult {
        return repository.bulkImport(rawText, overwrite)
    }

    fun exportWordsAsText(): String {
        return repository.exportCustomDataAsText()
    }

    fun exportWordsAsJson(): String {
        return repository.exportCustomDataAsJson()
    }

    fun clearAllCustomWords() {
        repository.clearCustomWordbook()
    }

    fun resetWordsToDefault() {
        repository.resetToDefaultWordbook()
    }

    fun loadV5LibraryIntoWordbook(context: Context, overwrite: Boolean = false): com.example.lipi.data.ImportResult {
        return try {
            val content = context.assets.open("lipi_v5_library.txt").bufferedReader().use { it.readText() }
            repository.bulkImport(content, overwrite)
        } catch (e: Exception) {
            com.example.lipi.data.ImportResult(0, 0, repository.customWordbook.value.size, e.message)
        }
    }

    fun checkSystemPermissions(context: Context) {
        _overlayPermissionGranted.value = Settings.canDrawOverlays(context)
    }
}
