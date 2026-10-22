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
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.lipi.engine.MixedScriptTokenizer

/**
 * Native Android Floating Overlay Assistant Service.
 * Provides a persistent draggable floating badge ('ল') over WhatsApp, Messenger, etc.
 * Supports interactive popup with reliable Paste and Copy functionality.
 */
class LipiOverlayService : Service() {

    companion object {
        const val ACTION_START_ASSISTANT = "com.example.lipi.action.START_ASSISTANT"
        const val ACTION_SHOW_BADGE = "com.example.lipi.action.SHOW_BADGE"
        const val ACTION_DISMISS = "com.example.lipi.action.DISMISS"
        const val EXTRA_DETECTED_TEXT = "extra_detected_text"
        const val EXTRA_BOUNDS_X = "extra_bounds_x"
        const val EXTRA_BOUNDS_Y = "extra_bounds_y"
    }

    private var windowManager: WindowManager? = null
    private var floatingBadgeView: View? = null
    private var translationPopupView: View? = null
    private var currentText: String = ""
    private var convertedBengali: String = ""
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
                stopSelf()
                return START_NOT_STICKY
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
            }
            ACTION_START_ASSISTANT -> {
                ensureFloatingBadgeVisible()
            }
            else -> {
                ensureFloatingBadgeVisible()
            }
        }

        return START_STICKY
    }

    private fun checkClipboardForBanglish() {
        try {
            val clip = clipboardManager?.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString() ?: return
            if (clip.isNotBlank() && MixedScriptTokenizer.hasBanglishContent(clip)) {
                currentText = clip
                convertedBengali = MixedScriptTokenizer.process(clip).convertedText
                mainHandler.post {
                    Toast.makeText(this, "লিপি: কপি করা মেসেজ প্রস্তুত ('ল' বোতামে চাপুন)", Toast.LENGTH_SHORT).show()
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

        val badge = TextView(this).apply {
            text = "ল"
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFF0F5161.toInt()) // Padma Teal
                setStroke((3 * displayMetrics.density).toInt(), 0xFF57DFCE.toInt()) // Teal Glow
            }
            elevation = 20f
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
                Toast.makeText(this, "লিপি ভাসমান 'ল' বল স্ক্রিনে দৃশ্যমান হয়েছে!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun onBadgeClicked(anchorX: Int, anchorY: Int) {
        // Read latest clipboard if available
        try {
            val clipText = clipboardManager?.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()
            if (!clipText.isNullOrBlank()) {
                currentText = clipText
                convertedBengali = MixedScriptTokenizer.process(clipText).convertedText
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
        val width = (320 * displayMetrics.density).toInt()
        val layoutParams = WindowManager.LayoutParams(
            width,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, // Allow focus so clipboard paste/copy works on Android 10+
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (anchorX - width + 80).coerceIn(20, displayMetrics.widthPixels - width - 20)
            y = (anchorY + 60).coerceIn(100, displayMetrics.heightPixels - 420)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
            background = GradientDrawable().apply {
                cornerRadius = 24f
                setColor(0xFF0C1518.toInt()) // Deep surface
                setStroke(2, 0xFF00A896.toInt())
            }
            elevation = 24f
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(this).apply {
            text = "লিপি অনুবাদ সহকারী"
            setTextColor(0xFF57DFCE.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setLayoutParams(LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }

        val closeBtn = Button(this).apply {
            text = "✕"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { dismissTranslationPopup() }
        }

        headerRow.addView(title)
        headerRow.addView(closeBtn)

        // Editable Input Field for writing or pasting
        val inputField = EditText(this).apply {
            hint = "বাংলিশ মেসেজ লিখুন বা পেস্ট করুন..."
            setHintTextColor(0xFF888888.toInt())
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(24, 18, 24, 18)
            background = GradientDrawable().apply {
                cornerRadius = 14f
                setColor(0xFF142226.toInt())
                setStroke(2, 0xFF0F5161.toInt())
            }
            if (currentText.isNotBlank()) {
                setText(currentText)
            }
        }

        // Live Translated Output Display
        val translationText = TextView(this).apply {
            text = if (convertedBengali.isNotBlank())
                convertedBengali
            else if (currentText.isNotBlank())
                MixedScriptTokenizer.process(currentText).convertedText
            else
                "এখানে বাংলা অনুবাদ দৃশ্যমান হবে..."
            setTextColor(0xFF57DFCE.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setLineSpacing(6f, 1.2f)
            setPadding(0, 14, 0, 16)
        }

        // Real-time transliteration as user types
        inputField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val input = s?.toString() ?: ""
                currentText = input
                if (input.isNotBlank()) {
                    convertedBengali = MixedScriptTokenizer.process(input).convertedText
                    translationText.text = convertedBengali
                } else {
                    convertedBengali = ""
                    translationText.text = "এখানে বাংলা অনুবাদ দৃশ্যমান হবে..."
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        // Paste & Translate Button
        val pasteAndTranslateBtn = Button(this).apply {
            text = "পেস্ট করুন"
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = 14f
                setColor(0xFF0F5161.toInt())
            }
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
                    convertedBengali = MixedScriptTokenizer.process(clipText).convertedText
                    translationText.text = convertedBengali
                    Toast.makeText(this@LipiOverlayService, "লেখা পেস্ট ও রূপান্তর হয়েছে!", Toast.LENGTH_SHORT).show()
                } else {
                    val typed = inputField.text.toString()
                    if (typed.isNotBlank()) {
                        convertedBengali = MixedScriptTokenizer.process(typed).convertedText
                        translationText.text = convertedBengali
                        Toast.makeText(this@LipiOverlayService, "রূপান্তর সম্পন্ন হয়েছে!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@LipiOverlayService, "ক্লিপবোর্ডে কোনো লেখা পাওয়া যায়নি।", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        // Copy Translated Bengali Button
        val copyBtn = Button(this).apply {
            text = "কপি করুন"
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = 14f
                setColor(0xFFD86B43.toInt()) // Terracotta
            }
            setOnClickListener {
                val textToCopy = if (convertedBengali.isNotBlank()) {
                    convertedBengali
                } else if (inputField.text.isNotBlank()) {
                    MixedScriptTokenizer.process(inputField.text.toString()).convertedText
                } else ""

                if (textToCopy.isNotBlank()) {
                    try {
                        val clip = ClipData.newPlainText("Bengali Translation", textToCopy)
                        clipboardManager?.setPrimaryClip(clip)
                        Toast.makeText(this@LipiOverlayService, "বাংলা অনুবাদ কপি হয়েছে!", Toast.LENGTH_SHORT).show()
                        dismissTranslationPopup()
                    } catch (e: Exception) {
                        Toast.makeText(this@LipiOverlayService, "কপি ব্যর্থ হয়েছে: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@LipiOverlayService, "কপি করার মতো কোনো বাংলা লেখা নেই।", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val spacer = View(this).apply {
            setLayoutParams(LinearLayout.LayoutParams(16, 0))
        }

        actionRow.addView(pasteAndTranslateBtn)
        actionRow.addView(spacer)
        actionRow.addView(copyBtn)

        val spacerMiddle = View(this).apply {
            setLayoutParams(LinearLayout.LayoutParams(0, 12))
        }

        container.addView(headerRow)
        container.addView(inputField)
        container.addView(translationText)
        container.addView(actionRow)

        translationPopupView = container
        try {
            windowManager?.addView(translationPopupView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun dismissTranslationPopup() {
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

    override fun onDestroy() {
        super.onDestroy()
        try {
            clipboardManager?.removePrimaryClipChangedListener(clipboardListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        dismissAllOverlays()
    }
}
