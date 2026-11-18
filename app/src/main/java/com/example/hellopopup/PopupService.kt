package com.example.hellopopup

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView

/**
 * Shows a floating popup window that says "hello world".
 * Drag the title bar to move it, drag the corner grip to resize it,
 * tap the X to close it.
 */
class PopupService : Service() {

    private lateinit var wm: WindowManager
    private lateinit var lp: WindowManager.LayoutParams
    private var popup: FrameLayout? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (popup == null) showPopup()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        popup?.let { runCatching { wm.removeView(it) } }
        popup = null
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    /** Calls onMove with the finger's offset (in px) from where the touch started. */
    private fun View.onDrag(onStart: () -> Unit, onMove: (dx: Int, dy: Int) -> Unit) {
        var downX = 0f
        var downY = 0f
        setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX
                    downY = e.rawY
                    onStart()
                }
                MotionEvent.ACTION_MOVE ->
                    onMove((e.rawX - downX).toInt(), (e.rawY - downY).toInt())
            }
            true
        }
    }

    private fun showPopup() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        lp = WindowManager.LayoutParams(
            dp(280), dp(200),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(40)
            y = dp(120)
        }

        val barHeight = dp(36)
        val minW = dp(140)
        val minH = dp(100)

        val card = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(14).toFloat()
                setStroke(dp(1), Color.LTGRAY)
            }
            outlineProvider = ViewOutlineProvider.BACKGROUND
            clipToOutline = true
        }

        // The "hello world" text; it scales to fill the window as you resize.
        val label = TextView(this).apply {
            text = "hello world"
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setAutoSizeTextTypeUniformWithConfiguration(12, 120, 2, TypedValue.COMPLEX_UNIT_SP)
        }
        card.addView(
            label,
            FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT).apply { topMargin = barHeight }
        )

        // Title bar (drag to move).
        val title = TextView(this).apply {
            text = "Drag to move"
            textSize = 12f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), 0, dp(40), 0)
            setBackgroundColor(Color.parseColor("#EEEEEE"))
        }
        card.addView(title, FrameLayout.LayoutParams(MATCH_PARENT, barHeight))

        // Close button.
        val close = TextView(this).apply {
            text = "✕"
            textSize = 16f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
            setOnClickListener { stopSelf() }
        }
        card.addView(
            close,
            FrameLayout.LayoutParams(barHeight, barHeight, Gravity.TOP or Gravity.END)
        )

        // Resize grip (bottom-right corner).
        val grip = TextView(this).apply {
            text = "◢"
            textSize = 20f
            setTextColor(Color.GRAY)
            gravity = Gravity.BOTTOM or Gravity.END
            setPadding(0, 0, dp(6), dp(2))
        }
        card.addView(
            grip,
            FrameLayout.LayoutParams(dp(44), dp(44), Gravity.BOTTOM or Gravity.END)
        )

        var startX = 0
        var startY = 0
        title.onDrag({ startX = lp.x; startY = lp.y }) { dx, dy ->
            lp.x = startX + dx
            lp.y = maxOf(0, startY + dy)
            wm.updateViewLayout(card, lp)
        }

        var startW = 0
        var startH = 0
        grip.onDrag({ startW = lp.width; startH = lp.height }) { dx, dy ->
            lp.width = maxOf(minW, startW + dx)
            lp.height = maxOf(minH, startH + dy)
            wm.updateViewLayout(card, lp)
        }

        wm.addView(card, lp)
        popup = card
    }
}
