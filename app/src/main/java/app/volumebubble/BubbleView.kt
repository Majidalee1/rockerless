package app.volumebubble

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.View
import kotlin.math.roundToInt

/**
 * The floating bubble: a tonal circle, a ring that shows the media level, and a speaker glyph
 * that switches to a muted glyph at zero. Everything is drawn from the [Palette] so it follows
 * the system light/dark theme and dynamic accent color.
 */
class BubbleView(context: Context) : View(context) {

    private var palette = Palette.of(context)
    private var fraction = 0f
    private var muted = false

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val ringWidth = Ui.dpf(context, 3.5f)
    private val inset = Ui.dpf(context, 3f)
    private val arc = RectF()

    private val volumeUp: Drawable = context.getDrawable(R.drawable.ic_volume)!!
    private val volumeOff: Drawable = context.getDrawable(R.drawable.ic_mute)!!

    fun applyPalette() {
        palette = Palette.of(context)
        invalidate()
    }

    fun setLevel(fraction: Float, muted: Boolean) {
        this.fraction = fraction.coerceIn(0f, 1f)
        this.muted = muted
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val size = width.toFloat()
        val center = size / 2f
        val ringRadius = size / 2f - inset - ringWidth / 2f
        val fillRadius = ringRadius - ringWidth / 2f - Ui.dpf(context, 1.5f)

        fillPaint.color = palette.container
        canvas.drawCircle(center, center, fillRadius, fillPaint)

        arc.set(center - ringRadius, center - ringRadius, center + ringRadius, center + ringRadius)
        ringPaint.strokeWidth = ringWidth

        ringPaint.color = palette.track
        canvas.drawOval(arc, ringPaint)

        if (!muted && fraction > 0f) {
            ringPaint.color = palette.ring
            canvas.drawArc(arc, -90f, 360f * fraction, false, ringPaint)
        }

        val glyph = if (muted) volumeOff else volumeUp
        val glyphSize = fillRadius * 1.1f
        val left = (center - glyphSize / 2f).roundToInt()
        glyph.setBounds(left, left, (left + glyphSize).roundToInt(), (left + glyphSize).roundToInt())
        glyph.setTint(if (muted) palette.muted else palette.onContainer)
        glyph.draw(canvas)
    }
}
