package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.WorkShortcutRepository
import com.example.data.local.model.ExcelRowEntity
import com.example.data.local.model.ProxyProfileEntity
import com.example.data.local.model.TwoFactorKeyEntity
import com.example.service.OverlayStateManager
import com.example.service.OverlayUiState
import com.example.ui.overlay.FloatingOverlayWindowContent
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BrandGreen
import com.example.ui.theme.BrandSky
import kotlin.math.roundToInt

enum class AppNavTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    SHORTCUTS("Apps", Icons.Filled.Apps, Icons.Outlined.Apps),
    PROXY("Proxy", Icons.Filled.Security, Icons.Outlined.Security),
    TWO_FACTOR("2FA", Icons.Filled.Key, Icons.Outlined.Key),
    EXCEL("Excel", Icons.Filled.TableChart, Icons.Outlined.TableChart),
    NAMES("Names", Icons.Filled.Person, Icons.Outlined.Person),
    CLEAR_DATA("Clean", Icons.Filled.CleaningServices, Icons.Outlined.CleaningServices),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: OverlayUiState,
    savedExcelRows: List<ExcelRowEntity>,
    savedProxies: List<ProxyProfileEntity>,
    savedTwoFactorKeys: List<TwoFactorKeyEntity>,
    repository: WorkShortcutRepository?,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(AppNavTab.NAMES) }
    var inAppBubbleVisible by remember { mutableStateOf(true) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        OverlayStateManager.requestedAppTab.collect { tabName ->
            when (tabName) {
                "PROXY" -> selectedTab = AppNavTab.PROXY
                "NAMES" -> selectedTab = AppNavTab.NAMES
                "EXCEL" -> selectedTab = AppNavTab.EXCEL
                "TWO_FACTOR" -> selectedTab = AppNavTab.TWO_FACTOR
                "APPS" -> selectedTab = AppNavTab.SHORTCUTS
                "CLEAR_DATA" -> selectedTab = AppNavTab.CLEAR_DATA
            }
        }
    }

    // Coordinates for in-app movable bubble
    var bubbleOffsetX by remember { mutableFloatStateOf(40f) }
    var bubbleOffsetY by remember { mutableFloatStateOf(240f) }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "My Short App",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    },
                    actions = {
                        // Quick Toggle for Floating Bubble Overlay
                        IconButton(
                            onClick = {
                                OverlayStateManager.toggleOverlayExpanded()
                            },
                            modifier = Modifier.testTag("appbar_bubble_toggle")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (state.proxyState.isConnected) {
                                        Badge(containerColor = BrandGreen)
                                    } else if (state.draftRow.duplicateColumn != null) {
                                        Badge(containerColor = AlertRed)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "Floating Bubble",
                                    tint = BrandSky
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)
                ) {
                    AppNavTab.entries.forEach { tab ->
                        val isSelected = selectedTab == tab
                        val badgeCount = when (tab) {
                            AppNavTab.EXCEL -> if (state.draftRow.duplicateColumn != null) "!" else null
                            AppNavTab.PROXY -> if (state.proxyState.isConnected) "ON" else null
                            else -> null
                        }

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { selectedTab = tab },
                            icon = {
                                if (badgeCount != null) {
                                    BadgedBox(
                                        badge = {
                                            Badge(
                                                containerColor = if (badgeCount == "!") AlertRed else BrandGreen
                                            ) {
                                                Text(badgeCount, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.title
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.title
                                    )
                                }
                            },
                            label = { Text(tab.title, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.name}")
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (selectedTab) {
                    AppNavTab.SHORTCUTS -> AppShortcutsSection(state = state)
                    AppNavTab.NAMES -> NameGeneratorSection(state = state)
                    AppNavTab.EXCEL -> ExcelCollectorSection(
                        state = state,
                        savedRows = savedExcelRows,
                        repository = repository
                    )
                    AppNavTab.TWO_FACTOR -> TwoFactorSection(
                        state = state,
                        savedKeys = savedTwoFactorKeys,
                        repository = repository
                    )
                    AppNavTab.PROXY -> ProxySection(
                        state = state,
                        savedProxies = savedProxies,
                        repository = repository
                    )
                    AppNavTab.CLEAR_DATA -> ClearDataSection(state = state)
                    AppNavTab.SETTINGS -> SettingsSection(
                        state = state,
                        inAppBubbleVisible = inAppBubbleVisible,
                        onToggleInAppBubble = { inAppBubbleVisible = it }
                    )
                }
            }
        }

        // Draggable In-App Movable Messenger Bubble (Interactive Preview & Real In-App Use)
        if (inAppBubbleVisible) {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            bubbleOffsetX.roundToInt(),
                            bubbleOffsetY.roundToInt()
                        )
                    }
                    .testTag("in_app_movable_bubble_container")
            ) {
                FloatingOverlayWindowContent(
                    state = state,
                    onDragStart = { _, _ -> },
                    onDragDelta = { dx, dy ->
                        bubbleOffsetX = (bubbleOffsetX + dx).coerceIn(10f, 750f)
                        bubbleOffsetY = (bubbleOffsetY + dy).coerceIn(120f, 1500f)
                    },
                    onToggleExpand = {
                        OverlayStateManager.toggleOverlayExpanded()
                    },
                    onCloseOverlay = {
                        inAppBubbleVisible = false
                    }
                )
            }
        }
    }
}
