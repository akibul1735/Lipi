package com.example.lipi.data

enum class LipiMode(val titleBn: String, val subtitleBn: String) {
    BANGLISH_TO_BENGALI("বাংলিশ → বাংলা", "ইংরেজি অক্ষরে লেখা বাংলা শব্দ রূপান্তর"),
    SPELL_DOCTOR("বাংলা বানান ডাক্তার", "শুদ্ধ বাংলা বানান ও কার-চিহ্ন সংশোধন"),
    SMART_AUTO("স্মার্ট অটো ডিটেক্ট", "বাংলিশ, বাংলা, ইংরেজি ও ইমোজি স্বয়ংক্রিয় প্রসেসিং")
}

enum class TokenType {
    BANGLISH,       // "Ami", "valo", "korba"
    BENGALI,        // "আমি", "ভালো"
    ENGLISH_TECH,   // "meeting", "zoom", "link", "office"
    NUMBER,         // "123", "5pm"
    PUNCTUATION_EMOJI // "?", "!", "😂", "❤️"
}

data class ProcessedToken(
    val original: String,
    val converted: String,
    val type: TokenType,
    val correctionNote: String? = null
)

data class ConversionResult(
    val originalText: String,
    val convertedText: String,
    val tokens: List<ProcessedToken>,
    val modeUsed: LipiMode,
    val spellingCorrectionsCount: Int = 0
)

data class WordbookEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val banglish: String,
    val bengali: String,
    val category: String = "কাস্টম"
)

data class TargetAppConfig(
    val packageName: String,
    val appName: String,
    val iconName: String,
    val isEnabled: Boolean = true
)

data class SimulatedChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val senderName: String,
    val messageText: String,
    val timestamp: String,
    val isMe: Boolean = false,
    val detectedBanglish: Boolean = true
)
