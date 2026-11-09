package app.volumebubble

import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.GestureDetector
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import kotlin.math.abs

/**
 * Hosts the floating bubble.
 *
 * The bubble is an application overlay (draw over other apps) rather than an accessibility
 * service: it needs one user-granted permission instead of full UI introspection.
 */
class BubbleService : Service() {

    companion object {
        private const val CHANNEL_ID = "bubble"
        private const val NOTIFICATION_ID = 7
        private const val ACTION_STOP = "stop"
        private const val ACTION_RELOAD = "reload"
        private const val IDLE_MS = 3000L
        private const val DIM_FACTOR = 0.45f
        private const val VISIBLE_FRACTION = 0.4f

        fun isEnabled(ctx: Context): Boolean = Prefs.enabled(ctx)

        fun canDrawOverlays(ctx: Context): Boolean = Settings.canDrawOverlays(ctx)

        fun start(ctx: Context) {
            Prefs.setEnabled(ctx, true)
            ctx.startForegroundService(Intent(ctx, BubbleService::class.java))
        }

        fun stop(ctx: Context) {
            Prefs.setEnabled(ctx, false)
            ctx.stopService(Intent(ctx, BubbleService::class.java))
        }

        fun reload(ctx: Context) {
            if (!isEnabled(ctx)) return
            ctx.startService(Intent(ctx, BubbleService::class.java).setAction(ACTION_RELOAD))
        }
    }

    private lateinit var windowManager: WindowManager
    private lateinit var params: WindowManager.LayoutParams

    private var bubble: BubbleView? = null
    private var volumeWatcher: Volume.Watcher? = null
    private var animator: ValueAnimator? = null

    private val handler = Handler(Looper.getMainLooper())
    private val idleRunnable = Runnable { onIdle() }

    private var screenWidth = 0
    private var screenHeight = 0
    private var bubbleSize = 0
    private var peeked = false

    private var downRawX = 0f
    private var downRawY = 0f
    private var grabX = 0
    private var grabY = 0
    private var dragged = false
    private var downWhilePeeked = false

