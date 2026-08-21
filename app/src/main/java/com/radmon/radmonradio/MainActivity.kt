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
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.common.util.concurrent.ListenableFuture
import com.radmon.radmonradio.playback.PlaybackService
import com.radmon.radmonradio.ui.theme.RadmonradioTheme
import com.radmon.radmonradio.util.Perf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentScreen = backStackEntry?.destination?.route

                Scaffold(
                    bottomBar = {
                        if (currentScreen in listOf("articles", "radio")) {
                            MyBottomAppBar(navController)
                        }
                    }
                ) { innerPadding ->
                    MyNavHost(
                        innerPadding = innerPadding,
                        navController = navController,
                        uiState = uiState,
                        mediaController = mediaController,
                        radioViewModel = radioViewModel
                    )
                }
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

@Composable
//bottom bar used in home/setting/medcoach/symptom screen
fun MyBottomAppBar(navController: NavHostController) {
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf(
        "articles",
        "radio",
    )
    //creating the bar
    NavigationBar {
        //for each item we want to be a bar
        items.forEachIndexed { index, item ->

            //making the items
            NavigationBarItem(
                //creating the icons
                icon = {
                    when (item) {
                        "articles" -> Icon(Icons.Filled.Description, contentDescription = "Home")
                        "radio" -> Icon(Icons.Filled.Leaderboard, contentDescription = "Radio")

                    }
                },
                label = {
                    when (item) {
                        "articles" -> "Articles"
                        "radio" -> "Radio"
                        else -> ""

                    }
                },
                //creating a variable to save where we want to go
                selected = selectedItem == index,

                //making it navigate to that screen when clicked
                onClick = {
                    selectedItem = index
                    navController.navigate(item)
                }
            )
        }
    }
}



