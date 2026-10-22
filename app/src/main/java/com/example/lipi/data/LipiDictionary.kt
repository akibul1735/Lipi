package com.example.lipi.data

/**
 * Rich offline lexicon and training rules for Lipi:
 * 1. High-frequency Banglish colloquial terms, SMS abbreviations, pronouns, and verb conjugations.
 * 2. Bengali spelling corrections (standard Bangla Academy orthography).
 * 3. English technical/borrowed words to preserve without transliterating.
 */
object LipiDictionary {

    // Common English borrowed/technical terms used in Bengali texting that must stay in English
    val PRESERVED_ENGLISH_WORDS = setOf(
        "ok", "okay", "pdf", "link", "zoom", "google", "fb", "facebook", "whatsapp",
        "email", "mail", "code", "app", "login", "password", "call", "missed",
        "video", "audio", "mic", "cam", "id", "url", "sms", "sim", "wifi", "data",
        "am", "pm", "vs", "hi", "hello", "hey", "bye", "pls", "please", "urgent",
        "presentation", "exam", "assignment", "class", "meet", "meeting", "hospital",
        "doctor", "nurse", "cv", "resume", "hr", "sir", "madam", "miss", "boss",
        "drive", "uber", "pathao", "bkash", "nagad", "rocket", "atm", "card",
        "laptop", "pc", "tab", "mobile", "charger", "battery", "selfie", "status",
        "story", "post", "like", "comment", "share", "profile", "inbox", "dm",
        "wait", "waiting", "done", "sure", "busy", "free", "send", "sent", "text",
        "msg", "typing", "late", "ready", "online", "offline", "group", "admin",
        "block", "unblock", "join", "gym", "coffee", "party", "pic", "photo",
        "tap", "learn", "more", "security", "changed", "change", "with", "your",
        "you", "see", "view", "read", "unread", "deleted", "encrypted", "encryption",
        "privacy", "settings", "terms", "policy", "support", "help", "info", "account",
        "github", "gitlab", "repo", "commit", "push", "pull", "merge", "branch",
        "download", "upload", "update", "server", "site", "web", "android", "ios",
        "software", "hardware", "screen", "display", "network", "bluetooth", "otg",
        "speaker", "headphone", "airpods", "earphone", "sim1", "sim2", "gb", "mb", "kb"
    )

    // Standard English vocabulary to detect when a message is pure English (not Banglish)
    val STANDARD_ENGLISH_WORDS = setOf(
        "the", "be", "to", "of", "and", "a", "in", "that", "have", "i", "it", "for",
        "not", "on", "with", "he", "as", "you", "do", "at", "this", "but", "his",
        "by", "from", "they", "we", "say", "her", "she", "or", "an", "will", "my",
        "one", "all", "would", "there", "their", "what", "so", "up", "out", "if",
        "about", "who", "get", "which", "go", "me", "when", "make", "can", "like",
        "time", "no", "just", "him", "know", "take", "people", "into", "year", "your",
        "good", "some", "could", "them", "see", "other", "than", "then", "now", "look",
        "only", "come", "its", "over", "think", "also", "back", "after", "use", "two",
        "how", "our", "work", "first", "well", "way", "even", "new", "want", "because",
        "any", "these", "give", "day", "most", "us", "security", "code", "changed",
        "tap", "learn", "more", "message", "messages", "deleted", "encrypted",
        "encryption", "calling", "missed", "call", "calls", "chat", "chats",
        "view", "unread", "sent", "received", "today", "yesterday", "tomorrow",
        "account", "verify", "password", "settings", "notifications", "notification",
        "privacy", "terms", "policy", "update", "download", "install", "open", "close",
        "cancel", "confirm", "continue", "please", "thank", "thanks", "welcome",
        "congratulations", "happy", "birthday", "morning", "night", "evening"
    )

