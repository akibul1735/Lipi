package com.example.lipi.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lipi.data.ConversionResult
import com.example.lipi.data.LipiMode
import com.example.lipi.data.LipiRepository
import com.example.lipi.data.SimulatedChatMessage
import com.example.lipi.data.TargetAppConfig
import com.example.lipi.data.WordbookEntry
import com.example.lipi.engine.MixedScriptTokenizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LipiViewModel(private val repository: LipiRepository) : ViewModel() {

    private val _inputText = MutableStateFlow("Ami valo asi, tumi kemon acho? Ajke 5pm e call dao.")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _selectedMode = MutableStateFlow(LipiMode.SMART_AUTO)
    val selectedMode: StateFlow<LipiMode> = _selectedMode.asStateFlow()

    private val _overlayPermissionGranted = MutableStateFlow(false)
    val overlayPermissionGranted: StateFlow<Boolean> = _overlayPermissionGranted.asStateFlow()

    private val _accessibilityEnabled = MutableStateFlow(false)
    val accessibilityEnabled: StateFlow<Boolean> = _accessibilityEnabled.asStateFlow()

    val targetApps: StateFlow<List<TargetAppConfig>> = repository.targetApps
    val isFloatingServiceEnabled: StateFlow<Boolean> = repository.isFloatingServiceEnabled
    val convertDigitsToBengali: StateFlow<Boolean> = repository.convertDigitsToBengali
    val customWordbook: StateFlow<List<WordbookEntry>> = repository.customWordbook
    val recentConversions: StateFlow<List<ConversionResult>> = repository.recentConversions

    // Interactive Chat Simulator state
    private val _simulatedMessages = MutableStateFlow<List<SimulatedChatMessage>>(
        listOf(
            SimulatedChatMessage(
                senderName = "তানভীর (Tanvir)",
                messageText = "Kire koi tui? Taratari ay!",
                timestamp = "10:14 AM",
                isMe = false,
                detectedBanglish = true
            ),
            SimulatedChatMessage(
                senderName = "আমি",
                messageText = "Ashchi 10 minute e. Jam e porechi.",
                timestamp = "10:15 AM",
                isMe = true,
                detectedBanglish = true
            ),
            SimulatedChatMessage(
                senderName = "তানভীর (Tanvir)",
                messageText = "Ajke ki korba? Meeting er zoom link pathao please.",
                timestamp = "10:16 AM",
                isMe = false,
                detectedBanglish = true
            ),
            SimulatedChatMessage(
                senderName = "তানভীর (Tanvir)",
                messageText = "Sobai wait korche, sob thik thak ache to? ❤️",
                timestamp = "10:17 AM",
                isMe = false,
                detectedBanglish = true
            )
        )
    )
    val simulatedMessages: StateFlow<List<SimulatedChatMessage>> = _simulatedMessages.asStateFlow()

    private val _activeSimulatedPopupId = MutableStateFlow<String?>(null)
    val activeSimulatedPopupId: StateFlow<String?> = _activeSimulatedPopupId.asStateFlow()

    private val _selectedSimulatorApp = MutableStateFlow("WhatsApp")
    val selectedSimulatorApp: StateFlow<String> = _selectedSimulatorApp.asStateFlow()

    // Real-time conversion result
    val currentResult: StateFlow<ConversionResult> = combine(
        _inputText,
        _selectedMode,
        convertDigitsToBengali,
        customWordbook
    ) { text, mode, convertDigits, wordbook ->
        val customMap = wordbook.associate { it.banglish to it.bengali }
        val res = MixedScriptTokenizer.process(
            inputText = text,
            mode = mode,
            convertDigitsToBengali = convertDigits,
            customWordbook = customMap
        )
        res
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

    fun toggleApp(packageName: String) {
        repository.toggleAppEnabled(packageName)
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

    fun setSimulatorApp(app: String) {
        _selectedSimulatorApp.value = app
    }

    fun toggleSimulatedPopup(messageId: String?) {
        _activeSimulatedPopupId.value = if (_activeSimulatedPopupId.value == messageId) null else messageId
    }

    fun sendSimulatedMessage(text: String) {
        if (text.isBlank()) return
        val isBanglish = MixedScriptTokenizer.hasBanglishContent(text)
        val newMsg = SimulatedChatMessage(
            senderName = "আমি",
            messageText = text.trim(),
            timestamp = "এখনই",
            isMe = true,
            detectedBanglish = isBanglish
        )
        _simulatedMessages.value = _simulatedMessages.value + newMsg
    }

    fun checkSystemPermissions(context: Context) {
        _overlayPermissionGranted.value = Settings.canDrawOverlays(context)

        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val myService = enabledServices.any {
            it.resolveInfo?.serviceInfo?.packageName == context.packageName
        }
        _accessibilityEnabled.value = myService
    }
}
