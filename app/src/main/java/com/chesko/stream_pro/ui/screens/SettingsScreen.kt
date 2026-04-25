package com.chesko.stream_pro.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import android.graphics.Color as AndroidColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.toArgb
import com.chesko.stream_pro.core.ui.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val maxVideoHeight by viewModel.maxVideoHeight.collectAsState()
    val playerEngine by viewModel.playerEngine.collectAsState()
    val hwAcceleration by viewModel.hwAcceleration.collectAsState()
    val bufferSize by viewModel.bufferSize.collectAsState()
    val darkMode by viewModel.darkMode.collectAsState()
    val accentColorInt by viewModel.accentColor.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    val accentColor = MaterialTheme.colorScheme.primary
    
    var showColorPicker by remember { mutableStateOf(false) }
    var showBufferDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showEngineDialog by remember { mutableStateOf(false) }
    var isBackInvoked by remember { mutableStateOf(false) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!isBackInvoked) {
                            isBackInvoked = true
                            onBack()
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Kembali", 
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            SettingsGroup(title = "Pemutar Video", accentColor = accentColor) {
                SettingsItem(
                    icon = Icons.Default.SettingsSuggest,
                    title = "Player Engine",
                    subtitle = "Saat ini: ${if (playerEngine == "EXO") "ExoPlayer (Default)" else "VLC Player"}",
                    onClick = { showEngineDialog = true }
                )
                SettingsItem(
                    icon = Icons.Default.HighQuality,
                    title = "Kualitas Video",
                    subtitle = if (maxVideoHeight == 0) "Otomatis (Auto)" else "Maksimal: ${maxVideoHeight}p",
                    onClick = { showQualityDialog = true }
                )
                SettingsToggleItem(
                    icon = Icons.Default.SlowMotionVideo,
                    title = "Hardware Acceleration",
                    subtitle = "Meningkatkan performa pemutaran",
                    checked = hwAcceleration,
                    onCheckedChange = { viewModel.setHwAcceleration(it) },
                    accentColor = accentColor
                )
                SettingsItem(
                    icon = Icons.Default.Timer,
                    title = "Buffer Size",
                    subtitle = "Ukuran buffer saat ini: ${bufferSize}s",
                    onClick = { showBufferDialog = true }
                )
            }

            SettingsGroup(title = "Tampilan", accentColor = accentColor) {
                SettingsToggleItem(
                    icon = Icons.Default.DarkMode,
                    title = "Mode Gelap",
                    subtitle = "Aktifkan tema gelap permanen",
                    checked = darkMode,
                    onCheckedChange = { viewModel.setDarkMode(it) },
                    accentColor = accentColor
                )
                SettingsItem(
                    icon = Icons.Default.Palette,
                    title = "Warna Aksen",
                    subtitle = "Pilih warna identitas aplikasi",
                    onClick = { showColorPicker = true }
                )
            }

            SettingsGroup(title = "Data & Penyimpanan", accentColor = accentColor) {
                SettingsItem(
                    icon = Icons.Default.DeleteSweep,
                    title = "Hapus Cache",
                    subtitle = "Bersihkan data sementara aplikasi",
                    onClick = { viewModel.clearCache() }
                )
            }
        }

        if (showColorPicker) {
            ColorPickerDialog(
                onDismiss = { showColorPicker = false },
                onColorSelected = { 
                    viewModel.setAccentColor(it.toArgb())
                    showColorPicker = false
                }
            )
        }

        if (showBufferDialog) {
            BufferPickerDialog(
                currentBuffer = bufferSize,
                onDismiss = { showBufferDialog = false },
                onBufferSelected = { 
                    viewModel.setBufferSize(it)
                    showBufferDialog = false
                }
            )
        }

        if (showQualityDialog) {
            QualityPickerDialog(
                currentHeight = maxVideoHeight,
                onDismiss = { showQualityDialog = false },
                onQualitySelected = { 
                    viewModel.setMaxVideoHeight(it)
                    showQualityDialog = false
                }
            )
        }

        if (showEngineDialog) {
            EnginePickerDialog(
                currentEngine = playerEngine,
                onDismiss = { showEngineDialog = false },
                onEngineSelected = { 
                    viewModel.setPlayerEngine(it)
                    showEngineDialog = false
                }
            )
        }
    }
}

