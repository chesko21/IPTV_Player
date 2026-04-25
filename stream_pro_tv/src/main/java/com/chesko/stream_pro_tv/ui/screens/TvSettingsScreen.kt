package com.chesko.stream_pro_tv.ui.screens

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
    
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val bgOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2000f,
        animationSpec = infiniteRepeatable(
            animation = tween(40000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bgOffset"
    )

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
        .background(Color(0xFF050505))) {
        
        // Animated Background (Same as HomeScreen)
        Canvas(modifier = Modifier
            .fillMaxSize()
            .blur(100.dp)
        ) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE50914).copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = center.copy(
                        x = center.x + (bgOffset % 1000 - 500), 
                        y = center.y + (bgOffset % 800 - 400)
                    ),
                    radius = size.minDimension * 1.5f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF2979FF).copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = center.copy(
                        x = center.x - (bgOffset % 1200 - 600), 
                        y = center.y - (bgOffset % 600 - 300)
                    ),
                    radius = size.minDimension * 1.2f
                )
            )
        }

        // Settings List
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(
                        horizontal = if (isSmall) 24.dp else 48.dp,
                        vertical = if (isSmall) 16.dp else 32.dp
                    )
                    .verticalScroll(rememberScrollState())
            ) {
                TvText(
                    text = "PENGATURAN",
                    style = if (isSmall) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(bottom = if (isSmall) 12.dp else 20.dp)
                )

                TvSettingsSection(title = "Pemutar Video") {
                    TvSettingsActionItem(
                        icon = Icons.Default.HighQuality,
                        title = "Kualitas Video",
                        subtitle = "Kualitas saat ini: ${if (maxVideoHeight == 0) "Auto" else "${maxVideoHeight}p"}",
                        onClick = { showQualityDialog = true }
                    )
                    TvSettingsToggleItem(
                        icon = Icons.Default.SlowMotionVideo,
                        title = "Hardware Acceleration",
                        subtitle = "Meningkatkan performa pemutaran",
                        checked = hwAcceleration,
                        onCheckedChange = { viewModel.setHwAcceleration(it) }
                    )
                    TvSettingsActionItem(
                        icon = Icons.Default.Timer,
                        title = "Buffer Size",
                        subtitle = "Ukuran buffer saat ini: ${bufferSize}s",
                        onClick = { showBufferDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                TvSettingsSection(title = "Manajemen Data") {
                    TvSettingsActionItem(
                        icon = Icons.Default.Refresh,
                        title = "Reload Playlist",
                        subtitle = "Perbarui daftar saluran dari server",
                        onClick = {
                            viewModel.refreshPlaylist()
                            scope.launch {
                                snackbarHostState.showSnackbar("Playlist berhasil diperbarui")
                            }
                        }
                    )
                    TvSettingsActionItem(
                        icon = Icons.Default.DeleteSweep,
                        title = "Hapus Cache",
                        subtitle = "Bersihkan data sementara aplikasi",
                        onClick = {
                            viewModel.clearCache()
                            scope.launch {
                                snackbarHostState.showSnackbar("Cache berhasil dihapus")
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                TvSettingsSection(title = "Informasi") {
                    TvSettingsActionItem(
                        icon = Icons.Default.Info,
                        title = "Tentang Aplikasi",
                        subtitle = "Versi $versionName - Stream Pro TV Edition",
                        onClick = { showAboutDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                TvSettingsSection(title = "Akun") {
                    TvSettingsActionItem(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        title = "Keluar (Logout)",
                        subtitle = "Hapus playlist dan keluar dari aplikasi",
                        onClick = {
                            viewModel.deleteCurrentPlaylist()
                            onLogout()
                        },
                        contentColor = Color(0xFFE50914)
                    )
                }
            }
        }

        // Snackbar Host for Notifications
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp, end = 24.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    containerColor = Color(0xFF323232),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.widthIn(max = 350.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (data.visuals.message.contains("Cache")) Icons.Default.CheckCircle else Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Material3Text(data.visuals.message, fontSize = 14.sp)
                    }
                }
            }
        }

        // Buffer Size Dialog
        if (showBufferDialog) {
            AlertDialog(
                onDismissRequest = { showBufferDialog = false },
                containerColor = Color(0xFF121212),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.width(360.dp),
                confirmButton = {
                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), contentAlignment = Alignment.Center) {
                        Button(
                            onClick = { showBufferDialog = false },
                            colors = ButtonDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Material3Text("Tutup", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                title = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Material3Text(
                            "Pilih Ukuran Buffer",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = Color.White
                        )
                    }
                },
                text = {
                    val bufferOptions = listOf(5, 10, 15, 30, 45, 60)
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        bufferOptions.forEach { seconds ->
                            val isSelected = bufferSize == seconds
                            Surface(
                                onClick = {
                                    viewModel.setBufferSize(seconds)
                                    showBufferDialog = false
                                },
                                modifier = Modifier.fillMaxWidth().height(40.dp),
                                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(20.dp)),
                                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
                                colors = ClickableSurfaceDefaults.colors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f),
                                    focusedContainerColor = Color.White,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                    focusedContentColor = Color.Black
                                ),
                                border = ClickableSurfaceDefaults.border(
                                    focusedBorder = Border(
                                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Material3Text(
                                        text = "$seconds Detik",
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }
        if (showQualityDialog) {
            AlertDialog(
                onDismissRequest = { showQualityDialog = false },
                containerColor = Color(0xFF121212),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.width(360.dp),
                confirmButton = {
                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), contentAlignment = Alignment.Center) {
                        Button(
                            onClick = { showQualityDialog = false },
                            colors = ButtonDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Material3Text("Tutup", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                title = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HighQuality,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Material3Text(
                            "Pilih Kualitas Video",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = Color.White
                        )
                    }
                },
                text = {
                    val qualityOptions = listOf(0, 240, 360, 480, 720, 1080)
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        qualityOptions.forEach { height ->
                            val isSelected = maxVideoHeight == height
                            Surface(
                                onClick = {
                                    viewModel.setMaxVideoHeight(height)
                                    showQualityDialog = false
                                },
                                modifier = Modifier.fillMaxWidth().height(40.dp),
                                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(20.dp)),
                                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
                                colors = ClickableSurfaceDefaults.colors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f),
                                    focusedContainerColor = Color.White,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                    focusedContentColor = Color.Black
                                ),
                                border = ClickableSurfaceDefaults.border(
                                    focusedBorder = Border(
                                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Material3Text(
                                        text = when(height) {
                                            0 -> "Otomatis (Auto)"
                                            else -> "${height}p"
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }

        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                containerColor = Color(0xFF121212),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.width(360.dp),
                confirmButton = {
                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), contentAlignment = Alignment.Center) {
                        Button(
                            onClick = { showAboutDialog = false },
                            colors = ButtonDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Material3Text("Tutup", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                title = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_icon_androidtv),
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(Modifier.height(12.dp))
                        Material3Text(
                            "Tentang Aplikasi",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = Color.White
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Material3Text(
                            "STREAM PRO TV",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Material3Text(
                            "Versi $versionName (Premium Edition)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(6.dp))
                        Material3Text(
                            "Aplikasi IPTV premium yang dirancang khusus untuk performa tinggi pada perangkat Android TV.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(Modifier.height(6.dp))
                        Material3Text(
                            "DEVELOPED BY",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                        Material3Text(
                            "CHESKO - High Precision Apps",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = Color.White
                        )
                        Spacer(Modifier.height(8.dp))
                        Material3Text(
                            "SUPPORT DEVELOPMENT (DANA)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2979FF),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                        Material3Text(
                            "08976248342 a/n Bae**** Ati***",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = Color.White
                        )
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvSettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(bottom = 6.dp)) {
        TvText(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
            fontSize = 9.sp
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.03f))
                .padding(1.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
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
        modifier = Modifier
            .fillMaxWidth(),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.White,
            contentColor = Color.White.copy(alpha = 0.9f),
            focusedContentColor = Color.Black
        ),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(Color.White.copy(alpha = 0.05f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    TvText(text = title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    TvText(text = subtitle, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 9.sp)
                }
            }
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = Color.Gray
                ),
                modifier = Modifier.size(16.dp)
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
        modifier = Modifier
            .fillMaxWidth(),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.White,
            contentColor = if (contentColor == Color.White) Color.White.copy(alpha = 0.9f) else contentColor,
            focusedContentColor = if (contentColor == Color.White) Color.Black else contentColor
        ),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(Color.White.copy(alpha = 0.05f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                TvText(text = title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                TvText(text = subtitle, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 9.sp)
            }
        }
    }
}
