package com.chesko.stream_pro_tv.ui.components

import android.os.Environment
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import androidx.compose.ui.res.stringResource
import com.chesko.stream_pro_tv.R
import java.io.File

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvFilePicker(
    onFileSelected: (File) -> Unit,
    onDismiss: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isSmallHeight = configuration.screenHeightDp < 580
    
    val backFocusRequester = remember { FocusRequester() }
    
    var currentDirectory by remember { mutableStateOf(Environment.getExternalStorageDirectory()) }
    val files = remember(currentDirectory) {
        try {
            currentDirectory.listFiles()?.filter {
                it.isDirectory || it.extension.lowercase() in listOf("m3u", "m3u8")
            }?.sortedWith(
                compareBy({ !it.isDirectory }, { it.name.lowercase() })
            ) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    LaunchedEffect(Unit) {
        backFocusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(if (isSmallHeight) 0.6f else 0.7f)
                .fillMaxHeight(if (isSmallHeight) 0.7f else 0.8f)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A1A1A).copy(alpha = 0.98f),
                            Color(0xFF0A0A0A).copy(alpha = 1.0f)
                        )
                    )
                )
                .border(
                    1.dp, 
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.1f),
                            Color(0xFFBB86FC).copy(alpha = 0.05f)
                        )
                    ), 
                    RoundedCornerShape(24.dp)
                )
                .padding(if (isSmallHeight) 16.dp else 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val backInteractionSource = remember { MutableInteractionSource() }
                
                Surface(
                    onClick = {
                        backFocusRequester.requestFocus()
                        onDismiss()
                    },
                    modifier = Modifier
                        .focusRequester(backFocusRequester)
                        .clickable(
                            interactionSource = backInteractionSource,
                            indication = null
                        ) {
                            backFocusRequester.requestFocus()
                            onDismiss()
                        },
                    interactionSource = backInteractionSource,
                    shape = ClickableSurfaceDefaults.shape(CircleShape),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = Color.White.copy(alpha = 0.05f),
                        focusedContainerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        focusedContentColor = Color.Black
                    ),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
                    border = ClickableSurfaceDefaults.border(
                        focusedBorder = Border(
                            border = BorderStroke(2.dp, Color.White),
                            shape = CircleShape
                        )
                    )
                ) {
                    Box(modifier = Modifier.size(if (isSmallHeight) 32.dp else 40.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = stringResource(R.string.content_desc_back), 
                            modifier = Modifier.size(if (isSmallHeight) 18.dp else 22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(if (isSmallHeight) 16.dp else 20.dp))
                Column {
                    Text(
                        text = stringResource(R.string.file_picker_explorer),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = currentDirectory.absolutePath.uppercase(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.3f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = if (isSmallHeight) 8.sp else 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isSmallHeight) 16.dp else 24.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.02f))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(if (isSmallHeight) 6.dp else 8.dp)
                ) {
                    if (currentDirectory.parentFile != null &&
                        currentDirectory.absolutePath != Environment.getExternalStorageDirectory().absolutePath &&
                        currentDirectory.absolutePath != "/"
                    ) {
                        item {
                            FileItem(
                                name = "..",
                                subtitle = stringResource(R.string.file_picker_parent_dir),
                                isDirectory = true,
                                isSmallHeight = isSmallHeight,
                                onClick = { currentDirectory = currentDirectory.parentFile!! }
                            )
                        }
                    }

                    items(files) { file ->
                        FileItem(
                            name = file.name,
                            subtitle = if (file.isDirectory) stringResource(R.string.file_picker_system_dir) else stringResource(R.string.file_picker_m3u_file, (file.length() / 1024).toInt()),
                            isDirectory = file.isDirectory,
                            isSmallHeight = isSmallHeight,
                            onClick = {
                                if (file.isDirectory) {
                                    currentDirectory = file
                                } else {
                                    onFileSelected(file)
                                }
                            }
                        )
                    }

                    if (files.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    stringResource(R.string.file_picker_no_files),
                                    color = Color.White.copy(alpha = 0.2f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FileItem(
    name: String,
    subtitle: String,
    isDirectory: Boolean,
    isSmallHeight: Boolean,
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
            .focusRequester(focusRequester)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                focusRequester.requestFocus()
                onClick()
            },
        interactionSource = interactionSource,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            focusedContentColor = Color.Black
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, Color.White),
                shape = RoundedCornerShape(12.dp)
            )
        ),
        glow = ClickableSurfaceDefaults.glow(
            focusedGlow = Glow(
                elevationColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                elevation = 15.dp
            )
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = if (isSmallHeight) 6.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(if (isSmallHeight) 32.dp else 36.dp)
                    .background(
                        if (isDirectory) Color.White.copy(alpha = 0.05f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDirectory) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = null,
                    modifier = Modifier.size(if (isSmallHeight) 16.dp else 18.dp),
                    tint = if (isDirectory) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = name,
                    fontSize = if (isSmallHeight) 13.sp else 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle.uppercase(),
                    fontSize = if (isSmallHeight) 8.sp else 9.sp,
                    color = Color.White.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    maxLines = 1
                )
            }
        }
    }
}
