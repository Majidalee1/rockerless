package app.volumebubble

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * Quick Settings tile. Tap opens the system volume panel; the tile mirrors the current mute state.
 */
class MuteTile : TileService() {

    private val watcher by lazy { Volume.Watcher(this) { refresh() } }

    override fun onStartListening() {
        refresh()
        watcher.start()
    }

    override fun onStopListening() {
        watcher.stop()
    }

    override fun onClick() {
        Volume.showPanel(this)
    }

    private fun refresh() {
        val tile = qsTile ?: return
        val muted = Volume.isMuted(this)
        tile.icon = Icon.createWithResource(
            this,
            if (muted) R.drawable.ic_mute else R.drawable.ic_volume,
        )
        tile.label = getString(R.string.tile_label)
        tile.subtitle = getString(if (muted) R.string.tile_muted else R.string.tile_on)
        tile.state = if (muted) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
