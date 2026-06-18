package com.chesko.stream_pro_tv.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.RectangleShape
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
import androidx.compose.ui.res.stringResource
import com.chesko.stream_pro_tv.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvNavigationWrapper(
    selectedRoute: String,
    onRouteSelected: (String) -> Unit,
    showSidebar: Boolean = true,
    content: @Composable () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    val sidebarWidth by animateDpAsState(
        targetValue = if (!showSidebar) 0.dp else if (isExpanded) 220.dp else 70.dp,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "sidebarWidth"
    )

    val (homeFR, searchFR, liveFR, moviesFR, sportFR, favFR, settingsFR) = remember { FocusRequester.createRefs() }

    BackHandler(enabled = isExpanded && showSidebar) {
        isExpanded = false
    }

    Row(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = showSidebar,
            enter = expandHorizontally(),
            exit = shrinkHorizontally()
        ) {
            Box(
                modifier = Modifier
                    .width(sidebarWidth)
                    .fillMaxHeight()
                    .onFocusChanged { isExpanded = it.hasFocus }
                    .focusGroup()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF0A0A0A),
                                Color(0xFF00020A)
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.Start
                ) {
                    BrandingSection(isExpanded)

                    Spacer(modifier = Modifier.height(8.dp))

                    SidebarItem(
                        label = stringResource(R.string.nav_home), icon = Icons.Default.Home, isSelected = selectedRoute == "home", isExpanded = isExpanded,
                        modifier = Modifier.focusRequester(homeFR).focusProperties { down = searchFR },
                        onClick = { onRouteSelected("home") }
                    )
                    SidebarItem(
                        label = stringResource(R.string.nav_search), icon = Icons.Default.Search, isSelected = selectedRoute == "search", isExpanded = isExpanded,
                        modifier = Modifier.focusRequester(searchFR).focusProperties { up = homeFR; down = liveFR },
                        onClick = { onRouteSelected("search") }
                    )
                    SidebarItem(
                        label = stringResource(R.string.nav_live), icon = Icons.Default.Tv, isSelected = selectedRoute == "live", isExpanded = isExpanded,
                        modifier = Modifier.focusRequester(liveFR).focusProperties { up = searchFR; down = moviesFR },
                        onClick = { onRouteSelected("live") }
                    )
                    SidebarItem(
                        label = stringResource(R.string.nav_movies), icon = Icons.Default.Movie, isSelected = selectedRoute == "movies", isExpanded = isExpanded,
                        modifier = Modifier.focusRequester(moviesFR).focusProperties { up = liveFR; down = sportFR },
                        onClick = { onRouteSelected("movies") }
                    )
                    SidebarItem(
                        label = stringResource(R.string.nav_sports), icon = Icons.Default.SportsSoccer, isSelected = selectedRoute == "sport", isExpanded = isExpanded,
                        modifier = Modifier.focusRequester(sportFR).focusProperties { up = moviesFR; down = favFR },
                        onClick = { onRouteSelected("sport") }
                    )
                    SidebarItem(
                        label = stringResource(R.string.nav_favorites), icon = Icons.Default.Favorite, isSelected = selectedRoute == "favorites", isExpanded = isExpanded,
                        modifier = Modifier.focusRequester(favFR).focusProperties { up = sportFR; down = settingsFR },
                        onClick = { onRouteSelected("favorites") }
                    )

                    SidebarItem(
                        label = stringResource(R.string.nav_settings), icon = Icons.Default.Settings, isSelected = selectedRoute == "settings", isExpanded = isExpanded,
                        modifier = Modifier.focusRequester(settingsFR).focusProperties { up = favFR },
                        onClick = { onRouteSelected("settings") }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Subtle divider between sidebar and content
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.1f),
                                    Color.Transparent
                                )
                            )
                        )
                        .align(Alignment.CenterEnd)
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
            .height(if (isExpanded) 70.dp else 60.dp)
            .padding(start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        AnimatedContent(
            targetState = isExpanded,
            transitionSpec = {
                fadeIn(animationSpec = tween(250)) +
                        slideInHorizontally(animationSpec = tween(250)) { -20 } togetherWith
                        fadeOut(animationSpec = tween(150))
            },
            label = "branding"
        ) { expanded ->
            if (expanded) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.app_icon_androidtv),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row {
                            Text("STREAM", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = (-1).sp)
                            Text("PRO", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, letterSpacing = (-1).sp)
                        }
                        Text(
                            text = stringResource(R.string.branding_premium_iptv),
                            style = MaterialTheme.typography.labelSmall, 
                            color = Color.White.copy(alpha = 0.4f), 
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            fontSize = 8.sp
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.app_icon_androidtv),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                }
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
            .height(48.dp)
            .padding(vertical = 1.dp)
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(RectangleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.primary,
            contentColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
            focusedContentColor = Color.White
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.0f)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier.width(56.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon, 
                    contentDescription = label, 
                    modifier = Modifier.size(20.dp)
                )
            }
            
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(animationSpec = tween(300)) + expandHorizontally(),
                exit = fadeOut(animationSpec = tween(150)) + shrinkHorizontally()
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}
