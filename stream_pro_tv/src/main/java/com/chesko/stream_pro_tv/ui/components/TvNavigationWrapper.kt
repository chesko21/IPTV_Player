package com.chesko.stream_pro_tv.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.focusGroup
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.chesko.stream_pro_tv.R

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvNavigationWrapper(
    selectedRoute: String,
    onRouteSelected: (String) -> Unit,
    content: @Composable () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    val sidebarWidth by animateDpAsState(
        targetValue = if (isExpanded) 240.dp else 80.dp,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "sidebarWidth"
    )

    val (homeFR, searchFR, liveFR, moviesFR, sportFR, favFR, settingsFR) = remember { FocusRequester.createRefs() }

    BackHandler(enabled = isExpanded) {
        isExpanded = false
    }

    Row(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .width(sidebarWidth)
                .fillMaxHeight()
                .onFocusChanged { isExpanded = it.hasFocus }
                .focusGroup()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF121212),
                            Color(0xFF1A1A1A)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                BrandingSection(isExpanded)

                Spacer(modifier = Modifier.height(20.dp))

                SidebarItem(
                    label = "Home", icon = Icons.Default.Home, isSelected = selectedRoute == "home", isExpanded = isExpanded,
                    modifier = Modifier.focusRequester(homeFR).focusProperties { down = searchFR },
                    onClick = { onRouteSelected("home") }
                )
                SidebarItem(
                    label = "Search", icon = Icons.Default.Search, isSelected = selectedRoute == "search", isExpanded = isExpanded,
                    modifier = Modifier.focusRequester(searchFR).focusProperties { up = homeFR; down = liveFR },
                    onClick = { onRouteSelected("search") }
                )
                SidebarItem(
                    label = "Live TV", icon = Icons.Default.Tv, isSelected = selectedRoute == "live", isExpanded = isExpanded,
                    modifier = Modifier.focusRequester(liveFR).focusProperties { up = searchFR; down = moviesFR },
                    onClick = { onRouteSelected("live") }
                )
                SidebarItem(
                    label = "Movies", icon = Icons.Default.Movie, isSelected = selectedRoute == "movies", isExpanded = isExpanded,
                    modifier = Modifier.focusRequester(moviesFR).focusProperties { up = liveFR; down = sportFR },
                    onClick = { onRouteSelected("movies") }
                )
                SidebarItem(
                    label = "Sports", icon = Icons.Default.SportsSoccer, isSelected = selectedRoute == "sport", isExpanded = isExpanded,
                    modifier = Modifier.focusRequester(sportFR).focusProperties { up = moviesFR; down = favFR },
                    onClick = { onRouteSelected("sport") }
                )
                SidebarItem(
                    label = "Favorites", icon = Icons.Default.Favorite, isSelected = selectedRoute == "favorites", isExpanded = isExpanded,
                    modifier = Modifier.focusRequester(favFR).focusProperties { up = sportFR; down = settingsFR },
                    onClick = { onRouteSelected("favorites") }
                )

                Spacer(modifier = Modifier.weight(1f))

                SidebarItem(
                    label = "Settings", icon = Icons.Default.Settings, isSelected = selectedRoute == "settings", isExpanded = isExpanded,
                    modifier = Modifier.focusRequester(settingsFR).focusProperties { up = favFR },
                    onClick = { onRouteSelected("settings") }
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            content()
        }
    }
}

@Composable
private fun BrandingSection(isExpanded: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        AnimatedContent(
            targetState = isExpanded,
            transitionSpec = {
                fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                        scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)) togetherWith
                        fadeOut(animationSpec = tween(90))
            },
            label = "branding"
        ) { expanded ->
            if (expanded) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.app_icon_androidtv),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row {
                            Text("STREAM", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = (-1).sp)
                            Text("PRO", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, letterSpacing = (-1).sp)
                        }
                        Text(text = "PREMIUM IPTV PLAYER By CHESKO", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Image(
                    painter = painterResource(id = R.drawable.app_icon_androidtv),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SidebarItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.primary,
            contentColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
            focusedContentColor = Color.White
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // Box dengan lebar tetap memastikan ikon tidak bergerak saat ekspansi
            Box(
                modifier = Modifier.width(56.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp))
            }
            
            // Animasi teks muncul dari samping
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(animationSpec = tween(300)) + expandHorizontally(),
                exit = fadeOut(animationSpec = tween(150)) + shrinkHorizontally()
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}
