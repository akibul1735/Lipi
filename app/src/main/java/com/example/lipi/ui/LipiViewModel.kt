package com.example.lipi.ui

import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lipi.data.ConversionResult
import com.example.lipi.data.LipiMode
import com.example.lipi.data.LipiRepository
import com.example.lipi.data.WordbookEntry
import com.example.lipi.engine.MixedScriptTokenizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class LipiViewModel(private val repository: LipiRepository) : ViewModel() {

    private val _inputText = MutableStateFlow("Ami valo asi, tumi kemon acho? Ajke 5pm e call dao.")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _selectedMode = MutableStateFlow(LipiMode.SMART_AUTO)
    val selectedMode: StateFlow<LipiMode> = _selectedMode.asStateFlow()

    private val _overlayPermissionGranted = MutableStateFlow(false)
    val overlayPermissionGranted: StateFlow<Boolean> = _overlayPermissionGranted.asStateFlow()

    val isFloatingServiceEnabled: StateFlow<Boolean> = repository.isFloatingServiceEnabled
    val convertDigitsToBengali: StateFlow<Boolean> = repository.convertDigitsToBengali
    val customWordbook: StateFlow<List<WordbookEntry>> = repository.customWordbook
    val recentConversions: StateFlow<List<ConversionResult>> = repository.recentConversions

    // Real-time conversion result
    val currentResult: StateFlow<ConversionResult> = combine(
        _inputText,
        _selectedMode,
        convertDigitsToBengali,
        customWordbook
    ) { text, mode, convertDigits, wordbook ->
        val customMap = wordbook.associate { it.banglish to it.bengali }
        MixedScriptTokenizer.process(
            inputText = text,
            mode = mode,
            convertDigitsToBengali = convertDigits,
            customWordbook = customMap
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MixedScriptTokenizer.process(_inputText.value, _selectedMode.value)
    )

    fun updateInput(text: String) {
        _inputText.value = text
    }

    fun setMode(mode: LipiMode) {
        _selectedMode.value = mode
    }

    fun setFloatingServiceEnabled(enabled: Boolean) {
        repository.setFloatingServiceEnabled(enabled)
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

    fun checkSystemPermissions(context: Context) {
        _overlayPermissionGranted.value = Settings.canDrawOverlays(context)
    }
}
