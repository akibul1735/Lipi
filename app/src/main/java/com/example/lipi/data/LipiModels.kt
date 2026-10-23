package com.example.lipi.data

enum class LipiMode(val titleBn: String, val subtitleBn: String) {
    BANGLISH_TO_BENGALI("বাংলিশ → বাংলা", "ইংরেজি অক্ষরে লেখা বাংলা শব্দ রূপান্তর"),
    BENGALI_TO_BANGLISH("বাংলা → বাংলিশ", "বাংলা বর্ণমালার লেখাকে ফোনেটিক বাংলিশে রূপান্তর"),
    SPELL_DOCTOR("বাংলা বানান ডাক্তার", "শুদ্ধ বাংলা বানান ও কার-চিহ্ন সংশোধন"),
    SMART_AUTO("স্মার্ট অটো ডিটেক্ট", "বাংলিশ, বাংলা, ইংরেজি ও ইমোজি স্বয়ংক্রিয় প্রসেসিং")
}

enum class TransliterationStyle(val titleBn: String, val subtitleBn: String) {
    ALL_BENGALI(
        "সকল টেক্সট বাংলায়",
        "কোন কিছুই ইংরেজিতে নয়। ইংরেজি হলেও বাংলাতে প্রচলিত শব্দ বাংলায় দেখাবে (যেমন: app → অ্যাপ, 5pm → বিকাল ৫টায়, meeting → মিটিং)"
    ),
    PRESERVE_ENGLISH(
        "কিছু ইংরেজি শব্দ preserve রাখা",
        "প্রচলিত প্রযুক্তি ও অফিস শব্দ ইংরেজিতেই থাকবে (যেমন: zoom, meeting, link, pdf)"
    )
}

enum class OnlineEngineMode(val titleBn: String, val subtitleBn: String) {
    OFFLINE_ONLY(
        "অফলাইন ইঞ্জিন (সবচেয়ে দ্রুত)",
        "০ মিলি-সেকেন্ড ল্যাটেন্সি, ইন্টারনেট ছাড়া অভ্যন্তরীণ সমৃদ্ধ শব্দকোষ ও ফোনেটিক অ্যালগরিদম"
    ),
    ONLINE_CLOUD(
        "Google ক্লাউড ট্রান্সলিটারেশন API",
        "বিনামূল্যে ও দ্রুত ক্লাউড এপিআই — জটিল বাক্য ও আধুনিক চ্যাট রূপান্তরে নির্ভুল"
    ),
    GEMINI_AI(
        "Gemini AI স্মার্ট ইঞ্জিন",
        "উন্নত এআই কনটেক্সট মডেল — চলিত ভাষা, স্ল্যাং ও বানানের সর্বোচ্চ শুদ্ধতা"
    )
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

data class ImportResult(
    val addedCount: Int,
    val skippedCount: Int,
    val totalEntries: Int,
    val errorMessage: String? = null
)