    // English loanwords commonly used in Bengali texting, with their authentic Bengali representations
    // Used when user chooses "সকল টেক্সট বাংলায়" (ALL_BENGALI style)
    val COMMON_ENGLISH_LOANWORDS_TO_BENGALI = mapOf(
        "app" to "অ্যাপ",
        "apps" to "অ্যাপস",
        "meeting" to "মিটিং",
        "meet" to "মিট",
        "zoom" to "জুম",
        "link" to "লিংক",
        "pdf" to "পিডিএফ",
        "call" to "কল",
        "calling" to "কলিং",
        "missed" to "মিসড",
        "video" to "ভিডিও",
        "audio" to "অডিও",
        "mic" to "মাইক",
        "cam" to "ক্যামেরা",
        "camera" to "ক্যামেরা",
        "email" to "ইমেইল",
        "mail" to "মেইল",
        "fb" to "ফেসবুক",
        "facebook" to "ফেসবুক",
        "whatsapp" to "হোয়াটসঅ্যাপ",
        "youtube" to "ইউটিউব",
        "google" to "গুগল",
        "code" to "কোড",
        "login" to "লগইন",
        "password" to "পাসওয়ার্ড",
        "group" to "গ্রুপ",
        "admin" to "অ্যাডমিন",
        "post" to "পোস্ট",
        "like" to "লাইক",
        "comment" to "কমেন্ট",
        "share" to "শেয়ার",
        "profile" to "প্রোফাইল",
        "inbox" to "ইনবক্স",
        "dm" to "ডিএম",
        "sms" to "এসএমএস",
        "sim" to "সিম",
        "wifi" to "ওয়াইফাই",
        "data" to "ডাটা",
        "wait" to "অপেক্ষা",
        "waiting" to "অপেক্ষমান",
        "busy" to "ব্যস্ত",
        "free" to "ফ্রি",
        "ready" to "রেডি",
        "late" to "লেট",
        "done" to "ডান",
        "sure" to "শিওর",
        "send" to "পাঠাও",
        "sent" to "পাঠানো হয়েছে",
        "text" to "টেক্সট",
        "msg" to "মেসেজ",
        "typing" to "টাইপিং",
        "online" to "অনলাইন",
        "offline" to "অফলাইন",
        "pic" to "ছবি",
        "photo" to "ছবি",
        "selfie" to "সেলফি",
        "hospital" to "হাসপাতাল",
        "doctor" to "ডাক্তার",
        "nurse" to "নার্স",
        "office" to "অফিস",
        "class" to "ক্লাস",
        "exam" to "পরীক্ষা",
        "assignment" to "অ্যাসাইনমেন্ট",
        "presentation" to "প্রেজেন্টেশন",
        "cv" to "সিভি",
        "resume" to "রিজিউমে",
        "hr" to "এইচআর",
        "sir" to "স্যার",
        "madam" to "ম্যাডাম",
        "boss" to "বস",
        "uber" to "উবার",
        "pathao" to "পাঠাও",
        "bkash" to "বিকাশ",
        "nagad" to "নগদ",
        "rocket" to "রকেট",
        "atm" to "এটিএম",
        "card" to "কার্ড",
        "laptop" to "ল্যাপটপ",
        "pc" to "পিসি",
        "tab" to "ট্যাব",
        "mobile" to "মোবাইল",
        "charger" to "চার্জার",
        "battery" to "ব্যাটারি",
        "screen" to "স্ক্রিন",
        "display" to "ডিসপ্লে",
        "coffee" to "কফি",
        "tea" to "চা",
        "gym" to "জিম",
        "party" to "পার্টি",
        "ok" to "ঠিক আছে",
        "okay" to "ঠিক আছে",
        "hi" to "হাই",
        "hello" to "হ্যালো",
        "hey" to "হেই",
        "bye" to "বিদায়",
        "pls" to "দয়া করে",
        "please" to "দয়া করে",
        "thanks" to "ধন্যবাদ",
        "thank" to "ধন্যবাদ",
        "welcome" to "স্বাগতম",
        "good" to "ভালো",
        "bad" to "খারাপ",
        "fine" to "ঠিক",
        "happy" to "খুশি",
        "sad" to "দুঃখিত",
        "love" to "ভালোবাসা"
    )

    /**
     * Converts colloquial time expressions such as "5pm" to "বিকাল পাঁচটায়", "10am" to "সকাল দশটায়".
     */
    fun formatBengaliTime(token: String): String? {
        val lower = token.trim().lowercase()
        val match = Regex("""^(\d{1,2})(?::(\d{2}))?\s*(am|pm)$""").matchEntire(lower) ?: return null
        val hour = match.groupValues[1].toIntOrNull() ?: return null
        val minutes = match.groupValues[2]
        val amPm = match.groupValues[3]

        val bengaliHour = when (hour) {
            1 -> "এক"
            2 -> "দুই"
            3 -> "তিন"
            4 -> "চার"
            5 -> "পাঁচ"
            6 -> "ছয়"
            7 -> "সাত"
            8 -> "আট"
            9 -> "নয়"
            10 -> "দশ"
            11 -> "এগারো"
            12 -> "বারো"
            else -> hour.toString().map { DIGIT_MAP[it] ?: it }.joinToString("")
        }

        val period = if (amPm == "am") {
            when (hour) {
                12, in 1..4 -> "রাত"
                in 5..11 -> "সকাল"
                else -> "সকাল"
            }
        } else {
            when (hour) {
                12 -> "দুপুর"
                in 1..3 -> "দুপুর"
                in 4..6 -> "বিকাল"
                in 7..11 -> "রাত"
                else -> "সন্ধ্যা"
            }
        }

        return if (minutes.isNotBlank() && minutes != "00") {
            val bnMin = minutes.map { DIGIT_MAP[it] ?: it }.joinToString("")
            "$period $bengaliHour টা $bnMin মিনিটে"
        } else {
            "$period ${bengaliHour}টায়"
        }
    }

