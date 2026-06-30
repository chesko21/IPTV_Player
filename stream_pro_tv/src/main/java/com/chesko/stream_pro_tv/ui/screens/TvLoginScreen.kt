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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.res.stringResource
import com.chesko.stream_pro_tv.R
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
    val isSmallHeight = configuration.screenHeightDp < 580

    val isLoading by viewModel.isLoading.collectAsState()
    val lastUrl by viewModel.lastUrl.collectAsState()
    val dynamicDemoUrls by viewModel.dynamicDemoUrls.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var url by remember { mutableStateOf(lastUrl) }

    LaunchedEffect(lastUrl) {
        url = lastUrl
    }

    var selectedFilePath by remember { mutableStateOf<String?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    val noFileSelectedMsg = stringResource(R.string.login_no_file)
    
    val displayFileName = if (selectedFilePath != null) selectedFileName else noFileSelectedMsg
    var showFilePicker by remember { mutableStateOf(false) }

    val buttonFocusRequester = remember { FocusRequester() }
    val firstServerFocusRequester = remember { FocusRequester() }
    val urlSurfaceFocusRequester = remember { FocusRequester() }
    var isEditingUrl by remember { mutableStateOf(false) }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    val glowAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        glowAlpha.animateTo(1f, tween(2000))
    }

    LaunchedEffect(Unit) {
        delay(300)
        if (dynamicDemoUrls.isNotEmpty()) {
            firstServerFocusRequester.requestFocus()
        } else {
            buttonFocusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { 
            keyboardController?.hide()
            isEditingUrl = false 
        },
        contentAlignment = Alignment.Center
    ) {
        errorMessage?.let { message ->
            LaunchedEffect(message) {
                delay(3000)
                viewModel.clearError()
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 24.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    colors = SurfaceDefaults.colors(
                        containerColor = Color.Red.copy(alpha = 0.8f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    TvText(
                        text = message,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

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

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isSmallHeight) 24.dp else 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                BrandingSection(isSmallHeight = isSmallHeight)
            }

            LoginFormSection(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                url = url,
                onUrlChange = { url = it.trim() },
                isEditingUrl = isEditingUrl,
                onEditingChange = { isEditingUrl = it },
                urlSurfaceFocusRequester = urlSurfaceFocusRequester,
                isLoading = isLoading,
                onConnect = { viewModel.loadPlaylist(url, onSuccess = onNavigateToHome) },
                buttonFocusRequester = buttonFocusRequester,
                firstServerFocusRequester = firstServerFocusRequester,
                selectedFileName = displayFileName,
                hasSelectedFile = selectedFilePath != null,
                onPickFile = { showFilePicker = true },
                onFileConnect = {
                    selectedFilePath?.let { path ->
                        val content = File(path).readText()
                        viewModel.loadPlaylistFromFile(content = content, onSuccess = onNavigateToHome)
                    }
                },
                lastUrl = lastUrl,
                modifier = Modifier.width(if (isSmallHeight) 360.dp else 420.dp),
                dynamicDemoUrls = dynamicDemoUrls,
                isSmallHeight = isSmallHeight
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
fun BrandingSection(isSmallHeight: Boolean = false) {
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

        val logoBoxSize = if (isSmallHeight) 80.dp else 110.dp
        val logoSize = if (isSmallHeight) 60.dp else 85.dp

        Box(
            modifier = Modifier.size(logoBoxSize),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(com.chesko.stream_pro_tv.R.drawable.app_icon_androidtv),
                contentDescription = "Logo",
                modifier = Modifier.size(logoSize)
            )
        }

        Spacer(modifier = Modifier.height(if (isSmallHeight) 16.dp else 32.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val textStyle = if (isSmallHeight) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displaySmall
                TvText(
                    text = "STREAM",
                    style = textStyle,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                )
                TvText(
                    text = "PRO",
                    style = textStyle,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                )
            }
            
            TvText(
                text = stringResource(R.string.branding_explore),
                style = if (isSmallHeight) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.5f),
                fontWeight = FontWeight.Bold,
                letterSpacing = if (isSmallHeight) 2.sp else 4.sp
            )
        }

        Spacer(modifier = Modifier.height(if (isSmallHeight) 12.dp else 24.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            TvText(
                text = stringResource(R.string.branding_design),
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
    buttonFocusRequester: FocusRequester,
    firstServerFocusRequester: FocusRequester = remember { FocusRequester() },
    selectedFileName: String,
    hasSelectedFile: Boolean,
    onPickFile: () -> Unit,
    onFileConnect: () -> Unit,
    lastUrl: String = "",
    modifier: Modifier = Modifier,
    dynamicDemoUrls: List<String> = emptyList(),
    isSmallHeight: Boolean = false
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
            .padding(if (isSmallHeight) 16.dp else 28.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TabItem(
                text = stringResource(R.string.login_tab_url),
                isSelected = selectedTab == 0,
                onClick = { onTabSelected(0) }
            )
            TabItem(
                text = stringResource(R.string.login_tab_file),
                isSelected = selectedTab == 1,
                onClick = { onTabSelected(1) }
            )
        }

        Spacer(modifier = Modifier.height(if (isSmallHeight) 20.dp else 32.dp))

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
                        buttonFocusRequester = buttonFocusRequester,
                        firstServerFocusRequester = firstServerFocusRequester,
                        lastUrl = lastUrl,
                        dynamicDemoUrls = dynamicDemoUrls,
                        isSmallHeight = isSmallHeight
                    )
                } else {
                    FileInputSection(
                        selectedFileName = selectedFileName,
                        hasSelectedFile = hasSelectedFile,
                        isLoading = isLoading,
                        onPickFile = onPickFile,
                        onConnect = onFileConnect,
                        isSmallHeight = isSmallHeight
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun RowScope.TabItem(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val focusRequester = remember { FocusRequester() }
    Surface(
        onClick = {
            focusRequester.requestFocus()
            onClick()
        },
        modifier = Modifier
            .weight(1f)
            .height(36.dp)
            .focusRequester(focusRequester)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                focusRequester.requestFocus()
                onClick()
            },
        interactionSource = interactionSource,
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
    buttonFocusRequester: FocusRequester,
    firstServerFocusRequester: FocusRequester = remember { FocusRequester() },
    lastUrl: String = "",
    dynamicDemoUrls: List<String> = emptyList(),
    isSmallHeight: Boolean = false
) {
    val inputHeight = if (isSmallHeight) 48.dp else 56.dp
    val buttonHeight = if (isSmallHeight) 44.dp else 50.dp
    val spacerHeight = if (isSmallHeight) 20.dp else 28.dp

    Column(modifier = Modifier.fillMaxWidth()) {
        TvText(
            text = stringResource(R.string.login_source_config),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.4f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Column(modifier = Modifier.fillMaxWidth().padding(bottom = if (isSmallHeight) 12.dp else 20.dp)) {
            TvText(
                text = stringResource(R.string.login_preset_demos),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.3f),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                dynamicDemoUrls.forEachIndexed { index, demoUrl ->
                    val label = stringResource(R.string.login_server_label, index + 1)
                    val isSelected = url == demoUrl
                    val interactionSource = remember { MutableInteractionSource() }
                    val itemFocusRequester = if (index == 0) firstServerFocusRequester else remember { FocusRequester() }
                    
                    Surface(
                        onClick = { 
                            itemFocusRequester.requestFocus()
                            onUrlChange(demoUrl)
                            onEditingChange(false)
                            buttonFocusRequester.requestFocus()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(if (isSmallHeight) 38.dp else 44.dp)
                            .focusRequester(itemFocusRequester)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                itemFocusRequester.requestFocus()
                                onUrlChange(demoUrl)
                                onEditingChange(false)
                                buttonFocusRequester.requestFocus()
                            },
                        interactionSource = interactionSource,
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
            val interactionSource = remember { MutableInteractionSource() }
            Surface(
                onClick = { 
                    urlSurfaceFocusRequester.requestFocus()
                    onEditingChange(true) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(inputHeight)
                    .focusRequester(urlSurfaceFocusRequester)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { 
                        urlSurfaceFocusRequester.requestFocus()
                        onEditingChange(true) 
                    },
                interactionSource = interactionSource,
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
                            Material3Text(stringResource(R.string.login_url_placeholder), color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Material3Text(stringResource(R.string.login_url_hint), color = Color.White.copy(alpha = 0.25f), fontSize = 11.sp)
                        } else {
                            val demoIndex = dynamicDemoUrls.indexOf(url)
                            val displayUrl = if (demoIndex != -1) stringResource(R.string.login_server_label, demoIndex + 1) else url
                            
                            Material3Text(stringResource(R.string.login_target_url), color = Color(0xFFBB86FC), fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                modifier = Modifier.fillMaxWidth().height(inputHeight).focusRequester(textFieldFocusRequester),
                placeholder = { Material3Text(stringResource(R.string.login_url_placeholder_example), color = Color.Gray, fontSize = 14.sp) },
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

        Spacer(modifier = Modifier.height(spacerHeight))

        val interactionSource = remember { MutableInteractionSource() }
        Surface(
            onClick = {
                buttonFocusRequester.requestFocus()
                onConnect()
            },
            enabled = !isLoading && url.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(buttonHeight)
                .focusRequester(buttonFocusRequester)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = !isLoading && url.isNotBlank()
                ) { 
                    buttonFocusRequester.requestFocus()
                    onConnect() 
                },
            interactionSource = interactionSource,
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
                if (isLoading && url.isNotBlank()) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 3.dp
                    )
                } else {
                    TvText(stringResource(R.string.login_btn_login), fontSize = 16.sp, fontWeight = FontWeight.Black)
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
    onConnect: () -> Unit,
    isSmallHeight: Boolean = false
) {
    val inputHeight = if (isSmallHeight) 48.dp else 56.dp
    val buttonHeight = if (isSmallHeight) 44.dp else 50.dp
    val spacerHeight = if (isSmallHeight) 20.dp else 28.dp

    val pickFileInteractionSource = remember { MutableInteractionSource() }
    val pickFileFocusRequester = remember { FocusRequester() }
    val connectInteractionSource = remember { MutableInteractionSource() }
    val connectFocusRequester = remember { FocusRequester() }

    Column(modifier = Modifier.fillMaxWidth()) {
        TvText(
            text = stringResource(R.string.login_file_config),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.4f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Surface(
            onClick = {
                pickFileFocusRequester.requestFocus()
                onPickFile()
            },
            modifier = Modifier.fillMaxWidth().height(inputHeight)
                .focusRequester(pickFileFocusRequester)
                .clickable(
                    interactionSource = pickFileInteractionSource,
                    indication = null
                ) { 
                    pickFileFocusRequester.requestFocus()
                    onPickFile() 
                },
            interactionSource = pickFileInteractionSource,
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
                        Material3Text(stringResource(R.string.login_file_placeholder), color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Material3Text(stringResource(R.string.login_file_hint), color = Color.White.copy(alpha = 0.25f), fontSize = 11.sp)
                    } else {
                        Material3Text(stringResource(R.string.login_file_selected), color = Color(0xFFBB86FC), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Material3Text(selectedFileName, color = Color.White, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(spacerHeight))

        Surface(
            onClick = {
                connectFocusRequester.requestFocus()
                onConnect()
            },
            enabled = hasSelectedFile && !isLoading,
            modifier = Modifier.fillMaxWidth().height(buttonHeight)
                .focusRequester(connectFocusRequester)
                .clickable(
                    interactionSource = connectInteractionSource,
                    indication = null,
                    enabled = hasSelectedFile && !isLoading
                ) { 
                    connectFocusRequester.requestFocus()
                    onConnect() 
                },
            interactionSource = connectInteractionSource,
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
                    TvText(stringResource(R.string.login_btn_connect_file), fontSize = 16.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
