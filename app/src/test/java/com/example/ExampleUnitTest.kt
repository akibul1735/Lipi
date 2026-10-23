package com.example

import com.example.lipi.data.LipiMode
import com.example.lipi.data.TransliterationStyle
import com.example.lipi.engine.BanglishPhoneticEngine
import com.example.lipi.engine.BengaliSpellDoctor
import com.example.lipi.engine.MixedScriptTokenizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testCoreBanglishTransliterationExamples() {
        val res1 = MixedScriptTokenizer.process("Ami valo asi", LipiMode.BANGLISH_TO_BENGALI)
        assertEquals("আমি ভালো আছি", res1.convertedText)

        val res2 = MixedScriptTokenizer.process("Tumi kemon acho?", LipiMode.BANGLISH_TO_BENGALI)
        assertEquals("তুমি কেমন আছো?", res2.convertedText)

        val res3 = MixedScriptTokenizer.process("Ajke ki korba?", LipiMode.BANGLISH_TO_BENGALI)
        assertTrue(res3.convertedText == "আজকে কি করবা?" || res3.convertedText == "আজকে কী করবা?")
    }

    @Test
    fun testUserRequestedAllBengaliStyleWithLoanwordsAndTime() {
        // User requested: "1। সকল টেক্সট বাংলায় (কোন কিছুই ইংরেজিতে নয়, সেসব শব্দ ইংরেজি হলেও বাংলাতে প্রচলিত, সেসবও বাংলাতে দেখাবে: app - অ্যাপ, 5pm - বিকাল পাঁচটায়, ইত্যাদি)"
        val input = "Ami app e 5pm e meeting er zoom link pathabo"
        val result = MixedScriptTokenizer.process(
            inputText = input,
            mode = LipiMode.SMART_AUTO,
            transliterationStyle = TransliterationStyle.ALL_BENGALI
        )

        // Verifies that loanwords are in authentic Bengali and no English text remains
        assertTrue("Expected 'অ্যাপ' in result", result.convertedText.contains("অ্যাপ"))
        assertTrue("Expected 'বিকাল পাঁচটায়' in result", result.convertedText.contains("বিকাল পাঁচটায়"))
        assertTrue("Expected 'মিটিং' in result", result.convertedText.contains("মিটিং"))
        assertTrue("Expected 'জুম' in result", result.convertedText.contains("জুম"))
        assertTrue("Expected 'লিংক' in result", result.convertedText.contains("লিংক"))
        assertFalse("Should not contain English word 'app'", result.convertedText.contains("app"))
        assertFalse("Should not contain English '5pm'", result.convertedText.contains("5pm"))
    }

    @Test
    fun testUserRequestedPreserveEnglishStyle() {
        // User requested: "2। কিছু ইংরেজি শব্দ preserve রাখা।"
        val input = "Sobai wait korche, sob thik thak ache to? ❤️"
        val result = MixedScriptTokenizer.process(
            inputText = input,
            mode = LipiMode.SMART_AUTO,
            transliterationStyle = TransliterationStyle.PRESERVE_ENGLISH
        )
        assertEquals("সবাই wait করছে, সব ঠিক থাক আছে তো? ❤️", result.convertedText)

        val mixed = "meeting er zoom link pathao 5pm e"
        val resPreserved = MixedScriptTokenizer.process(
            inputText = mixed,
            mode = LipiMode.SMART_AUTO,
            transliterationStyle = TransliterationStyle.PRESERVE_ENGLISH
        )
        assertTrue(resPreserved.convertedText.contains("meeting"))
        assertTrue(resPreserved.convertedText.contains("zoom"))
        assertTrue(resPreserved.convertedText.contains("link"))
    }

    @Test
    fun testEnglishSystemNotificationPreservation() {
        val engMsg = "Your security code with Akash changed. Tap to learn more."
        assertFalse(MixedScriptTokenizer.hasBanglishContent(engMsg))

        val res = MixedScriptTokenizer.process(
            inputText = engMsg,
            mode = LipiMode.SMART_AUTO,
            transliterationStyle = TransliterationStyle.PRESERVE_ENGLISH
        )
        assertEquals(engMsg, res.convertedText)
    }

    @Test
    fun testBengaliSpellingCorrection() {
        val spellResult = BengaliSpellDoctor.checkAndCorrect("আমি ভাল আছি")
        assertEquals("আমি ভালো আছি", spellResult.correctedText)
        assertTrue(spellResult.correctionsCount >= 1)

        val spellResult2 = BengaliSpellDoctor.checkAndCorrect("অসুস্হ মানুষটি")
        assertEquals("অসুস্থ মানুষটি", spellResult2.correctedText)
    }

    @Test
    fun testVersion5PhraseMatching() {
        val res1 = MixedScriptTokenizer.process("ami tomake bhalobashi", LipiMode.BANGLISH_TO_BENGALI)
        assertEquals("আমি তোমাকে ভালোবাসি", res1.convertedText)

        val res2 = MixedScriptTokenizer.process("assalamu alaikum, kemon achen?", LipiMode.BANGLISH_TO_BENGALI)
        assertEquals("আসসালামু আলাইকুম, কেমন আছেন?", res2.convertedText)

        val res3 = MixedScriptTokenizer.process("wa alaikum assalam, ami bhalo achi", LipiMode.BANGLISH_TO_BENGALI)
        assertEquals("ওয়ালাইকুম আসসালাম, আমি ভালো আছি", res3.convertedText)

        val res4 = MixedScriptTokenizer.process("kono shomossha nei, pore kotha hobe", LipiMode.BANGLISH_TO_BENGALI)
        assertEquals("কোনো সমস্যা নেই, পরে কথা হবে", res4.convertedText)

        val res5 = MixedScriptTokenizer.process("shubho jonmodin bondhu!", LipiMode.BANGLISH_TO_BENGALI)
        assertEquals("শুভ জন্মদিন বন্ধু!", res5.convertedText)
    }

    @Test
    fun testVersion5WordBank() {
        // Slang, numbers, everyday nouns
        val res1 = MixedScriptTokenizer.process("2day 2moro", LipiMode.BANGLISH_TO_BENGALI)
        assertEquals("আজ আগামীকাল", res1.convertedText)

        val res2 = MixedScriptTokenizer.process("wifi bkash faridpur", LipiMode.BANGLISH_TO_BENGALI)
        assertEquals("ওয়াইফাই বিকাশ ফরিদপুর", res2.convertedText)

        val res3 = MixedScriptTokenizer.process("afaik asap tbh", LipiMode.BANGLISH_TO_BENGALI)
        assertEquals("আমার জানা মতে যত দ্রুত সম্ভব সত্যি বলতে", res3.convertedText)
    }

    @Test
    fun testTextNormalizationRules() {
        // 1. Chat elongation normalization (repeated vowels/consonants in texting)
        val elongated = MixedScriptTokenizer.process("valoooo amiiii onnnnek khushi", LipiMode.BANGLISH_TO_BENGALI)
        assertEquals("ভালো আমি অনেক খুশি", elongated.convertedText)

        // 2. Bengali negative particle separation ("করবনা" -> "করব না", "যাবনা" -> "যাব না")
        val verbNa = BengaliSpellDoctor.checkAndCorrect("আমি কাল যাবনা এবং এটা করবনা")
        val normalizedVerbNa = com.example.lipi.engine.TextNormalizer.postNormalizeBengali(verbNa.correctedText)
        assertTrue(normalizedVerbNa.contains("যাব না"))
        assertTrue(normalizedVerbNa.contains("করব না"))

        // 3. Punctuation spacing and Dari normalization
        val punctuation = MixedScriptTokenizer.process("kemon acho ? ami bhalo asi |", LipiMode.BANGLISH_TO_BENGALI)
        assertTrue(punctuation.convertedText.contains("কেমন আছো?"))
        assertTrue(punctuation.convertedText.endsWith("।"))

        // 4. URL and Email preservation
        val urlText = "Lipi repo https://github.com/akibul1735/Lipi ekhane"
        val urlRes = MixedScriptTokenizer.process(urlText, LipiMode.SMART_AUTO)
        assertTrue(urlRes.convertedText.contains("https://github.com/akibul1735/Lipi"))

        // 5. Bengali spelling rules (আবিস্কার -> আবিষ্কার, পোষ্ট -> পোস্ট, মাষ্টার -> মাস্টার)
        val spellCheck = BengaliSpellDoctor.checkAndCorrect("নতুন পোষ্ট ও আবিস্কার")
        assertEquals("নতুন পোস্ট ও আবিষ্কার", spellCheck.correctedText)
    }

    @Test
    fun testBengaliToBanglishReverseTransliteration() {
        // Reverse Option: বাংলা - বাংলিশ
        val res1 = MixedScriptTokenizer.process("আমি ভালো আছি", LipiMode.BENGALI_TO_BANGLISH)
        assertEquals("ami bhalo achi", res1.convertedText)

        val res2 = MixedScriptTokenizer.process("তুমি কেমন আছো?", LipiMode.BENGALI_TO_BANGLISH)
        assertEquals("tumi kemon acho?", res2.convertedText)

        val res3 = MixedScriptTokenizer.process("কালকে দেখা হবে।", LipiMode.BENGALI_TO_BANGLISH)
        assertEquals("kalke dekha hobe.", res3.convertedText)

        val res4 = MixedScriptTokenizer.process("কোনো সমস্যা নেই, পরে কথা হবে", LipiMode.BENGALI_TO_BANGLISH)
        assertEquals("kono shomossha nei, pore kotha hobe", res4.convertedText)

        val res5 = MixedScriptTokenizer.process("ধন্যবাদ বন্ধু!", LipiMode.BENGALI_TO_BANGLISH)
        assertEquals("dhonnobad bondhu!", res5.convertedText)

        val res6 = MixedScriptTokenizer.process("৫০০ টাকা পাঠিয়ে দাও", LipiMode.BENGALI_TO_BANGLISH)
        assertEquals("500 taka pathiye dao", res6.convertedText)

        // Verifies user requirement: "haspatale" instead of "hasopatale" (extra 'o' removed)
        val resHospital = MixedScriptTokenizer.process("অসুস্থ মানুষটি হাসপাতালে ভর্তি", LipiMode.BENGALI_TO_BANGLISH)
        assertTrue("Expected 'haspatale' without extra 'o'", resHospital.convertedText.contains("haspatale"))
        assertFalse("Should not contain 'hasopatale'", resHospital.convertedText.contains("hasopatale"))

        val resHospitalForward = MixedScriptTokenizer.process("se haspatale ache", LipiMode.BANGLISH_TO_BENGALI)
        assertEquals("সে হাসপাতালে আছে", resHospitalForward.convertedText)
    }

    @Test
    fun testUserSentenceKhawyaSuruKorliNakiAbr() {
        // User reported: "Khawya suru korli naki abr??" -> "খাওয়া সুরু করলি নাকই আব্র??"
        // Verifies accurate transliteration to "খাওয়া শুরু করলি নাকি আবার??"
        val input = "Khawya suru korli naki abr??"
        val result = MixedScriptTokenizer.process(input, LipiMode.BANGLISH_TO_BENGALI)
        assertEquals("খাওয়া শুরু করলি নাকি আবার??", result.convertedText)
    }
}