    // Common Banglish words and SMS forms mapped to precise Bengali script
    private val BASE_BANGLISH_LEXICON = mapOf(
        // Pronouns & Collective
        "ami" to "আমি",
        "amra" to "আমরা",
        "amake" to "আমাকে",
        "amar" to "আমার",
        "amader" to "আমাদের",
        "tumi" to "তুমি",
        "tomra" to "তোমরা",
        "tomake" to "তোমাকে",
        "tomar" to "তোমার",
        "tomader" to "তোমাদের",
        "tui" to "তুই",
        "tora" to "তোরা",
        "toke" to "তোকে",
        "tor" to "তোর",
        "toder" to "তোদের",
        "apni" to "আপনি",
        "apnara" to "আপনারা",
        "apnake" to "আপনাকে",
        "apnar" to "আপনার",
        "apnader" to "আপনাদের",
        "se" to "সে",
        "tara" to "তারা",
        "take" to "তাকে",
        "tar" to "তার",
        "tader" to "তাদের",
        "tini" to "তিনি",
        "tai" to "তাই",
        "eta" to "এটা",
        "ota" to "ওটা",
        "egulo" to "এগুলো",
        "ogulo" to "ওগুলো",
        "egula" to "এগুলো",
        "ogula" to "ওগুলো",
        "shob" to "সব",
        "sob" to "সব",
        "shobai" to "সবাই",
        "sobai" to "সবাই",
        "sobay" to "সবাই",
        "shobay" to "সবাই",
        "shobaike" to "সবাইকে",
        "sobaike" to "সবাইকে",
        "shobar" to "সবার",
        "sobar" to "সবার",

        // Chat Abbreviations & Short forms
        "kmn" to "কেমন",
        "kno" to "কেন",
        "kn" to "কেন",
        "khbr" to "খবর",
        "thx" to "ধন্যবাদ",
        "thnx" to "ধন্যবাদ",
        "tnx" to "ধন্যবাদ",
        "sotti" to "সত্যি",
        "shotti" to "সত্যি",
        "mittha" to "মিথ্যা",
        "mithya" to "মিথ্যা",
        "misa" to "মিছা",
        "valobashi" to "ভালোবাসি",
        "bhalobashi" to "ভালোবাসি",
        "valobasha" to "ভালোবাসা",
        "bhalobasha" to "ভালোবাসা",
        "ekhn" to "এখন",
        "ekhon" to "এখন",
        "tokhon" to "তখন",
        "kokhon" to "কখন",
        "kothay" to "কোথায়",
        "kothaye" to "কোথায়",
        "koi" to "কই",
        "ki" to "কী",
        "kee" to "কী",
        "kisu" to "কিছু",
        "kichu" to "কিছু",
        "kisui" to "কিছুই",
        "kichui" to "কিছুই",
        "kobe" to "কবে",
        "koto" to "কত",

        // User's Core Cases & Verb Forms: "kor" (করা)
        "korche" to "করছে",
        "korchi" to "করছি",
        "korcho" to "করছো",
        "korchen" to "করছেন",
        "korba" to "করবা",
        "korbe" to "করবে",
        "korbo" to "করব",
        "kori" to "করি",
        "koro" to "করো",
        "koren" to "করেন",
        "kore" to "করে",
        "kora" to "করা",
        "korlam" to "করলাম",
        "korlo" to "করল",
        "korle" to "করলে",
        "korsi" to "করেছি",
        "korechi" to "করেছি",
        "koresi" to "করেছি",
        "korso" to "করেছো",
        "korecho" to "করেছো",
        "koreso" to "করেছো",
        "korsen" to "করেছেন",
        "korechen" to "করেছেন",
        "koresen" to "করেছেন",
        "korte" to "করতে",

        // User's Core Cases & Verb Forms: "ach" / "as" (আছি, আছো, আছে)
        "ache" to "আছে",
        "ase" to "আছে",
        "achi" to "আছি",
        "asi" to "আছি",
        "acho" to "আছো",
        "aso" to "আছো",
        "achen" to "আছেন",
        "asen" to "আছেন",
        "chilo" to "ছিল",
        "chilam" to "ছিলাম",
        "chile" to "ছিলে",
        "chilen" to "ছিলেন",

        // Adjectives & State
        "valo" to "ভালো",
        "bhalo" to "ভালো",
        "thik" to "ঠিক",
        "thak" to "থাক",
        "thaka" to "থাকা",
        "thako" to "থাকো",
        "thaken" to "থাকেন",
        "thake" to "থাকে",
        "thakbe" to "থাকবে",
        "thakbo" to "থাকব",
        "thakba" to "থাকবা",
        "thakuk" to "থাকুক",
        "thik thak" to "ঠিকঠাক",
        "thikthak" to "ঠিকঠাক",
        "thikase" to "ঠিক আছে",
        "thikache" to "ঠিক আছে",
        "pera" to "প্যারা",
        "osthir" to "অস্থির",
        "bindas" to "বিন্দাস",
        "joss" to "জোশ",
        "pagol" to "পাগল",
        "pagli" to "পাগলী",

        // Everyday Conversational Particles & Movement
        "kemon" to "কেমন",
        "ajke" to "আজকে",
        "aajke" to "আজকে",
        "kire" to "কিরে",
        "taratari" to "তাড়াতাড়ি",
        "ay" to "আয়",
        "aay" to "আয়",
        "asho" to "আসো",
        "ashe" to "আসে",
        "ashbo" to "আসব",
        "asbo" to "আসব",
        "ashba" to "আসবা",
        "asba" to "আসবা",
        "ashbe" to "আসবে",
        "asbe" to "আসবে",
        "ashlam" to "আসলাম",
        "aslam" to "আসলাম",
        "aschi" to "আসছি",
        "ashchi" to "আসছি",
        "ashsi" to "আসছি",
        "eshechi" to "এসেছি",
        "esechi" to "এসেছি",
        "esi" to "এসেছি",
        "jacchi" to "যাচ্ছি",
        "jachhi" to "যাচ্ছি",
        "jabo" to "যাব",
        "jaba" to "যাবা",
        "jabe" to "যাবে",
        "gelam" to "গেলাম",
        "gelo" to "গেল",
        "gele" to "গেলে",
        "gechi" to "গেছি",
        "gesi" to "গেছি",
        "jani" to "জানি",
        "jano" to "জানো",
        "janen" to "জানেন",
        "jane" to "জানে",
        "shune" to "শুনে",
        "sune" to "শুনে",
        "shuno" to "শুনো",
        "suno" to "শুনো",
        "shunbo" to "শুনব",
        "sunbo" to "শুনব",
        "shunche" to "শুনছে",
        "sunche" to "শুনছে",
        "shunchi" to "শুনছি",
        "sunchi" to "শুনছি",
        "shuncho" to "শুনছো",
        "suncho" to "শুনছো",
        "shunlam" to "শুনলাম",
        "sunlam" to "শুনলাম",
        "bolte" to "বলতে",
        "bolbo" to "বলব",
        "bolcho" to "বলছো",
        "bolche" to "বলছে",
        "bolchi" to "বলছি",
        "bolsi" to "বলছি",
        "bolchen" to "বলছেন",
        "bolba" to "বলবা",
        "bolbe" to "বলবে",
        "bolo" to "বলো",
        "bolen" to "বলেন",
        "bole" to "বলে",
        "bollam" to "বললাম",
        "bujhte" to "বুঝতে",
        "bujte" to "বুঝতে",
        "bujhlam" to "বুঝলাম",
        "bujlam" to "বুঝলাম",
        "bujhechi" to "বুঝেছি",
        "bujesi" to "বুঝেছি",
        "bujhini" to "বুঝিনি",
        "bujini" to "বুঝিনি",
        "dekha" to "দেখা",
        "dekho" to "দেখো",
        "dekhechi" to "দেখেছি",
        "dekesi" to "দেখেছি",
        "dekhe" to "দেখে",
        "dekhche" to "দেখছে",
        "dekhchi" to "দেখছি",
        "dekhcho" to "দেখছো",
        "dekhchen" to "দেখছেন",
        "dekhbo" to "দেখব",
        "dekhbe" to "দেখবে",
        "dekhte" to "দেখতে",
        "dekhlam" to "দেখলাম",
        "parbo" to "পারব",
        "parba" to "পারবা",
        "parbe" to "পারবে",
        "parchi" to "পারছি",
        "parsi" to "পারছি",
        "parlam" to "পারলাম",
        "parbona" to "পারব না",
        "hobe" to "হবে",
        "hoche" to "হচ্ছে",
        "hocche" to "হচ্ছে",
        "hoise" to "হইছে",
        "hoyeche" to "হয়েছে",
        "hoyese" to "হয়েছে",
        "hoy" to "হয়",
        "holo" to "হলো",
        "hole" to "হলে",
        "na" to "না",
        "nai" to "নাই",
        "nei" to "নেই",
        "haa" to "হ্যাঁ",
        "haan" to "হ্যাঁ",
        "ha" to "হ্যাঁ",
        "keno" to "কেন",
        "to" to "তো",
        "toh" to "তো",
        "ar" to "আর",
        "aar" to "আর",
        "kintu" to "কিন্তু",
        "ebong" to "এবং",
        "ba" to "বা",
        "othoba" to "অথবা",
        "shomoy" to "সময়",
        "somoy" to "সময়",
        "shokal" to "সকাল",
        "sokal" to "সকাল",
        "dupur" to "দুপুর",
        "bikal" to "বিকাল",
        "raat" to "রাত",
        "din" to "দিন",
        "kal" to "কাল",
        "kalke" to "কালকে",
        "poroshu" to "পরশু",
        "ghumabo" to "ঘুমাব",
        "ghumaite" to "ঘুমাতে",
        "ghumacche" to "ঘুমাচ্ছে",
        "ghumano" to "ঘুমানো",
        "shute" to "শুতে",
        "uthbo" to "উঠব",
        "uthlam" to "উঠলাম",
        "utho" to "উঠো",
        "dorkar" to "দরকার",
        "proyojon" to "প্রয়োজন",
        "somossha" to "সমস্যা",
        "shomoshya" to "সমস্যা",
        "shamadhan" to "সমাধান",
        "somadhan" to "সমাধান",
        "chinta" to "চিন্তা",
        "porikkha" to "পরীক্ষা",
        "shubho" to "শুভ",
        "subho" to "শুভ",
        "shubhoratri" to "শুভরাত্রি",
        "jonmodin" to "জন্মদিন",
        "shuvechha" to "শুভেচ্ছা",
        "ovinondon" to "অভিনন্দন",
        "jam" to "জ্যাম",
        "jame" to "জ্যামে",
        "bepar" to "ব্যাপার",
        "byapar" to "ব্যাপার",

        // Relationships & Courtesies
        "bhai" to "ভাই",
        "bhaiya" to "ভাইয়া",
        "apu" to "আপু",
        "dada" to "দাদা",
        "didi" to "দিদি",
        "bondhu" to "বন্ধু",
        "dost" to "দোস্ত",
        "mama" to "মামা",
        "chacha" to "চাচা",
        "kaku" to "কাকু",
        "dhonnobad" to "ধন্যবাদ",
        "shukriya" to "শুকরিয়া",
        "sukriya" to "শুকরিয়া",
        "shagotom" to "স্বাগতম",
        "sagotom" to "স্বাগতম",
        "biday" to "বিদায়",
        "inshallah" to "ইনশাআল্লাহ",
        "subhanallah" to "সুবহানাল্লাহ",
        "alhamdulillah" to "আলহামদুলিল্লাহ",
        "khuda" to "খোদা",
        "hafez" to "হাফেজ",
        "hafiz" to "হাফেজ",

        // Places & Everyday Nouns
        "basha" to "বাসা",
        "bashay" to "বাসায়",
        "bari" to "বাড়ি",
        "barite" to "বাড়িতে",
        "school" to "স্কুল",
        "college" to "কলেজ",
        "kaj" to "কাজ",
        "kaaj" to "কাজ",
        "khabar" to "খাবার",
        "khabo" to "খাব",
        "khaba" to "খাবা",
        "khabe" to "খাবে",
        "kheyechi" to "খেয়েছি",
        "kheyesi" to "খেয়েছি",
        "khelam" to "খেলাম",
        "khawa" to "খাওয়া",
        "khaoa" to "খাওয়া",
        "bhat" to "ভাত",
        "pani" to "পানি",
        "jol" to "জল",
        "cha" to "চা",
        "ranna" to "রান্না",
        "gaan" to "গান",
        "boi" to "বই",
        "khata" to "খাতা",
        "taka" to "টাকা",
        "poisa" to "পয়সা",
        "ekta" to "একটা",
        "duto" to "দুটো",
        "duita" to "দুইটা",
        "tin" to "তিন",
        "tinta" to "তিনটা",
        "char" to "চার",
        "charta" to "চারটা",
        "paach" to "পাঁচ",
        "pachta" to "পাঁচটা",
        "choy" to "ছয়",
        "choyta" to "ছয়টা",
        "saat" to "সাত",
        "sat" to "সাত",
        "satta" to "সাতটা",
        "aat" to "আট",
        "aatta" to "আটটা",
        "noy" to "নয়",
        "noyta" to "নয়টা",
        "dosh" to "দশ",
        "doshta" to "দশটা",
        "onek" to "অনেক",
        "khub" to "খুব",
        "beshi" to "বেশি",
        "besi" to "বেশি",
        "kom" to "কম",
        "olpo" to "অল্প",
        "ektu" to "একটু",
        "besh" to "বেশ",
        "shundor" to "সুন্দর",
        "sundor" to "সুন্দর",
        "kharap" to "খারাপ",
        "notun" to "নতুন",
        "puran" to "পুরানো",
        "shohoj" to "সহজ",
        "kothin" to "কঠিন",
        "druto" to "দ্রুত",
        "dhire" to "ধীরে",
        "shathe" to "সাথে",
        "sathe" to "সাথে",
        "songe" to "সঙ্গে",
        "shonge" to "সঙ্গে",
        "bangladesh" to "বাংলাদেশ",
        "bangla" to "বাংলা",
        "dhaka" to "ঢাকা",
        "kolkata" to "কলকাতা",
        "chittagong" to "চট্টগ্রাম",
        "sylhet" to "সিলেট",
        "rajshahi" to "রাজশাহী",
        "khulna" to "খুলনা",
        "barisal" to "বরিশাল",
        "pathao" to "পাঠাও",
        "pathiyechi" to "পাঠিয়েছি",
        "pathiye" to "পাঠিয়ে",
        "dao" to "দাও",
        "den" to "দিন",
        "debo" to "দেব",
        "dicchi" to "দিচ্ছি",
        "naw" to "নাও",
        "nen" to "নেন",
        "nebo" to "নেব",
        "nicchi" to "নিচ্ছি",
        "dilam" to "দিলাম",
        "nilam" to "নিলাম"
    )

