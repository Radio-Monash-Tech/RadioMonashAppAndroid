@file:OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    UnstableApi::class
)
@file:Suppress("OPT_IN_ARGUMENT_IS_NOT_MARKER")
/**
 * Main activity hosting the radio app UI.
 * Displays now playing screen with navigation to favorites and statistics.
 */
package com.radmon.radmonradio
import android.content.ComponentName
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.compose.rememberNavController
import com.google.common.util.concurrent.ListenableFuture
import com.radmon.radmonradio.playback.PlaybackService
import com.radmon.radmonradio.ui.theme.RadmonradioTheme
import com.radmon.radmonradio.util.Perf

@UnstableApi
class MainActivity : ComponentActivity() {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val radioViewModel: RadioViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        configureEdgeToEdgeDisplay()
        Perf.init(applicationContext)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val sm = getSystemService(ShortcutManager::class.java)
                val playShortcut = ShortcutInfo.Builder(this@MainActivity, "play")
                    .setShortLabel(getString(R.string.play_pause_button))
                    .setLongLabel(getString(R.string.play_pause_button))
                    .setIcon(Icon.createWithResource(this@MainActivity, R.drawable.ic_custom_play))
                    .setIntent(android.content.Intent(this@MainActivity, com.radmon.radmonradio.playback.PlaybackService::class.java).setAction("com.radmon.radmonradio.PLAY"))
                    .build()
                val pauseShortcut = ShortcutInfo.Builder(this@MainActivity, "pause")
                    .setShortLabel(getString(R.string.play_pause_button))
                    .setLongLabel(getString(R.string.play_pause_button))
                    .setIcon(Icon.createWithResource(this@MainActivity, R.drawable.ic_custom_pause))
                    .setIntent(android.content.Intent(this@MainActivity, com.radmon.radmonradio.playback.PlaybackService::class.java).setAction("com.radmon.radmonradio.PAUSE"))
                    .build()
                sm?.dynamicShortcuts = listOf(playShortcut, pauseShortcut)
            } catch (e: Exception) {
                android.util.Log.w("MainActivity", "Failed to create dynamic shortcuts", e)
            }
        }
        setContent {
            val navController = rememberNavController()
            val uiState by radioViewModel.uiState.collectAsStateWithLifecycle()
            RadmonradioTheme(userSelectedColor = Color(0xFF1DB954)) {
                val context = LocalContext.current
                var askedNotificationPermission by remember { mutableStateOf(false) }
                val prefs by remember {
                    mutableStateOf(context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE))
                }
                val notifPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) {  }
                LaunchedEffect(Unit) {
                    if (!askedNotificationPermission && android.os.Build.VERSION.SDK_INT >= 33) {
                        val alreadyPrompted = prefs.getBoolean("notif_perm_prompted", false)
                        if (!alreadyPrompted) {
                            val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.POST_NOTIFICATIONS
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            if (!granted) {
                                askedNotificationPermission = true
                                prefs.edit().putBoolean("notif_perm_prompted", true).apply()
notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    }
                }
                var mediaController by remember { mutableStateOf<MediaController?>(null) }
                DisposableEffect(Unit) {
                    val sessionToken = SessionToken(
                        this@MainActivity,
                        ComponentName(this@MainActivity, PlaybackService::class.java)
                    )
                    controllerFuture = MediaController.Builder(
                        this@MainActivity,
                        sessionToken
                    ).buildAsync()
                    controllerFuture?.addListener(
                        {
                            mediaController = controllerFuture?.get()
                            ContextCompat.startForegroundService(
                                this@MainActivity,
                                android.content.Intent(this@MainActivity, PlaybackService::class.java)
                            )
                        },
                        ContextCompat.getMainExecutor(this@MainActivity)
                    )
                    onDispose {
                        controllerFuture?.let { MediaController.releaseFuture(it) }
                    }
                }
                MyNavHost(
                    innerPadding = PaddingValues(),
                    navController = navController,
                    uiState = uiState,
                    mediaController = mediaController,
                    radioViewModel = radioViewModel
                )
            }
        }
    }
    override fun onDestroy() {
        controllerFuture?.let {
            try {
                MediaController.releaseFuture(it)
            } catch (e: Exception) {
            }
        }
        controllerFuture = null
        super.onDestroy()
    }
    
    private fun configureEdgeToEdgeDisplay() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }
}


