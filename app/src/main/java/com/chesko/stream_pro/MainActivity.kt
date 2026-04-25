package com.chesko.stream_pro

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.chesko.stream_pro.core.ui.MainViewModel
import com.chesko.stream_pro.core.utils.NetworkObserver
import com.chesko.stream_pro.ui.screens.*
import com.chesko.stream_pro.ui.theme.IPTV_PlayerTheme
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.chesko.stream_pro.core.data.model.IptvChannel
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        enableEdgeToEdge()

        setContent {
            val viewModel: MainViewModel = viewModel()
            val darkMode by viewModel.darkMode.collectAsState()
            val accentColorInt by viewModel.accentColor.collectAsState()
            val networkStatus by viewModel.networkStatus.collectAsState()

            IPTV_PlayerTheme(
                darkTheme = darkMode,
                accentColor = Color(accentColorInt)
            ) {
                val navController = rememberNavController()

                val snackbarHostState = remember { SnackbarHostState() }
                val errorMessage by viewModel.errorMessage.collectAsState()
                val scope = rememberCoroutineScope()

                LaunchedEffect(networkStatus) {
                    if (networkStatus is NetworkObserver.NetworkStatus.Lost) {
                        snackbarHostState.showSnackbar(
                            message = "Koneksi internet terputus",
                            duration = SnackbarDuration.Indefinite,
                            actionLabel = "OK"
                        )
                    } else {
                        if (snackbarHostState.currentSnackbarData?.visuals?.message == "Koneksi internet terputus") {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar("Koneksi internet terhubung kembali")
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
                        if (!navController.popBackStack()) {
                            (navController.context as? Activity)?.finish()
                        }
                    }
                }

                LaunchedEffect(errorMessage) {
                    errorMessage?.let { message ->
                        scope.launch {
                            snackbarHostState.showSnackbar(message)
                            viewModel.clearError()
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        containerColor = MaterialTheme.colorScheme.background,
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
                                        onNextScreen = {
                                            navController.navigate("login") {
                                                popUpTo("splash") { inclusive = true }
                                            }
                                        }
                                    )
                                }

                                composable("login") {
                                    LoginScreen(
                                        viewModel = viewModel,
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

                                    HomeScreen(
                                        viewModel = viewModel,
                                        onLogout = {
                                            val currentTime = System.currentTimeMillis()
                                            if (currentTime - lastNavigationTime > navigationDebounce) {
                                                lastNavigationTime = currentTime
                                                viewModel.deleteCurrentPlaylist()
                                                navController.navigate("login") {
                                                    popUpTo("home") { inclusive = true }
                                                }
                                            }
                                        },
                                        onNavigateBack = {
                                            val currentTime = System.currentTimeMillis()
                                            if (currentTime - lastNavigationTime > navigationDebounce) {
                                                lastNavigationTime = currentTime
                                                navController.navigate("login") {
                                                    popUpTo("home") { inclusive = true }
                                                }
                                            }
                                        },
                                        onOpenEpg = { safeNavigate("epg") },
                                        onOpenProfile = { safeNavigate("profile") },
                                        onOpenSettings = { safeNavigate("settings") },
                                        onOpenAbout = { safeNavigate("about") },
                                        onOpenHelp = { safeNavigate("help") },
                                        onOpenFavorites = { safeNavigate("favorites") },
                                        onSelectChannel = { channel ->
                                            val currentTime = System.currentTimeMillis()
                                            if (currentTime - lastNavigationTime > navigationDebounce) {
                                                lastNavigationTime = currentTime
                                                val channelJson = adapter.toJson(channel)
                                                val encodedJson = URLEncoder.encode(
                                                    channelJson,
                                                    StandardCharsets.UTF_8.toString()
                                                )
                                                navController.navigate("player/$encodedJson")
                                            }
                                        }
                                    )
                                }

                                composable("profile") {
                                    ProfileScreen(viewModel = viewModel, onBack = safeBack)
                                }

                                composable("settings") {
                                    SettingsScreen(viewModel = viewModel, onBack = safeBack)
                                }

                                composable("about") {
                                    AboutScreen(onBack = safeBack)
                                }

                                composable("help") {
                                    HelpScreen(onBack = safeBack)
                                }

                                composable("favorites") {
                                    FavoritesScreen(
                                        viewModel = viewModel,
                                        onBack = safeBack,
                                        onSelectChannel = { channel ->
                                            val currentTime = System.currentTimeMillis()
                                            if (currentTime - lastNavigationTime > navigationDebounce) {
                                                lastNavigationTime = currentTime
                                                val moshi =
                                                    Moshi.Builder().add(KotlinJsonAdapterFactory())
                                                        .build()
                                                val adapter = moshi.adapter(IptvChannel::class.java)
                                                val channelJson = adapter.toJson(channel)
                                                val encodedJson = URLEncoder.encode(
                                                    channelJson,
                                                    StandardCharsets.UTF_8.toString()
                                                )
                                                navController.navigate("player/$encodedJson")
                                            }
                                        }
                                    )
                                }

                                composable("epg") {
                                    val moshi = remember {
                                        Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                                    }
                                    val adapter =
                                        remember { moshi.adapter(IptvChannel::class.java) }
                                    EpgScreen(
                                        viewModel = viewModel,
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
                                                navController.navigate("player/$encodedJson")
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

                                    val encodedJson =
                                        backStackEntry.arguments?.getString("channelJson") ?: ""
                                    val channelJson = URLDecoder.decode(
                                        encodedJson,
                                        StandardCharsets.UTF_8.toString()
                                    )
                                    val channel = adapter.fromJson(channelJson)

                                    if (channel != null) {
                                        PlayerScreen(
                                            viewModel = viewModel,
                                            channel = channel,
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
                        }
                    }
                }
            }
        }
    }
}
