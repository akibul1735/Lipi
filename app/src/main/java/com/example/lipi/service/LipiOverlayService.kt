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
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.lipi.engine.MixedScriptTokenizer

/**
 * Native Android Floating Overlay Service.
 * Manages the floating action badge positioned near detected Banglish messages,
 * and handles the tap-to-expand translation popover.
 */
class LipiOverlayService : Service() {

    companion object {
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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
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
            .setContentText("মেসেজিং অ্যাপে বাংলিশ বার্তা শনাক্ত করার জন্য প্রস্তুত")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        if (intent.action == ACTION_DISMISS) {
            dismissAllOverlays()
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent.action == ACTION_SHOW_BADGE) {
            val text = intent.getStringExtra(EXTRA_DETECTED_TEXT) ?: return START_NOT_STICKY
            val x = intent.getIntExtra(EXTRA_BOUNDS_X, 300)
            val y = intent.getIntExtra(EXTRA_BOUNDS_Y, 400)
            handleDetectedText(text, x, y)
        }

        return START_STICKY
    }

    private fun handleDetectedText(text: String, posX: Int, posY: Int) {
        if (!Settings.canDrawOverlays(this)) {
            return
        }

        currentText = text
        val conversion = MixedScriptTokenizer.process(text)
        convertedBengali = conversion.convertedText

        showFloatingBadge(posX, posY)
    }

    private fun showFloatingBadge(x: Int, y: Int) {
        if (floatingBadgeView != null) {
            return // Badge already active
        }

        val dpSize = (44 * resources.displayMetrics.density).toInt()
        val layoutParams = WindowManager.LayoutParams(
            dpSize,
            dpSize,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x.coerceIn(50, resources.displayMetrics.widthPixels - dpSize - 20)
            this.y = y.coerceIn(100, resources.displayMetrics.heightPixels - dpSize - 100)
        }

        val badge = TextView(this).apply {
            text = "ল"
            textSize = 20f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFF0F5161.toInt()) // Padma Teal
                setStroke((2 * resources.displayMetrics.density).toInt(), 0xFF00A896.toInt())
            }
            elevation = 12f
        }

        // Click on badge opens the translation popup
        badge.setOnClickListener {
            showTranslationPopup(layoutParams.x, layoutParams.y)
        }

        floatingBadgeView = badge
        try {
            windowManager?.addView(floatingBadgeView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showTranslationPopup(anchorX: Int, anchorY: Int) {
        if (translationPopupView != null) return

        val width = (300 * resources.displayMetrics.density).toInt()
        val layoutParams = WindowManager.LayoutParams(
            width,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = (anchorX - width + 60).coerceIn(20, resources.displayMetrics.widthPixels - width - 20)
            this.y = (anchorY + 60).coerceIn(100, resources.displayMetrics.heightPixels - 300)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
            background = GradientDrawable().apply {
                cornerRadius = 24f
                setColor(0xFF0C1518.toInt()) // Deep surface
                setStroke(2, 0xFF00A896.toInt())
            }
            elevation = 16f
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(this).apply {
            text = "লিপি অনুবাদ (বাংলা)"
            setTextColor(0xFF57DFCE.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setLayoutParams(LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }

        val closeBtn = Button(this).apply {
            text = "✕"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener {
                dismissTranslationPopup()
            }
        }

        headerRow.addView(title)
        headerRow.addView(closeBtn)

        val translationText = TextView(this).apply {
            text = convertedBengali
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setLineSpacing(8f, 1.2f)
            setPadding(0, 12, 0, 16)
        }

        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        val copyBtn = Button(this).apply {
            text = "কপি করুন"
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = 16f
                setColor(0xFFD86B43.toInt()) // Terracotta
            }
            setOnClickListener {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Bengali Translation", convertedBengali)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this@LipiOverlayService, "বাংলা লেখা কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                dismissTranslationPopup()
            }
        }

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
        dismissAllOverlays()
    }
}
