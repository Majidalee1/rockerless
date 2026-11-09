package app.volumebubble

import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator

/** Expressive motion helpers. Springy presses that overshoot back, instead of flat fades. */
object Motion {

    /** Press-down squish with an overshooting release. Clicks still fire. */
    fun springPress(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> squish(v, 0.92f)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> release(v)
            }
            false
        }
    }

    fun squish(view: View, scale: Float) {
        view.animate()
            .scaleX(scale)
            .scaleY(scale)
            .setDuration(120)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    fun release(view: View) {
        view.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(380)
            .setInterpolator(OvershootInterpolator(2.2f))
            .start()
    }
}
