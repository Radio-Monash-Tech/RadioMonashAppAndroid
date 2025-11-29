/**
 * Quick Settings tile for controlling radio playback.
 * Allows play/pause directly from Android Quick Settings panel.
 */
package com.radmon.radmonradio.qs
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.radmon.radmonradio.R
import com.radmon.radmonradio.playback.PlaybackService
class RadioTileService : TileService() {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            label = getString(R.string.app_name)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                subtitle = getString(R.string.playing_on_radmon)
            }
            updateTileUi(isPlaying = false)
        }
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync().also { future ->
            future.addListener({
                try {
                    controller = future.get()
                    val isPlaying = controller?.isPlaying == true
                    qsTile?.apply { updateTileUi(isPlaying) }
                } catch (_: Exception) {  }
            }, ContextCompat.getMainExecutor(this))
        }
    }
    override fun onClick() {
        super.onClick()
        val isPlaying = controller?.isPlaying == true
        val action = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
        startForegroundService(Intent(this, PlaybackService::class.java).setAction(action))
        qsTile?.apply { updateTileUi(!isPlaying) }
    }
    override fun onStopListening() {
        super.onStopListening()
        controller?.release()
        controller = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
    }
    private fun Tile.updateTileUi(isPlaying: Boolean) {
        state = if (isPlaying) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        icon = Icon.createWithResource(
            this@RadioTileService,
            if (isPlaying) R.drawable.ic_custom_pause else R.drawable.ic_custom_play
        )
        updateTile()
    }
    companion object {
        private const val ACTION_PLAY = "com.radmon.radmonradio.PLAY"
        private const val ACTION_PAUSE = "com.radmon.radmonradio.PAUSE"
    }
}
