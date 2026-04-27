package com.chesko.stream_pro

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.show(WindowInsetsCompat.Type.statusBars())
        windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
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

                var topNotifMessage by remember { mutableStateOf<String?>(null) }
                var topNotifIcon by remember { mutableStateOf(Icons.Default.Wifi) }
                var topNotifColor by remember { mutableStateOf(Color(0xFF4CAF50)) }
                var isTopNotifVisible by remember { mutableStateOf(false) }

                LaunchedEffect(networkStatus) {
                    when (networkStatus) {
                        is NetworkObserver.NetworkStatus.Lost -> {
                            topNotifMessage = "Koneksi internet terputus"
                            topNotifIcon = Icons.Default.CloudOff
                            topNotifColor = Color(0xFFE53935)
                            isTopNotifVisible = true
                        }
                        is NetworkObserver.NetworkStatus.Available -> {
                            if (topNotifMessage == "Koneksi internet terputus") {
                                topNotifMessage = "Koneksi internet terhubung kembali"
                                topNotifIcon = Icons.Default.Wifi
                                topNotifColor = Color(0xFF4CAF50)
                                isTopNotifVisible = true
                                delay(3000)
                                isTopNotifVisible = false
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
                        contentWindowInsets = WindowInsets(0,0,0,0) // Abaikan insets global agar NavHost full screen
                    ) { _ ->
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            NavHost(
                                navController = navController,
                                startDestination = "splash",
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // ... (rest of NavHost remains the same)
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

                            // Top Notification Overlay
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
