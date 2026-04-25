package com.chesko.stream_pro.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.chesko.stream_pro.R
import com.chesko.stream_pro.core.ui.MainViewModel
import com.chesko.stream_pro.ui.components.shimmerEffect
import kotlinx.coroutines.launch

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

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
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
                    contentColor = MaterialTheme.colorScheme.onError,
                    snackbarData = data,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            MovingPosterWall()
            // Semi-transparent overlay to keep text readable while letting posters show through
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f)))

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LogoSection()
                    Spacer(modifier = Modifier.height(30.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { shadowElevation = 20f; shape = RoundedCornerShape(24.dp); clip = true }
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(0.1f), RoundedCornerShape(24.dp)),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 24.dp)
                                    .background(MaterialTheme.colorScheme.surface.copy(0.3f), CircleShape)
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                LoginMethodTab(
                                    text = "URL M3U",
                                    isSelected = selectedTab == 0,
                                    modifier = Modifier.weight(1f),
                                    onClick = { selectedTab = 0 }
                                )
                                LoginMethodTab(
                                    text = "FILE LOKAL",
                                    isSelected = selectedTab == 1,
                                    modifier = Modifier.weight(1f),
                                    onClick = { selectedTab = 1 }
                                )
                            }

                            AnimatedContent(
                                targetState = selectedTab,
                                transitionSpec = {
                                    if (targetState > initialState) {
                                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                                    } else {
                                        slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                                    }.using(SizeTransform(clip = false))
                                },
                                label = "LoginSlide"
                            ) { target ->
                                when (target) {
                                    0 -> UrlSlide(
                                        url = url,
                                        onUrlChange = { url = it },
                                        isLoading = isLoading,
                                        onConnect = {
                                            if (!url.startsWith("http")) {
                                                scope.launch { snackbarHostState.showSnackbar("URL harus diawali http:// atau https://") }
                                                return@UrlSlide
                                            }
                                            keyboardController?.hide()
                                            viewModel.loadPlaylist(url, onSuccess = {
                                                scope.launch { snackbarHostState.showSnackbar("Playlist Berhasil Dimuat!") }
                                                onNavigateToHome()
                                            })
                                        },
                                        onDemo = { demoUrl ->
                                            url = demoUrl
                                            keyboardController?.hide()
                                            viewModel.loadPlaylist(demoUrl, onSuccess = {
                                                scope.launch { snackbarHostState.showSnackbar("Playlist Demo Berhasil Dimuat!") }
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
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    if (selectedTab == 0) {
                        Text(
                            text = "Masukkan URL playlist M3U atau gunakan demo",
                            color = MaterialTheme.colorScheme.onBackground.copy(0.4f),
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoginMethodTab(text: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val backgroundColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(0.6f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
fun UrlSlide(
    url: String,
    onUrlChange: (String) -> Unit,
    isLoading: Boolean,
    onConnect: () -> Unit,
    onDemo: (String) -> Unit
) {
    var showDemoDialog by remember { mutableStateOf(false) }

    if (showDemoDialog) {
        AlertDialog(
            onDismissRequest = { showDemoDialog = false },
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
            title = { 
                Text(
                    "Pilih Playlist Demo", 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 18.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                ) 
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    MainViewModel.DEMO_URLS.forEachIndexed { index, demoUrl ->
                        Button(
                            onClick = {
                                showDemoDialog = false
                                onDemo(demoUrl)
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Demo Playlist ${index + 1}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showDemoDialog = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("URL Playlist", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
        Text("Masukkan tautan M3U remote Anda", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 20.dp))

        OutlinedTextField(
            value = url,
            onValueChange = onUrlChange,
            placeholder = { Text("https://contoh.com/playlist.m3u", fontSize = 14.sp) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Link, null, tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = {
                if (url.isNotEmpty()) {
                    IconButton(onClick = { onUrlChange("") }) {
                        Icon(Icons.Default.Clear, null, modifier = Modifier.size(18.dp))
                    }
                }
            },
            maxLines = 2,
            visualTransformation = VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Done
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(0.3f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(0.3f)
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Dua tombol dengan style yang sama persis
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Tombol LOGIN
            ShimmerButton(
                text = "LOGIN",
                isLoading = isLoading,
                enabled = !isLoading,
                onClick = onConnect,
                modifier = Modifier.weight(1f)
            )

            // Tombol DEMO
            ShimmerButton(
                text = "DEMO",
                isLoading = isLoading,
                enabled = !isLoading,
                onClick = { showDemoDialog = true },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun FileSlide(isLoading: Boolean, onPickFile: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 10.dp)) {
        Icon(Icons.Default.UploadFile, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text("Playlist Lokal", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
        Text("Pilih file M3U dari penyimpanan perangkat", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = onPickFile,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary)
            else {
                Icon(Icons.Default.FileOpen, null)
                Spacer(modifier = Modifier.width(12.dp))
                Text("PILIH FILE", fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun LogoSection() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(id = R.drawable.app_icon_android),
            contentDescription = "Logo",
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row {
            Text("STREAM", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
            Text("PRO", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
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
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scroll"
    )
    Box(modifier = Modifier
        .fillMaxSize()
        .rotate(-5f)
        .scale(1.4f)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier
                .requiredHeight(4000.dp)
                .fillMaxWidth()
                .graphicsLayer { translationY = scrollOffset },
            userScrollEnabled = false,
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(200) { index ->
                AsyncImage(
                    model = posters[index % posters.size],
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.7f)
                        .alpha(0.15f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
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
    val buttonHeight = 56.dp
    val fontSize = 14.sp
    
    if (isLoading) {
        Box(
            modifier = modifier
                .height(buttonHeight)
                .clip(RoundedCornerShape(14.dp))
                .shimmerEffect(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 3.dp
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
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (text == "DEMO") MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primary,
                contentColor = if (text == "DEMO") MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(0.3f)
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (enabled && text != "DEMO") {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(100.dp)
                            .graphicsLayer {
                                translationX = shimmerTranslate
                                rotationZ = 45f
                            }
                            .background(
                                Brush.linearGradient(
                                    listOf(Color.Transparent, Color.White.copy(0.2f), Color.Transparent)
                                )
                            )
                    )
                }
                Text(
                    text,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = fontSize),
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
            }
        }
    }
}

fun Modifier.scale(scale: Float) = graphicsLayer(scaleX = scale, scaleY = scale)