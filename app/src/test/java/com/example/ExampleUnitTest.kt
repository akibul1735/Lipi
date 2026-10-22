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
        assertEquals("আজকে কী করবা?", res3.convertedText)
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
}