    // Common multi-word Banglish phrases from Version 5 Library (longest first)
    val BANGLISH_PHRASES: List<Pair<String, String>> = LipiV5Lexicon.PHRASES

    // Combined lexicon containing complete Version 5 expanded word bank and base words
    val BANGLISH_LEXICON: Map<String, String> = LipiV5Lexicon.WORD_BANK + BASE_BANGLISH_LEXICON

    // High-priority colloquial reverse mappings for most natural Banglish output
    val PREFERRED_BENGALI_TO_BANGLISH: Map<String, String> = mapOf(
        "আমি" to "ami", "আমরা" to "amra", "আমাকে" to "amake", "আমার" to "amar", "আমাদের" to "amader",
        "তুমি" to "tumi", "তোমরা" to "tomra", "তোমাকে" to "tomake", "তোমার" to "tomar", "তোমাদের" to "tomader",
        "তুই" to "tui", "তোরা" to "tora", "তোকে" to "toke", "তোর" to "tor", "তোদের" to "toder",
        "আপনি" to "apni", "আপনারা" to "apnara", "আপনাকে" to "apnake", "আপনার" to "apnar", "আপনাদের" to "apnader",
        "সে" to "se", "তারা" to "tara", "তাকে" to "take", "তার" to "tar", "তাদের" to "tader",
        "তিনি" to "tini", "ইনি" to "ini", "উনি" to "uni", "এটা" to "eta", "ওটা" to "ota",
        "এই" to "ei", "ওই" to "oi", "সেই" to "shei", "এগুলো" to "egulo", "ওগুলো" to "ogulo",
        "ভালো" to "bhalo", "ভাল" to "bhalo", "আছি" to "achi", "আছো" to "acho", "আছেন" to "achen", "আছে" to "ache",
        "কেমন" to "kemon", "কি" to "ki", "কী" to "ki", "কেন" to "keno", "কোথায়" to "kothay", "কখন" to "kokhon",
        "কিভাবে" to "kibhabe", "কোথাও" to "kothao", "কোন" to "kono", "কোনো" to "kono",
        "ধন্যবাদ" to "dhonnobad", "স্বাগতম" to "shagotom", "সুন্দর" to "shundor", "বন্ধু" to "bondhu",
        "ভাই" to "bhai", "বোন" to "bon", "মা" to "ma", "বাবা" to "baba",
        "আজ" to "aaj", "আজকে" to "ajke", "কাল" to "kal", "কালকে" to "kalke", "গতকাল" to "gotokal", "আগামীকাল" to "agamikal",
        "সকাল" to "sokal", "সকালে" to "sokale", "দুপুর" to "dupur", "দুপুরে" to "dupure", "বিকাল" to "bikal", "বিকালে" to "bikale",
        "সন্ধ্যা" to "shondha", "সন্ধ্যায়" to "shondhay", "রাত" to "raat", "রাতে" to "rate",
        "হ্যাঁ" to "hae", "না" to "na", "নয়" to "noy", "হবে" to "hobe", "হচ্ছে" to "hocche", "হয়েছে" to "hoyeche",
        "করব" to "korbo", "করবো" to "korbo", "করছি" to "korchi", "করছো" to "korcho", "করছেন" to "korchen", "করেছি" to "korechi",
        "যাব" to "jabo", "যাবো" to "jabo", "যাচ্ছি" to "jacchi", "যাচ্ছ" to "jaccho", "যাচ্ছেন" to "jachen",
        "খাব" to "khabo", "খাবো" to "khabo", "খাচ্ছি" to "khacchi", "বলব" to "bolbo", "বলবো" to "bolbo", "বলছি" to "bolchi",
        "শুনছি" to "shunchi", "দেখছি" to "dekhchi", "দেখব" to "dekhbo", "দেখবো" to "dekhbo",
        "দেখা" to "dekha", "কথা" to "kotha", "কাজ" to "kaj", "সময়" to "shomoy", "টাকা" to "taka",
        "ঠিক" to "thik", "একটু" to "ektu", "অনেক" to "onek", "খুব" to "khub", "অল্প" to "olpo",
        "সমস্যা" to "shomossha", "নাই" to "nai", "নেই" to "nei", "প্যারা" to "pera", "চিল" to "chill",
        "সবাই" to "sobai", "সব" to "shob", "কিছু" to "kichu", "কোথা" to "kotha", "মানুষ" to "manush",
        "ফোন" to "phone", "মেসেজ" to "message", "কল" to "call", "লিংক" to "link", "ছবি" to "chobi"
    )