@Composable
fun EnginePickerDialog(currentEngine: String, onDismiss: () -> Unit, onEngineSelected: (String) -> Unit) {
    val engines = listOf(
        "EXO" to "ExoPlayer (Google)",
        "VLC" to "VLC Player (LibVLC)"
    )

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(280.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Player Engine",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    engines.forEach { (engine, label) ->
                        Surface(
                            onClick = { onEngineSelected(engine) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (engine == currentEngine) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else 
                                Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = engine == currentEngine,
                                    onClick = { onEngineSelected(engine) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (engine == currentEngine) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Tutup", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun QualityPickerDialog(currentHeight: Int, onDismiss: () -> Unit, onQualitySelected: (Int) -> Unit) {
    val qualityOptions = listOf(
        0 to "Otomatis (Auto)",
        360 to "360p",
        480 to "480p",
        720 to "720p (HD)",
        1080 to "1080p (FHD)"
    )

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(260.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Kualitas Video",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    qualityOptions.forEach { (height, label) ->
                        Surface(
                            onClick = { onQualitySelected(height) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (height == currentHeight) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else 
                                Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = height == currentHeight,
                                    onClick = { onQualitySelected(height) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (height == currentHeight) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Tutup", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BufferPickerDialog(currentBuffer: Int, onDismiss: () -> Unit, onBufferSelected: (Int) -> Unit) {
    val bufferOptions = listOf(5, 10, 15, 30, 45, 60, 90, 120)

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(280.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Ukuran Buffer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                Box(modifier = Modifier.heightIn(max = 300.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        bufferOptions.forEach { seconds ->
                            Surface(
                                onClick = { onBufferSelected(seconds) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (seconds == currentBuffer) 
                                    MaterialTheme.colorScheme.primaryContainer 
                                else 
                                    Color.Transparent,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = seconds == currentBuffer,
                                        onClick = { onBufferSelected(seconds) },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "$seconds Detik",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (seconds == currentBuffer) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Tutup", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ColorPickerDialog(onDismiss: () -> Unit, onColorSelected: (Color) -> Unit) {
    val currentPrimary = MaterialTheme.colorScheme.primary
    var hsv by remember { 
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(currentPrimary.toArgb(), hsv)
        mutableStateOf(hsv) 
    }
    
    val selectedColor = remember(hsv) { Color.hsv(hsv[0], hsv[1], hsv[2]) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(320.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Warna Aksen",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )
                    
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "#${Integer.toHexString(selectedColor.toArgb()).uppercase().takeLast(6)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                SaturationValuePicker(
                    hue = hsv[0],
                    saturation = hsv[1],
                    value = hsv[2],
                    onValueChange = { s, v ->
                        hsv = floatArrayOf(hsv[0], s, v)
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                HuePicker(
                    hue = hsv[0],
                    onHueChange = { h ->
                        hsv = floatArrayOf(h, hsv[1], hsv[2])
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = CircleShape,
                        color = selectedColor,
                        border = androidx.compose.foundation.BorderStroke(4.dp, Color.White.copy(0.2f)),
                        shadowElevation = 4.dp
                    ) {}

                    Spacer(modifier = Modifier.weight(1f))

                    TextButton(onClick = onDismiss) {
                        Text("Batal", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = { onColorSelected(selectedColor) },
                        colors = ButtonDefaults.buttonColors(containerColor = selectedColor),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        val isLightColor = hsv[2] > 0.7f && hsv[1] < 0.4f
                        Text(
                            "Terapkan", 
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isLightColor) Color.Black else Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SaturationValuePicker(
    hue: Float,
    saturation: Float,
    value: Float,
    onValueChange: (Float, Float) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(hue) {
                detectTapGestures { offset ->
                    val s = (offset.x / size.width).coerceIn(0f, 1f)
                    val v = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                    onValueChange(s, v)
                }
            }
            .pointerInput(hue) {
                detectDragGestures { change, _ ->
                    val s = (change.position.x / size.width).coerceIn(0f, 1f)
                    val v = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                    onValueChange(s, v)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val saturationGradient = Brush.horizontalGradient(
                colors = listOf(Color.White, Color.hsv(hue, 1f, 1f))
            )
            drawRect(brush = saturationGradient)

            val valueGradient = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black)
            )
            drawRect(brush = valueGradient)

            // Selector
            val posX = saturation * width
            val posY = (1f - value) * height
            drawCircle(
                color = Color.White,
                radius = 8.dp.toPx(),
                center = Offset(posX, posY),
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = Color.Black,
                radius = 7.dp.toPx(),
                center = Offset(posX, posY),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

@Composable
fun HuePicker(
    hue: Float,
    onHueChange: (Float) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(CircleShape)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onHueChange((offset.x / size.width).coerceIn(0f, 1f) * 360f)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    onHueChange((change.position.x / size.width).coerceIn(0f, 1f) * 360f)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val hues = (0..360 step 60).map { Color.hsv(it.toFloat(), 1f, 1f) }
            val hueGradient = Brush.horizontalGradient(colors = hues)
            drawRect(brush = hueGradient)

            // Selector
            val posX = (hue / 360f) * width
            drawCircle(
                color = Color.White,
                radius = 9.dp.toPx(),
                center = Offset(posX, center.y),
                style = Stroke(width = 3.dp.toPx())
            )
            drawCircle(
                color = Color.Black.copy(0.2f),
                radius = 10.dp.toPx(),
                center = Offset(posX, center.y),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

@Composable
fun SettingsGroup(title: String, accentColor: Color = MaterialTheme.colorScheme.primary, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = accentColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f))
        }
    }
}

@Composable
fun SettingsToggleItem(
    icon: ImageVector, 
    title: String, 
    subtitle: String, 
    checked: Boolean, 
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = accentColor, 
                checkedTrackColor = accentColor.copy(0.3f)
            )
        )
    }
}
