package com.chesko.stream_pro.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chesko.stream_pro.R
import com.chesko.stream_pro.core.ui.MainViewModel
import android.graphics.Color as AndroidColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val maxVideoHeight by viewModel.maxVideoHeight.collectAsState()
    val playerEngine by viewModel.playerEngine.collectAsState()
    val hwAcceleration by viewModel.hwAcceleration.collectAsState()
    val bufferSize by viewModel.bufferSize.collectAsState()
    val darkMode by viewModel.darkMode.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    val accentColor = MaterialTheme.colorScheme.primary

    var showColorPicker by remember { mutableStateOf(false) }
    var showBufferDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showEngineDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
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
                title = {
                    Text(
                        stringResource(R.string.settings_title),
                        fontWeight = FontWeight.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!isBackInvoked) {
                            isBackInvoked = true
                            onBack()
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
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
            SettingsGroup(
                title = stringResource(R.string.group_player),
                accentColor = accentColor
            ) {
                SettingsItem(
                    icon = Icons.Default.SettingsSuggest,
                    title = stringResource(R.string.item_engine_title),
                    subtitle = stringResource(
                        R.string.item_engine_subtitle,
                        if (playerEngine == "EXO") "ExoPlayer (Default)" else "VLC Player"
                    ),
                    onClick = { showEngineDialog = true }
                )
                SettingsItem(
                    icon = Icons.Default.HighQuality,
                    title = stringResource(R.string.item_quality_title),
                    subtitle = if (maxVideoHeight == 0) stringResource(R.string.item_quality_auto) else stringResource(
                        R.string.item_quality_subtitle,
                        maxVideoHeight
                    ),
                    onClick = { showQualityDialog = true }
                )
                SettingsToggleItem(
                    icon = if (hwAcceleration) Icons.Default.Bolt else Icons.Default.SettingsSuggest,
                    title = stringResource(R.string.item_hw_title),
                    subtitle = if (hwAcceleration) stringResource(R.string.item_hw_subtitle_on) else stringResource(
                        R.string.item_hw_subtitle_off
                    ),
                    checked = hwAcceleration,
                    onCheckedChange = { viewModel.setHwAcceleration(it) },
                    accentColor = accentColor
                )
                SettingsItem(
                    icon = Icons.Default.Timer,
                    title = stringResource(R.string.item_buffer_title),
                    subtitle = stringResource(R.string.item_buffer_subtitle, bufferSize),
                    onClick = { showBufferDialog = true }
                )
            }

            SettingsGroup(
                title = stringResource(R.string.group_display),
                accentColor = accentColor
            ) {
                SettingsToggleItem(
                    icon = Icons.Default.DarkMode,
                    title = stringResource(R.string.item_dark_mode),
                    subtitle = stringResource(R.string.item_dark_mode_subtitle),
                    checked = darkMode,
                    onCheckedChange = { viewModel.setDarkMode(it) },
                    accentColor = accentColor
                )
                val langLabel = when (appLanguage) {
                    "in" -> "Indonesia"
                    "ms" -> "Malay"
                    "ar" -> "Arabic"
                    else -> "English"
                }
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = stringResource(R.string.item_language),
                    subtitle = stringResource(R.string.item_language_subtitle, langLabel),
                    onClick = { showLanguageDialog = true }
                )
                SettingsItem(
                    icon = Icons.Default.Palette,
                    title = stringResource(R.string.item_accent_color),
                    subtitle = stringResource(R.string.item_accent_subtitle),
                    onClick = { showColorPicker = true }
                )
            }

            SettingsGroup(title = stringResource(R.string.group_data), accentColor = accentColor) {
                SettingsItem(
                    icon = Icons.Default.DeleteSweep,
                    title = stringResource(R.string.item_clear_cache),
                    subtitle = stringResource(R.string.item_cache_subtitle),
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

        if (showLanguageDialog) {
            LanguagePickerDialog(
                currentLanguage = appLanguage,
                onDismiss = { showLanguageDialog = false },
                onLanguageSelected = {
                    viewModel.setAppLanguage(it)
                    showLanguageDialog = false
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
fun LanguagePickerDialog(
    currentLanguage: String,
    onDismiss: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val languages = listOf(
        "in" to "Indonesia",
        "en" to "English (United States)",
        "ms" to "Malay (Malaysia)",
        "ar" to "Arabic (العربية)"
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
                    stringResource(R.string.dialog_language_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    languages.forEach { (code, label) ->
                        Surface(
                            onClick = {
                                onLanguageSelected(code)
                                (context as? android.app.Activity)?.recreate()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (code == currentLanguage)
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
                                    selected = code == currentLanguage,
                                    onClick = {
                                        onLanguageSelected(code)
                                        (context as? android.app.Activity)?.recreate()
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (code == currentLanguage) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.btn_close), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EnginePickerDialog(
    currentEngine: String,
    onDismiss: () -> Unit,
    onEngineSelected: (String) -> Unit
) {
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
                    stringResource(R.string.dialog_engine_title),
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
                    Text(stringResource(R.string.btn_close), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun QualityPickerDialog(
    currentHeight: Int,
    onDismiss: () -> Unit,
    onQualitySelected: (Int) -> Unit
) {
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
                    stringResource(R.string.dialog_quality_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    qualityOptions.forEach { (height, label) ->
                        val displayLabel =
                            if (height == 0) stringResource(R.string.item_quality_auto) else label
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
                                    text = displayLabel,
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
                    Text(stringResource(R.string.btn_close), fontWeight = FontWeight.Bold)
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
                    stringResource(R.string.dialog_buffer_title),
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
                                    modifier = Modifier.padding(
                                        vertical = 10.dp,
                                        horizontal = 12.dp
                                    ),
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
                                        text = stringResource(R.string.unit_seconds, seconds),
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
                    Text(stringResource(R.string.btn_close), fontWeight = FontWeight.Bold)
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
                .width(340.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            stringResource(R.string.item_accent_color),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            stringResource(R.string.dialog_color_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        modifier = Modifier.size(24.dp),
                        shape = CircleShape,
                        color = selectedColor,
                        border = androidx.compose.foundation.BorderStroke(
                            2.dp,
                            MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {}
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Custom Pickers
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        stringResource(R.string.dialog_color_custom),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    SaturationValuePicker(
                        hue = hsv[0],
                        saturation = hsv[1],
                        value = hsv[2],
                        onValueChange = { s, v -> hsv = floatArrayOf(hsv[0], s, v) }
                    )

                    HuePicker(
                        hue = hsv[0],
                        onHueChange = { h -> hsv = floatArrayOf(h, hsv[1], hsv[2]) }
                    )
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.btn_cancel), fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onColorSelected(selectedColor) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = selectedColor)
                    ) {
                        val isLightColor = hsv[2] > 0.7f && hsv[1] < 0.4f
                        Text(
                            stringResource(R.string.btn_apply),
                            fontWeight = FontWeight.Bold,
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
fun SettingsGroup(
    title: String,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable ColumnScope.() -> Unit
) {
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
            Icon(
                icon,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f)
            )
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
        Icon(
            icon,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f),
            modifier = Modifier.size(24.dp)
        )
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
