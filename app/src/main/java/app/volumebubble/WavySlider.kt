package app.volumebubble

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * A Material 3 Expressive slider: the active part is a wavy line that ripples while you drag,
 * and the inactive part is a straight line. The thumb is a rounded bar.
 */
class WavySlider(context: Context) : View(context) {

    var min = 0
    var max = 100

    var value = 0
        set(newValue) {
            field = newValue.coerceIn(min, max)
            invalidate()
        }

    var onValueChange: ((Int) -> Unit)? = null
    var onRelease: (() -> Unit)? = null

    private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val inactivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val path = Path()

    private val stroke = Ui.dpf(context, 5f)
    private val waveAmplitude = Ui.dpf(context, 3f)
    private val wavelength = Ui.dpf(context, 22f)
    private val pad = Ui.dpf(context, 14f)
    private val thumbWidth = Ui.dpf(context, 4f)
    private val thumbHeight = Ui.dpf(context, 28f)
    private val thumbGap = Ui.dpf(context, 5f)

    private var phase = 0f

    private val wave = ValueAnimator.ofFloat(0f, 2f * PI.toFloat()).apply {
        duration = 1100L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            phase = it.animatedValue as Float
            invalidate()
        }
    }
    private val settle = Runnable { wave.cancel() }

    fun setColors(active: Int, inactive: Int, thumb: Int) {
        activePaint.color = active
        inactivePaint.color = inactive
        thumbPaint.color = thumb
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            MeasureSpec.getSize(widthMeasureSpec),
            Ui.dp(context, 48),
        )
    }

    override fun onDraw(canvas: Canvas) {
        val y = height / 2f
        val left = pad
        val right = width - pad
        val fraction = if (max > min) (value - min).toFloat() / (max - min) else 0f
        val x = left + (right - left) * fraction

        inactivePaint.strokeWidth = stroke
        if (right > x + thumbWidth / 2f + thumbGap) {
            canvas.drawLine(x + thumbWidth / 2f + thumbGap, y, right, y, inactivePaint)
        }

        if (x > left) {
            path.rewind()
            val steps = maxOf(8, ((x - left) / 2f).toInt())
            for (i in 0..steps) {
                val px = left + (x - left) * i / steps
                val py = y + waveAmplitude * sin(2f * PI.toFloat() * (px - left) / wavelength + phase)
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            activePaint.strokeWidth = stroke
            canvas.drawPath(path, activePaint)
        }

        canvas.drawRoundRect(
            x - thumbWidth / 2f,
            y - thumbHeight / 2f,
            x + thumbWidth / 2f,
            y + thumbHeight / 2f,
            thumbWidth / 2f,
            thumbWidth / 2f,
            thumbPaint,
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                removeCallbacks(settle)
                if (!wave.isStarted) wave.start()
                setFrom(event.x)
            }
            MotionEvent.ACTION_MOVE -> setFrom(event.x)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                onRelease?.invoke()
                postDelayed(settle, 700)
            }
        }
        return true
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(settle)
        wave.cancel()
        super.onDetachedFromWindow()
    }

    private fun setFrom(touchX: Float) {
        val left = pad
        val right = width - pad
        if (right <= left) return
        val fraction = ((touchX - left) / (right - left)).coerceIn(0f, 1f)
        val newValue = min + (fraction * (max - min)).roundToInt()
        if (newValue != value) {
            value = newValue
            onValueChange?.invoke(newValue)
        }
    }
}
