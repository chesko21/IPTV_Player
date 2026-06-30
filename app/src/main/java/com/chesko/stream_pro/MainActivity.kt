package com.chesko.stream_pro

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chesko.stream_pro.core.data.model.IptvChannel
import com.chesko.stream_pro.core.ui.MainViewModel
import com.chesko.stream_pro.core.utils.LocaleHelper
import com.chesko.stream_pro.core.utils.NetworkObserver
import com.chesko.stream_pro.ui.components.AppBackground
import com.chesko.stream_pro.ui.screens.AboutScreen
import com.chesko.stream_pro.ui.screens.EpgScreen
import com.chesko.stream_pro.ui.screens.HelpScreen
import com.chesko.stream_pro.ui.screens.HomeScreen
import com.chesko.stream_pro.ui.screens.LoginScreen
import com.chesko.stream_pro.ui.screens.PlayerScreen
import com.chesko.stream_pro.ui.screens.ProfileScreen
import com.chesko.stream_pro.ui.screens.SettingsScreen
import com.chesko.stream_pro.ui.screens.SplashScreen
import com.chesko.stream_pro.ui.theme.IPTV_PlayerTheme
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : FragmentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("iptv_player_prefs", MODE_PRIVATE)
        val lang = prefs.getString("app_language", "en") ?: "en"
        super.attachBaseContext(LocaleHelper.applyLocale(newBase, lang))
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val viewModel: com.chesko.stream_pro.core.ui.MainViewModel = androidx.lifecycle.ViewModelProvider(this)[com.chesko.stream_pro.core.ui.MainViewModel::class.java]
        
        if (viewModel.isCasting.value) {
            viewModel.stopCasting()
        } else {
            val currentRoute = viewModel.currentRoute.value
            if (currentRoute?.startsWith("player") == true) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    try {
                        val params = android.app.PictureInPictureParams.Builder()
                            .setAspectRatio(android.util.Rational(16, 9))
                            .setActions(emptyList())
                            .build()
                        enterPictureInPictureMode(params)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        val viewModel: com.chesko.stream_pro.core.ui.MainViewModel = androidx.lifecycle.ViewModelProvider(this)[com.chesko.stream_pro.core.ui.MainViewModel::class.java]
        viewModel.setInPipMode(isInPictureInPictureMode)
        
        if (isInPictureInPictureMode) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val params = android.app.PictureInPictureParams.Builder()
                    .setAspectRatio(android.util.Rational(16, 9))
                    .setActions(emptyList())
                    .build()
                setPictureInPictureParams(params)
            }
        } else {
            if (isFinishing || lifecycle.currentState == androidx.lifecycle.Lifecycle.State.CREATED || lifecycle.currentState == androidx.lifecycle.Lifecycle.State.DESTROYED) {
                if (viewModel.isCasting.value) {
                    viewModel.stopCasting()
                }

                viewModel.setSelectedChannel(null)

                val pid = android.os.Process.myPid()
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    if (isFinishing || lifecycle.currentState == androidx.lifecycle.Lifecycle.State.DESTROYED) {
                        android.os.Process.killProcess(pid)
                    }
                }, 800)

                finishAndRemoveTask()
            }
        }
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_DEFAULT

        setContent {
            val windowSize = calculateWindowSizeClass(this)
            val viewModel: MainViewModel = viewModel()
            val darkMode by viewModel.darkMode.collectAsState()
            val accentColorInt by viewModel.accentColor.collectAsState()
            val networkStatus by viewModel.networkStatus.collectAsState()
            val backgroundType by viewModel.backgroundType.collectAsState()
            val backgroundColorInt by viewModel.backgroundColor.collectAsState()
            val backgroundOverride = if (backgroundType == "color") Color(backgroundColorInt) else null

            IPTV_PlayerTheme(
                darkTheme = darkMode,
                accentColor = Color(accentColorInt),
                backgroundOverride = backgroundOverride
            ) {
                val context = LocalContext.current

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { _ -> }

                LaunchedEffect(Unit) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                val navController = rememberNavController()

                LaunchedEffect(navController) {
                    navController.addOnDestinationChangedListener { _, destination, _ ->
                        viewModel.setCurrentRoute(destination.route)
                    }
                }

                val snackbarHostState = remember { SnackbarHostState() }
                val errorMessage by viewModel.errorMessage.collectAsState()
                val scope = rememberCoroutineScope()

                var topNotifMessage by remember { mutableStateOf<String?>(null) }
                var topNotifIcon by remember { mutableStateOf(Icons.Default.Wifi) }
                var topNotifColor by remember { mutableStateOf(Color(0xFF4CAF50)) }
                var isTopNotifVisible by remember { mutableStateOf(false) }
                LaunchedEffect(networkStatus) {
                    when (networkStatus) {
                        is NetworkObserver.NetworkStatus.Lost -> {
                            delay(2000) // Debounce: wait 2 seconds before showing "Lost"
                            topNotifMessage = context.getString(R.string.net_lost)
                            topNotifIcon = Icons.Default.CloudOff
                            topNotifColor = Color(0xFFE53935)
                            isTopNotifVisible = true
                        }

                        is NetworkObserver.NetworkStatus.Available -> {
                            if (topNotifMessage == context.getString(R.string.net_lost)) {
                                topNotifMessage = context.getString(R.string.net_restored)
                                topNotifIcon = Icons.Default.Wifi
                                topNotifColor = Color(0xFF4CAF50)
                                isTopNotifVisible = true
                                delay(3000)
                                isTopNotifVisible = false
                                topNotifMessage = null // Reset message
                            }
                        }
                    }
                }

                var lastNavigationTime by remember { mutableLongStateOf(0L) }
                val navigationDebounce = 300L

                val safeNavigate: (String) -> Unit = { route ->
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastNavigationTime > navigationDebounce) {
                        if (navController.currentDestination?.route != route) {
                            lastNavigationTime = currentTime
                            navController.navigate(route)
                        }
                    }
                }

                val safeBack: () -> Unit = {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastNavigationTime > navigationDebounce) {
                        lastNavigationTime = currentTime
                        
                        val currentRoute = navController.currentDestination?.route
                        if (currentRoute in listOf("settings", "profile", "epg", "about", "help")) {
                            navController.previousBackStackEntry?.savedStateHandle?.set("open_drawer", true)
                        }

                        if (!navController.popBackStack()) {
                            (navController.context as? Activity)?.finish()
                        }
                    }
                }

                LaunchedEffect(errorMessage) {
                    errorMessage?.let { message ->
                        val currentRoute = navController.currentDestination?.route
                         if (currentRoute != "login" && currentRoute != "settings") {
                            scope.launch {
                                snackbarHostState.showSnackbar(message)
                                viewModel.clearError()
                            }
                        }
                    }
                }

                AppBackground(viewModel = viewModel) {
                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0)
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            NavHost(
                                navController = navController,
                                startDestination = "splash",
                                modifier = Modifier.fillMaxSize()
                            ) {
                                composable("splash") {
                                    SplashScreen(
                                        viewModel = viewModel,
                                        onNextScreen = { route ->
                                            navController.navigate(route) {
                                                popUpTo("splash") { inclusive = true }
                                            }
                                        }
                                    )
                                }

                                composable("login") {
                                    LoginScreen(
                                        viewModel = viewModel,
                                        windowSize = windowSize,
                                        onNavigateToHome = {
                                            navController.navigate("home") {
                                                popUpTo("login") { inclusive = true }
                                            }
                                        }
                                    )
                                }

                                composable("home") {
                                    val moshi = remember {
                                        Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                                    }
                                    val adapter =
                                        remember { moshi.adapter(IptvChannel::class.java) }

                                    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
                                    val openDrawer by savedStateHandle?.getLiveData<Boolean>("open_drawer")?.observeAsState(false) ?: remember { mutableStateOf(false) }

                                    HomeScreen(
                                        viewModel = viewModel,
                                        windowSize = windowSize,
                                        onLogout = {
                                            viewModel.deleteCurrentPlaylist()
                                            navController.navigate("login") {
                                                popUpTo("home") { inclusive = true }
                                            }
                                        },
                                        onOpenEpg = { safeNavigate("epg") },
                                        onOpenProfile = { safeNavigate("profile") },
                                        onOpenSettings = { safeNavigate("settings") },
                                        onOpenAbout = { safeNavigate("about") },
                                        onOpenHelp = { safeNavigate("help") },
                                        onSelectChannel = { channel ->
                                            val currentTime = System.currentTimeMillis()
                                            if (currentTime - lastNavigationTime > navigationDebounce) {
                                                lastNavigationTime = currentTime
                                                val channelJson = adapter.toJson(channel)
                                                val encodedJson = URLEncoder.encode(
                                                    channelJson,
                                                    StandardCharsets.UTF_8.toString()
                                                )
                                                navController.navigate("player/$encodedJson") {
                                                    popUpTo("home")
                                                }
                                            }
                                        },
                                        shouldOpenDrawer = openDrawer
                                    )

                                    LaunchedEffect(openDrawer) {
                                        if (openDrawer) {
                                            savedStateHandle?.remove<Boolean>("open_drawer")
                                        }
                                    }
                                }

                                composable("profile") {
                                    ProfileScreen(viewModel = viewModel, windowSize = windowSize, onBack = safeBack)
                                }

                                composable("settings") {
                                    SettingsScreen(viewModel = viewModel, windowSize = windowSize, onBack = safeBack)
                                }

                                composable("about") {
                                    AboutScreen(windowSize = windowSize, onBack = safeBack)
                                }

                                composable("help") {
                                    HelpScreen(windowSize = windowSize, onBack = safeBack)
                                }

                                composable("epg") {
                                    val moshi = remember {
                                        Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                                    }
                                    val adapter =
                                        remember { moshi.adapter(IptvChannel::class.java) }
                                    EpgScreen(
                                        viewModel = viewModel,
                                        windowSize = windowSize,
                                        onBack = safeBack,
                                        onSelectChannel = { channel ->
                                            val currentTime = System.currentTimeMillis()
                                            if (currentTime - lastNavigationTime > navigationDebounce) {
                                                lastNavigationTime = currentTime
                                                val channelJson = adapter.toJson(channel)
                                                val encodedJson = URLEncoder.encode(
                                                    channelJson,
                                                    StandardCharsets.UTF_8.toString()
                                                )
                                                navController.navigate("player/$encodedJson") {
                                                    popUpTo("home")
                                                }
                                            }
                                        }
                                    )
                                }

                                composable(
                                    route = "player/{channelJson}",
                                    arguments = listOf(navArgument("channelJson") {
                                        type = NavType.StringType
                                    })
                                ) { backStackEntry ->
                                    val moshi =
                                        Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                                    val adapter = moshi.adapter(IptvChannel::class.java)

                                    val channelJson =
                                        backStackEntry.arguments?.getString("channelJson") ?: ""
                                    val channel = adapter.fromJson(channelJson)

                                    if (channel != null) {
                                        PlayerScreen(
                                            viewModel = viewModel,
                                            channel = channel,
                                            windowSize = windowSize,
                                            onBack = {
                                                if (navController.currentDestination?.route?.startsWith(
                                                        "player"
                                                    ) == true
                                                ) {
                                                    navController.popBackStack(
                                                        "home",
                                                        inclusive = false
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            AnimatedVisibility(
                                visible = isTopNotifVisible,
                                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 16.dp)
                                    .statusBarsPadding()
                            ) {
                                Card(
                                    modifier = Modifier
                                        .padding(horizontal = 24.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    colors = CardDefaults.cardColors(
                                        containerColor = topNotifColor.copy(alpha = 0.95f)
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = topNotifIcon,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = topNotifMessage ?: "",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
