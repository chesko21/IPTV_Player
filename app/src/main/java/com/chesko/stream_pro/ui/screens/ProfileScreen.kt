package com.chesko.stream_pro.ui.screens

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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import coil.compose.AsyncImage
import com.chesko.stream_pro.R
import com.chesko.stream_pro.core.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val userName by viewModel.userName.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val profileImageUri by viewModel.profileImageUri.collectAsState()
    val deviceId by viewModel.deviceId.collectAsState()
    val memberSince by viewModel.memberSince.collectAsState()
    val favoriteChannels by viewModel.favoriteChannels.collectAsState()
    
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showEditEmailDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf("") }
    var tempEmail by remember { mutableStateOf("") }
    var isBackInvoked by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.saveProfileImage(it) }
    }

    if (showEditNameDialog) {
        ModernEditDialog(
            title = "Ubah Nama",
            value = tempName,
            onValueChange = { tempName = it },
            onDismiss = { showEditNameDialog = false },
            onSave = {
                viewModel.updateProfile(tempName, userEmail)
                showEditNameDialog = false
            },
            label = "Nama"
        )
    }

    if (showEditEmailDialog) {
        ModernEditDialog(
            title = "Ubah Email",
            value = tempEmail,
            onValueChange = { tempEmail = it },
            onDismiss = { showEditEmailDialog = false },
            onSave = {
                viewModel.updateProfile(userName, tempEmail)
                showEditEmailDialog = false
            },
            label = "Email"
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil Saya", fontWeight = FontWeight.Black) },
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
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Image & Info
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    )
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { launcher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (profileImageUri != null) {
                    AsyncImage(
                        model = profileImageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = painterResource(id = R.drawable.app_icon_android),
                        placeholder = painterResource(id = R.drawable.app_icon_android)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.app_icon_android),
                        contentDescription = null,
                        modifier = Modifier.size(80.dp)
                    )
                }
                
                // Overlay edit icon
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            null,
                            modifier = Modifier.size(18.dp).padding(vertical = 4.dp),
                            tint = Color.White
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        tempName = userName
                        showEditNameDialog = true
                    }
                ) {
                    Text(
                        userName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Nama",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Text(
                    userEmail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "PREMIUM MEMBER",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Stats Section (Modern Card)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ProfileStatItem("Favorit", favoriteChannels.size.toString())
                    VerticalDivider(modifier = Modifier.height(40.dp).width(1.dp), color = MaterialTheme.colorScheme.outline.copy(0.1f))
                    ProfileStatItem("Playlist", "1")
                    VerticalDivider(modifier = Modifier.height(40.dp).width(1.dp), color = MaterialTheme.colorScheme.outline.copy(0.1f))
                    ProfileStatItem("Expired", "Never")
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Details Section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Informasi Akun",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
                
                ProfileDetailItem(
                    icon = Icons.Default.Email,
                    label = "Email",
                    value = userEmail,
                    onClick = {
                        tempEmail = userEmail
                        showEditEmailDialog = true
                    }
                )
                ProfileDetailItem(
                    icon = Icons.Default.Devices,
                    label = "Device ID",
                    value = deviceId,
                    onClick = {
                        clipboardManager.setText(AnnotatedString(deviceId))
                        scope.launch {
                            snackbarHostState.showSnackbar("Device ID berhasil disalin!")
                        }
                    }
                )
                ProfileDetailItem(Icons.Default.DateRange, "Member Sejak", memberSince)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { /* viewModel.logout() */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                    contentColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Text("Keluar dari Akun", fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun ModernEditDialog(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    label: String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black
            )
        },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text("Simpan", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
fun ProfileStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
    }
}

@Composable
fun ProfileDetailItem(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            Text(value, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
        if (onClick != null) {
            Icon(
                Icons.Default.Edit,
                contentDescription = "Edit",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
        }
    }
}
