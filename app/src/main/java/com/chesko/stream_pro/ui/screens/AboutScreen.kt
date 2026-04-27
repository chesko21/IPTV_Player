package com.chesko.stream_pro.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chesko.stream_pro.BuildConfig
import com.chesko.stream_pro.R
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    var isBackInvoked by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tentang Aplikasi", fontWeight = FontWeight.Black) },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            
            // App Logo
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.app_icon_android),
                    contentDescription = null,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    Text(
                        "STREAM",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "PRO",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            val versionName = BuildConfig.VERSION_NAME
            Text(
                "Versi $versionName (Stable)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "Stream Pro adalah pemutar IPTV modern yang dirancang untuk memberikan pengalaman menonton terbaik dengan antarmuka yang bersih dan performa tinggi.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Info Section
            val uriHandler = LocalUriHandler.current
            AboutInfoItem("Pengembang", "Chesko Dev Team")
            AboutInfoItem("Lisensi", "Premium Subscription")
            AboutInfoItem(
                label = "Website", 
                value = "https://chesko-25.vercel.app",
                onClick = { uriHandler.openUri("https://chesko-25.vercel.app") }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Donation Section
            DonationSection()

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                "© $currentYear Stream Pro IPTV. All rights reserved.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
fun DonationSection() {
    val clipboardManager = LocalClipboardManager.current
    val danaNumber = "08976248342"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Dukung Pengembang",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Jika Anda menyukai aplikasi ini, Anda dapat memberikan donasi untuk mendukung pengembangan lebih lanjut.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        clipboardManager.setText(AnnotatedString(danaNumber))
                        // Note: Scaffold in the main Composable will need its own snackbar host, 
                        // but for simplicity here we just use the click.
                    },
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "DANA",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF118EEA), // DANA Blue
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            danaNumber,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "a/n Bae***** Ati**",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Salin Nomor",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AboutInfoItem(
    label: String, 
    value: String, 
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label, 
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), 
            fontWeight = FontWeight.Medium
        )
        Text(
            value, 
            color = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
    }
}
