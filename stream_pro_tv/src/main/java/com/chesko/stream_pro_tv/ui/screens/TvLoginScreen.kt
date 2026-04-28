package com.chesko.stream_pro_tv.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text as Material3Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import androidx.tv.material3.Text as TvText
import coil.compose.AsyncImage
import com.chesko.stream_pro.core.ui.MainViewModel
import kotlinx.coroutines.delay
import com.chesko.stream_pro_tv.ui.components.TvFilePicker
import java.io.File
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvLoginScreen(
    viewModel: MainViewModel,
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val isLoading by viewModel.isLoading.collectAsState()
    val lastUrl by viewModel.lastUrl.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var url by remember { mutableStateOf(lastUrl) }

    // Sync local url state when lastUrl from ViewModel changes
    LaunchedEffect(lastUrl) {
        url = lastUrl
    }

    var selectedFilePath by remember { mutableStateOf<String?>(null) }
    var selectedFileName by remember { mutableStateOf("Belum ada file dipilih") }
    var showFilePicker by remember { mutableStateOf(false) }

    val buttonFocusRequester = remember { FocusRequester() }
    val urlSurfaceFocusRequester = remember { FocusRequester() }
    var isEditingUrl by remember { mutableStateOf(false) }

    val glowAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        glowAlpha.animateTo(1f, tween(2000))
    }

    LaunchedEffect(Unit) {
        delay(300)
        buttonFocusRequester.requestFocus()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Universe Background
        UniverseBackground(MaterialTheme.colorScheme.primary, glowAlpha.value)

        // Subtle gradient overlay for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.4f)
                        )
                    )
                )
        )

        // Unified Row Layout (Always Row for Premium Landscape Experience)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Left Side: Branding & Logo
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                BrandingSection()
            }

            // Right Side: Login Form
            LoginFormSection(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                url = url,
                onUrlChange = { url = it },
                isEditingUrl = isEditingUrl,
                onEditingChange = { isEditingUrl = it },
                urlSurfaceFocusRequester = urlSurfaceFocusRequester,
                isLoading = isLoading,
                onConnect = { viewModel.loadPlaylist(url, onSuccess = onNavigateToHome) },
                onDemo = {
                    val demoUrl = MainViewModel.BASE_URL
                    url = demoUrl
                    isEditingUrl = false
                    viewModel.loadPlaylist(demoUrl, onSuccess = onNavigateToHome)
                },
                buttonFocusRequester = buttonFocusRequester,
                selectedFileName = selectedFileName,
                hasSelectedFile = selectedFilePath != null,
                onPickFile = { showFilePicker = true },
                onFileConnect = {
                    selectedFilePath?.let { path ->
                        val content = File(path).readText()
                        viewModel.loadPlaylistFromFile(content = content, onSuccess = onNavigateToHome)
                    }
                },
                lastUrl = lastUrl,
                modifier = Modifier.width(440.dp)
            )
        }

        if (showFilePicker) {
            Dialog(
                onDismissRequest = { showFilePicker = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                TvFilePicker(
                    onFileSelected = { file ->
                        selectedFilePath = file.absolutePath
                        selectedFileName = file.name
                        showFilePicker = false
                    },
                    onDismiss = { showFilePicker = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun BrandingSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "branding")
        val glowScale by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.4f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow"
        )

        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            // Cosmic Glow behind logo
            Canvas(modifier = Modifier.size(140.dp * glowScale)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFBB86FC).copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
            }
            
            Image(
                painter = painterResource(com.chesko.stream_pro_tv.R.drawable.app_icon_androidtv),
                contentDescription = "Logo",
                modifier = Modifier.size(110.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TvText(
                    text = "STREAM",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                )
                TvText(
                    text = "PRO",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                )
            }
            
            TvText(
                text = "EXPLORE THE CINEMATIC UNIVERSE",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.5f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            TvText(
                text = "DESIGN By CHESKO",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.3f),
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
fun LoginFormSection(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    url: String,
    onUrlChange: (String) -> Unit,
    isEditingUrl: Boolean,
    onEditingChange: (Boolean) -> Unit,
    urlSurfaceFocusRequester: FocusRequester,
    isLoading: Boolean,
    onConnect: () -> Unit,
    onDemo: () -> Unit,
    buttonFocusRequester: FocusRequester,
    selectedFileName: String,
    hasSelectedFile: Boolean,
    onPickFile: () -> Unit,
    onFileConnect: () -> Unit,
    lastUrl: String = "",
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF0A0A0A).copy(alpha = 0.7f))
            .border(
                1.dp, 
                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.2f), Color(0xFFBB86FC).copy(alpha = 0.1f))), 
                RoundedCornerShape(24.dp)
            )
            .padding(if (modifier == Modifier.fillMaxWidth()) 24.dp else 36.dp)
    ) {
        // Tab Selector - Universe Glass Look
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TabItem(
                text = "M3U URL",
                isSelected = selectedTab == 0,
                onClick = { onTabSelected(0) }
            )
            TabItem(
                text = "LOCAL FILE",
                isSelected = selectedTab == 1,
                onClick = { onTabSelected(1) }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                fadeIn(animationSpec = tween(400)) + slideInHorizontally { if (targetState > initialState) it else -it } togetherWith
                        fadeOut(animationSpec = tween(200)) + slideOutHorizontally { if (targetState > initialState) -it else it }
            },
            label = "TabTransition"
        ) { targetTab ->
            Column(modifier = Modifier.fillMaxWidth()) {
                if (targetTab == 0) {
                    UrlInputSection(
                        url = url,
                        onUrlChange = onUrlChange,
                        isEditingUrl = isEditingUrl,
                        onEditingChange = onEditingChange,
                        urlSurfaceFocusRequester = urlSurfaceFocusRequester,
                        isLoading = isLoading,
                        onConnect = onConnect,
                        onDemo = onDemo,
                        buttonFocusRequester = buttonFocusRequester,
                        lastUrl = lastUrl
                    )
                } else {
                    FileInputSection(
                        selectedFileName = selectedFileName,
                        hasSelectedFile = hasSelectedFile,
                        isLoading = isLoading,
                        onPickFile = onPickFile,
                        onConnect = onFileConnect
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun RowScope.TabItem(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .height(44.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else Color.Transparent,
            focusedContainerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f)
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            TvText(
                text = text,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f)
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
                        .alpha(0.12f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF222222)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun UrlInputSection(
    url: String,
    onUrlChange: (String) -> Unit,
    isEditingUrl: Boolean,
    onEditingChange: (Boolean) -> Unit,
    urlSurfaceFocusRequester: FocusRequester,
    isLoading: Boolean,
    onConnect: () -> Unit,
    onDemo: () -> Unit,
    buttonFocusRequester: FocusRequester,
    lastUrl: String = ""
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TvText(
            text = "SOURCE CONFIGURATION",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.4f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        // Preset Demo URLs Section
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
            TvText(
                text = "PRESET DEMO URLS",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.3f),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val labels = listOf("SERVER 1", "SERVER 2", "SERVER 3", "SERVER 4", "SERVER 5")
                MainViewModel.DEMO_URLS.forEachIndexed { index, demoUrl ->
                    val label = labels.getOrElse(index) { "SERVER ${index + 1}" }
                    val isSelected = url == demoUrl
                    
                    Surface(
                        onClick = { onUrlChange(demoUrl) },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f),
                            focusedContainerColor = Color.White,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f),
                            focusedContentColor = Color.Black
                        ),
                        border = ClickableSurfaceDefaults.border(
                            focusedBorder = Border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary)),
                            border = Border(BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f)))
                        ),
                        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            TvText(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }

        if (!isEditingUrl) {
            Surface(
                onClick = { onEditingChange(true) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .focusRequester(urlSurfaceFocusRequester),
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color.White.copy(alpha = 0.03f),
                    focusedContainerColor = Color.White.copy(alpha = 0.1f),
                    contentColor = Color.White,
                    focusedContentColor = Color.White
                ),
                border = ClickableSurfaceDefaults.border(
                    border = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))),
                    focusedBorder = Border(BorderStroke(2.dp, Color(0xFFBB86FC)))
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = if (url.isEmpty()) Color.Gray else Color(0xFFBB86FC),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        if (url.isEmpty()) {
                            Material3Text("Playlist M3U URL", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Material3Text("Click to enter address", color = Color.White.copy(alpha = 0.25f), fontSize = 11.sp)
                        } else {
                            val demoIndex = MainViewModel.DEMO_URLS.indexOf(url)
                            val displayUrl = if (demoIndex != -1) "SERVER ${demoIndex + 1}" else url
                            
                            Material3Text("Target URL", color = Color(0xFFBB86FC), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Material3Text(displayUrl, color = Color.White, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        } else {
            val textFieldFocusRequester = remember { FocusRequester() }
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                modifier = Modifier.fillMaxWidth().height(68.dp).focusRequester(textFieldFocusRequester),
                placeholder = { Material3Text("https://example.com/playlist.m3u", color = Color.Gray, fontSize = 14.sp) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFBB86FC),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    onEditingChange(false)
                    urlSurfaceFocusRequester.requestFocus()
                })
            )
            LaunchedEffect(Unit) { textFieldFocusRequester.requestFocus() }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // LOGIN BUTTON
            Surface(
                onClick = onConnect,
                enabled = !isLoading && url.isNotBlank(),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .focusRequester(buttonFocusRequester),
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    focusedContainerColor = Color.White,
                    contentColor = Color.White,
                    focusedContentColor = Color.Black,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                ),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val isUrlLoading = isLoading && url.isNotBlank() && url != MainViewModel.BASE_URL
                    if (isUrlLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.Black,
                            strokeWidth = 3.dp
                        )
                    } else {
                        TvText("LOGIN", fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            // DEMO BUTTON
            Surface(
                onClick = onDemo,
                enabled = !isLoading,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color.White.copy(alpha = 0.06f),
                    focusedContainerColor = Color.White,
                    contentColor = Color.White.copy(alpha = 0.7f),
                    focusedContentColor = Color.Black,
                    disabledContainerColor = Color.White.copy(alpha = 0.03f)
                ),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val isDemoLoading = isLoading && (url == MainViewModel.BASE_URL || lastUrl == MainViewModel.BASE_URL)
                    if (isDemoLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = LocalContentColor.current,
                            strokeWidth = 3.dp
                        )
                    } else {
                        TvText("DEMO", fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FileInputSection(
    selectedFileName: String,
    hasSelectedFile: Boolean,
    isLoading: Boolean,
    onPickFile: () -> Unit,
    onConnect: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TvText(
            text = "FILE CONFIGURATION",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.4f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Surface(
            onClick = onPickFile,
            modifier = Modifier.fillMaxWidth().height(68.dp),
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color.White.copy(alpha = 0.03f),
                focusedContainerColor = Color.White.copy(alpha = 0.1f),
                contentColor = Color.White,
                focusedContentColor = Color.White
            ),
            border = ClickableSurfaceDefaults.border(
                border = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))),
                focusedBorder = Border(BorderStroke(2.dp, Color(0xFFBB86FC)))
            )
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = null,
                    tint = if (hasSelectedFile) Color(0xFFBB86FC) else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    if (!hasSelectedFile) {
                        Material3Text("Select Local M3U File", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Material3Text("Browse internal storage", color = Color.White.copy(alpha = 0.25f), fontSize = 11.sp)
                    } else {
                        Material3Text("Selected File", color = Color(0xFFBB86FC), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Material3Text(selectedFileName, color = Color.White, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Surface(
            onClick = onConnect,
            enabled = hasSelectedFile && !isLoading,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = MaterialTheme.colorScheme.primary,
                focusedContainerColor = Color.White,
                contentColor = Color.White,
                focusedContentColor = Color.Black
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
               val isFileLoading = isLoading && hasSelectedFile
                if (isFileLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.Black,
                        strokeWidth = 3.dp
                    )
                } else {
                    TvText("CONNECT FILE", fontSize = 16.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