    // Reverse multi-word phrases (longest first)
    val REVERSE_PHRASES: List<Pair<String, String>> by lazy {
        val list = mutableListOf<Pair<String, String>>()
        for ((banglish, bengali) in LipiV5Lexicon.PHRASES) {
            list.add(bengali to banglish)
        }
        list.sortByDescending { it.first.length }
        list
    }

    // Lazy reverse dictionary lookup for Bengali to Banglish
    val BENGALI_TO_BANGLISH_MAP: Map<String, String> by lazy {
        val map = HashMap<String, String>(LipiV5Lexicon.WORD_BANK.size + BASE_BANGLISH_LEXICON.size)
        // 1. Base from WORD_BANK
        for ((banglish, bengali) in LipiV5Lexicon.WORD_BANK) {
            val key = bengali.trim()
            if (!map.containsKey(key)) {
                map[key] = banglish
            }
        }
        // 2. Overwrite with BASE_BANGLISH_LEXICON
        for ((banglish, bengali) in BASE_BANGLISH_LEXICON) {
            map[bengali.trim()] = banglish
        }
        // 3. Overwrite with preferred colloquial Banglish spellings
        map.putAll(PREFERRED_BENGALI_TO_BANGLISH)
        map
    }

    // Bengali Spelling Correction Dictionary:
    // Maps common Bengali spelling errors to correct standard Bengali with explanation
    data class SpellingRule(
        val incorrect: String,
        val correct: String,
        val reason: String
    )

