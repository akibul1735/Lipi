package com.example.lipi.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.lipi.data.LipiRepository
import com.example.lipi.engine.MixedScriptTokenizer

/**
 * Native Android Accessibility Service for Lipi.
 * Inspects chat messages in selected applications (WhatsApp, Messenger, Telegram, etc.),
 * detects readable Banglish text, and notifies LipiOverlayService to present the floating badge.
 */
class LipiAccessibilityService : AccessibilityService() {

    private val tag = "LipiAccessibility"
    private lateinit var repository: LipiRepository

    override fun onCreate() {
        super.onCreate()
        repository = LipiRepository(applicationContext)
        Log.i(tag, "LipiAccessibilityService created")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Check if master floating toggle is enabled
        if (!repository.isFloatingServiceEnabled.value) return

        val packageName = event.packageName?.toString() ?: return
        val enabledApps = repository.targetApps.value
        val isTargetApp = enabledApps.any { it.isEnabled && it.packageName == packageName }

        if (!isTargetApp) return

        // Process event types of interest
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
        ) {
            val rootNode = rootInActiveWindow ?: return
            try {
                findBanglishNodes(rootNode)
            } finally {
                // Node resources recycled by Android accessibility manager
            }
        }
    }

    private fun findBanglishNodes(node: AccessibilityNodeInfo?) {
        if (node == null) return

        val text = node.text?.toString()
        if (!text.isNullOrBlank() && text.length > 3) {
            if (MixedScriptTokenizer.hasBanglishContent(text)) {
                val rect = Rect()
                node.getBoundsInScreen(rect)

                // Only show overlay if the node is visible on screen
                if (rect.width() > 0 && rect.height() > 0 && rect.top >= 0) {
                    showFloatingBadgeForText(text, rect)
                }
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            findBanglishNodes(child)
        }
    }

    private fun showFloatingBadgeForText(detectedText: String, bounds: Rect) {
        val intent = Intent(this, LipiOverlayService::class.java).apply {
            action = LipiOverlayService.ACTION_SHOW_BADGE
            putExtra(LipiOverlayService.EXTRA_DETECTED_TEXT, detectedText)
            putExtra(LipiOverlayService.EXTRA_BOUNDS_X, bounds.right)
            putExtra(LipiOverlayService.EXTRA_BOUNDS_Y, bounds.top)
        }
        try {
            startService(intent)
        } catch (e: Exception) {
            Log.e(tag, "Failed to send overlay trigger: ${e.message}")
        }
    }

    override fun onInterrupt() {
        Log.w(tag, "LipiAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(tag, "LipiAccessibilityService destroyed")
    }
}
