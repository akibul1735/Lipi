package com.example.lipi.service

import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.TypedValue
import android.animation.ValueAnimator
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.lipi.engine.MixedScriptTokenizer
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import com.example.lipi.engine.LipiOnlineEngine
import com.example.lipi.data.OnlineEngineMode
import com.example.lipi.data.LipiMode

/**
 * Native Android Floating Overlay Assistant Service.
 * Provides:
 * 1. Persistent draggable floating badge ('ল') over WhatsApp, Messenger, etc.
 * 2. Automated Clipboard Monitoring: Instantly detects copied Banglish, shows quick floating translation preview pill.
 * 3. Interactive translation popup with reliable Paste and Copy functionality.
 * 4. Persistent Notification Bar for 1-tap fastest opening and translation actions anywhere.
 */
class LipiOverlayService : Service() {

    companion object {
        const val ACTION_START_ASSISTANT = "com.example.lipi.action.START_ASSISTANT"
        const val ACTION_SHOW_BADGE = "com.example.lipi.action.SHOW_BADGE"
        const val ACTION_DISMISS = "com.example.lipi.action.DISMISS"
        const val ACTION_KILL_SERVICE = "com.example.lipi.action.KILL_SERVICE"
        const val ACTION_OPEN_POPUP = "com.example.lipi.action.OPEN_POPUP"
        const val ACTION_TOGGLE_BADGE = "com.example.lipi.action.TOGGLE_BADGE"
        const val ACTION_UPDATE_NOTIFICATION = "com.example.lipi.action.UPDATE_NOTIFICATION"
        const val EXTRA_DETECTED_TEXT = "extra_detected_text"
        const val EXTRA_BOUNDS_X = "extra_bounds_x"
        const val EXTRA_BOUNDS_Y = "extra_bounds_y"

        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "lipi_persistent_quick_bar"
    }

    private var windowManager: WindowManager? = null
    private var floatingBadgeView: View? = null
    private var translationPopupView: View? = null
    private var quickPreviewView: View? = null
    private var quickPreviewDismissRunnable: Runnable? = null
    private var dismissTargetView: View? = null
    private var isDismissTargetShowing: Boolean = false
    private var isBallHoveringDismiss: Boolean = false