    val SPELLING_RULES = listOf(
        SpellingRule("ভাল", "ভালো", "বিশেষণ হিসেবে এবং ও-কারান্ত উচ্চারণে 'ভালো' প্রমিত রূপ।"),
        SpellingRule("করছিলা", "করছিলে", "সম্ভ্রমসূচক বা সাধারণ অতীত ক্রিয়ায় 'করছিলে' প্রমিত।"),
        SpellingRule("অসুস্হ", "অসুস্থ", "যুক্তাক্ষর হিসেবে 'স' এর সাথে 'থ' (স্থ) হবে, 'হ' নয়।"),
        SpellingRule("পুরষ্কার", "পুরস্কার", "অ-কার ও আ-কারের পর বিসর্গ সন্ধিতে দন্ত্য 'স' হয় (পুরস্কার)।"),
        SpellingRule("আবিষ্কার", "আবিষ্কার", "ই-কার বা উ-কারের পর মূর্ধন্য 'ষ' হয়।"),
        SpellingRule("আবিস্কার", "আবিষ্কার", "'আবিস্কার' নয়, ই-কারান্ত উপসর্গের পর মূর্ধন্য 'ষ' (আবিষ্কার)।"),
        SpellingRule("পোষ্ট", "পোস্ট", "বিদেশি শব্দে মূর্ধন্য 'ষ' বর্জিত হয়ে দন্ত্য 'স' (পোস্ট)।"),
        SpellingRule("মাষ্টার", "মাস্টার", "ইংরেজি বা বিদেশি শব্দে 'ষ্ট' এর পরিবর্তে 'স্ট' হবে (মাস্টার)।"),
        SpellingRule("রেজিষ্ট্রেশন", "রেজিস্ট্রেশন", "বিদেশি শব্দে 'ষ্ট' এর বদলে 'স্ট' (রেজিস্ট্রেশন)।"),
        SpellingRule("ষ্টেশন", "স্টেশন", "বিদেশি শব্দে 'ষ্ট' নয়, 'স্ট' (স্টেশন)।"),
        SpellingRule("দাবী", "দাবি", "বাংলা একাডেমি প্রমিত নিয়মে অতৎসম শব্দে হ্রস্ব ই-কার (দাবি)।"),
        SpellingRule("হাতী", "হাতি", "অতৎসম বাংলা শব্দে হ্রস্ব ই-কার (হাতি)।"),
        SpellingRule("উপকারীতা", "উপকারিতা", "'তা' প্রত্যয় যুক্ত হলে পূর্বের দীর্ঘ ঈ-কার হ্রস্ব ই-কার হয় (উপকারিতা)।"),
        SpellingRule("নীরবতা", "নীরবতা", "সংস্কৃত নিয়মে 'নিঃ + রব' সন্ধিজাত শব্দে দীর্ঘ ঈ-কার 'নীরব'।"),
        SpellingRule("মরিচিকা", "মরীচিকা", "শুদ্ধ বানানে প্রথমটি দীর্ঘ 'রী' এবং দ্বিতীয়টি হ্রস্ব 'চি' (মরীচিকা)।"),
        SpellingRule("ইতিমধ্যে", "ইতোমধ্যে", "সন্ধির নিয়মে অ-কারের পর অন্তস্থ 'ব' থাকলে ও-কার হয় (ইতোমধ্যে)।"),
        SpellingRule("শ্রদ্ধাঞ্জলী", "শ্রদ্ধাঞ্জলি", "আঞ্জলি প্রত্যয়যুক্ত শব্দে হ্রস্ব ই-কার হয় (শ্রদ্ধাঞ্জলি, গীতাঞ্জলি)।"),
        SpellingRule("গীতাঞ্জলী", "গীতাঞ্জলি", "আঞ্জলি প্রত্যয়যুক্ত শব্দে হ্রস্ব ই-কার হয় (গীতাঞ্জলি)।"),
        SpellingRule("সান্তনা", "সান্ত্বনা", "শুদ্ধ বানানে ত-এ ব-ফলা (ত্ব) হবে (সান্ত্বনা)।"),
        SpellingRule("স্বাগতম", "স্বাগতম", "দন্ত্য স-এ ব-ফলা যুক্ত হয়ে 'স্বাগতম' প্রমিত রূপ।"),
        SpellingRule("মুহুর্মুহু", "মুহুর্মুহু", "শুদ্ধ বানান: হ্রস্ব উ-কারযুক্ত উভয় অংশে 'মুহুর্মুহু'।"),
        SpellingRule("শহীদ", "শহিদ", "বাংলা একাডেমি প্রমিত বানানে আরবি-ফারসি শব্দে হ্রস্ব ই-কার (শহিদ)।"),
        SpellingRule("ঈদ", "ঈদ", "ঐতিহ্যবাহী বানানে দীর্ঘ ঈ-কার সমর্থিত (ঈদ)।"),
        SpellingRule("উপলক্ষে", "উপলক্ষে", "শুদ্ধ রূপ 'উপলক্ষে' (উপলক্ষ্য নয়)।"),
        SpellingRule("দূরবস্থা", "দুরবস্থা", "দুঃ + অবস্থা = দুরবস্থা (হ্রস্ব উ-কার)।"),
        SpellingRule("সঠিকতা", "সঠিকতা", "তদ্ধিত প্রত্যয়ে 'সঠিকতা' বা 'যথার্থতা'।"),
        SpellingRule("ভাষাগত", "ভাষিক", "ব্যাকরণিক শুদ্ধ রূপ 'ভাষিক'।"),
        SpellingRule("কৃতি", "কৃতী", "দক্ষ বা সফল অর্থে দীর্ঘ ঈ-কারযুক্ত 'কৃতী'।"),
        SpellingRule("দারিদ্রতা", "দরিদ্রতা", "'দরিদ্রতা' অথবা 'দারিদ্র্য' প্রমিত রূপ।"),
        SpellingRule("অধ্যায়ন", "অধ্যয়ন", "শুদ্ধ সংস্কৃত রূপ 'অধ্যয়ন' (আ-কার বর্জিত)।"),
        SpellingRule("পড়াশুনা", "পড়াশোনা", "ও-কারান্ত প্রমিত রূপ 'পড়াশোনা'।"),
        SpellingRule("খুজছি", "খুঁজছি", "নাসিক্য উচ্চারণে চন্দ্রবিন্দু (ঁ) আবশ্যক: 'খুঁজছি'।"),
        SpellingRule("বাধা", "বাঁধা", "বন্ধন অর্থে চন্দ্রবিন্দুযুক্ত 'বাঁধা', প্রতিবন্ধকতা অর্থে 'বাধা'।"),
        SpellingRule("ফোটো", "ছবি", "প্রমিত বাংলায় 'ছবি' বা বানান 'ফটো'।"),
        SpellingRule("মুহুর্ত", "মুহূর্ত", "শুদ্ধ বানানে দীর্ঘ ঊ-কারযুক্ত 'মুহূর্ত'।"),
        SpellingRule("শ্রেণী", "শ্রেণি", "বাংলা একাডেমি প্রমিত নিয়মে অতৎসম ও তৎসম আধুনিক বানানে হ্রস্ব ই-কার (শ্রেণি)।"),
        SpellingRule("পাখী", "পাখি", "অতৎসম বাংলা শব্দে সর্বদাই হ্রস্ব ই-কার (পাখি)।"),
        SpellingRule("ধরণ", "ধরন", "ণ-ত্ব বিধান অনুযায়ী অতৎসম শব্দে মূর্ধন্য 'ণ' হয় না, দন্ত্য 'ন' (ধরন)।"),
        SpellingRule("ভূবন", "ভুবন", "শুদ্ধ সংস্কৃত বানানে হ্রস্ব উ-কারযুক্ত 'ভুবন'।"),
        SpellingRule("প্রতিযোগীতা", "প্রতিযোগিতা", "'তা' প্রত্যয় যুক্ত হলে পূর্বের দীর্ঘ ঈ-কার হ্রস্ব ই-কার হয় (প্রতিযোগিতা)।"),
        SpellingRule("সহযোগীতা", "সহযোগিতা", "'তা' প্রত্যয় যুক্ত হলে হ্রস্ব ই-কার হয় (সহযোগিতা)।"),
        SpellingRule("কাহানী", "কাহিনি", "বাংলা একাডেমি নিয়মে হ্রস্ব ই-কারযুক্ত 'কাহিনি'।"),
        SpellingRule("বাঙালী", "বাঙালি", "বাংলা একাডেমি প্রমিত বানানে হ্রস্ব ই-কার (বাঙালি)।"),
        SpellingRule("উজ্জল", "উজ্জ্বল", "শুদ্ধ বানানে ব-ফলা যুক্ত হয়ে 'উজ্জ্বল'।"),
        SpellingRule("সমীচিন", "সমীচীন", "শুদ্ধ সংস্কৃত বানানে উভয়টি দীর্ঘ ঈ-কার 'সমীচীন'।")
    )

    // Lookup map for fast correction
    val SPELLING_MAP: Map<String, SpellingRule> = SPELLING_RULES.associateBy { it.incorrect }

    // Number conversion map (English digits to Bengali numerals)
    val DIGIT_MAP = mapOf(
        '0' to '০', '1' to '১', '2' to '২', '3' to '৩', '4' to '৪',
        '5' to '৫', '6' to '৬', '7' to '৭', '8' to '৮', '9' to '৯'
    )
}
