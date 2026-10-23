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
    }

    fun setGeminiModel(model: String) {
        repository.setGeminiModel(model)
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
        val provider = when (mode) {
            OnlineEngineMode.GEMINI_AI -> LipiOnlineEngine.Provider.GEMINI_AI
            else -> LipiOnlineEngine.Provider.GOOGLE_INPUT_TOOLS
        }

        viewModelScope.launch {
            _isOnlineTranslating.value = true
            _onlineFeedback.value = "অনলাইন এপিআই থেকে ডেটা আনা হচ্ছে..."
            val result = LipiOnlineEngine.transliterateOnline(
                inputText = currentInput,
                provider = provider,
                customGeminiApiKey = repository.geminiApiKey.value,
                geminiModel = repository.geminiModel.value
            )
            _isOnlineTranslating.value = false
            result.onSuccess { converted ->
                _onlineRefinedResult.value = converted
                _onlineFeedback.value = "অনলাইন ক্লাউড রূপান্তর সফল!"
                val finalResult = ConversionResult(
                    originalText = currentInput,
                    convertedText = converted,
                    tokens = listOf(ProcessedToken(currentInput, converted, TokenType.BANGLISH)),
                    modeUsed = _selectedMode.value
                )
                repository.addRecentConversion(finalResult)
            }.onFailure { err ->
                _onlineFeedback.value = "অনলাইন ত্রুটি: ${err.message ?: "কানেকশন ব্যাহত হয়েছে"}"
            }
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
