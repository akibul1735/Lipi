package com.example

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.lipi.data.LipiRepository
import com.example.lipi.service.LipiOverlayService
import com.example.lipi.ui.LipiMainScreen
import com.example.lipi.ui.LipiViewModel
import com.example.ui.theme.LipiTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: LipiViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = LipiRepository(applicationContext)
        viewModel = LipiViewModel(repository)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // Safely catch any unexpected global exceptions to prevent force closes
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("LipiCrashGuard", "Caught uncaught exception in thread: ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        setContent {
            LipiTheme {
                LipiMainScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) {
            viewModel.checkSystemPermissions(this)
            val canOverlay = Settings.canDrawOverlays(this)
            val isFloating = viewModel.isFloatingServiceEnabled.value
            val isNotification = viewModel.isPersistentNotificationEnabled.value
            if (canOverlay && (isFloating || isNotification)) {
                try {
                    val intent = Intent(this, LipiOverlayService::class.java).apply {
                        action = if (isFloating) LipiOverlayService.ACTION_START_ASSISTANT else LipiOverlayService.ACTION_UPDATE_NOTIFICATION
                    }
                    androidx.core.content.ContextCompat.startForegroundService(this, intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
