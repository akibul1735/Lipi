package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.lipi.data.LipiRepository
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
        val targetApps = repository.targetApps.value
        assertTrue(targetApps.any { it.appName == "WhatsApp" && it.isEnabled })
        assertTrue(targetApps.any { it.appName == "Messenger" && it.isEnabled })
        assertTrue(targetApps.any { it.appName == "Telegram" && it.isEnabled })
    }
}
