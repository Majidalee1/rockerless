package app.volumebubble

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Brings the bubble back after a reboot, but only if the user had it on. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        if (!BubbleService.isEnabled(context)) return
        if (!BubbleService.canDrawOverlays(context)) return
        runCatching { BubbleService.start(context) }
    }
}
