package com.chesko.stream_pro_tv.ui.screens

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
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

@OptIn(ExperimentalTvMaterial3Api::class)
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
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    BackHandler {
        onBack()
    }

    var showBufferDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFF00020A))) {
        
        // Immersive Background
        UniverseBackground(
            primaryColor = MaterialTheme.colorScheme.primary,
            glowAlpha = 0.6f
        )

        // Settings Container
        Row(modifier = Modifier.fillMaxSize()) {
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
                        text = "CONFIGURATION",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp
                    )
                    TvText(
                        text = "UNIVERSE SETTINGS",
                        style = if (isSmall) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp
                    )
                }

                TvSettingsSection(title = "CINEMATIC ENGINE") {
                    TvSettingsActionItem(
                        icon = Icons.Default.HighQuality,
                        title = "Video Quality",
                        subtitle = "Current Target: ${if (maxVideoHeight == 0) "Auto" else "${maxVideoHeight}p"}",
                        onClick = { showQualityDialog = true }
                    )
                    TvSettingsToggleItem(
                        icon = Icons.Default.SlowMotionVideo,
                        title = "Hardware Acceleration",
                        subtitle = "Optimize playback performance for your device",
                        checked = hwAcceleration,
                        onCheckedChange = { viewModel.setHwAcceleration(it) }
                    )
                    TvSettingsActionItem(
                        icon = Icons.Default.Timer,
                        title = "Star-Buffer Size",
                        subtitle = "Current latency buffer: ${bufferSize}s",
                        onClick = { showBufferDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                TvSettingsSection(title = "STAR-CHART MANAGEMENT") {
                    TvSettingsActionItem(
                        icon = Icons.Default.Refresh,
                        title = "Reload Playlist",
                        subtitle = "Re-sync star-charts from the master server",
                        onClick = {
                            viewModel.refreshPlaylist()
                            scope.launch {
                                snackbarHostState.showSnackbar("Playlist synced successfully")
                            }
                        }
                    )
                    TvSettingsActionItem(
                        icon = Icons.Default.DeleteSweep,
                        title = "Clear Space Cache",
                        subtitle = "Clean temporary data and stardust",
                        onClick = {
                            viewModel.clearCache()
                            scope.launch {
                                snackbarHostState.showSnackbar("Cache cleared")
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                TvSettingsSection(title = "APPLICATION LOGS") {
                    TvSettingsActionItem(
                        icon = Icons.Default.Info,
                        title = "About Universe",
                        subtitle = "Version $versionName - Premium Cinematic Edition",
                        onClick = { showAboutDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                TvSettingsSection(title = "STATION EXIT") {
                    TvSettingsActionItem(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        title = "Logout & Disconnect",
                        subtitle = "Clear all charts and exit to terminal",
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

        // Snackbar Host
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

        // --- Dialogs with Universe Styling ---
        
        if (showBufferDialog) {
            UniverseAlertDialog(
                onDismiss = { showBufferDialog = false },
                title = "SELECT BUFFER SIZE",
                icon = Icons.Default.Timer
            ) {
                val bufferOptions = listOf(5, 10, 15, 30, 45, 60)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    bufferOptions.forEach { seconds ->
                        UniverseDialogItem(
                            label = "$seconds SECONDS",
                            isSelected = bufferSize == seconds,
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
                title = "VIDEO TARGET",
                icon = Icons.Default.HighQuality
            ) {
                val qualityOptions = listOf(0, 240, 360, 480, 720, 1080)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    qualityOptions.forEach { height ->
                        UniverseDialogItem(
                            label = if (height == 0) "AUTO-ADAPTIVE" else "${height}P RESOLUTION",
                            isSelected = maxVideoHeight == height,
                            onClick = {
                                viewModel.setMaxVideoHeight(height)
                                showQualityDialog = false
                            }
                        )
                    }
                }
            }
        }

        if (showAboutDialog) {
            UniverseAlertDialog(
                onDismiss = { showAboutDialog = false },
                title = "ABOUT UNIVERSE",
                icon = Icons.Default.Info
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_icon_androidtv),
                        contentDescription = null,
                        modifier = Modifier.size(80.dp).clip(RoundedCornerShape(16.dp))
                    )
                    TvText(
                        "STREAM PRO TV",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    TvText(
                        "PREMIUM EDITION • v$versionName",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    TvText(
                        "Designed for high-performance cinematic experiences on Android TV.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))
                    
                    TvText(
                        "DEVELOPED BY CHESKO",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.4f),
                        letterSpacing = 4.sp
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
    content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0A0A0A),
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier.width(420.dp).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(32.dp)),
        confirmButton = {
            Surface(
                onClick = onDismiss,
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color.White.copy(alpha = 0.05f),
                    focusedContainerColor = Color.White,
                    contentColor = Color.White,
                    focusedContentColor = Color.Black
                ),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    TvText("DISMISS", fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                }
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(16.dp))
                TvText(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                content()
            }
        }
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun UniverseDialogItem(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f),
            focusedContainerColor = MaterialTheme.colorScheme.primary,
            contentColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
            focusedContentColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TvText(label, fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold, letterSpacing = 1.sp)
            if (isSelected) Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(20.dp))
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
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        modifier = Modifier.fillMaxWidth(),
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
    contentColor: Color = Color.White
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
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
