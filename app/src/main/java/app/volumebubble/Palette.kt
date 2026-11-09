package app.volumebubble

import android.content.Context
import android.content.res.Configuration
import android.os.Build

/**
 * Colors that follow the system theme. On Android 12+ these come from the wallpaper-based
 * dynamic palette; older versions use Material 3 baseline colors.
 */
data class Palette(
    val surface: Int,
    val card: Int,
    val onSurface: Int,
    val onSurfaceDim: Int,
    val outline: Int,
    val container: Int,
    val onContainer: Int,
    val ring: Int,
    val track: Int,
    val muted: Int,
) {
    companion object {
        fun of(ctx: Context): Palette {
            val night = (ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
            val dynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

            fun c(res: Int, fallback: Int): Int = if (dynamic) ctx.getColor(res) else fallback

            return if (night) {
                val onContainer = c(android.R.color.system_accent1_100, 0xFFEADDFF.toInt())
                Palette(
                    surface = c(android.R.color.system_neutral1_900, 0xFF1C1B1F.toInt()),
                    card = c(android.R.color.system_neutral1_800, 0xFF2B2930.toInt()),
                    onSurface = c(android.R.color.system_neutral1_100, 0xFFE6E1E5.toInt()),
                    onSurfaceDim = c(android.R.color.system_neutral2_300, 0xFFCAC4D0.toInt()),
                    outline = c(android.R.color.system_neutral2_500, 0xFF938F99.toInt()),
                    container = c(android.R.color.system_accent1_800, 0xFF4F378B.toInt()),
                    onContainer = onContainer,
                    ring = c(android.R.color.system_accent1_200, 0xFFD0BCFF.toInt()),
                    track = onContainer.withAlpha(0x40),
                    muted = 0xFFFFB4AB.toInt(),
                )
            } else {
                val onContainer = c(android.R.color.system_accent1_900, 0xFF21005D.toInt())
                Palette(
                    surface = c(android.R.color.system_neutral1_50, 0xFFFEF7FF.toInt()),
                    card = c(android.R.color.system_neutral1_100, 0xFFECE6F0.toInt()),
                    onSurface = c(android.R.color.system_neutral1_900, 0xFF1C1B1F.toInt()),
                    onSurfaceDim = c(android.R.color.system_neutral2_700, 0xFF49454F.toInt()),
                    outline = c(android.R.color.system_neutral2_400, 0xFF79747E.toInt()),
                    container = c(android.R.color.system_accent1_100, 0xFFEADDFF.toInt()),
                    onContainer = onContainer,
                    ring = c(android.R.color.system_accent1_600, 0xFF6750A4.toInt()),
                    track = onContainer.withAlpha(0x33),
                    muted = 0xFFBA1A1A.toInt(),
                )
            }
        }
    }
}

fun Int.withAlpha(alpha: Int): Int = (this and 0x00FFFFFF) or (alpha shl 24)
