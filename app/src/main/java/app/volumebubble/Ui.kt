package app.volumebubble

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.widget.TextView
import kotlin.math.roundToInt

object Ui {

    fun dp(ctx: Context, v: Number): Int =
        (v.toFloat() * ctx.resources.displayMetrics.density).roundToInt()

    fun dpf(ctx: Context, v: Number): Float =
        v.toFloat() * ctx.resources.displayMetrics.density

    fun card(ctx: Context, radiusDp: Float, fill: Int): GradientDrawable =
        GradientDrawable().apply {
            cornerRadius = dpf(ctx, radiusDp)
            setColor(fill)
        }

    fun pill(ctx: Context, fill: Int): GradientDrawable =
        GradientDrawable().apply {
            cornerRadius = dpf(ctx, 100f)
            setColor(fill)
        }

    fun ripple(content: Drawable, color: Int): RippleDrawable =
        RippleDrawable(ColorStateList.valueOf(color), content, null)

    fun text(
        ctx: Context,
        value: CharSequence,
        sizeSp: Float,
        color: Int,
        weight: Int = Typeface.NORMAL,
    ): TextView = TextView(ctx).apply {
        text = value
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setTextColor(color)
        includeFontPadding = false
        typeface = Typeface.create("sans-serif", weight)
    }
}
