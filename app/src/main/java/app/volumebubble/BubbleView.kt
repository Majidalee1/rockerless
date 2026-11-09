package app.volumebubble

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.Drawable
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The floating bubble, in Material 3 Expressive style: a nine-lobed cookie container, a wavy
 * level ring, and a speaker glyph. The wave and the cookie ripple briefly whenever the volume
 * changes or the bubble is touched, then settle into a still shape.
 */
class BubbleView(context: Context) : View(context) {

    private var palette = Palette.of(context)
    private var fraction = 0f
    private var muted = false
    private var phase = 0f

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val levelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val cookiePath = Path()
    private val levelPath = Path()

    private val trackWidth = Ui.dpf(context, 2f)
    private val levelWidth = Ui.dpf(context, 4f)
    private val waveAmplitude = Ui.dpf(context, 2.2f)
    private val gap = Ui.dpf(context, 2f)

    private val volumeUp: Drawable = context.getDrawable(R.drawable.ic_volume)!!
    private val volumeOff: Drawable = context.getDrawable(R.drawable.ic_mute)!!

    private val spin = ValueAnimator.ofFloat(0f, 2f * PI.toFloat()).apply {
        duration = 1600L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            phase = it.animatedValue as Float
            invalidate()
        }
    }
    private val settle = Runnable { spin.cancel() }

    fun applyPalette() {
        palette = Palette.of(context)
        invalidate()
    }

    fun setLevel(fraction: Float, muted: Boolean) {
        this.fraction = fraction.coerceIn(0f, 1f)
        this.muted = muted
        pulse()
        invalidate()
    }

    /** Ripples the wave and the cookie for a moment, then lets them rest. */
    fun pulse() {
        if (!spin.isStarted) spin.start()
        removeCallbacks(settle)
        postDelayed(settle, 1400)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(settle)
        spin.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val size = width.toFloat()
        val center = size / 2f
        val ringRadius = center - waveAmplitude - levelWidth / 2f - Ui.dpf(context, 2f)
        val cookieRadius = ringRadius - waveAmplitude - levelWidth / 2f - gap

        canvas.save()
        canvas.rotate(phase * 180f / PI.toFloat() * 0.5f, center, center)
        drawCookie(canvas, center, cookieRadius)
        canvas.restore()

        trackPaint.strokeWidth = trackWidth
        trackPaint.color = palette.track
        canvas.drawCircle(center, center, ringRadius, trackPaint)

        if (!muted && fraction > 0f) {
            levelPaint.strokeWidth = levelWidth
            levelPaint.color = palette.ring
            buildWavyArc(center, ringRadius, -PI.toFloat() / 2f, 2f * PI.toFloat() * fraction)
            canvas.drawPath(levelPath, levelPaint)
        }

        val glyph = if (muted) volumeOff else volumeUp
        val glyphSize = (cookieRadius * 1.05f).roundToInt()
        val left = (center - glyphSize / 2f).roundToInt()
        glyph.setBounds(left, left, left + glyphSize, left + glyphSize)
        glyph.setTint(if (muted) palette.muted else palette.onContainer)
        glyph.draw(canvas)
    }

    private fun drawCookie(canvas: Canvas, center: Float, radius: Float) {
        cookiePath.rewind()
        val steps = 180
        for (i in 0..steps) {
            val t = i.toFloat() / steps * 2f * PI.toFloat()
            val r = radius * (1f + COOKIE_DEPTH * cos(COOKIE_LOBES * t))
            val x = center + r * cos(t)
            val y = center + r * sin(t)
            if (i == 0) cookiePath.moveTo(x, y) else cookiePath.lineTo(x, y)
        }
        cookiePath.close()
        fillPaint.color = palette.container
        canvas.drawPath(cookiePath, fillPaint)
    }

    private fun buildWavyArc(center: Float, radius: Float, start: Float, sweep: Float) {
        levelPath.rewind()
        val steps = max(16, (sweep * 40f).toInt())
        for (i in 0..steps) {
            val a = start + sweep * i / steps
            val r = radius + waveAmplitude * sin(WAVES * a + phase)
            val x = center + r * cos(a)
            val y = center + r * sin(a)
            if (i == 0) levelPath.moveTo(x, y) else levelPath.lineTo(x, y)
        }
    }

    private companion object {
        const val COOKIE_LOBES = 9f
        const val COOKIE_DEPTH = 0.05f
        const val WAVES = 8f
    }
}
