package com.chesko.stream_pro_tv.ui.screens

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text as Material3Text
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Snackbar
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Image
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.chesko.stream_pro_tv.R
import androidx.tv.material3.*
import androidx.tv.material3.Text as TvText
import com.chesko.stream_pro.core.ui.MainViewModel

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun TvSettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isSmall = screenWidth < 600.dp
    val context = LocalContext.current
    
    val versionName = remember {
        try {
            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    val maxVideoHeight by viewModel.maxVideoHeight.collectAsState()
    val hwAcceleration by viewModel.hwAcceleration.collectAsState()
    val bufferSize by viewModel.bufferSize.collectAsState()
    
    val appLanguage by viewModel.appLanguage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val firstItemFocusRequester = remember { FocusRequester() }

    BackHandler {
        onBack()
    }

    var showBufferDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFF00020A))) {

        UniverseBackground(
            primaryColor = MaterialTheme.colorScheme.primary,
            glowAlpha = 0.6f
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .focusProperties {
                    @Suppress("DEPRECATION")
                    enter = { firstItemFocusRequester }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 800.dp)
                    .padding(
                        horizontal = if (isSmall) 24.dp else 48.dp,
                        vertical = if (isSmall) 16.dp else 32.dp
                    )
                    .verticalScroll(rememberScrollState())
            ) {
                Column(modifier = Modifier.padding(bottom = 32.dp)) {
                    TvText(
                        text = stringResource(R.string.settings_config_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp
                    )
                    TvText(
                        text = stringResource(R.string.settings_title),
                        style = if (isSmall) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp
                    )
                }

                TvSettingsSection(title = stringResource(R.string.settings_group_engine)) {
                    TvSettingsActionItem(
                        icon = Icons.Default.HighQuality,
                        title = stringResource(R.string.settings_quality_title),
                        subtitle = stringResource(R.string.settings_quality_subtitle, if (maxVideoHeight == 0) stringResource(R.string.settings_quality_auto) else "${maxVideoHeight}p"),
                        onClick = { showQualityDialog = true },
                        modifier = Modifier.focusRequester(firstItemFocusRequester)
                    )
                    TvSettingsToggleItem(
                        icon = Icons.Default.SlowMotionVideo,
                        title = stringResource(R.string.settings_hw_title),
                        subtitle = stringResource(R.string.settings_hw_subtitle),
                        checked = hwAcceleration,
                        onCheckedChange = { viewModel.setHwAcceleration(it) }
                    )
                    TvSettingsActionItem(
                        icon = Icons.Default.Timer,
                        title = stringResource(R.string.settings_buffer_title),
                        subtitle = stringResource(R.string.settings_buffer_subtitle, bufferSize),
                        onClick = { showBufferDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                TvSettingsSection(title = stringResource(R.string.settings_group_display)) {
                    val langLabel = when (appLanguage) {
                        "en" -> stringResource(R.string.language_en)
                        "in" -> stringResource(R.string.language_in)
                        "ms" -> stringResource(R.string.language_ms)
                        "ar" -> stringResource(R.string.language_ar)
                        "es" -> stringResource(R.string.language_es)
                        "fr" -> stringResource(R.string.language_fr)
                        "pt" -> stringResource(R.string.language_pt)
                        "ru" -> stringResource(R.string.language_ru)
                        "ja" -> stringResource(R.string.language_ja)
                        "zh" -> stringResource(R.string.language_zh)
                        "th" -> stringResource(R.string.language_th)
                        "vi" -> stringResource(R.string.language_vi)
                        else -> stringResource(R.string.label_system_default)
                    }
                    TvSettingsActionItem(
                        icon = Icons.Default.Language,
                        title = stringResource(R.string.settings_language_title),
                        subtitle = stringResource(R.string.settings_language_subtitle, langLabel),
                        onClick = { showLanguageDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                TvSettingsSection(title = stringResource(R.string.settings_group_management)) {
                    TvSettingsActionItem(
                        icon = Icons.Default.Refresh,
                        title = stringResource(R.string.settings_reload_title),
                        subtitle = stringResource(R.string.settings_reload_subtitle),
                        onClick = {
                            viewModel.refreshPlaylist()
                            scope.launch {
                                snackbarHostState.showSnackbar(context.getString(R.string.settings_reload_success))
                            }
                        }
                    )
                    TvSettingsActionItem(
                        icon = Icons.Default.DeleteSweep,
                        title = stringResource(R.string.settings_cache_title),
                        subtitle = stringResource(R.string.settings_cache_subtitle),
                        onClick = {
                            viewModel.clearCache()
                            scope.launch {
                                snackbarHostState.showSnackbar(context.getString(R.string.settings_cache_success))
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                TvSettingsSection(title = stringResource(R.string.settings_group_logs)) {
                    TvSettingsActionItem(
                        icon = Icons.Default.Info,
                        title = stringResource(R.string.settings_about_title),
                        subtitle = stringResource(R.string.settings_about_subtitle, versionName),
                        onClick = { showAboutDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                TvSettingsSection(title = stringResource(R.string.settings_group_exit)) {
                    TvSettingsActionItem(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        title = stringResource(R.string.settings_logout_title),
                        subtitle = stringResource(R.string.settings_logout_subtitle),
                        onClick = {
                            viewModel.deleteCurrentPlaylist()
                            onLogout()
                        },
                        contentColor = MaterialTheme.colorScheme.error
                    )
                }
                
                Spacer(modifier = Modifier.height(48.dp))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp, end = 32.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    containerColor = Color(0xFF1A1A1A),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.widthIn(max = 400.dp).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Material3Text(data.visuals.message, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        
        if (showBufferDialog) {
            UniverseAlertDialog(
                onDismiss = { showBufferDialog = false },
                title = stringResource(R.string.dialog_buffer_title),
                icon = Icons.Default.Timer,
                isSmall = isSmall
            ) {
                val bufferOptions = listOf(5, 10, 15, 30, 45, 60)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    bufferOptions.forEach { seconds ->
                        UniverseDialogItem(
                            label = "$seconds ${stringResource(R.string.dialog_unit_seconds)}",
                            isSelected = bufferSize == seconds,
                            isSmall = isSmall,
                            onClick = {
                                viewModel.setBufferSize(seconds)
                                showBufferDialog = false
                            }
                        )
                    }
                }
            }
        }

        if (showQualityDialog) {
            UniverseAlertDialog(
                onDismiss = { showQualityDialog = false },
                title = stringResource(R.string.dialog_quality_title),
                icon = Icons.Default.HighQuality,
                isSmall = isSmall
            ) {
                val qualityOptions = listOf(0, 240, 360, 480, 720, 1080)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    qualityOptions.forEach { height ->
                        UniverseDialogItem(
                            label = if (height == 0) stringResource(R.string.dialog_auto_adaptive) else stringResource(R.string.dialog_resolution_p, height),
                            isSelected = maxVideoHeight == height,
                            isSmall = isSmall,
                            onClick = {
                                viewModel.setMaxVideoHeight(height)
                                showQualityDialog = false
                            }
                        )
                    }
                }
            }
        }

        if (showLanguageDialog) {
            UniverseAlertDialog(
                onDismiss = { showLanguageDialog = false },
                title = stringResource(R.string.dialog_language_title),
                icon = Icons.Default.Language,
                isSmall = isSmall
            ) {
                val languages = listOf(
                    "en" to stringResource(R.string.language_en),
                    "in" to stringResource(R.string.language_in),
                    "ms" to stringResource(R.string.language_ms),
                    "ar" to stringResource(R.string.language_ar),
                    "es" to stringResource(R.string.language_es),
                    "fr" to stringResource(R.string.language_fr),
                    "pt" to stringResource(R.string.language_pt),
                    "ru" to stringResource(R.string.language_ru),
                    "ja" to stringResource(R.string.language_ja),
                    "zh" to stringResource(R.string.language_zh),
                    "th" to stringResource(R.string.language_th),
                    "vi" to stringResource(R.string.language_vi)
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    languages.forEach { (code, label) ->
                        UniverseDialogItem(
                            label = label,
                            isSelected = appLanguage == code,
                            isSmall = isSmall,
                            onClick = {
                                viewModel.setAppLanguage(code)
                                showLanguageDialog = false
                                (context as? android.app.Activity)?.recreate()
                            }
                        )
                    }
                }
            }
        }

        if (showAboutDialog) {
            UniverseAlertDialog(
                onDismiss = { showAboutDialog = false },
                title = stringResource(R.string.dialog_about_title),
                icon = Icons.Default.Info,
                isSmall = isSmall
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_icon_androidtv),
                        contentDescription = null,
                        modifier = Modifier.size(if (isSmall) 44.dp else 56.dp).clip(RoundedCornerShape(12.dp))
                    )
                    TvText(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    TvText(
                        "${stringResource(R.string.branding_premium)} • v$versionName",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    TvText(
                        stringResource(R.string.branding_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        fontSize = if (isSmall) 8.sp else 10.sp,
                        lineHeight = 14.sp
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))
                    
                    TvText(
                        stringResource(R.string.branding_developed),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.4f),
                        letterSpacing = 2.sp,
                        fontSize = 8.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun UniverseAlertDialog(
    onDismiss: () -> Unit,
    title: String,
    icon: ImageVector,
    isSmall: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0A0A0A),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(if (isSmall) 200.dp else 240.dp)
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
        confirmButton = {
            val interactionSource = remember { MutableInteractionSource() }
            val focusRequester = remember { FocusRequester() }
            Surface(
                onClick = {
                    focusRequester.requestFocus()
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isSmall) 28.dp else 32.dp)
                    .focusRequester(focusRequester)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        focusRequester.requestFocus()
                        onDismiss()
                    },
                interactionSource = interactionSource,
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color.White.copy(alpha = 0.05f),
                    focusedContainerColor = Color.White,
                    contentColor = Color.White,
                    focusedContentColor = Color.Black
                )
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    TvText(
                        stringResource(R.string.dialog_dismiss), 
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black, 
                        letterSpacing = 0.5.sp,
                        fontSize = if (isSmall) 9.sp else 10.sp
                    )
                }
            }
        },
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally, 
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isSmall) 18.dp else 22.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon, 
                        null, 
                        tint = MaterialTheme.colorScheme.primary, 
                        modifier = Modifier.size(if (isSmall) 10.dp else 12.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                TvText(
                    title.uppercase(), 
                    style = MaterialTheme.typography.labelSmall, 
                    fontWeight = FontWeight.Black, 
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = if (isSmall) 160.dp else 220.dp)
                    .padding(top = 2.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                content()
            }
        }
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun UniverseDialogItem(
    label: String, 
    isSelected: Boolean, 
    isSmall: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focusRequester = remember { FocusRequester() }
    Surface(
        onClick = {
            focusRequester.requestFocus()
            onClick()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isSmall) 28.dp else 32.dp)
            .focusRequester(focusRequester)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                focusRequester.requestFocus()
                onClick()
            },
        interactionSource = interactionSource,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f),
            focusedContainerColor = MaterialTheme.colorScheme.primary,
            contentColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
            focusedContentColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TvText(
                label, 
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold, 
                letterSpacing = 0.2.sp,
                fontSize = if (isSmall) 9.sp else 10.sp
            )
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle, 
                    null, 
                    modifier = Modifier.size(if (isSmall) 10.dp else 12.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvSettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        TvText(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
            letterSpacing = 2.sp
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.03f))
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvSettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focusRequester = remember { FocusRequester() }
    Surface(
        onClick = { 
            focusRequester.requestFocus()
            onCheckedChange(!checked) 
        },
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                focusRequester.requestFocus()
                onCheckedChange(!checked)
            },
        interactionSource = interactionSource,
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.White,
            contentColor = Color.White,
            focusedContentColor = Color.Black
        ),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.05f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    TvText(text = title, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    TvText(text = subtitle, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = Color.Gray
                )
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvSettingsActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = Color.White
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focusRequester = remember { FocusRequester() }
    Surface(
        onClick = { 
            focusRequester.requestFocus()
            onClick() 
        },
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                focusRequester.requestFocus()
                onClick()
            },
        interactionSource = interactionSource,
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = if (contentColor != Color.White) contentColor else Color.White,
            contentColor = contentColor,
            focusedContentColor = if (contentColor != Color.White) Color.White else Color.Black
        ),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.05f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                TvText(text = title, fontWeight = FontWeight.Black, fontSize = 14.sp)
                TvText(text = subtitle, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
