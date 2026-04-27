package com.chesko.stream_pro.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit) {
    var isBackInvoked by remember { mutableStateOf(false) }
    val faqList = remember {
        listOf(
            FaqItem(
                "Bagaimana cara menambah playlist?",
                "Anda dapat menambah playlist dengan menekan tombol 'Tambah' di layar login, lalu masukkan URL M3U yang valid atau pilih file dari penyimpanan perangkat Anda."
            ),
            FaqItem(
                "Kenapa saluran tidak bisa diputar?",
                "Pastikan koneksi internet Anda stabil. Beberapa saluran mungkin memerlukan VPN atau memiliki batasan wilayah. Jika masalah berlanjut, hubungi penyedia playlist Anda."
            ),
            FaqItem(
                "Bagaimana cara melihat jadwal TV (EPG)?",
                "Buka Menu Drawer (ikon garis tiga di pojok kiri atas) lalu pilih 'Panduan TV'. EPG akan otomatis terunduh jika sumber playlist menyediakannya."
            ),
            FaqItem(
                "Bagaimana cara menambah ke Favorit?",
                "Saat memutar video, tekan ikon 'Bintang' di kontrol player. Atau Anda bisa menekan lama pada item saluran di daftar beranda."
            ),
            FaqItem(
                "Aplikasi terasa lambat, apa yang harus dilakukan?",
                "Anda bisa mencoba membersihkan cache di Pengaturan Android, atau kurangi jumlah playlist yang aktif jika terlalu banyak."
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pusat Bantuan", fontWeight = FontWeight.Black) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Pertanyaan yang Sering Diajukan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "Cari jawaban untuk masalah Anda di bawah ini",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }

            items(faqList) { faq ->
                FaqExpandableItem(faq)
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Masih Butuh Bantuan?",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Hubungi tim dukungan kami melalui email di https://chesko-25.vercel.app untuk bantuan lebih lanjut.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

data class FaqItem(val question: String, val answer: String)

@Composable
fun FaqExpandableItem(faq: FaqItem) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded },
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = faq.question,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = faq.answer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}
