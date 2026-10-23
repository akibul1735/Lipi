package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.lipi.data.LipiRepository
import org.junit.Rule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import com.example.lipi.ui.SettingsScreen
import com.example.lipi.ui.LipiViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Lipi (লিপি)", appName)
    }

    @Test
    fun `test repository initialization and default settings`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = LipiRepository(context)
        assertTrue(repository.isFloatingServiceEnabled.value)
        val wordbook = repository.customWordbook.value
        assertNotNull(wordbook)
        assertTrue(wordbook.isNotEmpty())
    }

    @Test
    fun `test bulk import from text lines and export`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = LipiRepository(context)
        repository.clearCustomWordbook()

        val rawData = """
            # This is a sample custom dictionary
            dhonnobad=ধন্যবাদ
            shagotom,স্বাগতম
            apni kemon achen:আপনি কেমন আছেন
            // Another comment
            meeting=মিটিং
        """.trimIndent()

        val importResult = repository.bulkImport(rawData, overwrite = true)
        assertEquals(4, importResult.addedCount)
        assertEquals(4, repository.customWordbook.value.size)

        // Verify content
        val exportedText = repository.exportCustomDataAsText()
        assertTrue(exportedText.contains("dhonnobad=ধন্যবাদ"))
        assertTrue(exportedText.contains("shagotom=স্বাগতম"))
        assertTrue(exportedText.contains("apni kemon achen=আপনি কেমন আছেন"))
        assertTrue(exportedText.contains("meeting=মিটিং"))

        // Verify JSON export
        val exportedJson = repository.exportCustomDataAsJson()
        assertTrue(exportedJson.contains("dhonnobad"))
        assertTrue(exportedJson.contains("ধন্যবাদ"))
    }

    @Test
    fun `test bulk import json format`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = LipiRepository(context)

        val jsonInput = """
            [
              {"banglish": "pera nai", "bengali": "প্যারা নাই"},
              {"banglish": "chill koro", "bengali": "চিল করো"}
            ]
        """.trimIndent()

        val result = repository.bulkImport(jsonInput, overwrite = true)
        assertEquals(2, result.addedCount)
        assertEquals(2, repository.customWordbook.value.size)
    }

    @Test
    fun `test single entry addition and removal`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = LipiRepository(context)

        repository.addWordbookEntry("valo theko", "ভালো থেকো", "শুভেচ্ছা")
        val found = repository.customWordbook.value.find { it.banglish == "valo theko" }
        assertNotNull(found)
        assertEquals("ভালো থেকো", found?.bengali)

        repository.removeWordbookEntry(found!!.id)
        val removed = repository.customWordbook.value.find { it.banglish == "valo theko" }
        assertTrue(removed == null)
    }

    @Test
    fun `test settings screen renders without crashing`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = LipiRepository(context)
        val viewModel = LipiViewModel(repository)

        composeTestRule.setContent {
            SettingsScreen(viewModel = viewModel)
        }

        // Verify root screen displays all 4 unexpanded group cards
        composeTestRule.onNodeWithTag("settings_group_floating_notification").assertExists()
        composeTestRule.onNodeWithTag("settings_group_engine_formatting").assertExists()
        composeTestRule.onNodeWithTag("settings_group_wordbook_data").assertExists()
        composeTestRule.onNodeWithTag("settings_group_system_battery").assertExists()

        // Click into Group 1 (Floating & Notification)
        composeTestRule.onNodeWithTag("settings_group_floating_notification").performClick()
        composeTestRule.onNodeWithTag("floating_master_switch").assertExists()

        // Navigate back
        composeTestRule.onNodeWithTag("settings_back_button").performClick()
        composeTestRule.onNodeWithTag("settings_group_wordbook_data").assertExists()

        // Click into Group 3 (Wordbook Data)
        composeTestRule.onNodeWithTag("settings_group_wordbook_data").performClick()
        composeTestRule.onNodeWithTag("single_data_entry_card").assertExists()
        composeTestRule.onNodeWithTag("single_data_entry_banglish_input").assertExists()
        composeTestRule.onNodeWithTag("save_single_data_entry_button").assertExists()
    }

    @Test
    fun `test clipboard transliteration auto detection`() {
        val clipBanglish = "kemon acho"
        val hasBanglish = com.example.lipi.engine.MixedScriptTokenizer.hasBanglishContent(clipBanglish)
        assertTrue(hasBanglish)

        val conversion = com.example.lipi.engine.MixedScriptTokenizer.process(clipBanglish)
        assertTrue(conversion.convertedText.contains("কেমন আছো"))
    }

    @Test
    fun `test gemini model selection and persistence`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = LipiRepository(context)
        val viewModel = LipiViewModel(repository)

        // Default model should be gemini-2.5-flash
        assertEquals("gemini-2.5-flash", repository.geminiModel.value)
        assertEquals("gemini-2.5-flash", viewModel.geminiModel.value)

        // Update to gemini-2.5-pro
        viewModel.setGeminiModel("gemini-2.5-pro")
        assertEquals("gemini-2.5-pro", repository.geminiModel.value)
        assertEquals("gemini-2.5-pro", viewModel.geminiModel.value)

        // Update to custom model
        viewModel.setGeminiModel("gemini-2.0-flash-lite")
        assertEquals("gemini-2.0-flash-lite", repository.geminiModel.value)
        assertEquals("gemini-2.0-flash-lite", viewModel.geminiModel.value)

        // Reset with empty should fallback safely to default
        viewModel.setGeminiModel("")
        assertEquals("gemini-2.5-flash", repository.geminiModel.value)

        // Verify available models contains latest models
        val availableModels = com.example.lipi.engine.LipiOnlineEngine.AVAILABLE_GEMINI_MODELS
        assertTrue(availableModels.any { it.modelId == "gemini-2.5-flash" })
        assertTrue(availableModels.any { it.modelId == "gemini-2.5-pro" })
        assertTrue(availableModels.any { it.modelId == "gemini-2.0-flash" })
    }
}