    private var activeInputField: EditText? = null
    private var activeTranslationText: TextView? = null

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("LipiOverlayService", "Handled uncaught coroutine exception", throwable)
    }
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob() + coroutineExceptionHandler)
    private var currentTransliterationJob: Job? = null
    private var isServiceKilled: Boolean = false
    private val repository by lazy { com.example.lipi.data.LipiRepository(this) }

    private fun safeAddView(view: View?, params: WindowManager.LayoutParams): Boolean {
        if (view == null) return false
        if (!Settings.canDrawOverlays(this)) return false
        return try {
            if (!view.isAttachedToWindow && view.parent == null) {
                windowManager?.addView(view, params)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("LipiOverlayService", "Error adding overlay view: ${e.message}")
            false
        }
    }

    private fun safeRemoveView(view: View?) {
        if (view == null) return
        try {
            if (view.isAttachedToWindow || view.parent != null) {
                windowManager?.removeViewImmediate(view)
            }
        } catch (e: Exception) {
            try {
                windowManager?.removeView(view)
            } catch (_: Exception) {}
        }
    }

    private fun safeUpdateViewLayout(view: View?, params: WindowManager.LayoutParams) {
        if (view == null) return
        try {
            if (view.isAttachedToWindow || view.parent != null) {
                windowManager?.updateViewLayout(view, params)
            }
        } catch (e: Exception) {
            Log.e("LipiOverlayService", "Error updating view layout: ${e.message}")
        }
    }

    private fun safeToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        mainHandler.post {
            try {
                Toast.makeText(applicationContext, message, duration).show()
            } catch (e: Exception) {
                Log.w("LipiOverlayService", "Failed to show toast: ${e.message}")
            }
        }
    }

    private var lastAppLogicResult: String = ""
    private var lastGeminiResult: String = ""
    private var currentDisplaySource: String = "logic" // "gemini" or "logic"

    private var outputStatusLabelView: TextView? = null
    private var aiFeedbackLabelView: TextView? = null
    private var aiSwitchBtnView: TextView? = null

    private var liveTypingRunnable: Runnable? = null
    private var debouncedAiRunnable: Runnable? = null

    private var currentText: String = ""
    private var convertedBengali: String = ""
    private var lastObservedClipText: String = ""
    private var isReverseMode: Boolean = false

    private var clipboardManager: ClipboardManager? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var lastBadgeX = -1
    private var lastBadgeY = 300

    private val clipboardDebounceRunnable = Runnable {
        checkClipboardForBanglish()
    }

    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        mainHandler.removeCallbacks(clipboardDebounceRunnable)
        mainHandler.postDelayed(clipboardDebounceRunnable, 150)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        try {
            clipboardManager?.addPrimaryClipChangedListener(clipboardListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            // Satisfy Android 8+ foreground service requirements immediately if notification is enabled
            val prefs = getSharedPreferences("lipi_preferences", Context.MODE_PRIVATE)
            val isNotificationEnabled = prefs.getBoolean("persistent_notification_enabled", true)
            if (isNotificationEnabled && intent?.action != ACTION_DISMISS && intent?.action != ACTION_KILL_SERVICE) {
                updatePersistentNotification()
            }

            if (!Settings.canDrawOverlays(this)) {
                dismissAllOverlays()
                if (intent?.action == ACTION_DISMISS || intent?.action == ACTION_KILL_SERVICE) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                if (!isNotificationEnabled) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                return START_STICKY
            }

            when (intent?.action) {
                ACTION_KILL_SERVICE, ACTION_DISMISS -> {
                    isServiceKilled = true
                    dismissAllOverlays()
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            stopForeground(STOP_FOREGROUND_REMOVE)
                        } else {
                            @Suppress("DEPRECATION")
                            stopForeground(true)
                        }
                    } catch (_: Exception) {}
                    cancelPersistentNotification()
                    safeToast("লিপি ব্যাকগ্রাউন্ড সার্ভিস ও নোটিফিকেশন বন্ধ করা হয়েছে")
                    stopSelf()
                    return START_NOT_STICKY
                }
            ACTION_OPEN_POPUP -> {
                val displayMetrics = resources.displayMetrics
                val centerX = displayMetrics.widthPixels / 2
                val popupY = (displayMetrics.heightPixels * 0.16f).toInt().coerceAtLeast(100)
                if (translationPopupView != null) {
                    dismissTranslationPopup()
                }
                try {
                    val clipText = clipboardManager?.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()
                    if (!clipText.isNullOrBlank()) {
                        currentText = clipText
                        val textSample = if (clipText.length > 800) clipText.take(800) else clipText
                        val hasBengali = textSample.any { it in '\u0980'..'\u09FF' }
                        val hasBanglish = MixedScriptTokenizer.hasBanglishContent(textSample)
                        if (hasBengali && !hasBanglish) {
                            isReverseMode = true
                        } else if (hasBanglish && !hasBengali) {
                            isReverseMode = false
                        }
                        convertedBengali = processText(clipText)
                        lastAppLogicResult = convertedBengali
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                showTranslationPopup(centerX, popupY, autoPasteFromClipboard = true)
                updatePersistentNotification()
            }
            ACTION_TOGGLE_BADGE -> {
                if (floatingBadgeView != null && floatingBadgeView?.visibility == View.VISIBLE) {
                    floatingBadgeView?.visibility = View.GONE
                    safeToast("ভাসমান বল লুকানো হয়েছে")
                } else {
                    ensureFloatingBadgeVisible()
                    safeToast("ভাসমান বল দৃশ্যমান হয়েছে")
                }
                updatePersistentNotification()
            }
            ACTION_UPDATE_NOTIFICATION -> {
                updatePersistentNotification()
            }
            ACTION_SHOW_BADGE -> {
                val text = intent.getStringExtra(EXTRA_DETECTED_TEXT)
                if (!text.isNullOrBlank()) {
                    val x = intent.getIntExtra(EXTRA_BOUNDS_X, 300)
                    val y = intent.getIntExtra(EXTRA_BOUNDS_Y, 400)
                    handleDetectedText(text, x, y)
                } else {
                    ensureFloatingBadgeVisible()
                }
                updatePersistentNotification()
            }
            ACTION_START_ASSISTANT -> {
                ensureFloatingBadgeVisible()
                updatePersistentNotification()
            }
            else -> {
                ensureFloatingBadgeVisible()
                updatePersistentNotification()
            }
        }
        return START_STICKY
    } catch (t: Throwable) {
        t.printStackTrace()
        return START_NOT_STICKY
    }
}

    private fun getTransliterationStyle(): com.example.lipi.data.TransliterationStyle {
        val prefs = getSharedPreferences("lipi_preferences", Context.MODE_PRIVATE)
        val styleName = prefs.getString("transliteration_style", com.example.lipi.data.TransliterationStyle.ALL_BENGALI.name)
        return try {
            com.example.lipi.data.TransliterationStyle.valueOf(styleName ?: com.example.lipi.data.TransliterationStyle.ALL_BENGALI.name)
        } catch (e: Exception) {
            com.example.lipi.data.TransliterationStyle.ALL_BENGALI
        }
    }

    /**
     * Automated clipboard monitoring: Triggered whenever user copies any text on Android.
     * Optimized to avoid CPU thrashing and unbounded memory allocations.
     */
    private fun checkClipboardForBanglish() {
        if (translationPopupView != null) return
        val prefs = getSharedPreferences("lipi_preferences", Context.MODE_PRIVATE)
        val isMonitoringEnabled = prefs.getBoolean("auto_clipboard_monitoring", true)
        if (!isMonitoringEnabled) return

        try {
            val clip = clipboardManager?.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString() ?: return
            // Guard: Immediately skip empty or unchanged clipboard without doing heavy checks
            if (clip.isBlank() || clip == lastObservedClipText) return

            // Cap sample length for ultra-fast, RAM-friendly detection on very long texts
            val textSample = if (clip.length > 800) clip.take(800) else clip
            val hasBengali = textSample.any { it in '\u0980'..'\u09FF' }
            val hasBanglish = MixedScriptTokenizer.hasBanglishContent(textSample)

            if (!hasBanglish && !hasBengali) return

            lastObservedClipText = clip
            currentText = clip
            val convertDigits = prefs.getBoolean("convert_digits", false)
            val isReverse = hasBengali && !hasBanglish
            isReverseMode = isReverse
            val mode = if (isReverse) com.example.lipi.data.LipiMode.BENGALI_TO_BANGLISH else com.example.lipi.data.LipiMode.SMART_AUTO
            val conversion = MixedScriptTokenizer.process(
                inputText = textSample,
                mode = mode,
                convertDigitsToBengali = convertDigits,
                transliterationStyle = getTransliterationStyle()
            )
            convertedBengali = conversion.convertedText

            mainHandler.post {
                // Pulse the floating badge
                floatingBadgeView?.animate()
                    ?.scaleX(1.18f)?.scaleY(1.18f)
                    ?.setDuration(120)
                    ?.withEndAction {
                        floatingBadgeView?.animate()?.scaleX(1f)?.scaleY(1f)?.setDuration(120)?.start()
                    }?.start()

                // Update active open popup if user is inside it
                activeInputField?.setText(currentText)
                activeTranslationText?.text = convertedBengali

                // Show sleek quick preview floating card
                val prefix = if (isReverse) "বাংলিশ: " else "বাংলা: "
                showQuickPreviewCard(prefix + convertedBengali)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleDetectedText(text: String, posX: Int, posY: Int) {
        if (!Settings.canDrawOverlays(this)) return

        currentText = text
        val conversion = MixedScriptTokenizer.process(text)
        convertedBengali = conversion.convertedText

        lastBadgeX = posX
        lastBadgeY = posY
        ensureFloatingBadgeVisible()
    }

    private fun ensureFloatingBadgeVisible() {
        if (!Settings.canDrawOverlays(this)) return
        if (floatingBadgeView != null) {
            floatingBadgeView?.visibility = View.VISIBLE
            return
        }

        val displayMetrics = resources.displayMetrics
        val dpSize = (54 * displayMetrics.density).toInt()

        // Default to right edge of screen
        if (lastBadgeX < 0) {
            lastBadgeX = displayMetrics.widthPixels - dpSize - 24
        }

        val layoutParams = WindowManager.LayoutParams(
            dpSize,
            dpSize,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = lastBadgeX.coerceIn(16, displayMetrics.widthPixels - dpSize - 16)
            y = lastBadgeY.coerceIn(100, displayMetrics.heightPixels - dpSize - 100)
        }

        val badge = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                colors = intArrayOf(0xFF07232B.toInt(), 0xFF0F5161.toInt())
                gradientType = GradientDrawable.LINEAR_GRADIENT
                setStroke((2.5f * displayMetrics.density).toInt(), 0xFF57DFCE.toInt()) // Neon Cyan Glow
            }
            elevation = 24f

            // Inner Center Content: A⇄অ & LIPI
            val centerContent = LinearLayout(this@LipiOverlayService).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                val title = TextView(this@LipiOverlayService).apply {
                    text = "A⇄অ"
                    textSize = 12.5f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                }
                val subtitle = TextView(this@LipiOverlayService).apply {
                    text = "LIPI"
                    textSize = 8f
                    setTextColor(0xFF57DFCE.toInt())
                    gravity = Gravity.CENTER
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
                addView(title)
                addView(subtitle)
            }

            val centerParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
            addView(centerContent, centerParams)

            // Active Listening Green Indicator Dot
            val dot = View(this@LipiOverlayService).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(0xFF00E676.toInt()) // Vivid Emerald Active Indicator
                    setStroke(1, Color.BLACK)
                }
            }
            val dotSize = (9 * displayMetrics.density).toInt()
            val dotParams = FrameLayout.LayoutParams(dotSize, dotSize).apply {
                gravity = Gravity.TOP or Gravity.END
                topMargin = (5 * displayMetrics.density).toInt()
                rightMargin = (5 * displayMetrics.density).toInt()
            }
            addView(dot, dotParams)
        }

        // Draggable touch listener with bottom dismiss target on hold / drag
        badge.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isClick = false
            private val holdHandler = Handler(Looper.getMainLooper())
            private val showTargetRunnable = Runnable {
                showDismissTarget()
            }

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (event == null) return false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isClick = true

                        // If user holds the ball (250ms), show the bottom close icon
                        holdHandler.postDelayed(showTargetRunnable, 250)
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            if (isClick) {
                                isClick = false
                                holdHandler.removeCallbacks(showTargetRunnable)
                                showDismissTarget()
                            }
                        }

                        layoutParams.x = initialX + dx
                        layoutParams.y = initialY + dy

                        // Calculate collision with bottom dismiss target
                        val screenWidth = displayMetrics.widthPixels
                        val screenHeight = displayMetrics.heightPixels
                        val density = displayMetrics.density
                        val targetCenterX = screenWidth / 2f
                        val targetCenterY = screenHeight - (60 * density)
                        val triggerRadius = (70 * density)

                        val badgeCenterX = layoutParams.x + dpSize / 2f
                        val badgeCenterY = layoutParams.y + dpSize / 2f
                        val distance = Math.hypot(
                            (badgeCenterX - targetCenterX).toDouble(),
                            (badgeCenterY - targetCenterY).toDouble()
                        )

                        val isHovering = distance <= triggerRadius
                        updateDismissTargetHover(isHovering, badge)

                        if (isHovering) {
                            // Magnetically capture badge into close target center
                            layoutParams.x = (targetCenterX - dpSize / 2).toInt()
                            layoutParams.y = (targetCenterY - dpSize / 2 - (12 * density)).toInt()
                        }

                        safeUpdateViewLayout(badge, layoutParams)
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        holdHandler.removeCallbacks(showTargetRunnable)

                        if (event.action == MotionEvent.ACTION_UP && isBallHoveringDismiss) {
                            // Captured in close zone: Animate ball scaling down to 0 and dismiss!
                            badge.animate()
                                .scaleX(0f)
                                .scaleY(0f)
                                .alpha(0f)
                                .setDuration(160)
                                .withEndAction {
                                    dismissFloatingBadge()
                                }
                                .start()
                            hideDismissTarget()
                            return true
                        }

                        hideDismissTarget()
                        badge.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(100).start()

                        if (isClick && event.action == MotionEvent.ACTION_UP) {
                            lastBadgeX = layoutParams.x
                            lastBadgeY = layoutParams.y
                            dismissQuickPreview()
                            onBadgeClicked(layoutParams.x, layoutParams.y)
                        } else if (event.action == MotionEvent.ACTION_UP) {
                            val screenWidth = displayMetrics.widthPixels
                            val targetX = if (layoutParams.x + dpSize / 2 < screenWidth / 2) {
                                16
                            } else {
                                screenWidth - dpSize - 16
                            }
                            lastBadgeX = targetX
                            lastBadgeY = layoutParams.y.coerceIn(100, displayMetrics.heightPixels - dpSize - 100)

                            // Smooth animated snap to screen edge
                            val animator = ValueAnimator.ofInt(layoutParams.x, targetX).apply {
                                duration = 180
                                interpolator = DecelerateInterpolator()
                                addUpdateListener { va ->
                                    layoutParams.x = va.animatedValue as Int
                                    safeUpdateViewLayout(badge, layoutParams)
                                }
                            }
                            animator.start()
                        }
                        return true
                    }
                }
                return false
            }
        })

        floatingBadgeView = badge
        if (safeAddView(floatingBadgeView, layoutParams)) {
            safeToast("লিপি 'A ⇄ অ' অনুবাদক বল স্ক্রিনে দৃশ্যমান হয়েছে!")
        }
    }

    /**
     * Shows a bottom dismiss circle with close '✕' icon when the floating ball is selected/held or dragged.
     */
    private fun showDismissTarget() {
        if (dismissTargetView != null || !Settings.canDrawOverlays(this)) return

        val displayMetrics = resources.displayMetrics
        val density = displayMetrics.density
        val targetHeight = (130 * density).toInt()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            targetHeight,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 0
        }

        val container = FrameLayout(this).apply {
            clipChildren = false
            clipToPadding = false
        }

        // Bottom subtle dark vignette
        val gradientBackdrop = View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.BOTTOM_TOP,
                intArrayOf(0xBB000000.toInt(), 0x00000000)
            )
        }
        container.addView(gradientBackdrop, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        // Center close icon circle & label
        val centerGroup = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            clipChildren = false
            clipToPadding = false
        }

        val circleSize = (62 * density).toInt()
        val circleView = FrameLayout(this).apply {
            tag = "dismiss_circle"
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xDD1E1E1E.toInt())
                setStroke((2f * density).toInt(), 0xFFEF5350.toInt())
            }
            elevation = 16f * density

            val crossIcon = TextView(this@LipiOverlayService).apply {
                text = "✕"
                textSize = 22f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
            }
            addView(crossIcon, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER })
        }

        val labelView = TextView(this).apply {
            tag = "dismiss_label"
            text = "বন্ধ করতে এখানে আনুন"
            textSize = 11.5f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setShadowLayer(4f, 0f, 2f, 0xAA000000.toInt())
            setPadding(0, (6 * density).toInt(), 0, 0)
        }

        centerGroup.addView(circleView, LinearLayout.LayoutParams(circleSize, circleSize).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        })
        centerGroup.addView(labelView)

        val groupParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = (18 * density).toInt()
        }
        container.addView(centerGroup, groupParams)

        container.alpha = 0f
        container.translationY = 50 * density

        dismissTargetView = container
        isDismissTargetShowing = true
        isBallHoveringDismiss = false

        if (safeAddView(container, params)) {
            container.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(180)
                .setInterpolator(DecelerateInterpolator())
                .start()
        } else {
            dismissTargetView = null
            isDismissTargetShowing = false
        }
    }

    private fun updateDismissTargetHover(isHovering: Boolean, badge: View) {
        if (isBallHoveringDismiss == isHovering) return
        isBallHoveringDismiss = isHovering

        val container = dismissTargetView ?: return
        val circleView = container.findViewWithTag<FrameLayout>("dismiss_circle") ?: return
        val labelView = container.findViewWithTag<TextView>("dismiss_label") ?: return
        val density = resources.displayMetrics.density

        if (isHovering) {
            circleView.animate()
                .scaleX(1.22f)
                .scaleY(1.22f)
                .setDuration(120)
                .setInterpolator(OvershootInterpolator(1.4f))
                .start()

            (circleView.background as? GradientDrawable)?.apply {
                setColor(0xFFD32F2F.toInt())
                setStroke((3f * density).toInt(), Color.WHITE)
            }
            labelView.text = "ছেড়ে দিলে বন্ধ হবে"
            labelView.setTextColor(0xFFFFCDD2.toInt())

            badge.animate()
                .scaleX(0.78f)
                .scaleY(0.78f)
                .alpha(0.85f)
                .setDuration(100)
                .start()

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    vibrator?.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    badge.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }
            } catch (_: Exception) {}
        } else {
            circleView.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(120)
                .setInterpolator(DecelerateInterpolator())
                .start()

            (circleView.background as? GradientDrawable)?.apply {
                setColor(0xDD1E1E1E.toInt())
                setStroke((2f * density).toInt(), 0xFFEF5350.toInt())
            }
            labelView.text = "বন্ধ করতে এখানে আনুন"
            labelView.setTextColor(Color.WHITE)

            badge.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(100)
                .start()
        }
    }

    private fun hideDismissTarget(onEnd: (() -> Unit)? = null) {
        val target = dismissTargetView
        dismissTargetView = null
        isDismissTargetShowing = false
        isBallHoveringDismiss = false

        if (target != null) {
            val density = resources.displayMetrics.density
            target.animate()
                .alpha(0f)
                .translationY(40 * density)
                .setDuration(150)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    safeRemoveView(target)
                    onEnd?.invoke()
                }
                .start()
        } else {
            onEnd?.invoke()
        }
    }

    private fun dismissFloatingBadge() {
        dismissQuickPreview()
        dismissTranslationPopup()
        hideDismissTarget()

        floatingBadgeView?.let {
            safeRemoveView(it)
            floatingBadgeView = null
        }

        val prefs = getSharedPreferences("lipi_preferences", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("floating_enabled", false).apply()
        updatePersistentNotification()
        safeToast("ভাসমান অনুবাদক বল বন্ধ করা হয়েছে")
    }

    /**
     * Shows a lightweight floating preview pill right next to the badge when clipboard is copied.
     */
    private fun showQuickPreviewCard(bengaliText: String) {
        if (!Settings.canDrawOverlays(this)) return
        if (translationPopupView != null) return
        dismissQuickPreview()

        val displayMetrics = resources.displayMetrics
        // Enlarged floating window width: 345dp
        val targetWidthDp = 345
        val width = (targetWidthDp * displayMetrics.density).toInt().coerceAtMost((displayMetrics.widthPixels * 0.95f).toInt())

        val layoutParams = WindowManager.LayoutParams(
            width,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            val posX = if (lastBadgeX > displayMetrics.widthPixels / 2) {
                lastBadgeX - width - 16
            } else {
                lastBadgeX + (58 * displayMetrics.density).toInt()
            }
            x = posX.coerceIn(16, displayMetrics.widthPixels - width - 16)
            y = lastBadgeY.coerceIn(80, displayMetrics.heightPixels - 250)
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 22, 30, 22)
            background = GradientDrawable().apply {
                cornerRadius = 24f
                setColor(0xFF0F1B20.toInt())
                setStroke((2 * displayMetrics.density).toInt(), 0xFF57DFCE.toInt())
            }
            elevation = 28f
            // Dismiss when touched outside
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_OUTSIDE) {
                    dismissQuickPreview()
                    true
                } else false
            }
        }

        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(this).apply {
            text = "📋 স্বয়ংক্রিয় বাংলা রূপান্তর:"
            setTextColor(0xFF57DFCE.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
            setLayoutParams(LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }

        val closeBtn = TextView(this).apply {
            text = "✕"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setPadding(8, 0, 8, 8)
            setOnClickListener { dismissQuickPreview() }
        }

        titleRow.addView(title)
        titleRow.addView(closeBtn)

        val preview = TextView(this).apply {
            text = bengaliText
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.5f)
            maxLines = 3
            ellipsize = TextUtils.TruncateAt.END
            setPadding(0, 10, 0, 14)
        }

        val copyActionBtn = Button(this).apply {
            text = "কপি করুন"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
            background = GradientDrawable().apply {
                cornerRadius = 16f
                setColor(0xFFD86B43.toInt()) // Terracotta
            }
            setOnClickListener {
                try {
                    lastObservedClipText = bengaliText
                    val clip = ClipData.newPlainText("Bengali Translation", bengaliText)
                    clipboardManager?.setPrimaryClip(clip)
                    safeToast("✓ বাংলা অনুবাদ ক্লিপবোর্ডে কপি হয়েছে!")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                dismissQuickPreview()
            }
        }

        card.addView(titleRow)
        card.addView(preview)
        card.addView(copyActionBtn)

        card.alpha = 0f
        card.scaleX = 0.85f
        card.scaleY = 0.85f

        quickPreviewView = card
        if (safeAddView(quickPreviewView, layoutParams)) {
            card.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(240)
                .setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
                .start()
        }

        // Auto dismiss after 6 seconds
        quickPreviewDismissRunnable = Runnable { dismissQuickPreview() }
        mainHandler.postDelayed(quickPreviewDismissRunnable!!, 6000)
    }

    private fun dismissQuickPreview() {
        quickPreviewDismissRunnable?.let { mainHandler.removeCallbacks(it) }
        quickPreviewDismissRunnable = null
        quickPreviewView?.let {
            safeRemoveView(it)
            quickPreviewView = null
        }
    }

    private fun getOnlineEngineMode(): OnlineEngineMode {
        val prefs = getSharedPreferences("lipi_preferences", Context.MODE_PRIVATE)
        val modeStr = prefs.getString("online_engine_mode", OnlineEngineMode.OFFLINE_ONLY.name)
        return try {
            OnlineEngineMode.valueOf(modeStr ?: OnlineEngineMode.OFFLINE_ONLY.name)
        } catch (_: Exception) {
            OnlineEngineMode.OFFLINE_ONLY
        }
    }

    private fun getGeminiApiKey(): String {
        val prefs = getSharedPreferences("lipi_preferences", Context.MODE_PRIVATE)
        return prefs.getString("custom_gemini_api_key", "") ?: ""
    }

    private fun getGeminiModel(): String {
        val prefs = getSharedPreferences("lipi_preferences", Context.MODE_PRIVATE)
        return prefs.getString("custom_gemini_model", "gemini-3.8-flash") ?: "gemini-3.8-flash"
    }

    private fun triggerGeminiTransliteration(
        inputText: String,
        force: Boolean = false,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        val trimmed = inputText.trim()
        if (trimmed.isBlank()) {
            outputStatusLabelView?.text = if (isReverseMode) "লিপি: বাংলিশ ফলাফল" else "লিপি: বাংলা ফলাফল"
            aiFeedbackLabelView?.visibility = View.GONE
            return
        }

        val engineMode = getOnlineEngineMode()
        val targetMode = if (isReverseMode) LipiMode.BENGALI_TO_BANGLISH else LipiMode.BANGLISH_TO_BENGALI
        val customKey = getGeminiApiKey()
        val model = getGeminiModel()
        val chain = repository.getGeminiModelsChain()
        val convertDigits = getSharedPreferences("lipi_preferences", Context.MODE_PRIVATE).getBoolean("convert_digits", false)
        val style = getTransliterationStyle()
        val customWordMap = repository.customWordbook.value.associate { it.banglish.lowercase() to it.bengali }

        val unknownWords = com.example.lipi.engine.MixedScriptTokenizer.identifyUnknownWords(trimmed, customWordMap, targetMode, style)

        // Case 1: If all words in the input are already in the wordbank and user didn't force load:
        if (!force && unknownWords.isEmpty()) {
            outputStatusLabelView?.text = "📱 সব শব্দ শব্দভাণ্ডারে বিদ্যমান"
            outputStatusLabelView?.setTextColor(0xFF81C784.toInt())
            aiFeedbackLabelView?.visibility = View.VISIBLE
            aiFeedbackLabelView?.text = "✓ শব্দভাণ্ডার থেকে তাৎক্ষণিক প্রদর্শিত (Gemini লোডের প্রয়োজন নেই)"
            aiFeedbackLabelView?.setTextColor(0xFF81C784.toInt())
            aiSwitchBtnView?.visibility = View.VISIBLE
            aiSwitchBtnView?.text = "Gemini লোড"
            onComplete?.invoke(true)
            return
        }

        // Case 2: If there are unknown words and Gemini is active, only load those unknown words from Gemini!
        val hasKey = customKey.isNotBlank() || (try { com.example.BuildConfig.GEMINI_API_KEY.isNotBlank() } catch (_: Exception) { false })
        if (!force && unknownWords.isNotEmpty() && hasKey && engineMode == OnlineEngineMode.GEMINI_AI) {
            currentTransliterationJob?.cancel()
            outputStatusLabelView?.text = "🔍 ${unknownWords.size}টি শব্দ শব্দভাণ্ডারে নেই"
            outputStatusLabelView?.setTextColor(0xFFFFD54F.toInt())
            aiFeedbackLabelView?.visibility = View.VISIBLE
            aiFeedbackLabelView?.text = "Gemini থেকে নতুন শব্দ লোড হচ্ছে (${unknownWords.joinToString(", ")})..."
            aiFeedbackLabelView?.setTextColor(0xFF8BA2A8.toInt())

            currentTransliterationJob = serviceScope.launch {
                try {
                    val res = com.example.lipi.engine.LipiOnlineEngine.fetchUnknownWordsFromGemini(
                        words = unknownWords,
                        contextSentence = trimmed,
                        customApiKey = customKey,
                        model = model,
                        modelsChain = chain
                    )
                    res.onSuccess { data ->
                        val learned = repository.autoAddLearnedWords(data.wordMappings)
                        val newBengali = processText(trimmed)
                        convertedBengali = newBengali
                        lastGeminiResult = newBengali
                        activeTranslationText?.text = newBengali
                        outputStatusLabelView?.text = "✨ Gemini নতুন শব্দ যুক্ত সম্পন্ন"
                        outputStatusLabelView?.setTextColor(0xFF57DFCE.toInt())
                        val learnedStr = learned.joinToString(", ") { "${it.first} → ${it.second}" }
                        aiFeedbackLabelView?.text = "✓ শব্দভাণ্ডারে অটো-যুক্ত: $learnedStr"
                        aiFeedbackLabelView?.setTextColor(0xFF81C784.toInt())
                        aiSwitchBtnView?.visibility = View.VISIBLE
                        aiSwitchBtnView?.text = "লজিক দেখুন"
                        onComplete?.invoke(true)
                    }.onFailure {
                        outputStatusLabelView?.text = "📱 অ্যাপ লজিক ফলাফল"
                        aiFeedbackLabelView?.text = "⚠️ Gemini লোড করা যায়নি (অফলাইন লজিক সক্রিয়)"
                        aiFeedbackLabelView?.setTextColor(0xFFFFB74D.toInt())
                        onComplete?.invoke(false)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return
        }

        if (!force && engineMode == OnlineEngineMode.OFFLINE_ONLY) {
            outputStatusLabelView?.text = "📱 অ্যাপ লজিক ফলাফল"
            aiFeedbackLabelView?.visibility = View.GONE
            return
        }

        currentTransliterationJob?.cancel()
        outputStatusLabelView?.text = "⏳ Gemini AI রূপান্তর আনা হচ্ছে..."
        outputStatusLabelView?.setTextColor(0xFFFFD54F.toInt())
        aiFeedbackLabelView?.visibility = View.VISIBLE
        aiFeedbackLabelView?.text = "Gemini ক্লাউড মডেল অনুসন্ধান করা হচ্ছে..."
        aiFeedbackLabelView?.setTextColor(0xFF8BA2A8.toInt())

        currentTransliterationJob = serviceScope.launch {
            try {
                val smartResult = LipiOnlineEngine.transliterateSmart(
                    inputText = trimmed,
                    mode = targetMode,
                    enginePreference = if (force) OnlineEngineMode.GEMINI_AI else engineMode,
                    customGeminiApiKey = customKey,
                    geminiModel = model,
                    geminiModelsChain = chain,
                    customWordbook = customWordMap,
                    convertDigitsToBengali = convertDigits,
                    transliterationStyle = style
                )

                if (activeTranslationText != null) {
                    if (smartResult.engineUsed == OnlineEngineMode.GEMINI_AI && !smartResult.isFallbackToOffline) {
                        lastGeminiResult = smartResult.convertedText
                        convertedBengali = smartResult.convertedText
                        currentDisplaySource = "gemini"
                        activeTranslationText?.text = smartResult.convertedText

                        // Auto-catch learned words from Gemini (e.g. 'clg' -> 'কলেজ') and save to wordbook
                        val learned = repository.learnWordMappingFromGemini(trimmed, smartResult.convertedText)

                        val actualModel = smartResult.modelUsed ?: model
                        val shortModel = actualModel.replace("gemini-", "")
                        outputStatusLabelView?.text = "✨ Gemini AI ($shortModel) ফলাফল"
                        outputStatusLabelView?.setTextColor(0xFF57DFCE.toInt())
                        aiFeedbackLabelView?.visibility = View.VISIBLE
                        if (learned.isNotEmpty()) {
                            val learnedStr = learned.joinToString(", ") { "${it.first} → ${it.second}" }
                            aiFeedbackLabelView?.text = "✓ Gemini রূপান্তর সম্পন্ন (নতুন শব্দ যুক্ত: $learnedStr)"
                        } else {
                            aiFeedbackLabelView?.text = "✓ Gemini AI দ্বারা কনটেক্সট-সচেতন শুদ্ধ রূপান্তর"
                        }
                        aiFeedbackLabelView?.setTextColor(0xFF81C784.toInt())
                        aiSwitchBtnView?.visibility = View.VISIBLE
                        aiSwitchBtnView?.text = "লজিক দেখুন"
                        onComplete?.invoke(true)
                    } else if (smartResult.isFallbackToOffline) {
                        lastAppLogicResult = smartResult.convertedText
                        convertedBengali = smartResult.convertedText
                        currentDisplaySource = "logic"
                        activeTranslationText?.text = smartResult.convertedText
                        outputStatusLabelView?.text = "📱 অ্যাপ লজিক ফলাফল"
                        outputStatusLabelView?.setTextColor(0xFF8BA2A8.toInt())
                        aiFeedbackLabelView?.visibility = View.VISIBLE
                        val msg = smartResult.feedbackMessage ?: "এআই সাড়া দেয়নি (অফলাইন লজিক দেখানো হচ্ছে)"
                        aiFeedbackLabelView?.text = "⚠️ $msg"
                        aiFeedbackLabelView?.setTextColor(0xFFFFB74D.toInt())
                        aiSwitchBtnView?.visibility = View.GONE
                        onComplete?.invoke(false)
                    } else {
                        convertedBengali = smartResult.convertedText
                        activeTranslationText?.text = smartResult.convertedText
                        outputStatusLabelView?.text = if (smartResult.engineUsed == OnlineEngineMode.ONLINE_CLOUD)
                            "☁️ Google ক্লাউড ফলাফল"
                        else
                            "📱 অ্যাপ লজিক ফলাফল"
                        outputStatusLabelView?.setTextColor(0xFF57DFCE.toInt())
                        aiFeedbackLabelView?.visibility = View.GONE
                        aiSwitchBtnView?.visibility = View.GONE
                        onComplete?.invoke(true)
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Silently handle cooperative cancellation (e.g., typing resumed, clear clicked, or popup closed)
                // NEVER display "StandaloneCoroutine was cancelled" as an error to the user!
            } catch (e: Exception) {
                val cleanMsg = e.message ?: "কানেকশন ব্যাহত"
                if (!cleanMsg.contains("cancel", ignoreCase = true) && activeTranslationText != null) {
                    outputStatusLabelView?.text = "📱 অ্যাপ লজিক ফলাফল"
                    outputStatusLabelView?.setTextColor(0xFF8BA2A8.toInt())
                    aiFeedbackLabelView?.visibility = View.VISIBLE
                    aiFeedbackLabelView?.text = "⚠️ ত্রুটি: ${cleanMsg.take(50)}"
                    aiFeedbackLabelView?.setTextColor(0xFFFFB74D.toInt())
                }
                onComplete?.invoke(false)
            }
        }
    }

    private fun processText(text: String): String {
        val prefs = getSharedPreferences("lipi_preferences", Context.MODE_PRIVATE)
        val convertDigits = prefs.getBoolean("convert_digits", false)
        val mode = if (isReverseMode) com.example.lipi.data.LipiMode.BENGALI_TO_BANGLISH else com.example.lipi.data.LipiMode.SMART_AUTO
        val customWordMap = repository.customWordbook.value.associate { it.banglish.lowercase() to it.bengali }
        return MixedScriptTokenizer.process(
            inputText = text,
            mode = mode,
            convertDigitsToBengali = convertDigits,
            customWordbook = customWordMap,
            transliterationStyle = getTransliterationStyle()
        ).convertedText
    }

    private fun onBadgeClicked(anchorX: Int, anchorY: Int) {
        // Read latest clipboard and auto-transliterate
        try {
            val clipText = clipboardManager?.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()
            if (!clipText.isNullOrBlank()) {
                currentText = clipText
                val textSample = if (clipText.length > 800) clipText.take(800) else clipText
                val hasBengali = textSample.any { it in '\u0980'..'\u09FF' }
                val hasBanglish = MixedScriptTokenizer.hasBanglishContent(textSample)
                if (hasBengali && !hasBanglish) {
                    isReverseMode = true
                } else if (hasBanglish && !hasBengali) {
                    isReverseMode = false
                }
                convertedBengali = processText(clipText)
                lastAppLogicResult = convertedBengali
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        showTranslationPopup(anchorX, anchorY, autoPasteFromClipboard = true)
    }

    private fun showTranslationPopup(anchorX: Int, anchorY: Int, autoPasteFromClipboard: Boolean = true) {
        if (!Settings.canDrawOverlays(this)) return
        dismissQuickPreview()
        if (translationPopupView != null) {
            dismissTranslationPopup()
            return
        }

        val displayMetrics = resources.displayMetrics
        // Floating window width: 345dp (comfortably enlarged for better readability)
        val targetWidthDp = 345
        val width = (targetWidthDp * displayMetrics.density).toInt().coerceAtMost((displayMetrics.widthPixels * 0.94f).toInt())

        // Root overlay covering full screen to intercept outside touches and close the window
        val rootOverlay = FrameLayout(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            isClickable = true
            isFocusable = true
        }

        val windowLayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 12, 16, 12)
            background = GradientDrawable().apply {
                cornerRadius = 18f
                setColor(0xFF0C1417.toInt()) // Deep surface
                setStroke((1.5f * displayMetrics.density).toInt(), 0xFF1B3439.toInt()) // Subtle outer border
            }
            elevation = 16f
        }

        // Intercept touches anywhere outside the dialog card to immediately close the window
        rootOverlay.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val rect = android.graphics.Rect()
                container.getHitRect(rect)
                if (!rect.contains(event.x.toInt(), event.y.toInt())) {
                    dismissTranslationPopup()
                    return@setOnTouchListener true
                }
            }
            false
        }

        val isNearCenter = Math.abs(anchorX - displayMetrics.widthPixels / 2) < 80
        val targetLeft = if (isNearCenter) {
            (displayMetrics.widthPixels - width) / 2
        } else {
            anchorX - width + 60
        }
        val cardLayoutParams = FrameLayout.LayoutParams(
            width,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            leftMargin = targetLeft.coerceIn(12, displayMetrics.widthPixels - width - 12)
            topMargin = (anchorY + 30).coerceIn(50, displayMetrics.heightPixels - 380)
        }
        container.layoutParams = cardLayoutParams

        // 1. Sleek, minimal header row (compact title, mode toggle pill, and subtle close button)
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 4)
            }
            layoutParams = params
        }

        val brandTitle = TextView(this).apply {
            text = "লিপি"
            setTextColor(0xFF57DFCE.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        // Compact interactive mode pill button
        val modeToggleBtn = TextView(this).apply {
            text = if (!isReverseMode) "বাংলিশ → বাংলা ⇄" else "বাংলা → বাংলিশ ⇄"
            setTextColor(0xFFE0F7F4.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(10, 4, 10, 4)
            background = GradientDrawable().apply {
                cornerRadius = 10f
                setColor(0x2200A896.toInt())
                setStroke(1, 0x5500A896.toInt())
            }
            val p = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(8, 0, 8, 0)
            }
            layoutParams = p
        }

        val headerSpacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        }

        // Engine indicator badge in header (unobtrusive and subtle)
        val engineBadge = TextView(this).apply {
            val currentMode = getOnlineEngineMode()
            text = when (currentMode) {
                OnlineEngineMode.GEMINI_AI -> "✨ AI"
                OnlineEngineMode.ONLINE_CLOUD -> "☁️ ক্লাউড"
                else -> "📱 অফলাইন"
            }
            setTextColor(0xFF6F868C.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f)
            setPadding(6, 2, 6, 2)
        }

        val closeBtn = TextView(this).apply {
            text = "✕"
            setTextColor(0xFF90A4AE.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            gravity = Gravity.CENTER
            setPadding(8, 3, 6, 3)
            setOnClickListener { dismissTranslationPopup() }
        }

        headerRow.addView(brandTitle)
        headerRow.addView(modeToggleBtn)
        headerRow.addView(headerSpacer)
        headerRow.addView(engineBadge)
        headerRow.addView(closeBtn)

        // 2. Secondary Input Field (clean, functional, quiet styling)
        val inputField = EditText(this).apply {
            hint = if (isReverseMode) "বাংলা লিখুন বা পেস্ট..." else "বাংলিশ লিখুন বা পেস্ট..."
            setHintTextColor(0xFF60777C.toInt())
            setTextColor(0xFFD2DFE2.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
            setPadding(12, 8, 12, 8)
            minLines = 1
            maxLines = 3
            background = GradientDrawable().apply {
                cornerRadius = 10f
                setColor(0xFF121B1E.toInt())
                setStroke(1, 0xFF24363A.toInt())
            }
            val p = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 2, 0, 4)
            }
            layoutParams = p
            if (currentText.isNotBlank()) {
                setText(currentText)
                try {
                    setSelection(currentText.length)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        activeInputField = inputField

        // 3. Primary HERO Section: Output Card with highest visual focus!
        val outputCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 10, 14, 12)
            background = GradientDrawable().apply {
                cornerRadius = 13f
                setColor(0xFF082025.toInt()) // Deep illuminated rich teal
                setStroke((1.75f * displayMetrics.density).toInt(), 0xFF00BFA5.toInt()) // Glowing emerald teal
            }
            elevation = 6f
            val p = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 4, 0, 4)
            }
            layoutParams = p
        }

        val outputHeaderRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val outputHeaderLabel = TextView(this).apply {
            text = if (isReverseMode) "বাংলিশ ফলাফল" else "বাংলা ফলাফল"
            setTextColor(0xFF4DD0E1.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setLayoutParams(LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }

        val aiSwitchBtn = TextView(this).apply {
            text = "লজিক দেখুন"
            setTextColor(0xFF80CBC4.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f)
            visibility = View.GONE
            setPadding(8, 2, 8, 2)
            background = GradientDrawable().apply {
                cornerRadius = 6f
                setColor(0x2800A896.toInt())
                setStroke(1, 0x4400A896.toInt())
            }
        }
        aiSwitchBtnView = aiSwitchBtn

        outputHeaderRow.addView(outputHeaderLabel)
        outputHeaderRow.addView(aiSwitchBtn)

        // Hero Output Display (17sp bold text, crisp white for maximum legibility & focus)
        val translationText = TextView(this).apply {
            text = if (convertedBengali.isNotBlank())
                convertedBengali
            else if (currentText.isNotBlank()) {
                val logicRes = processText(currentText)
                lastAppLogicResult = logicRes
                logicRes
            } else if (isReverseMode)
                "এখানে বাংলিশ রূপান্তর দৃশ্যমান হবে..."
            else
                "এখানে বাংলা রূপান্তর দৃশ্যমান হবে..."
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setLineSpacing(5f, 1.25f)
            setPadding(0, 4, 0, 2)
            setTextIsSelectable(true)
        }
        activeTranslationText = translationText

        outputCard.addView(outputHeaderRow)
        outputCard.addView(translationText)

        aiSwitchBtn.setOnClickListener {
            if (currentDisplaySource == "gemini") {
                if (lastAppLogicResult.isNotBlank()) {
                    currentDisplaySource = "logic"
                    translationText.text = lastAppLogicResult
                    aiSwitchBtn.text = "এআই দেখুন"
                    outputStatusLabelView?.text = "📱 অফলাইন লজিক"
                    outputStatusLabelView?.setTextColor(0xFF6F868C.toInt())
                }
            } else {
                if (lastGeminiResult.isNotBlank()) {
                    currentDisplaySource = "gemini"
                    translationText.text = lastGeminiResult
                    aiSwitchBtn.text = "লজিক দেখুন"
                    val shortModel = getGeminiModel().replace("gemini-", "")
                    outputStatusLabelView?.text = "✨ Gemini ($shortModel)"
                    outputStatusLabelView?.setTextColor(0xFF6F868C.toInt())
                }
            }
        }

        // 4. Subtle metadata & status row (non-intrusive, low visual priority, stacked vertically to avoid horizontal squishing)
        val statusRow = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.START
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(2, 2, 2, 4)
            }
            layoutParams = params
        }

        val outputStatusLabel = TextView(this).apply {
            val mode = getOnlineEngineMode()
            text = when {
                mode == OnlineEngineMode.GEMINI_AI -> "✨ Gemini AI"
                mode == OnlineEngineMode.ONLINE_CLOUD -> "☁️ ক্লাউড"
                else -> "📱 অফলাইন লজিক"
            }
            setTextColor(0xFF6F868C.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            val p = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams = p
        }
        outputStatusLabelView = outputStatusLabel

        val aiFeedbackLabel = TextView(this).apply {
            text = ""
            setTextColor(0xFF6F868C.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            visibility = View.GONE
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            setPadding(0, 2, 0, 0)
            val p = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams = p
        }
        aiFeedbackLabelView = aiFeedbackLabel

        statusRow.addView(outputStatusLabel)
        statusRow.addView(aiFeedbackLabel)

        fun updateToggleAppearance() {
            if (!isReverseMode) {
                modeToggleBtn.text = "বাংলিশ → বাংলা ⇄"
                inputField.hint = "বাংলিশ লিখুন বা পেস্ট..."
                outputHeaderLabel.text = "বাংলা ফলাফল"
                if (convertedBengali.isBlank()) {
                    translationText.text = "এখানে বাংলা রূপান্তর দৃশ্যমান হবে..."
                }
            } else {
                modeToggleBtn.text = "বাংলা → বাংলিশ ⇄"
                inputField.hint = "বাংলা লিখুন বা পেস্ট..."
                outputHeaderLabel.text = "বাংলিশ ফলাফল"
                if (convertedBengali.isBlank()) {
                    translationText.text = "এখানে বাংলিশ রূপান্তর দৃশ্যমান হবে..."
                }
            }
        }
        updateToggleAppearance()

        modeToggleBtn.setOnClickListener {
            isReverseMode = !isReverseMode
            updateToggleAppearance()
            val prevOutput = convertedBengali
            if (prevOutput.isNotBlank()) {
                inputField.setText(prevOutput)
                currentText = prevOutput
            }
            val inText = inputField.text.toString()
            convertedBengali = processText(inText)
            lastAppLogicResult = convertedBengali
            translationText.text = if (convertedBengali.isNotBlank())
                convertedBengali
            else if (isReverseMode)
                "এখানে বাংলিশ রূপান্তর দৃশ্যমান হবে..."
            else
                "এখানে বাংলা রূপান্তর দৃশ্যমান হবে..."
            outputStatusLabel.text = "📱 অফলাইন লজিক"
            outputStatusLabel.setTextColor(0xFF6F868C.toInt())
            if (getOnlineEngineMode() == OnlineEngineMode.GEMINI_AI && inText.isNotBlank()) {
                triggerGeminiTransliteration(inText, force = false)
            }
        }

        // Real-time transliteration as user types with immediate logic + debounced Gemini AI
        inputField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val input = s?.toString() ?: ""
                currentText = input

                liveTypingRunnable?.let { mainHandler.removeCallbacks(it) }
                debouncedAiRunnable?.let { mainHandler.removeCallbacks(it) }
                currentTransliterationJob?.cancel()
                currentTransliterationJob = null

                if (input.isNotBlank()) {
                    liveTypingRunnable = Runnable {
                        val logicRes = processText(input)
                        lastAppLogicResult = logicRes
                        convertedBengali = logicRes
                        currentDisplaySource = "logic"
                        translationText.text = logicRes
                        if (getOnlineEngineMode() == OnlineEngineMode.GEMINI_AI) {
                            outputStatusLabel.text = "📱 অফলাইন লজিক (AI প্রক্রিয়াধীন...)"
                            outputStatusLabel.setTextColor(0xFF6F868C.toInt())
                        } else {
                            outputStatusLabel.text = "📱 অফলাইন লজিক"
                            outputStatusLabel.setTextColor(0xFF6F868C.toInt())
                        }
                        aiSwitchBtn.visibility = View.GONE
                    }
                    mainHandler.postDelayed(liveTypingRunnable!!, 35)

                    if (getOnlineEngineMode() == OnlineEngineMode.GEMINI_AI) {
                        debouncedAiRunnable = Runnable {
                            val latest = inputField.text.toString().trim()
                            if (latest.isNotBlank()) {
                                triggerGeminiTransliteration(latest, force = false)
                            }
                        }
                        mainHandler.postDelayed(debouncedAiRunnable!!, 1200)
                    }
                } else {
                    convertedBengali = ""
                    lastAppLogicResult = ""
                    lastGeminiResult = ""
                    currentTransliterationJob?.cancel()
                    currentTransliterationJob = null
                    translationText.text = if (isReverseMode) "এখানে বাংলিশ রূপান্তর দৃশ্যমান হবে..." else "এখানে বাংলা রূপান্তর দৃশ্যমান হবে..."
                    outputStatusLabel.text = if (isReverseMode) "বাংলিশ মোড" else "বাংলা মোড"
                    outputStatusLabel.setTextColor(0xFF6F868C.toInt())
                    aiFeedbackLabel.visibility = View.GONE
                    aiSwitchBtn.visibility = View.GONE
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // 5. Compact Action Buttons ("নীচের বাটন গুলো কিছুটা ছোট করো")
        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 4, 0, 0)
            }
            layoutParams = params
        }

        val btnHeight = (31 * displayMetrics.density).toInt()

        // 1. Paste Button ("পেস্ট")
        val pasteBtn = Button(this).apply {
            text = "পেস্ট"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 0)
            background = GradientDrawable().apply {
                cornerRadius = 8f
                setColor(0xFF123F47.toInt())
            }
            val p = LinearLayout.LayoutParams(0, btnHeight, 1f).apply {
                setMargins(0, 0, 2, 0)
            }
            layoutParams = p
            setOnClickListener {
                val clipText = try {
                    if (clipboardManager?.hasPrimaryClip() == true) {
                        clipboardManager?.primaryClip?.getItemAt(0)?.coerceToText(this@LipiOverlayService)?.toString()
                    } else null
                } catch (e: Exception) {
                    null
                }

                if (!clipText.isNullOrBlank()) {
                    inputField.setText(clipText)
                    currentText = clipText
                    val logicRes = processText(clipText)
                    lastAppLogicResult = logicRes
                    convertedBengali = logicRes
                    translationText.text = logicRes
                    outputStatusLabel.text = "📱 অফলাইন লজিক"
                    Toast.makeText(this@LipiOverlayService, "লেখা পেস্ট হয়েছে!", Toast.LENGTH_SHORT).show()
                    if (getOnlineEngineMode() == OnlineEngineMode.GEMINI_AI) {
                        triggerGeminiTransliteration(clipText, force = false)
                    }
                } else {
                    val typed = inputField.text.toString()
                    if (typed.isNotBlank()) {
                        val logicRes = processText(typed)
                        lastAppLogicResult = logicRes
                        convertedBengali = logicRes
                        translationText.text = logicRes
                        outputStatusLabel.text = "📱 অফলাইন লজিক"
                        Toast.makeText(this@LipiOverlayService, "রূপান্তর সম্পন্ন হয়েছে!", Toast.LENGTH_SHORT).show()
                        if (getOnlineEngineMode() == OnlineEngineMode.GEMINI_AI) {
                            triggerGeminiTransliteration(typed, force = false)
                        }
                    } else {
                        Toast.makeText(this@LipiOverlayService, "ক্লিপবোর্ডে কোনো লেখা পাওয়া যায়নি।", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        // 2. Clear / Erase Button ("মুছুন")
        val clearBtn = Button(this).apply {
            text = "মুছুন"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(0xFFFFCDD2.toInt())
            setPadding(0, 0, 0, 0)
            background = GradientDrawable().apply {
                cornerRadius = 8f
                setColor(0xFF242C30.toInt())
                setStroke(1, 0xFF3D494F.toInt())
            }
            val p = LinearLayout.LayoutParams(0, btnHeight, 1f).apply {
                setMargins(2, 0, 2, 0)
            }
            layoutParams = p
            setOnClickListener {
                liveTypingRunnable?.let { mainHandler.removeCallbacks(it) }
                debouncedAiRunnable?.let { mainHandler.removeCallbacks(it) }
                currentTransliterationJob?.cancel()
                currentTransliterationJob = null
                inputField.setText("")
                currentText = ""
                convertedBengali = ""
                lastAppLogicResult = ""
                lastGeminiResult = ""
                translationText.text = if (isReverseMode)
                    "এখানে বাংলিশ রূপান্তর দৃশ্যমান হবে..."
                else
                    "এখানে বাংলা রূপান্তর দৃশ্যমান হবে..."
                outputStatusLabel.text = if (isReverseMode) "বাংলিশ মোড" else "বাংলা মোড"
                outputStatusLabel.setTextColor(0xFF6F868C.toInt())
                aiFeedbackLabel.visibility = View.GONE
                aiSwitchBtn.visibility = View.GONE
                Toast.makeText(this@LipiOverlayService, "লেখা মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
            }
        }

        // 3. AI Refine Button ("✨ এআই")
        val aiRefineBtn = Button(this).apply {
            text = "✨ এআই"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 0)
            background = GradientDrawable().apply {
                cornerRadius = 8f
                setColor(0xFF154442.toInt())
                setStroke(1, 0xFF57DFCE.toInt())
            }
            val p = LinearLayout.LayoutParams(0, btnHeight, 1.1f).apply {
                setMargins(2, 0, 2, 0)
            }
            layoutParams = p
            setOnClickListener {
                val textToProcess = inputField.text.toString().ifBlank { currentText }
                if (textToProcess.isNotBlank()) {
                    triggerGeminiTransliteration(textToProcess, force = true)
                } else {
                    Toast.makeText(this@LipiOverlayService, "রূপান্তরের জন্য লেখা লিখুন বা পেস্ট করুন", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 4. Copy Button ("কপি")
        val copyBtn = Button(this).apply {
            text = "কপি"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 0)
            background = GradientDrawable().apply {
                cornerRadius = 8f
                setColor(0xFFBD542E.toInt())
            }
            val p = LinearLayout.LayoutParams(0, btnHeight, 1f).apply {
                setMargins(2, 0, 0, 0)
            }
            layoutParams = p
            setOnClickListener {
                val textToCopy = if (convertedBengali.isNotBlank()) {
                    convertedBengali
                } else if (inputField.text.isNotBlank()) {
                    processText(inputField.text.toString())
                } else ""

                if (textToCopy.isNotBlank()) {
                    try {
                        lastObservedClipText = textToCopy
                        val clipLabel = if (isReverseMode) "Banglish Translation" else "Bengali Translation"
                        val clip = ClipData.newPlainText(clipLabel, textToCopy)
                        clipboardManager?.setPrimaryClip(clip)
                        val toastMsg = if (isReverseMode) "বাংলিশ লেখা কপি হয়েছে!" else "বাংলা অনুবাদ কপি হয়েছে!"
                        safeToast(toastMsg)
                        dismissTranslationPopup()
                    } catch (e: Exception) {
                        safeToast("কপি ব্যর্থ হয়েছে: ${e.message}")
                    }
                } else {
                    val emptyMsg = if (isReverseMode) "কপি করার মতো কোনো বাংলিশ লেখা নেই।" else "কপি করার মতো কোনো বাংলা লেখা নেই।"
                    safeToast(emptyMsg)
                }
            }
        }

        actionRow.addView(pasteBtn)
        actionRow.addView(clearBtn)
        actionRow.addView(aiRefineBtn)
        actionRow.addView(copyBtn)

        container.addView(headerRow)
        container.addView(inputField)
        container.addView(outputCard)
        container.addView(statusRow)
        container.addView(actionRow)

        rootOverlay.addView(container)
        translationPopupView = rootOverlay

        safeAddView(translationPopupView, windowLayoutParams)

        // If opened with pre-existing text or auto-paste, trigger Gemini AI if enabled
        if (currentText.isNotBlank() && getOnlineEngineMode() == OnlineEngineMode.GEMINI_AI) {
            triggerGeminiTransliteration(currentText, force = false)
        }

        if (autoPasteFromClipboard) {
            var toastShown = false
            if (currentText.isNotBlank()) {
                safeToast("✓ ক্লিপবোর্ড থেকে স্বয়ংক্রিয় পেস্ট সম্পন্ন!")
                toastShown = true
            }
            rootOverlay.post {
                try {
                    val clip = clipboardManager?.primaryClip
                    if (clip != null && clip.itemCount > 0) {
                        val clipText = clip.getItemAt(0)?.coerceToText(this@LipiOverlayService)?.toString()
                        if (!clipText.isNullOrBlank() && (inputField.text.isNullOrBlank() || inputField.text.toString() != clipText)) {
                            val textSample = if (clipText.length > 800) clipText.take(800) else clipText
                            val hasBengali = textSample.any { it in '\u0980'..'\u09FF' }
                            val hasBanglish = MixedScriptTokenizer.hasBanglishContent(textSample)
                            if (hasBengali && !hasBanglish) {
                                isReverseMode = true
                            } else if (hasBanglish && !hasBengali) {
                                isReverseMode = false
                            }
                            updateToggleAppearance()
                            currentText = clipText
                            inputField.setText(clipText)
                            try {
                                inputField.setSelection(clipText.length)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            val logicRes = processText(clipText)
                            lastAppLogicResult = logicRes
                            convertedBengali = logicRes
                            translationText.text = logicRes
                            outputStatusLabel.text = "📱 অ্যাপ লজিক ফলাফল"

                            if (getOnlineEngineMode() == OnlineEngineMode.GEMINI_AI) {
                                triggerGeminiTransliteration(clipText, force = false)
                            }

                            if (!toastShown) {
                                safeToast("✓ ক্লিপবোর্ড থেকে স্বয়ংক্রিয় পেস্ট সম্পন্ন!")
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun dismissTranslationPopup() {
        liveTypingRunnable?.let { mainHandler.removeCallbacks(it) }
        debouncedAiRunnable?.let { mainHandler.removeCallbacks(it) }
        currentTransliterationJob?.cancel()
        currentTransliterationJob = null
        activeInputField = null
        activeTranslationText = null
        outputStatusLabelView = null
        aiFeedbackLabelView = null
        aiSwitchBtnView = null
        translationPopupView?.let {
            safeRemoveView(it)
            translationPopupView = null
        }
    }

    private fun dismissAllOverlays() {
        dismissQuickPreview()
        dismissTranslationPopup()
        dismissTargetView?.let {
            safeRemoveView(it)
            dismissTargetView = null
            isDismissTargetShowing = false
            isBallHoveringDismiss = false
        }
        floatingBadgeView?.let {
            safeRemoveView(it)
            floatingBadgeView = null
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "লিপি দ্রুত এক্সেস বার (Quick Access)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "বাংলিশ ও বাংলা দ্রুত অনুবাদের জন্য স্থায়ী নোটিফিকেশন বার"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun updatePersistentNotification() {
        val prefs = getSharedPreferences("lipi_preferences", Context.MODE_PRIVATE)
        val isNotificationEnabled = prefs.getBoolean("persistent_notification_enabled", true)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        if (!isNotificationEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            cancelPersistentNotification()
            return
        }

        createNotificationChannel()

        // 1. PendingIntent to open the 300dp floating translation popup directly
        val openPopupIntent = Intent(this, LipiOverlayService::class.java).apply {
            action = ACTION_OPEN_POPUP
        }
        val openPopupPendingIntent = PendingIntent.getService(
            this,
            101,
            openPopupIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. PendingIntent to open the Lipi main app / studio
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            102,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. PendingIntent to toggle the floating ball
        val toggleBadgeIntent = Intent(this, LipiOverlayService::class.java).apply {
            action = ACTION_TOGGLE_BADGE
        }
        val toggleBadgePendingIntent = PendingIntent.getService(
            this,
            103,
            toggleBadgeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 4. PendingIntent to kill the persistent notification and service completely
        val killServiceIntent = Intent(this, LipiOverlayService::class.java).apply {
            action = ACTION_KILL_SERVICE
        }
        val killServicePendingIntent = PendingIntent.getService(
            this,
            104,
            killServiceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isBadgeShowing = floatingBadgeView != null && floatingBadgeView?.visibility == View.VISIBLE

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_lipi)
            .setContentTitle("লিপি দ্রুত অনুবাদ বার")
            .setContentText("উইন্ডো খুলতে বা যেকোনো অ্যাপে তাৎক্ষণিক বাংলা অনুবাদের জন্য ট্যাপ করুন")
            .setSubText("Lipi Quick Access")
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openPopupPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_edit,
                "উইন্ডো খুলুন",
                openPopupPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_view,
                "লিপি স্টুডিও",
                openAppPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_compass,
                if (isBadgeShowing) "বল লুকান" else "বল দেখান",
                toggleBadgePendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "কিল করুন",
                killServicePendingIntent
            )
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build().apply {
                flags = flags or Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR
            }

        // Start in Foreground so the system does not kill the service or dismiss the notification
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                try {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } catch (e: Exception) {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to notify
            try {
                notificationManager?.notify(NOTIFICATION_ID, notification)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun cancelPersistentNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        try {
            notificationManager?.cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (isServiceKilled) return
        // Keep foreground service and notification active if enabled
        val prefs = getSharedPreferences("lipi_preferences", Context.MODE_PRIVATE)
        val isNotificationEnabled = prefs.getBoolean("persistent_notification_enabled", true)
        val isFloatingEnabled = prefs.getBoolean("floating_enabled", true)
        if (isNotificationEnabled || isFloatingEnabled) {
            updatePersistentNotification()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        try {
            clipboardManager?.removePrimaryClipChangedListener(clipboardListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mainHandler.removeCallbacksAndMessages(null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        cancelPersistentNotification()
        dismissAllOverlays()
    }
}
