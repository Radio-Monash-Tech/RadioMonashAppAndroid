/**
 * Manages media notifications for radio playback.
 * Creates and updates notifications with track artwork and playback controls.
 */
package com.radmon.radmonradio.playback
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.core.app.NotificationCompat
import android.os.Handler
import android.os.Looper
import androidx.core.app.ServiceCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaStyleNotificationHelper
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import com.radmon.radmonradio.MainActivity
import com.radmon.radmonradio.R

@UnstableApi
class CustomNotificationManager(
    private val service: MediaSessionService,
    private val player: Player,
    private val session: MediaSession
) : Player.Listener {
    private val systemNotificationManager: NotificationManager =
        service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    init {
        initializeNotificationInfrastructure()
    }
    
    private fun initializeNotificationInfrastructure() {
        createNotificationChannel()
        registerPlayerListener()
        displayInitialNotification()
    }
    
    private fun registerPlayerListener() {
        player.addListener(this)
    }
    
    private fun displayInitialNotification() {
        updateNotification()
    }
    
    fun hideNotification() {
        isDestroyed = true
        unregisterPlayerListener()
        dismissNotification()
        cancelPauseTimeout()
        recycleBitmap()
    }
    
    private fun recycleBitmap() {
        currentLargeIcon?.let {
            if (!it.isRecycled) {
                try {
                    it.recycle()
                } catch (e: Exception) {
                }
            }
        }
        currentLargeIcon = null
    }
    
    private fun unregisterPlayerListener() {
        player.removeListener(this)
    }
    
    private fun dismissNotification() {
        systemNotificationManager.cancel(NOTIFICATION_ID)
    }
    
    private fun terminateService() {
        service.stopSelf()
    }
    
    override fun onEvents(player: Player, events: Player.Events) {
        if (shouldUpdateNotification(events)) {
            updateNotification()
        }
    }
    
    private fun shouldUpdateNotification(events: Player.Events): Boolean {
        return events.containsAny(
            Player.EVENT_MEDIA_METADATA_CHANGED,
            Player.EVENT_IS_PLAYING_CHANGED
        )
    }
    
    @SuppressLint("MissingPermission")
    private var currentLargeIcon: Bitmap? = null
    private var currentAccentColor: Int = DEFAULT_ACCENT
    @Volatile
    private var timeoutHandler: Handler? = null
    private var timeoutRunnable: Runnable? = null
    @Volatile
    private var isDestroyed: Boolean = false

    private fun updateNotification() {
        val metadata = player.mediaMetadata
        val isPlaying = player.isPlaying
        val notification = buildNotification(isPlaying, metadata.title?.toString(), metadata.artist?.toString(), currentLargeIcon)
        val artworkUrl = metadata.artworkUri?.toString()
        if (artworkUrl != null) {
            loadAndCacheArtwork(artworkUrl, isPlaying)
        } else {
            currentLargeIcon = null
            currentAccentColor = DEFAULT_ACCENT
        }
        manageForegroundServiceState(notification, isPlaying)
    }
    
    private fun buildNotification(
        isPlaying: Boolean,
        title: String?,
        artist: String?,
        largeIcon: Bitmap?
    ): Notification {
        val builder = NotificationCompat.Builder(service, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_custom_radio_statusbar)
            .setContentTitle(title ?: DEFAULT_TITLE)
            .setContentText(artist ?: DEFAULT_ARTIST)
            .setContentIntent(createContentIntent())
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setShowWhen(false)
            .setColorized(true)
            .setColor(currentAccentColor)
            .setStyle(
                MediaStyleNotificationHelper.MediaStyle(session)
                    .setShowActionsInCompactView(0, 1)
            )
        largeIcon?.let { builder.setLargeIcon(it) }
        val playPauseAction = if (isPlaying) {
            NotificationCompat.Action(
                R.drawable.ic_custom_pause,
                service.getString(R.string.play_pause_button),
                createPlayPauseIntent(true)
            )
        } else {
            NotificationCompat.Action(
                R.drawable.ic_custom_play,
                service.getString(R.string.play_pause_button),
                createPlayPauseIntent(false)
            )
        }
        val isMuted = player.volume == 0f
        val muteAction = NotificationCompat.Action(
            if (isMuted) R.drawable.ic_vol_up else R.drawable.ic_vol_off,
            if (isMuted) "Unmute" else "Mute",
            createToggleMuteIntent()
        )
        builder.addAction(playPauseAction)
        builder.addAction(muteAction)
        return builder.build()
    }
    
    @SuppressLint("MissingPermission")
    private fun loadAndCacheArtwork(artworkUrl: String, isPlaying: Boolean) {
        val request = ImageRequest.Builder(service)
            .data(artworkUrl)
            .allowHardware(false)
            .target(
                onSuccess = { result ->
                    val bmp = (result as BitmapDrawable).bitmap
                    currentLargeIcon?.let { oldBitmap ->
                        if (!oldBitmap.isRecycled && oldBitmap != bmp) {
                            try {
                                oldBitmap.recycle()
                            } catch (e: Exception) {
                            }
                        }
                    }
                    currentLargeIcon = bmp
                    Palette.from(bmp).generate { palette ->
                        if (isDestroyed) return@generate
                        currentAccentColor = palette?.getVibrantColor(DEFAULT_ACCENT)
                            ?: palette?.getDominantColor(DEFAULT_ACCENT)
                            ?: DEFAULT_ACCENT
                        val metadata = player.mediaMetadata
                        val notif = buildNotification(
                            isPlaying,
                            metadata?.title?.toString(),
                            metadata?.artist?.toString(),
                            currentLargeIcon
                        )
                        try {
                            systemNotificationManager.notify(NOTIFICATION_ID, notif)
                        } catch (e: Exception) {
                        }
                    }
                }
            )
            .build()
        service.imageLoader.enqueue(request)
    }
    
    private fun createPlayPauseIntent(isPlaying: Boolean): PendingIntent {
        val action = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
        val intent = Intent(service, PlaybackService::class.java).setAction(action)
        return PendingIntent.getForegroundService(
            service,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    private fun createToggleMuteIntent(): PendingIntent {
        val intent = Intent(service, PlaybackService::class.java).setAction(PlaybackService.ACTION_TOGGLE_MUTE)
        return PendingIntent.getForegroundService(
            service,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    private fun createContentIntent(): PendingIntent {
        val intent = Intent(service, MainActivity::class.java)
        return PendingIntent.getActivity(
            service,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    @SuppressLint("MissingPermission")
    private fun manageForegroundServiceState(
        notification: Notification,
        isPlaying: Boolean
    ) {
        systemNotificationManager.notify(
            NOTIFICATION_ID,
            notification
        )

        if (isPlaying) {
            cancelPauseTimeout()
        } else {
            schedulePauseTimeout()
        }
    }


    private fun schedulePauseTimeout() {
        cancelPauseTimeout()
        val handler = Handler(service.mainLooper)
        val runnable = Runnable {
            if (isDestroyed) return@Runnable
            if (player.isPlaying == false) {
                try {
                    systemNotificationManager.cancel(NOTIFICATION_ID)
                } catch (e: Exception) {
                }
            }
        }
        timeoutHandler = handler
        timeoutRunnable = runnable
        handler.postDelayed(runnable, PAUSE_TIMEOUT_MS)
    }
    private fun cancelPauseTimeout() {
        val h = timeoutHandler
        val r = timeoutRunnable
        if (h != null && r != null) h.removeCallbacks(r)
        timeoutHandler = null
        timeoutRunnable = null
    }
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            service.getString(R.string.playback_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = CHANNEL_DESCRIPTION
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        systemNotificationManager.createNotificationChannel(channel)
    }
    companion object {
        private const val NOTIFICATION_ID = 101
        private const val CHANNEL_ID = "radmon_radio_playback_channel"
        private const val CHANNEL_DESCRIPTION = "Media playback controls for Radmon Radio"
        private const val ACTION_PLAY = "com.radmon.radmonradio.PLAY"
        private const val ACTION_PAUSE = "com.radmon.radmonradio.PAUSE"
        private const val DEFAULT_TITLE = "Radio Monash"
        private const val DEFAULT_ARTIST = "Live Stream"
        private const val DEFAULT_ACCENT = 0xFF1DB954.toInt()
        private const val PAUSE_TIMEOUT_MS = 5 * 60 * 1000L
    }
}
