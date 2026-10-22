package com.example

import com.example.lipi.data.LipiMode
import com.example.lipi.engine.BanglishPhoneticEngine
import com.example.lipi.engine.BengaliSpellDoctor
import com.example.lipi.engine.MixedScriptTokenizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testCoreBanglishTransliterationExamples() {
        // User's explicit prompt examples
        val res1 = MixedScriptTokenizer.process("Ami valo asi", LipiMode.BANGLISH_TO_BENGALI)
        assertEquals("আমি ভালো আছি", res1.convertedText)

        val res2 = MixedScriptTokenizer.process("Tumi kemon acho?", LipiMode.BANGLISH_TO_BENGALI)
        assertEquals("তুমি কেমন আছো?", res2.convertedText)

        val res3 = MixedScriptTokenizer.process("Ajke ki korba?", LipiMode.BANGLISH_TO_BENGALI)
        assertEquals("আজকে কী করবা?", res3.convertedText)
    }

    @Test
    fun testUserSpecificFeedbackPhrase() {
        // User's reported phrase: "Sobai wait korche, sob thik thak ache to? ❤️"
        val res = MixedScriptTokenizer.process("Sobai wait korche, sob thik thak ache to? ❤️", LipiMode.SMART_AUTO)
        assertEquals("সবাই wait করছে, সব ঠিক থাক আছে তো? ❤️", res.convertedText)
    }

    @Test
    fun testBengaliSpellingCorrection() {
        // User's explicit prompt example: "আমি ভাল আছি" → "আমি ভালো আছি"
        val spellResult = BengaliSpellDoctor.checkAndCorrect("আমি ভাল আছি")
        assertEquals("আমি ভালো আছি", spellResult.correctedText)
        assertTrue(spellResult.correctionsCount >= 1)

        val spellResult2 = BengaliSpellDoctor.checkAndCorrect("অসুস্হ মানুষটি")
        assertEquals("অসুস্থ মানুষটি", spellResult2.correctedText)
    }

    @Test
    fun testMixedScriptPreservation() {
        // Mixed text: Banglish + English tech terms + emojis + numbers
        val mixed = "Ami valo asi, meeting er zoom link pathao 5pm e ❤️"
        val result = MixedScriptTokenizer.process(mixed, LipiMode.SMART_AUTO)

        // Verifies Banglish is transliterated while 'meeting', 'zoom', 'link', '5pm', '❤️' are preserved
        assertTrue(result.convertedText.contains("আমি"))
        assertTrue(result.convertedText.contains("ভালো"))
        assertTrue(result.convertedText.contains("meeting"))
        assertTrue(result.convertedText.contains("zoom"))
        assertTrue(result.convertedText.contains("link"))
        assertTrue(result.convertedText.contains("❤️"))
    }
}
