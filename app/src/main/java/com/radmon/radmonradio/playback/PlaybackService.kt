/**
 * Foreground service managing radio stream playback.
 * Handles MediaSession, notifications, and listening time tracking.
 */
package com.radmon.radmonradio.playback
import android.content.Intent
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.upstream.DefaultAllocator
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.radmon.radmonradio.data.repository.ListeningStatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@UnstableApi
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var notificationManager: CustomNotificationManager? = null
    private var player: ExoPlayer? = null
    private var statsRepository: ListeningStatsRepository? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isServiceStarted = false
    private var lastVolume: Float = 1f
    private var connectivityCallback: android.net.ConnectivityManager.NetworkCallback? = null
    @Volatile
    private var isDestroyed = false
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service initialization started")
        initializeStatsRepository()
        initializeMediaPlayer()
        initializeMediaSession()
        initializeNotificationManager()
        val cm = getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val cb = object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                super.onAvailable(network)
                if (!isDestroyed) {
                    serviceScope.launch { attemptRecovery() }
                }
            }
        }
        cm.registerDefaultNetworkCallback(cb)
        connectivityCallback = cb
    }
    
    private fun initializeStatsRepository() {
        statsRepository = ListeningStatsRepository.getInstance(applicationContext)
    }
    
    private fun initializeMediaPlayer() {
        player = createConfiguredPlayer().apply {
            addListener(createPlayerListener())
            setMediaItem(createStreamMediaItem())
            playWhenReady = true
            prepare()
            if (volume > 0f) lastVolume = volume
        }
    }
    
    private fun createConfiguredPlayer(): ExoPlayer {
        val loadControl = DefaultLoadControl.Builder()
            .setAllocator(DefaultAllocator(true, ALLOCATOR_TRIM_THRESHOLD))
            .setBufferDurationsMs(
                MIN_BUFFER_MS,
                MAX_BUFFER_MS,
                BUFFER_FOR_PLAYBACK_MS,
                BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .setTargetBufferBytes(C.LENGTH_UNSET)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
        return ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }
    
    private fun createPlayerListener(): Player.Listener {
        return object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "Playback error encountered: ${error.message}", error)
                handlePlaybackError(error)
            }
            override fun onPlaybackStateChanged(@Player.State playbackState: Int) {
                logPlaybackStateChange(playbackState)
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                handlePlaybackStateForStats(isPlaying)
                player?.let { if (it.volume > 0f) lastVolume = it.volume }
            }
        }
    }
    
    private fun handlePlaybackStateForStats(isPlaying: Boolean) {
        serviceScope.launch {
            if (isPlaying) {
                statsRepository?.recordSessionStart()
                Log.d(TAG, "Session started")
            } else {
                statsRepository?.recordSessionEnd()
                Log.d(TAG, "Session ended")
            }
        }
    }
    
    private fun createStreamMediaItem(): MediaItem {
        return MediaItem.Builder()
            .setUri(STREAM_URL)
            .setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder()
                    .setMaxPlaybackSpeed(MAX_LIVE_PLAYBACK_SPEED)
                    .build()
            )
            .build()
    }
    
    private fun initializeMediaSession() {
        val contentIntent = android.app.PendingIntent.getActivity(
            this,
            0,
            android.content.Intent(this, com.radmon.radmonradio.MainActivity::class.java),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        mediaSession = MediaSession.Builder(this, requireNotNull(player))
            .setSessionActivity(contentIntent)
            .build()
    }
    
    private fun initializeNotificationManager() {
notificationManager = CustomNotificationManager(
            this,
            requireNotNull(player),
            requireNotNull(mediaSession)
        )
    }
    
    private fun handlePlaybackError(error: PlaybackException) {
        when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_TIMEOUT -> {
                attemptRecovery()
            }
            else -> {
                Log.e(TAG, "Non-recoverable error: ${error.errorCode}")
            }
        }
    }
    
    private fun attemptRecovery() {
        if (isDestroyed) {
            Log.w(TAG, "Skipping recovery - service destroyed")
            return
        }
        Log.w(TAG, "Attempting playback recovery")
        player?.apply {
            try {
                stop()
                prepare()
                play()
            } catch (e: Exception) {
                Log.e(TAG, "Recovery failed", e)
            }
        }
    }
    
    private fun logPlaybackStateChange(@Player.State state: Int) {
        val stateDescription = when (state) {
            Player.STATE_IDLE -> "IDLE"
            Player.STATE_BUFFERING -> "BUFFERING"
            Player.STATE_READY -> "READY"
            Player.STATE_ENDED -> "ENDED"
            else -> "UNKNOWN"
        }
        Log.d(TAG, "Playback state: $stateDescription")
    }
    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        super.onStartCommand(intent, flags, startId)

        isServiceStarted = true
        handleIntentAction(intent?.action)

        return START_STICKY
    }
    
    private fun handleIntentAction(action: String?) {
        when (action) {
            ACTION_PLAY -> {
                Log.d(TAG, "Play action received")
                mediaSession?.player?.play()
            }
            ACTION_PAUSE -> {
                Log.d(TAG, "Pause action received")
                mediaSession?.player?.pause()
            }
            ACTION_TOGGLE_MUTE -> {
                val p = mediaSession?.player
                if (p != null) {
                    if (p.volume == 0f) {
                        p.volume = if (lastVolume > 0f) lastVolume else 1f
                    } else {
                        lastVolume = p.volume
                        p.volume = 0f
                    }
                }
            }
        }
    }
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }
    override fun onTaskRemoved(rootIntent: Intent?) {
        handleTaskRemoval()
    }
    
    private fun handleTaskRemoval() {
        val shouldStopService = mediaSession?.player?.playWhenReady == false
        if (shouldStopService) {
            stopSelf()
        }
    }
    override fun onDestroy() {
        Log.d(TAG, "Service cleanup initiated")
        isDestroyed = true
        val cm = getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        connectivityCallback?.let { runCatching { cm.unregisterNetworkCallback(it) } }
        connectivityCallback = null
        releaseResources()
        super.onDestroy()
    }
    
    private fun releaseResources() {
        recordFinalSessionEnd()
        releaseNotificationManager()
        releaseMediaSession()
        releaseMediaPlayer()
        releaseCoroutineScope()
    }
    
    private fun recordFinalSessionEnd() {
        try {
            kotlinx.coroutines.runBlocking {
                statsRepository?.recordSessionEnd()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record final session", e)
        }
    }
    
    private fun releaseCoroutineScope() {
        serviceScope.cancel()
    }
    
    private fun releaseNotificationManager() {
notificationManager?.hideNotification()
notificationManager = null
    }
    
    private fun releaseMediaSession() {
        mediaSession?.run {
            release()
        }
        mediaSession = null
    }
    
    private fun releaseMediaPlayer() {
        player?.release()
        player = null
    }
    companion object {
        private const val TAG = "PlaybackService"
        private const val ACTION_PLAY = "com.radmon.radmonradio.PLAY"
        private const val ACTION_PAUSE = "com.radmon.radmonradio.PAUSE"
        const val ACTION_TOGGLE_MUTE = "com.radmon.radmonradio.TOGGLE_MUTE"
        private const val STREAM_URL = "https://radio-monash.radiocult.fm/stream"
        private const val ALLOCATOR_TRIM_THRESHOLD = 16
        private const val MIN_BUFFER_MS = 15000
        private const val MAX_BUFFER_MS = 50000
        private const val BUFFER_FOR_PLAYBACK_MS = 2500
        private const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 5000
        private const val MAX_LIVE_PLAYBACK_SPEED = 1.02f
    }
}
