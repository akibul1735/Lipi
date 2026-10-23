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
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
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
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.lipi.engine.MixedScriptTokenizer

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

    private var activeInputField: EditText? = null
    private var activeTranslationText: TextView? = null

    private var currentText: String = ""
    private var convertedBengali: String = ""
    private var lastObservedClipText: String = ""
    private var isReverseMode: Boolean = false

    private var clipboardManager: ClipboardManager? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var lastBadgeX = -1
    private var lastBadgeY = 300

    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        checkClipboardForBanglish()
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
        if (!Settings.canDrawOverlays(this)) {
            mainHandler.post {
                Toast.makeText(this, "লিপি: ভাসমান উইন্ডো অনুমতি প্রয়োজন", Toast.LENGTH_LONG).show()
            }
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_DISMISS -> {
                dismissAllOverlays()
                cancelPersistentNotification()
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
                        convertedBengali = processText(clipText)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                showTranslationPopup(centerX, popupY)
                updatePersistentNotification()
            }
            ACTION_TOGGLE_BADGE -> {
                if (floatingBadgeView != null && floatingBadgeView?.visibility == View.VISIBLE) {
                    floatingBadgeView?.visibility = View.GONE
                    Toast.makeText(this, "ভাসমান বল লুকানো হয়েছে", Toast.LENGTH_SHORT).show()
                } else {
                    ensureFloatingBadgeVisible()
                    Toast.makeText(this, "ভাসমান বল দৃশ্যমান হয়েছে", Toast.LENGTH_SHORT).show()
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
     */
    private fun checkClipboardForBanglish() {
        val prefs = getSharedPreferences("lipi_preferences", Context.MODE_PRIVATE)
        val isMonitoringEnabled = prefs.getBoolean("auto_clipboard_monitoring", true)
        if (!isMonitoringEnabled) return

        try {
            val clip = clipboardManager?.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString() ?: return
            val hasBengali = clip.any { it in '\u0980'..'\u09FF' }
            val hasBanglish = MixedScriptTokenizer.hasBanglishContent(clip)

            if (clip.isNotBlank() && clip != lastObservedClipText && (hasBanglish || hasBengali)) {
                lastObservedClipText = clip
                currentText = clip
                val convertDigits = prefs.getBoolean("convert_digits", false)
                val isReverse = hasBengali && !hasBanglish
                val mode = if (isReverse) com.example.lipi.data.LipiMode.BENGALI_TO_BANGLISH else com.example.lipi.data.LipiMode.SMART_AUTO
                val conversion = MixedScriptTokenizer.process(
                    inputText = clip,
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

        // Draggable touch listener
        badge.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isClick = false

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (event == null) return false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isClick = true
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(dx) > 12 || Math.abs(dy) > 12) {
                            isClick = false
                        }
                        layoutParams.x = initialX + dx
                        layoutParams.y = initialY + dy
                        try {
                            windowManager?.updateViewLayout(badge, layoutParams)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isClick) {
                            lastBadgeX = layoutParams.x
                            lastBadgeY = layoutParams.y
                            dismissQuickPreview()
                            onBadgeClicked(layoutParams.x, layoutParams.y)
                        } else {
                            val screenWidth = displayMetrics.widthPixels
                            layoutParams.x = if (layoutParams.x + dpSize / 2 < screenWidth / 2) {
                                16
                            } else {
                                screenWidth - dpSize - 16
                            }
                            lastBadgeX = layoutParams.x
                            try {
                                windowManager?.updateViewLayout(badge, layoutParams)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        return true
                    }
                }
                return false
            }
        })

        floatingBadgeView = badge
        try {
            windowManager?.addView(floatingBadgeView, layoutParams)
            mainHandler.post {
                Toast.makeText(this, "লিপি 'A ⇄ অ' অনুবাদক বল স্ক্রিনে দৃশ্যমান হয়েছে!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Shows a lightweight floating preview pill right next to the badge when clipboard is copied.
     */
    private fun showQuickPreviewCard(bengaliText: String) {
        if (!Settings.canDrawOverlays(this)) return
        dismissQuickPreview()

        val displayMetrics = resources.displayMetrics
        val width = (260 * displayMetrics.density).toInt()

        val layoutParams = WindowManager.LayoutParams(
            width,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
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
            setPadding(28, 20, 28, 20)
            background = GradientDrawable().apply {
                cornerRadius = 24f
                setColor(0xFF0F1B20.toInt())
                setStroke((2 * displayMetrics.density).toInt(), 0xFF57DFCE.toInt())
            }
            elevation = 28f
        }

        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(this).apply {
            text = "📋 স্বয়ংক্রিয় বাংলা রূপান্তর:"
            setTextColor(0xFF57DFCE.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setLayoutParams(LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }

        val closeBtn = TextView(this).apply {
            text = "✕"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(8, 0, 8, 8)
            setOnClickListener { dismissQuickPreview() }
        }

        titleRow.addView(title)
        titleRow.addView(closeBtn)

        val preview = TextView(this).apply {
            text = bengaliText
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            maxLines = 3
            ellipsize = TextUtils.TruncateAt.END
            setPadding(0, 8, 0, 14)
        }

        val copyActionBtn = Button(this).apply {
            text = "কপি করুন"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            background = GradientDrawable().apply {
                cornerRadius = 16f
                setColor(0xFFD86B43.toInt()) // Terracotta
            }
            setOnClickListener {
                try {
                    val clip = ClipData.newPlainText("Bengali Translation", bengaliText)
                    clipboardManager?.setPrimaryClip(clip)
                    Toast.makeText(this@LipiOverlayService, "✓ বাংলা অনুবাদ ক্লিপবোর্ডে কপি হয়েছে!", Toast.LENGTH_SHORT).show()
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
        try {
            windowManager?.addView(quickPreviewView, layoutParams)
            card.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(240)
                .setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
                .start()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Auto dismiss after 6 seconds
        quickPreviewDismissRunnable = Runnable { dismissQuickPreview() }
        mainHandler.postDelayed(quickPreviewDismissRunnable!!, 6000)
    }

    private fun dismissQuickPreview() {
        quickPreviewDismissRunnable?.let { mainHandler.removeCallbacks(it) }
        quickPreviewDismissRunnable = null
        if (quickPreviewView != null) {
            try {
                windowManager?.removeView(quickPreviewView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            quickPreviewView = null
        }
    }

    private fun processText(text: String): String {
        val prefs = getSharedPreferences("lipi_preferences", Context.MODE_PRIVATE)
        val convertDigits = prefs.getBoolean("convert_digits", false)
        val mode = if (isReverseMode) com.example.lipi.data.LipiMode.BENGALI_TO_BANGLISH else com.example.lipi.data.LipiMode.SMART_AUTO
        return MixedScriptTokenizer.process(
            inputText = text,
            mode = mode,
            convertDigitsToBengali = convertDigits,
            transliterationStyle = getTransliterationStyle()
        ).convertedText
    }

    private fun onBadgeClicked(anchorX: Int, anchorY: Int) {
        // Read latest clipboard if available
        try {
            val clipText = clipboardManager?.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()
            if (!clipText.isNullOrBlank()) {
                currentText = clipText
                convertedBengali = processText(clipText)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        showTranslationPopup(anchorX, anchorY)
    }

    private fun showTranslationPopup(anchorX: Int, anchorY: Int) {
        if (translationPopupView != null) {
            dismissTranslationPopup()
            return
        }

        val displayMetrics = resources.displayMetrics
        // Floating window width: 300dp as requested by user
        val targetWidthDp = 300
        val width = (targetWidthDp * displayMetrics.density).toInt().coerceAtMost((displayMetrics.widthPixels * 0.95f).toInt())

        // Root overlay covering full screen to intercept outside touches and close the window
        val rootOverlay = FrameLayout(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            isClickable = true
            isFocusable = true
            // Clicking outside the floating dialog immediately closes it
            setOnClickListener {
                dismissTranslationPopup()
            }
        }

        val windowLayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, // Allow keyboard and focus inside input
            PixelFormat.TRANSLUCENT
        )

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 12, 16, 12)
            background = GradientDrawable().apply {
                cornerRadius = 18f
                setColor(0xFF0C1518.toInt()) // Deep surface
                setStroke(2, 0xFF00A896.toInt())
            }
            elevation = 16f
            // Important: prevent clicks inside the dialog card from dismissing it
            setOnClickListener { /* consume click */ }
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
            leftMargin = targetLeft.coerceIn(16, displayMetrics.widthPixels - width - 16)
            topMargin = (anchorY + 30).coerceIn(60, displayMetrics.heightPixels - 360)
        }
        container.layoutParams = cardLayoutParams

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(this).apply {
            text = if (isReverseMode) "লিপি: বাংলা → বাংলিশ" else "লিপি: বাংলিশ → বাংলা"
            setTextColor(0xFF57DFCE.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setLayoutParams(LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }

        val closeBtn = TextView(this).apply {
            text = "✕"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            gravity = Gravity.CENTER
            setPadding(12, 4, 12, 4)
            background = GradientDrawable().apply {
                cornerRadius = 10f
                setColor(0x25FFFFFF)
            }
            setOnClickListener { dismissTranslationPopup() }
        }

        headerRow.addView(title)
        headerRow.addView(closeBtn)

        // Editable Input Field for writing or pasting (compact height)
        val inputField = EditText(this).apply {
            hint = if (isReverseMode) "বাংলা লিখুন বা পেস্ট..." else "বাংলিশ লিখুন বা পেস্ট..."
            setHintTextColor(0xFF888888.toInt())
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(12, 8, 12, 8)
            minLines = 1
            maxLines = 3
            background = GradientDrawable().apply {
                cornerRadius = 10f
                setColor(0xFF142226.toInt())
                setStroke(1, 0xFF0F5161.toInt())
            }
            if (currentText.isNotBlank()) {
                setText(currentText)
            }
        }
        activeInputField = inputField

        // Live Translated Output Display in a sleek highlighted box
        val translationText = TextView(this).apply {
            text = if (convertedBengali.isNotBlank())
                convertedBengali
            else if (currentText.isNotBlank())
                processText(currentText)
            else if (isReverseMode)
                "এখানে বাংলিশ রূপান্তর দৃশ্যমান হবে..."
            else
                "এখানে বাংলা রূপান্তর দৃশ্যমান হবে..."
            setTextColor(0xFF57DFCE.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setLineSpacing(3f, 1.15f)
            setPadding(12, 8, 12, 8)
            background = GradientDrawable().apply {
                cornerRadius = 10f
                setColor(0xFF0F1E22.toInt())
                setStroke(1, 0xFF00A896.toInt())
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 6, 0, 8)
            }
            layoutParams = params
        }
        activeTranslationText = translationText

        // Modern Segmented Mode Toggle Button: [ বাংলিশ → বাংলা | বাংলা → বাংলিশ ]
        val toggleBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(2, 2, 2, 2)
            background = GradientDrawable().apply {
                cornerRadius = 10f
                setColor(0xFF142226.toInt())
                setStroke(1, 0xFF0F5161.toInt())
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 4, 0, 8)
            }
            layoutParams = params
        }

        val btnBanglishToBengali = TextView(this).apply {
            text = "বাংলিশ → বাংলা"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            gravity = Gravity.CENTER
            setPadding(4, 6, 4, 6)
            setLayoutParams(LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }

        val btnBengaliToBanglish = TextView(this).apply {
            text = "বাংলা → বাংলিশ"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            gravity = Gravity.CENTER
            setPadding(4, 6, 4, 6)
            setLayoutParams(LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }

        fun updateToggleAppearance() {
            if (!isReverseMode) {
                btnBanglishToBengali.background = GradientDrawable().apply {
                    cornerRadius = 8f
                    setColor(0xFF00A896.toInt())
                }
                btnBanglishToBengali.setTextColor(Color.WHITE)
                btnBanglishToBengali.setTypeface(null, android.graphics.Typeface.BOLD)

                btnBengaliToBanglish.setBackgroundColor(Color.TRANSPARENT)
                btnBengaliToBanglish.setTextColor(0xFF8BA2A8.toInt())
                btnBengaliToBanglish.setTypeface(null, android.graphics.Typeface.NORMAL)

                title.text = "লিপি: বাংলিশ → বাংলা"
                inputField.hint = "বাংলিশ লিখুন বা পেস্ট..."
                if (convertedBengali.isBlank()) {
                    translationText.text = "এখানে বাংলা রূপান্তর দৃশ্যমান হবে..."
                }
            } else {
                btnBengaliToBanglish.background = GradientDrawable().apply {
                    cornerRadius = 8f
                    setColor(0xFF00A896.toInt())
                }
                btnBengaliToBanglish.setTextColor(Color.WHITE)
                btnBengaliToBanglish.setTypeface(null, android.graphics.Typeface.BOLD)

                btnBanglishToBengali.setBackgroundColor(Color.TRANSPARENT)
                btnBanglishToBengali.setTextColor(0xFF8BA2A8.toInt())
                btnBanglishToBengali.setTypeface(null, android.graphics.Typeface.NORMAL)

                title.text = "লিপি: বাংলা → বাংলিশ"
                inputField.hint = "বাংলা লিখুন বা পেস্ট..."
                if (convertedBengali.isBlank()) {
                    translationText.text = "এখানে বাংলিশ রূপান্তর দৃশ্যমান হবে..."
                }
            }
        }
        updateToggleAppearance()

        btnBanglishToBengali.setOnClickListener {
            if (isReverseMode) {
                isReverseMode = false
                updateToggleAppearance()
                val prevOutput = convertedBengali
                if (prevOutput.isNotBlank()) {
                    inputField.setText(prevOutput)
                    currentText = prevOutput
                }
                convertedBengali = processText(inputField.text.toString())
                translationText.text = if (convertedBengali.isNotBlank()) convertedBengali else "এখানে বাংলা রূপান্তর দৃশ্যমান হবে..."
            }
        }

        btnBengaliToBanglish.setOnClickListener {
            if (!isReverseMode) {
                isReverseMode = true
                updateToggleAppearance()
                val prevOutput = convertedBengali
                if (prevOutput.isNotBlank()) {
                    inputField.setText(prevOutput)
                    currentText = prevOutput
                }
                convertedBengali = processText(inputField.text.toString())
                translationText.text = if (convertedBengali.isNotBlank()) convertedBengali else "এখানে বাংলিশ রূপান্তর দৃশ্যমান হবে..."
            }
        }

        toggleBar.addView(btnBanglishToBengali)
        toggleBar.addView(btnBengaliToBanglish)

        // Real-time transliteration as user types
        inputField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val input = s?.toString() ?: ""
                currentText = input
                if (input.isNotBlank()) {
                    convertedBengali = processText(input)
                    translationText.text = convertedBengali
                } else {
                    convertedBengali = ""
                    translationText.text = if (isReverseMode) "এখানে বাংলিশ রূপান্তর দৃশ্যমান হবে..." else "এখানে বাংলা রূপান্তর দৃশ্যমান হবে..."
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 2, 0, 0)
            }
            layoutParams = params
        }

        val btnHeight = (36 * displayMetrics.density).toInt()

        // 1. Paste Button ("পেস্ট করুন")
        val pasteBtn = Button(this).apply {
            text = "পেস্ট করুন"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(Color.WHITE)
            setPadding(4, 0, 4, 0)
            background = GradientDrawable().apply {
                cornerRadius = 10f
                setColor(0xFF0F5161.toInt()) // Deep teal
            }
            val p = LinearLayout.LayoutParams(0, btnHeight, 1f).apply {
                setMargins(0, 0, 3, 0)
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
                    convertedBengali = processText(clipText)
                    translationText.text = convertedBengali
                    Toast.makeText(this@LipiOverlayService, "লেখা পেস্ট ও রূপান্তর হয়েছে!", Toast.LENGTH_SHORT).show()
                } else {
                    val typed = inputField.text.toString()
                    if (typed.isNotBlank()) {
                        convertedBengali = processText(typed)
                        translationText.text = convertedBengali
                        Toast.makeText(this@LipiOverlayService, "রূপান্তর সম্পন্ন হয়েছে!", Toast.LENGTH_SHORT).show()
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
            setPadding(4, 0, 4, 0)
            background = GradientDrawable().apply {
                cornerRadius = 10f
                setColor(0xFF2E383D.toInt()) // Slate dark
                setStroke(1, 0xFF4F5D65.toInt())
            }
            val p = LinearLayout.LayoutParams(0, btnHeight, 1f).apply {
                setMargins(3, 0, 3, 0)
            }
            layoutParams = p
            setOnClickListener {
                inputField.setText("")
                currentText = ""
                convertedBengali = ""
                translationText.text = if (isReverseMode)
                    "এখানে বাংলিশ রূপান্তর দৃশ্যমান হবে..."
                else
                    "এখানে বাংলা রূপান্তর দৃশ্যমান হবে..."
                Toast.makeText(this@LipiOverlayService, "লেখা মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
            }
        }

        // 3. Copy Button ("কপি করুন")
        val copyBtn = Button(this).apply {
            text = "কপি করুন"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(Color.WHITE)
            setPadding(4, 0, 4, 0)
            background = GradientDrawable().apply {
                cornerRadius = 10f
                setColor(0xFFD86B43.toInt()) // Terracotta
            }
            val p = LinearLayout.LayoutParams(0, btnHeight, 1f).apply {
                setMargins(3, 0, 0, 0)
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
                        val clipLabel = if (isReverseMode) "Banglish Translation" else "Bengali Translation"
                        val clip = ClipData.newPlainText(clipLabel, textToCopy)
                        clipboardManager?.setPrimaryClip(clip)
                        val toastMsg = if (isReverseMode) "বাংলিশ লেখা কপি হয়েছে!" else "বাংলা অনুবাদ কপি হয়েছে!"
                        Toast.makeText(this@LipiOverlayService, toastMsg, Toast.LENGTH_SHORT).show()
                        dismissTranslationPopup()
                    } catch (e: Exception) {
                        Toast.makeText(this@LipiOverlayService, "কপি ব্যর্থ হয়েছে: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val emptyMsg = if (isReverseMode) "কপি করার মতো কোনো বাংলিশ লেখা নেই।" else "কপি করার মতো কোনো বাংলা লেখা নেই।"
                    Toast.makeText(this@LipiOverlayService, emptyMsg, Toast.LENGTH_SHORT).show()
                }
            }
        }

        actionRow.addView(pasteBtn)
        actionRow.addView(clearBtn)
        actionRow.addView(copyBtn)

        container.addView(headerRow)
        container.addView(toggleBar)
        container.addView(inputField)
        container.addView(translationText)
        container.addView(actionRow)

        rootOverlay.addView(container)
        translationPopupView = rootOverlay

        try {
            windowManager?.addView(translationPopupView, windowLayoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun dismissTranslationPopup() {
        activeInputField = null
        activeTranslationText = null
        if (translationPopupView != null) {
            try {
                windowManager?.removeView(translationPopupView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            translationPopupView = null
        }
    }

    private fun dismissAllOverlays() {
        dismissQuickPreview()
        dismissTranslationPopup()
        if (floatingBadgeView != null) {
            try {
                windowManager?.removeView(floatingBadgeView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
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

        val isBadgeShowing = floatingBadgeView != null && floatingBadgeView?.visibility == View.VISIBLE

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_lipi)
            .setContentTitle("লিপি দ্রুত অনুবাদ বার")
            .setContentText("উইন্ডো খুলতে বা যেকোনো অ্যাপে তাৎক্ষণিক বাংলা অনুবাদের জন্য ট্যাপ করুন")
            .setSubText("Lipi Quick Access")
            .setOngoing(true)
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
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        try {
            notificationManager?.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            e.printStackTrace()
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

    override fun onDestroy() {
        super.onDestroy()
        try {
            clipboardManager?.removePrimaryClipChangedListener(clipboardListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        cancelPersistentNotification()
        dismissAllOverlays()
    }
}
