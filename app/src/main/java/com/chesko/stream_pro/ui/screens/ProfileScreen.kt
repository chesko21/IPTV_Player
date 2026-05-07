package com.chesko.stream_pro.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalClipboard
import android.content.ClipData
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.chesko.stream_pro.R
import com.chesko.stream_pro.core.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    windowSize: WindowSizeClass,
    onBack: () -> Unit
) {
    val userName by viewModel.userName.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val profileImageUri by viewModel.profileImageUri.collectAsState()
    val deviceId by viewModel.deviceId.collectAsState()
    val memberSince by viewModel.memberSince.collectAsState()
    val favoriteChannels by viewModel.favoriteChannels.collectAsState()

    var refreshTrigger by remember { mutableStateOf(0) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showEditEmailDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf("") }
    var tempEmail by remember { mutableStateOf("") }
    var isBackInvoked by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboard.current
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.saveProfileImage(it)
            refreshTrigger++
            scope.launch {
                snackbarHostState.showSnackbar(
                    context.getString(R.string.msg_photo_updated),
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    if (showEditNameDialog) {
        AnimatedEditDialog(
            title = stringResource(R.string.edit_name),
            currentValue = userName,
            onValueChange = { tempName = it },
            onDismiss = {
                showEditNameDialog = false
                tempName = ""
            },
            onSave = {
                if (tempName.isNotBlank()) {
                    viewModel.updateProfile(tempName, userEmail)
                    showEditNameDialog = false
                    tempName = ""
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.msg_name_updated),
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            },
            placeholder = stringResource(R.string.hint_enter_name),
            icon = Icons.Default.Person,
            accentColor = MaterialTheme.colorScheme.primary
        )
    }

    if (showEditEmailDialog) {
        AnimatedEditDialog(
            title = stringResource(R.string.edit_email),
            currentValue = userEmail,
            onValueChange = { tempEmail = it },
            onDismiss = {
                showEditEmailDialog = false
                tempEmail = ""
            },
            onSave = {
                if (tempEmail.isNotBlank() && tempEmail.contains("@")) {
                    viewModel.updateProfile(userName, tempEmail)
                    showEditEmailDialog = false
                    tempEmail = ""
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.msg_email_updated),
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            },
            placeholder = stringResource(R.string.hint_enter_email),
            icon = Icons.Default.Email,
            accentColor = MaterialTheme.colorScheme.primary,
            isEmail = true
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.profile_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
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
                            contentDescription = stringResource(R.string.btn_cancel),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val contentPadding = if (windowSize.widthSizeClass == WindowWidthSizeClass.Expanded) 48.dp else 20.dp
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = contentPadding)
                .padding(top = 8.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (windowSize.widthSizeClass == WindowWidthSizeClass.Expanded) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(48.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ProfileHeader(
                            profileImageUri,
                            refreshTrigger,
                            launcher,
                            userName,
                            userEmail,
                            onEditName = {
                                tempName = userName
                                showEditNameDialog = true
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        StatsRow(favoriteChannels)
                    }
                    
                    Column(
                        modifier = Modifier.weight(1.2f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ProfileDetails(
                            userEmail,
                            deviceId,
                            memberSince,
                            onEditEmail = {
                                tempEmail = userEmail
                                showEditEmailDialog = true
                            },
                            clipboardManager,
                            snackbarHostState,
                            context,
                            scope
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        LogoutButton()
                    }
                }
            } else {
                ProfileHeader(
                    profileImageUri,
                    refreshTrigger,
                    launcher,
                    userName,
                    userEmail,
                    onEditName = {
                        tempName = userName
                        showEditNameDialog = true
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                StatsRow(favoriteChannels)

                Spacer(modifier = Modifier.height(24.dp))

                ProfileDetails(
                    userEmail,
                    deviceId,
                    memberSince,
                    onEditEmail = {
                        tempEmail = userEmail
                        showEditEmailDialog = true
                    },
                    clipboardManager,
                    snackbarHostState,
                    context,
                    scope
                )

                Spacer(modifier = Modifier.height(32.dp))

                LogoutButton()
            }
        }
    }
}

@Composable
fun ProfileHeader(
    profileImageUri: String?,
    refreshTrigger: Int,
    launcher: androidx.activity.result.ActivityResultLauncher<String>,
    userName: String,
    userEmail: String,
    onEditName: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Profile Image
        Box(
            modifier = Modifier
                .size(110.dp)
                .clickable { launcher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                key(profileImageUri, refreshTrigger) {
                    if (profileImageUri != null) {
                        AsyncImage(
                            model = profileImageUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // User Info
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onEditName() }
            ) {
                Text(
                    userName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }

            Text(
                userEmail,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun StatsRow(favoriteChannels: List<com.chesko.stream_pro.core.data.model.IptvChannel>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        MinimalStatItem(
            value = favoriteChannels.size.toString(),
            label = stringResource(R.string.stat_favorite)
        )
        MinimalStatItem(
            value = "1",
            label = stringResource(R.string.stat_playlist)
        )
        MinimalStatItem(
            value = "∞",
            label = stringResource(R.string.stat_expired)
        )
    }
}

@Composable
fun ProfileDetails(
    userEmail: String,
    deviceId: String,
    memberSince: String,
    onEditEmail: () -> Unit,
    clipboardManager: androidx.compose.ui.platform.Clipboard,
    snackbarHostState: SnackbarHostState,
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.account_info),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )

        MinimalDetailItem(
            icon = Icons.Default.Email,
            label = stringResource(R.string.label_email),
            value = userEmail,
            onClick = onEditEmail
        )
        MinimalDetailItem(
            icon = Icons.Default.ContentCopy,
            label = stringResource(R.string.label_device_id),
            value = deviceId.take(12) + "...",
            onClick = {
                scope.launch {
                    clipboardManager.setClipEntry(androidx.compose.ui.platform.ClipEntry(android.content.ClipData.newPlainText("Device ID", deviceId)))
                    snackbarHostState.showSnackbar(context.getString(R.string.msg_device_id_copied))
                }
            }
        )
        MinimalDetailItem(
            icon = Icons.Default.DateRange,
            label = stringResource(R.string.label_member_since),
            value = memberSince
        )
    }
}

@Composable
fun LogoutButton() {
    OutlinedButton(
        onClick = { /* viewModel.logout() */ },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error
        ),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
    ) {
        Icon(
            Icons.AutoMirrored.Filled.Logout,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            stringResource(R.string.btn_logout_account),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimatedEditDialog(
    title: String,
    currentValue: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    placeholder: String,
    icon: ImageVector,
    accentColor: Color,
    isEmail: Boolean = false
) {
    var showDialog by remember { mutableStateOf(true) }
    var textValue by remember { mutableStateOf(currentValue) }
    var isFieldFocused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isFieldFocused) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "scale"
    )

    LaunchedEffect(textValue) {
        onValueChange(textValue)
    }

    if (showDialog) {
        Dialog(
            onDismissRequest = {
                showDialog = false
                onDismiss()
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            AnimatedVisibility(
                visible = showDialog,
                enter = fadeIn(animationSpec = tween(300)) +
                        slideInVertically(initialOffsetY = { -it }, animationSpec = tween(400, easing = EaseOutBack)),
                exit = fadeOut(animationSpec = tween(250)) +
                        slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(300, easing = EaseInCubic))
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .scale(scale),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 8.dp,
                        focusedElevation = 12.dp
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            accentColor.copy(alpha = 0.1f),
                                            accentColor.copy(alpha = 0.05f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = accentColor
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            stringResource(if (isEmail) R.string.dialog_update_email else R.string.dialog_update_name),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        OutlinedTextField(
                            value = textValue,
                            onValueChange = { textValue = it },
                            placeholder = {
                                Text(
                                    placeholder,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focusState ->
                                    isFieldFocused = focusState.isFocused
                                },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                cursorColor = accentColor,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                            ),
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            ),
                            leadingIcon = {
                                Icon(
                                    if (isEmail) Icons.Default.Email else Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (isFieldFocused) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            },
                            trailingIcon = {
                                if (textValue.isNotEmpty()) {
                                    IconButton(
                                        onClick = { textValue = "" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Clear",
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    showDialog = false
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Text(
                                    stringResource(R.string.btn_cancel),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Button(
                                onClick = {
                                    showDialog = false
                                    onSave()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accentColor
                                ),
                                enabled = textValue.isNotBlank() &&
                                        if (isEmail) textValue.contains("@") else true
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.btn_save),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MinimalStatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun MinimalDetailItem(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    value,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (onClick != null) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )
            }
        }
    }
}
