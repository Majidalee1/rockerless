package app.volumebubble

import android.content.Context

/** All app settings live in one file, so the bubble service and the setup screen always agree. */
object Prefs {

    private const val NAME = "bubble_prefs"
    private const val KEY_ENABLED = "bubble_enabled"
    private const val KEY_X = "bubble_x"
    private const val KEY_Y = "bubble_y"
    private const val KEY_SIZE = "size_dp"
    private const val KEY_OPACITY = "opacity_pct"
    private const val KEY_AUTO_HIDE = "auto_hide"

    const val DEFAULT_SIZE_DP = 56
    const val DEFAULT_OPACITY = 100

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun enabled(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_ENABLED, false)

    fun setEnabled(ctx: Context, value: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_ENABLED, value).apply()
    }

    fun x(ctx: Context): Int = prefs(ctx).getInt(KEY_X, -1)

    fun y(ctx: Context): Int = prefs(ctx).getInt(KEY_Y, -1)

    fun savePosition(ctx: Context, x: Int, y: Int) {
        prefs(ctx).edit().putInt(KEY_X, x).putInt(KEY_Y, y).apply()
    }

    fun sizeDp(ctx: Context): Int = prefs(ctx).getInt(KEY_SIZE, DEFAULT_SIZE_DP)

    fun setSizeDp(ctx: Context, value: Int) {
        prefs(ctx).edit().putInt(KEY_SIZE, value).apply()
    }

    fun opacityPct(ctx: Context): Int = prefs(ctx).getInt(KEY_OPACITY, DEFAULT_OPACITY)

    fun setOpacityPct(ctx: Context, value: Int) {
        prefs(ctx).edit().putInt(KEY_OPACITY, value).apply()
    }

    fun autoHide(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_AUTO_HIDE, false)

    fun setAutoHide(ctx: Context, value: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_AUTO_HIDE, value).apply()
    }
}
