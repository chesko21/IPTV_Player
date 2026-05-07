package com.chesko.stream_pro_tv

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import com.chesko.stream_pro.core.ui.MainViewModel
import com.chesko.stream_pro.core.utils.LocaleHelper
import com.chesko.stream_pro.core.utils.NetworkObserver
import com.chesko.stream_pro_tv.ui.components.ExitConfirmDialog
import com.chesko.stream_pro_tv.ui.components.TvNavigationWrapper
import com.chesko.stream_pro_tv.ui.screens.HomeScreen
import com.chesko.stream_pro_tv.ui.screens.TvLoginScreen
import com.chesko.stream_pro_tv.ui.screens.TvPlayerScreen
import com.chesko.stream_pro_tv.ui.screens.TvSearchScreen
import com.chesko.stream_pro_tv.ui.screens.TvSettingsScreen
import com.chesko.stream_pro_tv.ui.screens.TvSplashScreen
import com.chesko.stream_pro_tv.ui.theme.IPTV_PlayerTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("iptv_player_prefs", Context.MODE_PRIVATE)
        val lang = prefs.getString("app_language", "en") ?: "en"
        super.attachBaseContext(LocaleHelper.applyLocale(newBase, lang))
    }

    private fun isTvDevice(): Boolean {
        val uiModeManager = getSystemService(UI_MODE_SERVICE) as android.app.UiModeManager
        return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val isTv = isTvDevice()
        if (isTv) {
            setTheme(R.style.Theme_IPTV_Player_TV)
        } else {
            setTheme(R.style.Theme_IPTV_Player)
        }

        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            val viewModel: MainViewModel = viewModel()
            val accentColorInt by viewModel.accentColor.collectAsState()
            
            IPTV_PlayerTheme(accentColor = Color(accentColorInt)) {
                AppNavigation(isTvDevice = isTv, viewModel = viewModel)
            }
        }
    }
}

