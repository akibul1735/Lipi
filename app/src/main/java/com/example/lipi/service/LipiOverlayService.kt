package com.example.lipi.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
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
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.lipi.engine.MixedScriptTokenizer

/**
 * Native Android Floating Overlay Assistant Service.
 * Provides a persistent draggable floating badge ('ল') over WhatsApp, Messenger, etc.
 * Supports auto-translating copied text from any messaging app and on-tap conversion popover.
 */
class LipiOverlayService : Service() {

    companion object {
        const val ACTION_START_ASSISTANT = "com.example.lipi.action.START_ASSISTANT"
        const val ACTION_SHOW_BADGE = "com.example.lipi.action.SHOW_BADGE"
        const val ACTION_DISMISS = "com.example.lipi.action.DISMISS"
        const val EXTRA_DETECTED_TEXT = "extra_detected_text"
        const val EXTRA_BOUNDS_X = "extra_bounds_x"
        const val EXTRA_BOUNDS_Y = "extra_bounds_y"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "lipi_floating_channel"
    }

    private var windowManager: WindowManager? = null
    private var floatingBadgeView: View? = null
    private var translationPopupView: View? = null
    private var currentText: String = ""
    private var convertedBengali: String = ""
    private var clipboardManager: ClipboardManager? = null

    private var lastBadgeX = 60
    private var lastBadgeY = 300

    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        checkClipboardForBanglish()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboardManager?.addPrimaryClipChangedListener(clipboardListener)

        startForegroundServiceWithNotification()
    }

    private fun startForegroundServiceWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Lipi Floating Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "মেসেজিং অ্যাপে বাংলিশ রূপান্তর সহকারী"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("লিপি (Lipi) সক্রিয় আছে")
            .setContentText("হোয়াটসঅ্যাপ/মেসেঞ্জারের ওপর ভাসমান সহকারী প্রস্তুত")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            ensureFloatingBadgeVisible()
            return START_STICKY
        }

        when (intent.action) {
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
            val clip = clipboardManager?.primaryClip?.getItemAt(0)?.text?.toString() ?: return
            if (clip.isNotBlank() && MixedScriptTokenizer.hasBanglishContent(clip)) {
                currentText = clip
                convertedBengali = MixedScriptTokenizer.process(clip).convertedText
                Toast.makeText(this, "লিপি: কপি করা মেসেজ অনুবাদের জন্য প্রস্তুত ('ল' ট্যাপ করুন)", Toast.LENGTH_SHORT).show()
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
        if (floatingBadgeView != null) return // Already visible

        val dpSize = (48 * resources.displayMetrics.density).toInt()
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
            x = lastBadgeX.coerceIn(20, resources.displayMetrics.widthPixels - dpSize - 20)
            y = lastBadgeY.coerceIn(100, resources.displayMetrics.heightPixels - dpSize - 100)
        }

        val badge = TextView(this).apply {
            text = "ল"
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFF0F5161.toInt()) // Padma Teal
                setStroke((2.5f * resources.displayMetrics.density).toInt(), 0xFF00A896.toInt())
            }
            elevation = 16f
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
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun onBadgeClicked(anchorX: Int, anchorY: Int) {
        // If we don't have text yet, check clipboard
        if (currentText.isBlank()) {
            val clipText = clipboardManager?.primaryClip?.getItemAt(0)?.text?.toString()
            if (!clipText.isNullOrBlank()) {
                currentText = clipText
                convertedBengali = MixedScriptTokenizer.process(clipText).convertedText
            }
        }
        showTranslationPopup(anchorX, anchorY)
    }

    private fun showTranslationPopup(anchorX: Int, anchorY: Int) {
        if (translationPopupView != null) {
            dismissTranslationPopup()
            return
        }

        val width = (320 * resources.displayMetrics.density).toInt()
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
            x = (anchorX - width + 80).coerceIn(20, resources.displayMetrics.widthPixels - width - 20)
            y = (anchorY + 60).coerceIn(100, resources.displayMetrics.heightPixels - 350)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
            background = GradientDrawable().apply {
                cornerRadius = 24f
                setColor(0xFF0C1518.toInt()) // Deep surface
                setStroke(2, 0xFF00A896.toInt())
            }
            elevation = 20f
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(this).apply {
            text = "লিপি অনুবাদ সহকারী"
            setTextColor(0xFF57DFCE.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
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

        val translationText = TextView(this).apply {
            text = if (convertedBengali.isNotBlank()) convertedBengali else "কোনো টেক্সট পাওয়া যায়নি। কোনো মেসেজ কপি করে 'ল' বাটনে চাপুন।"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setLineSpacing(8f, 1.2f)
            setPadding(0, 12, 0, 16)
        }

        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        val pasteAndTranslateBtn = Button(this).apply {
            text = "পেস্ট করুন"
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = 14f
                setColor(0xFF0F5161.toInt())
            }
            setOnClickListener {
                val clipText = clipboardManager?.primaryClip?.getItemAt(0)?.text?.toString()
                if (!clipText.isNullOrBlank()) {
                    currentText = clipText
                    convertedBengali = MixedScriptTokenizer.process(clipText).convertedText
                    translationText.text = convertedBengali
                } else {
                    Toast.makeText(this@LipiOverlayService, "ক্লিপবোর্ড খালি", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val copyBtn = Button(this).apply {
            text = "কপি করুন"
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = 14f
                setColor(0xFFD86B43.toInt()) // Terracotta
            }
            setOnClickListener {
                if (convertedBengali.isNotBlank()) {
                    val clip = ClipData.newPlainText("Bengali Translation", convertedBengali)
                    clipboardManager?.setPrimaryClip(clip)
                    Toast.makeText(this@LipiOverlayService, "বাংলা অনুবাদ কপি হয়েছে!", Toast.LENGTH_SHORT).show()
                    dismissTranslationPopup()
                }
            }
        }

        val spacer = View(this).apply {
            setLayoutParams(LinearLayout.LayoutParams(16, 0))
        }

        actionRow.addView(pasteAndTranslateBtn)
        actionRow.addView(spacer)
        actionRow.addView(copyBtn)

        container.addView(headerRow)
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
        clipboardManager?.removePrimaryClipChangedListener(clipboardListener)
        dismissAllOverlays()
    }
}
