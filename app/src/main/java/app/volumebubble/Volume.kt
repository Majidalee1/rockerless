package app.volumebubble

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.widget.Toast

object Volume {

    private const val PREFS = "bubble_prefs"
    private const val KEY_LAST_LEVEL = "last_media_level"
    private const val STREAM = AudioManager.STREAM_MUSIC
    private const val WATCH_INTERVAL_MS = 400L

    fun isMuted(ctx: Context): Boolean =
        audio(ctx).getStreamVolume(STREAM) == 0

    fun toggleMute(ctx: Context): Boolean {
        val am = audio(ctx)
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val muted = am.getStreamVolume(STREAM) != 0

        if (muted) {
            prefs.edit().putInt(KEY_LAST_LEVEL, am.getStreamVolume(STREAM)).apply()
            am.setStreamVolume(STREAM, 0, 0)
        } else {
            val restore = prefs.getInt(KEY_LAST_LEVEL, 0).takeIf { it > 0 }
                ?: (am.getStreamMaxVolume(STREAM) / 3).coerceAtLeast(1)
            am.setStreamVolume(STREAM, restore, 0)
        }

        val message = if (muted) R.string.toast_muted else R.string.toast_unmuted
        Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
        return muted
    }

    fun level(ctx: Context): Int = audio(ctx).getStreamVolume(STREAM)

    fun max(ctx: Context): Int = audio(ctx).getStreamMaxVolume(STREAM).coerceAtLeast(1)

    fun showPanel(ctx: Context) {
        audio(ctx).adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
    }

    /**
     * Calls [onChanged] whenever the media level changes, however it changed (volume keys,
     * the system panel, another app). Android doesn't broadcast this to apps, so it is polled.
     */
    class Watcher(private val ctx: Context, private val onChanged: () -> Unit) {

        private val handler = Handler(Looper.getMainLooper())
        private var last = -1

        private val tick = object : Runnable {
            override fun run() {
                val level = audio(ctx).getStreamVolume(STREAM)
                if (level != last) {
                    last = level
                    onChanged()
                }
                handler.postDelayed(this, WATCH_INTERVAL_MS)
            }
        }

        fun start() {
            handler.removeCallbacks(tick)
            last = -1
            handler.post(tick)
        }

        fun stop() {
            handler.removeCallbacks(tick)
        }
    }

    private fun audio(ctx: Context) = ctx.getSystemService(AudioManager::class.java)
}
