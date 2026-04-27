package com.chesko.stream_pro.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.chesko.stream_pro.R
import com.chesko.stream_pro.core.ui.MainViewModel
import com.chesko.stream_pro.core.utils.NetworkObserver
import com.chesko.stream_pro.ui.components.shimmerEffect
import kotlinx.coroutines.launch

/**
 * Universal Cinematic LoginScreen
 * Implements high-end glassmorphism and cosmic animations
 */
@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    onNavigateToHome: () -> Unit
) {
    val lastUrl by viewModel.lastUrl.collectAsState()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val networkStatus by viewModel.networkStatus.collectAsState()
    val isOffline = networkStatus is NetworkObserver.NetworkStatus.Lost

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Cinematic Entrance state
    val entranceAlpha = remember { Animatable(0f) }
    val entranceScale = remember { Animatable(0.95f) }

    LaunchedEffect(Unit) {
        launch { entranceAlpha.animateTo(1f, tween(1500, easing = EaseOutQuart)) }
        launch { entranceScale.animateTo(1f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow)) }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
    }

    var selectedTab by remember {
        mutableIntStateOf(if (lastUrl.startsWith("file://")) 1 else 0)
    }

    var url by remember {
        mutableStateOf(if (lastUrl.startsWith("http")) lastUrl else "")
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                val content = stream.bufferedReader().use { reader -> reader.readText() }
                viewModel.loadPlaylistFromFile(content, onNavigateToHome)
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    containerColor = if (errorMessage != null) MaterialTheme.colorScheme.error else Color(0xFF2E7D32),
                    contentColor = Color.White,
                    snackbarData = data,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. COSMIC BACKGROUND LAYERS
            MovingPosterWall()
            
            // Atmospheric Radial Glows
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            0f to MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            0.7f to Color.Transparent,
                            center = androidx.compose.ui.geometry.Offset(0f, 0f)
                        )
                    )
            )

            // 2. MAIN CONTENT
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .graphicsLayer {
                        alpha = entranceAlpha.value
                        scaleX = entranceScale.value
                        scaleY = entranceScale.value
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LogoSection()
                    
                    Spacer(modifier = Modifier.height(40.dp))

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp, 
                                    Brush.linearGradient(
                                        listOf(MaterialTheme.colorScheme.onSurface.copy(0.1f), Color.Transparent)
                                    ), 
                                    RoundedCornerShape(24.dp)
                                ),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f),
                            shape = RoundedCornerShape(24.dp),
                            tonalElevation = 0.dp
                        ) {
                            Box {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .blur(if (isLoading) 2.dp else 0.dp), 
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Custom Tab Switcher
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp)
                                            .background(MaterialTheme.colorScheme.onSurface.copy(0.05f), CircleShape)
                                            .padding(4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        LoginMethodTab(
                                            text = "REMOTE URL",
                                            isSelected = selectedTab == 0,
                                            modifier = Modifier.weight(1f),
                                            onClick = { selectedTab = 0 }
                                        )
                                        LoginMethodTab(
                                            text = "LOCAL FILE",
                                            isSelected = selectedTab == 1,
                                            modifier = Modifier.weight(1f),
                                            onClick = { selectedTab = 1 }
                                        )
                                    }

                                    AnimatedContent(
                                        targetState = selectedTab,
                                        transitionSpec = {
                                            fadeIn(tween(400)) + scaleIn(initialScale = 0.95f) togetherWith fadeOut(tween(400))
                                        },
                                        label = "LoginMode"
                                    ) { target ->
                                        when (target) {
                                            0 -> UrlSlide(
                                                url = url,
                                                onUrlChange = { url = it },
                                                isLoading = isLoading,
                                                isOffline = isOffline,
                                                onConnect = {
                                                    if (!url.startsWith("http")) {
                                                        scope.launch { snackbarHostState.showSnackbar("Invalid URL protocol") }
                                                        return@UrlSlide
                                                    }
                                                    keyboardController?.hide()
                                                    viewModel.loadPlaylist(url, onSuccess = {
                                                        onNavigateToHome()
                                                    })
                                                },
                                                onDemo = { demoUrl ->
                                                    url = demoUrl
                                                    keyboardController?.hide()
                                                    viewModel.loadPlaylist(demoUrl, onSuccess = {
                                                        onNavigateToHome()
                                                    })
                                                }
                                            )
                                            1 -> FileSlide(
                                                isLoading = isLoading,
                                                onPickFile = { filePickerLauncher.launch("*/*") }
                                            )
                                        }
                                    }
                                }
                                
                                if (isLoading) {
                                    Box(
                                        modifier = Modifier.matchParentSize().background(Color.Black.copy(0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }

                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Text(
                        text = "SECURE ENCRYPTED CONNECTION",
                        color = MaterialTheme.colorScheme.onSurface.copy(0.3f),
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

@Composable
fun LoginMethodTab(text: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val backgroundColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(400)
    )
    val contentColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(0.4f),
        animationSpec = tween(400)
    )
    
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text, 
            color = contentColor, 
            fontWeight = FontWeight.Black, 
            fontSize = 11.sp,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun UrlSlide(
    url: String,
    onUrlChange: (String) -> Unit,
    isLoading: Boolean,
    isOffline: Boolean = false,
    onConnect: () -> Unit,
    onDemo: (String) -> Unit
) {
    var showDemoDialog by remember { mutableStateOf(false) }

    if (showDemoDialog) {
        Dialog(onDismissRequest = { showDemoDialog = false }) {
            Surface(
                modifier = Modifier.width(260.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "EXPLORE DEMO", 
                        fontWeight = FontWeight.Black, 
                        letterSpacing = 1.sp, 
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        MainViewModel.DEMO_URLS.forEachIndexed { index, demoUrl ->
                            Surface(
                                onClick = { showDemoDialog = false; onDemo(demoUrl) },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(0.04f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(0.06f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Dns, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text("Playlist Demo ${index + 1}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    TextButton(
                        onClick = { showDemoDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("CLOSE", color = MaterialTheme.colorScheme.onSurface.copy(0.5f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }

    val displayUrl = remember(url) {
        val demoIndex = MainViewModel.DEMO_URLS.indexOf(url)
        if (demoIndex != -1) "Universal Playlist ${demoIndex + 1}" else url
    }

    Column(horizontalAlignment = Alignment.Start) {
        Text(
            "M3U PLAYLIST ENDPOINT", 
            color = MaterialTheme.colorScheme.primary, 
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            style = MaterialTheme.typography.labelSmall
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = displayUrl,
            onValueChange = { newValue ->
                if (MainViewModel.DEMO_URLS.contains(url)) {
                    onUrlChange(newValue)
                } else {
                    onUrlChange(newValue)
                }
            },
            placeholder = { Text("https://your-provider.com/playlist.m3u", color = MaterialTheme.colorScheme.onSurface.copy(0.3f), fontSize = 13.sp) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Language, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.5f), modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (url.isNotEmpty()) {
                    IconButton(onClick = { onUrlChange("") }) {
                        Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                    }
                }
            },
            readOnly = MainViewModel.DEMO_URLS.contains(url),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(0.1f),
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(0.05f),
                unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(0.05f)
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ShimmerButton(
                text = "CONNECT",
                isLoading = isLoading,
                enabled = !isLoading && !isOffline,
                onClick = onConnect,
                modifier = Modifier.weight(1.5f)
            )

            Button(
                onClick = { showDemoDialog = true },
                modifier = Modifier.height(42.dp).weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(0.05f), contentColor = MaterialTheme.colorScheme.onSurface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(0.1f))
            ) {
                Text("DEMO", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun FileSlide(isLoading: Boolean, onPickFile: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 10.dp)) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(MaterialTheme.colorScheme.primary.copy(0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.CloudUpload, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("LOCAL REPOSITORY", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontSize = 14.sp)
        Text(
            "Import .m3u or .m3u8 files from your device storage", 
            style = MaterialTheme.typography.bodySmall, 
            color = MaterialTheme.colorScheme.onSurface.copy(0.4f), 
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onPickFile,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(42.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.FolderZip, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("BROWSE FILES", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
fun LogoSection() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.onSurface.copy(0.05f),
            border = BorderStroke(1.5.dp, Brush.sweepGradient(listOf(MaterialTheme.colorScheme.primary, Color.Transparent, MaterialTheme.colorScheme.primary)))
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(18.dp)) {
                Image(
                    painter = painterResource(id = R.drawable.app_icon_android),
                    contentDescription = "Logo",
                    modifier = Modifier.fillMaxSize()
                )

            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "STREAM", 
                style = MaterialTheme.typography.headlineSmall, 
                fontWeight = FontWeight.Black, 
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-1).sp
            )
            Text(
                "PRO", 
                style = MaterialTheme.typography.headlineSmall, 
                fontWeight = FontWeight.Black, 
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = (-1).sp,
                modifier = Modifier.shimmerEffect()
            )
        }
    }
}

@Composable
fun MovingPosterWall() {
    val posters = listOf(
        "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=400",
        "https://images.unsplash.com/photo-1542204113-e935100c31e7?w=400",
        "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=400",
        "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=400",
        "https://images.unsplash.com/photo-1594909122845-11baa439b7bf?w=400",
        "https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=400",
        "https://images.unsplash.com/photo-1440404653325-ab127d49abc1?w=400",
        "https://images.unsplash.com/photo-1535016120720-40c646bebbdc?w=400"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "wall")
    val scrollOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -3000f,
        animationSpec = infiniteRepeatable(
            animation = tween(80000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scroll"
    )
    Box(modifier = Modifier
        .fillMaxSize()
        .rotate(-8f)
        .graphicsLayer {
            scaleX = 1.5f
            scaleY = 1.5f
        }
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier
                .requiredHeight(4000.dp)
                .fillMaxWidth()
                .graphicsLayer { translationY = scrollOffset },
            userScrollEnabled = false,
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(200) { index ->
                AsyncImage(
                    model = posters[index % posters.size],
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.7f)
                        .alpha(0.12f) // Increased from 0.05f for better visibility
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
fun ShimmerButton(
    text: String,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonHeight = 42.dp
    
    if (isLoading) {
        Box(
            modifier = modifier
                .height(buttonHeight)
                .clip(RoundedCornerShape(12.dp))
                .shimmerEffect(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        }
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
        val shimmerTranslate by infiniteTransition.animateFloat(
            initialValue = -500f,
            targetValue = 500f,
            animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
            label = "shimmer"
        )
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier
                .height(buttonHeight)
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(0.3f)
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (enabled) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(120.dp)
                            .graphicsLayer {
                                translationX = shimmerTranslate
                                rotationZ = 45f
                            }
                            .background(
                                Brush.linearGradient(
                                    listOf(Color.Transparent, MaterialTheme.colorScheme.onPrimary.copy(0.2f), Color.Transparent)
                                )
                            )
                    )
                }
                Text(
                    text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontSize = 12.sp
                )
            }
        }
    }
}