@Composable
fun AppNavigation(isTvDevice: Boolean, viewModel: MainViewModel) {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Global Network Observer
    val networkObserver = remember { NetworkObserver(context) }
    val networkStatus by networkObserver.networkStatus.collectAsState(initial = NetworkObserver.NetworkStatus.Available)
    
    var showBackOnlineAlert by remember { mutableStateOf(false) }
    var lastKnownNetworkAvailable by remember { mutableStateOf(true) }

    LaunchedEffect(networkStatus) {
        when (networkStatus) {
            is NetworkObserver.NetworkStatus.Lost -> {
                lastKnownNetworkAvailable = false
                showBackOnlineAlert = false
            }
            is NetworkObserver.NetworkStatus.Available -> {
                if (!lastKnownNetworkAvailable) {
                    lastKnownNetworkAvailable = true
                    showBackOnlineAlert = true
                    delay(4000)
                    showBackOnlineAlert = false
                }
            }
        }
    }

    val lastUrl by viewModel.lastUrl.collectAsState()
    val selectedChannel by viewModel.selectedChannel.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val configuration = LocalConfiguration.current

    val showNavigation = currentRoute in listOf("home", "favorites", "settings", "search", "live", "movies", "sport")

    var showExitDialog by remember { mutableStateOf(false) }

    if (showNavigation && isTvDevice) {
        BackHandler {
            showExitDialog = true
        }
    }

    if (showExitDialog) {
        Dialog(
            onDismissRequest = { showExitDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            ExitConfirmDialog(
                onConfirm = { (context as? Activity)?.finish() },
                onDismiss = { showExitDialog = false }
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showNavigation) {
            if (isTvDevice) {
                TvNavigationWrapper(
                    selectedRoute = currentRoute ?: "home",
                    onRouteSelected = { route ->
                        if (currentRoute != route) {
                            navController.navigate(route) {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                ) {
                    NavHostContent(
                        navController = navController,
                        viewModel = viewModel,
                        lastUrl = lastUrl,
                        selectedChannel = selectedChannel,
                        networkStatus = networkStatus
                    )
                }
            } else {
                val screenWidth = configuration.screenWidthDp.dp

                ResponsiveNavigationLayout(
                    isSmallScreen = screenWidth < 600.dp,
                    selectedRoute = currentRoute ?: "home",
                    onRouteSelected = { route ->
                        if (currentRoute != route) {
                            navController.navigate(route) {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    navController = navController,
                    viewModel = viewModel,
                    lastUrl = lastUrl,
                    selectedChannel = selectedChannel,
                    networkStatus = networkStatus
                )
            }
        } else {
            NavHostContent(
                navController = navController,
                viewModel = viewModel,
                lastUrl = lastUrl,
                selectedChannel = selectedChannel,
                networkStatus = networkStatus
            )
        }

        // Global Network Alert
        AnimatedVisibility(
            visible = networkStatus is NetworkObserver.NetworkStatus.Lost || showBackOnlineAlert,
            enter = expandVertically(),
            exit = shrinkVertically(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            val isLost = networkStatus is NetworkObserver.NetworkStatus.Lost
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = SurfaceDefaults.colors(
                    containerColor = if (isLost) Color(0xFFE50914) else Color(0xFF2E7D32),
                    contentColor = Color.White
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        if (isLost) Icons.Default.WifiOff else Icons.Default.Wifi,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        if (isLost) stringResource(R.string.net_lost) else stringResource(R.string.net_restored),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ResponsiveNavigationLayout(
    isSmallScreen: Boolean,
    selectedRoute: String,
    onRouteSelected: (String) -> Unit,
    navController: androidx.navigation.NavHostController,
    viewModel: MainViewModel,
    lastUrl: String,
    selectedChannel: com.chesko.stream_pro.core.data.model.IptvChannel?,
    networkStatus: NetworkObserver.NetworkStatus
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    val isLargeScreen = screenWidth >= 1240.dp

    if (screenWidth < 600.dp) {
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail(
                modifier = Modifier.width(64.dp),
                containerColor = Color(0xFF0A0A0A)
            ) {
                Column(
                    modifier = Modifier.fillMaxHeight().padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val items = listOf(
                        "home" to Icons.Filled.Home,
                        "live" to Icons.Filled.Tv,
                        "movies" to Icons.Filled.Movie,
                        "favorites" to Icons.Filled.Favorite,
                        "settings" to Icons.Filled.Settings
                    )
                    items.forEach { (route, icon) ->
                        NavigationRailItem(
                            selected = selectedRoute == route,
                            onClick = { onRouteSelected(route) },
                            icon = { Icon(icon, contentDescription = route, modifier = Modifier.size(20.dp)) },
                            colors = androidx.compose.material3.NavigationRailItemDefaults.colors(
                                selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                unselectedIconColor = Color.White.copy(alpha = 0.5f),
                                indicatorColor = Color.Transparent
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                NavHostContent(navController, viewModel, lastUrl, selectedChannel, networkStatus)
            }
        }
    } else {
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail(
                modifier = Modifier.width(if (isLargeScreen) 100.dp else 80.dp),
                containerColor = Color(0xFF0A0A0A),
                header = {
                    Icon(
                        Icons.Filled.Tv, 
                        null, 
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 24.dp).size(32.dp)
                    )
                }
            ) {
                Column(
                    modifier = Modifier.fillMaxHeight().padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val items = listOf(
                        "home" to Icons.Filled.Home,
                        "live" to Icons.Filled.Tv,
                        "movies" to Icons.Filled.Movie,
                        "favorites" to Icons.Filled.Favorite,
                        "settings" to Icons.Filled.Settings
                    )
                    items.forEach { (route, icon) ->
                        NavigationRailItem(
                            selected = selectedRoute == route,
                            onClick = { onRouteSelected(route) },
                            icon = { Icon(icon, contentDescription = route) },
                            label = { 
                                Text(
                                    route.replaceFirstChar { it.uppercase() }, 
                                    fontSize = if (isLargeScreen) 12.sp else 10.sp,
                                    fontWeight = if (selectedRoute == route) FontWeight.Bold else FontWeight.Normal
                                ) 
                            },
                            colors = androidx.compose.material3.NavigationRailItemDefaults.colors(
                                selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                unselectedIconColor = Color.White.copy(alpha = 0.5f),
                                unselectedTextColor = Color.White.copy(alpha = 0.5f),
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                NavHostContent(navController, viewModel, lastUrl, selectedChannel, networkStatus)
            }
        }
    }
}

@Composable
fun NavHostContent(
    navController: androidx.navigation.NavHostController,
    viewModel: MainViewModel,
    lastUrl: String,
    selectedChannel: com.chesko.stream_pro.core.data.model.IptvChannel?,
    networkStatus: NetworkObserver.NetworkStatus
) {
    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            TvSplashScreen(
                onNextScreen = {
                    val destination = if (lastUrl.isEmpty()) "login" else "home"
                    navController.navigate(destination) {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }
        composable("login") {
            TvLoginScreen(
                viewModel = viewModel,
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("live") {
            LaunchedEffect(Unit) { 
                viewModel.setSelectedGroup(null)
                viewModel.setCategoryFilter("live") 
            }
            HomeScreen(
                viewModel = viewModel,
                showGroupSelector = false,
                onChannelClick = { channel ->
                    viewModel.setSelectedChannel(channel)
                    navController.navigate("player")
                }
            )
        }
        composable("movies") {
            LaunchedEffect(Unit) { 
                viewModel.setSelectedGroup(null)
                viewModel.setCategoryFilter("movies") 
            }
            HomeScreen(
                viewModel = viewModel,
                showGroupSelector = false, // Sembunyikan untuk Movies
                onChannelClick = { channel ->
                    viewModel.setSelectedChannel(channel)
                    navController.navigate("player")
                }
            )
        }
        composable("sport") {
            LaunchedEffect(Unit) { 
                viewModel.setSelectedGroup(null)
                viewModel.setCategoryFilter("sport") 
            }
            HomeScreen(
                viewModel = viewModel,
                showGroupSelector = false,
                onChannelClick = { channel ->
                    viewModel.setSelectedChannel(channel)
                    navController.navigate("player")
                }
            )
        }
        composable("favorites") {
            val favoritesLabel = stringResource(com.chesko.stream_pro.core.R.string.group_favorites)
            LaunchedEffect(Unit) {
                viewModel.setCategoryFilter(null)
                viewModel.setSelectedGroup(favoritesLabel)
            }
            HomeScreen(
                viewModel = viewModel,
                showGroupSelector = false,
                onChannelClick = { channel ->
                    viewModel.setSelectedChannel(channel)
                    navController.navigate("player")
                }
            )
        }
        composable("home") {
            LaunchedEffect(Unit) { 
                viewModel.setSelectedGroup(null)
                viewModel.setCategoryFilter(null) 
            }
            HomeScreen(
                viewModel = viewModel,
                onChannelClick = { channel ->
                    viewModel.setSelectedChannel(channel)
                    navController.navigate("player")
                }
            )
        }
        composable("search") {
            TvSearchScreen(
                viewModel = viewModel,
                onBack = { 
                    navController.navigate("home") {
                        popUpTo("search") { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onChannelClick = { channel ->
                    viewModel.setSelectedChannel(channel)
                    navController.navigate("player")
                }
            )
        }
        composable("settings") {
            TvSettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable("player") {
            selectedChannel?.let { channel ->
                TvPlayerScreen(
                    viewModel = viewModel,
                    channel = channel,
                    networkStatus = networkStatus,
                    onBack = { navController.popBackStack() }
                )
            } ?: LaunchedEffect(Unit) {
                navController.popBackStack()
            }
        }
    }
}