    private val gestures by lazy {
        GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                if (!dragged && !downWhilePeeked) openPanel()
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                if (!dragged && !downWhilePeeked) toggleMute()
            }
        })
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WindowManager::class.java)
        readScreen()
        startForegroundNotification()
        addBubble()
        volumeWatcher = Volume.Watcher(this) { refreshVisuals() }.also { it.start() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stop(this)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_RELOAD -> applySettings()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        readScreen()
        bubble?.applyPalette()
        snapToEdge()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        animator?.cancel()
        volumeWatcher?.stop()
        volumeWatcher = null
        bubble?.let { runCatching { windowManager.removeView(it) } }
        bubble = null
        super.onDestroy()
    }

    // ------------------------------------------------------------- geometry

    private fun readScreen() {
        val bounds = windowManager.currentWindowMetrics.bounds
        screenWidth = bounds.width()
        screenHeight = bounds.height()
        bubbleSize = Ui.dp(this, Prefs.sizeDp(this))
    }

    private fun maxX() = (screenWidth - bubbleSize).coerceAtLeast(0)

    private fun maxY() = (screenHeight - bubbleSize).coerceAtLeast(0)

    private fun isRight() = params.x + bubbleSize / 2 >= screenWidth / 2

    private fun restX(right: Boolean): Int =
        if (right) screenWidth - bubbleSize - Ui.dp(this, 4) else Ui.dp(this, 4)

    private fun peekX(right: Boolean): Int =
        if (right) screenWidth - (bubbleSize * VISIBLE_FRACTION).toInt()
        else -(bubbleSize * (1 - VISIBLE_FRACTION)).toInt()

    private fun opacity() = Prefs.opacityPct(this) / 100f

    // ---------------------------------------------------------- notification

    private fun startForegroundNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notif_channel),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply { setShowBadge(false) },
            )
        }

        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, BubbleService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_volume)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setContentIntent(open)
            .addAction(
                Notification.Action.Builder(null, getString(R.string.notif_stop), stop).build(),
            )
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    // ------------------------------------------------------------------ bubble

    private fun addBubble() {
        val view = BubbleView(this)
        val savedX = Prefs.x(this)
        val savedY = Prefs.y(this)

        params = WindowManager.LayoutParams(
            bubbleSize,
            bubbleSize,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = if (savedX >= 0) savedX.coerceIn(0, maxX()) else restX(right = true)
            y = if (savedY >= 0) savedY.coerceIn(0, maxY()) else (screenHeight * 0.45f).toInt()
        }

        view.setOnTouchListener { v, event ->
            onTouch(v, event)
            gestures.onTouchEvent(event)
            true
        }

        try {
            windowManager.addView(view, params)
        } catch (_: Exception) {
            stopSelf()
            return
        }

        bubble = view
        view.alpha = opacity()
        refreshVisuals()
        scheduleIdle()
    }

    private fun applySettings() {
        val view = bubble ?: return
        readScreen()
        params.width = bubbleSize
        params.height = bubbleSize
        runCatching { windowManager.updateViewLayout(view, params) }
        view.applyPalette()
        view.alpha = opacity()
        snapToEdge()
        refreshVisuals()
        scheduleIdle()
    }

    private fun refreshVisuals() {
        val fraction = Volume.level(this).toFloat() / Volume.max(this)
        bubble?.setLevel(fraction, Volume.isMuted(this))
    }

    private fun onTouch(view: View, event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                animator?.cancel()
                downRawX = event.rawX
                downRawY = event.rawY
                grabX = params.x
                grabY = params.y
                dragged = false
                downWhilePeeked = peeked
                handler.removeCallbacks(idleRunnable)
                view.alpha = opacity()
                refreshVisuals()
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downRawX
                val dy = event.rawY - downRawY
                val slop = Ui.dpf(this, 6)
                if (!dragged && (abs(dx) > slop || abs(dy) > slop)) {
                    dragged = true
                    if (peeked) {
                        peeked = false
                        params.x = restX(isRight())
                        grabX = params.x
                        downRawX = event.rawX
                    }
                }
                if (dragged) {
                    params.x = (grabX + event.rawX - downRawX).toInt().coerceIn(0, maxX())
                    params.y = (grabY + event.rawY - downRawY).toInt().coerceIn(0, maxY())
                    runCatching { windowManager.updateViewLayout(view, params) }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (dragged) snapToEdge()
                else if (downWhilePeeked) unpeek()
                scheduleIdle()
            }
        }
    }

    // ------------------------------------------------------- idle and peeking

    private fun scheduleIdle() {
        handler.removeCallbacks(idleRunnable)
        handler.postDelayed(idleRunnable, IDLE_MS)
    }

    private fun onIdle() {
        val view = bubble ?: return
        if (Prefs.autoHide(this)) {
            peek()
        } else {
            view.animate().alpha((opacity() * DIM_FACTOR).coerceAtLeast(0.2f)).setDuration(220).start()
        }
    }

    private fun peek() {
        if (peeked) return
        peeked = true
        animateTo(peekX(isRight()))
    }

    private fun unpeek() {
        peeked = false
        animateTo(restX(isRight()))
    }

    private fun snapToEdge() {
        peeked = false
        val target = restX(isRight())
        animateTo(target)
        Prefs.savePosition(this, target, params.y)
    }

    private fun animateTo(target: Int) {
        val view = bubble ?: return
        val from = params.x
        if (from == target) return
        animator?.cancel()
        animator = ValueAnimator.ofInt(from, target).apply {
            duration = 200
            interpolator = DecelerateInterpolator(1.8f)
            addUpdateListener {
                params.x = it.animatedValue as Int
                runCatching { windowManager.updateViewLayout(view, params) }
            }
            start()
        }
    }

    // ------------------------------------------------------------------ actions

    private fun openPanel() {
        Volume.showPanel(this)
        bubble?.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        scheduleIdle()
    }

    private fun toggleMute() {
        Volume.toggleMute(this)
        refreshVisuals()
        bubble?.let { view ->
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            view.animate().scaleX(0.88f).scaleY(0.88f).setDuration(90).withEndAction {
                view.animate().scaleX(1f).scaleY(1f).setDuration(140).start()
            }.start()
        }
    }
}
