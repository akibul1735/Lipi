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
 * Watches the enabled messaging apps and asks LipiOverlayService to show the badge
 * for the newest (lowest on screen) Banglish message.
 *
 * Fixes over the previous version:
 *  - Settings are re-read on every (throttled) event; the app and this service use separate
 *    LipiRepository instances, so toggles made in the app were never seen by the service.
 *  - Events are throttled and only ONE badge request is sent per pass (previously one
 *    startService call per matching node, which flooded the overlay service).
 *  - Text the user is typing (editable fields) and Lipi's own windows are ignored.
 *  - Tree traversal has a depth limit.
 */
class LipiAccessibilityService : AccessibilityService() {

    private val tag = "LipiAccessibility"
    private var lastProcessedAt = 0L
    private var lastSentText: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(tag, "LipiAccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName == applicationContext.packageName) return

        val type = event.eventType
        if (type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            type != AccessibilityEvent.TYPE_VIEW_SCROLLED
        ) return

        // Throttle: chat apps fire many content-change events per second
        val now = System.currentTimeMillis()
        if (now - lastProcessedAt < 600) return
        lastProcessedAt = now

        // Fresh read so changes made in the Lipi app apply immediately
        val repository = LipiRepository(applicationContext)
        if (!repository.isFloatingServiceEnabled.value) return
        val isTargetApp = repository.targetApps.value.any { it.isEnabled && it.packageName == packageName }
        if (!isTargetApp) return

        val root = rootInActiveWindow ?: return
        var bestText: String? = null
        var bestRect: Rect? = null

        fun visit(node: AccessibilityNodeInfo?, depth: Int) {
            if (node == null || depth > 40) return
            val text = node.text?.toString()
            if (!node.isEditable && !text.isNullOrBlank() && text.length > 3 &&
                MixedScriptTokenizer.hasBanglishContent(text)
            ) {
                val rect = Rect()
                node.getBoundsInScreen(rect)
                if (rect.width() > 0 && rect.height() > 0 && rect.top >= 0) {
                    // keep the lowest message on screen (the newest one)
                    if (bestRect == null || rect.top >= bestRect!!.top) {
                        bestText = text
                        bestRect = Rect(rect)
                    }
                }
            }
            for (i in 0 until node.childCount) visit(node.getChild(i), depth + 1)
        }

        try {
            visit(root, 0)
        } catch (e: Exception) {
            Log.e(tag, "Tree scan failed: ${e.message}")
            return
        }

        val text = bestText ?: return
        val rect = bestRect ?: return
        if (text == lastSentText) return
        lastSentText = text
        showFloatingBadgeForText(text, rect)
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
}
