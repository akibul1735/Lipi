package com.example.lipi.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class LipiRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("lipi_preferences", Context.MODE_PRIVATE)

    private val _targetApps = MutableStateFlow<List<TargetAppConfig>>(loadTargetApps())
    val targetApps: StateFlow<List<TargetAppConfig>> = _targetApps.asStateFlow()

    private val _isFloatingServiceEnabled = MutableStateFlow(
        prefs.getBoolean("floating_enabled", true)
    )
    val isFloatingServiceEnabled: StateFlow<Boolean> = _isFloatingServiceEnabled.asStateFlow()

    private val _convertDigitsToBengali = MutableStateFlow(
        prefs.getBoolean("convert_digits", false)
    )
    val convertDigitsToBengali: StateFlow<Boolean> = _convertDigitsToBengali.asStateFlow()

    private val _customWordbook = MutableStateFlow<List<WordbookEntry>>(loadCustomWordbook())
    val customWordbook: StateFlow<List<WordbookEntry>> = _customWordbook.asStateFlow()

    private val _recentConversions = MutableStateFlow<List<ConversionResult>>(emptyList())
    val recentConversions: StateFlow<List<ConversionResult>> = _recentConversions.asStateFlow()

    private fun loadTargetApps(): List<TargetAppConfig> {
        val defaultApps = listOf(
            TargetAppConfig("com.whatsapp", "WhatsApp", "whatsapp", true),
            TargetAppConfig("com.facebook.orca", "Messenger", "messenger", true),
            TargetAppConfig("org.telegram.messenger", "Telegram", "telegram", true),
            TargetAppConfig("com.google.android.apps.messaging", "Google Messages", "sms", true),
            TargetAppConfig("com.instagram.android", "Instagram", "instagram", false)
        )
        val saved = prefs.getString("target_apps_json", null) ?: return defaultApps
        return try {
            val jsonArray = JSONArray(saved)
            val list = mutableListOf<TargetAppConfig>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    TargetAppConfig(
                        packageName = obj.getString("packageName"),
                        appName = obj.getString("appName"),
                        iconName = obj.optString("iconName", "chat"),
                        isEnabled = obj.optBoolean("isEnabled", true)
                    )
                )
            }
            if (list.isEmpty()) defaultApps else list
        } catch (e: Exception) {
            defaultApps
        }
    }

    private fun saveTargetApps(apps: List<TargetAppConfig>) {
        val jsonArray = JSONArray()
        apps.forEach { app ->
            val obj = JSONObject().apply {
                put("packageName", app.packageName)
                put("appName", app.appName)
                put("iconName", app.iconName)
                put("isEnabled", app.isEnabled)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString("target_apps_json", jsonArray.toString()).apply()
        _targetApps.value = apps
    }

    fun toggleAppEnabled(packageName: String) {
        val updated = _targetApps.value.map {
            if (it.packageName == packageName) it.copy(isEnabled = !it.isEnabled) else it
        }
        saveTargetApps(updated)
    }

    fun setFloatingServiceEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("floating_enabled", enabled).apply()
        _isFloatingServiceEnabled.value = enabled
    }

    fun setConvertDigitsToBengali(enabled: Boolean) {
        prefs.edit().putBoolean("convert_digits", enabled).apply()
        _convertDigitsToBengali.value = enabled
    }

    private fun loadCustomWordbook(): List<WordbookEntry> {
        val saved = prefs.getString("custom_wordbook_json", null)
        val defaultEntries = listOf(
            WordbookEntry(banglish = "ki obosta", bengali = "কী অবস্থা", category = "কথোপকথন"),
            WordbookEntry(banglish = "pera nai", bengali = "প্যারা নাই", category = "কথোপকথন"),
            WordbookEntry(banglish = "chill koro", bengali = "চিল করো", category = "কথোপকথন")
        )
        if (saved == null) return defaultEntries
        return try {
            val jsonArray = JSONArray(saved)
            val list = mutableListOf<WordbookEntry>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    WordbookEntry(
                        id = obj.getString("id"),
                        banglish = obj.getString("banglish"),
                        bengali = obj.getString("bengali"),
                        category = obj.optString("category", "কাস্টম")
                    )
                )
            }
            list
        } catch (e: Exception) {
            defaultEntries
        }
    }

    fun addWordbookEntry(banglish: String, bengali: String, category: String = "কাস্টম") {
        val newEntry = WordbookEntry(
            banglish = banglish.trim().lowercase(),
            bengali = bengali.trim(),
            category = category
        )
        val updated = _customWordbook.value + newEntry
        saveCustomWordbook(updated)
    }

    fun removeWordbookEntry(id: String) {
        val updated = _customWordbook.value.filter { it.id != id }
        saveCustomWordbook(updated)
    }

    private fun saveCustomWordbook(entries: List<WordbookEntry>) {
        val jsonArray = JSONArray()
        entries.forEach { entry ->
            val obj = JSONObject().apply {
                put("id", entry.id)
                put("banglish", entry.banglish)
                put("bengali", entry.bengali)
                put("category", entry.category)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString("custom_wordbook_json", jsonArray.toString()).apply()
        _customWordbook.value = entries
    }

    fun addRecentConversion(result: ConversionResult) {
        val current = _recentConversions.value.toMutableList()
        current.add(0, result)
        if (current.size > 20) {
            _recentConversions.value = current.take(20)
        } else {
            _recentConversions.value = current
        }
    }
}
