package com.wavemusic.player.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.wavemusic.player.ui.theme.WaveBackground
import com.wavemusic.player.ui.theme.WaveBlue
import com.wavemusic.player.ui.theme.WavePink
import com.wavemusic.player.ui.theme.WavePurple
import com.wavemusic.player.ui.theme.WaveTextSecondary

enum class WaveTab(val label: String) {
    Home("Início"),
    Search("Buscar"),
    Library("Biblioteca"),
    Settings("Config.")
}

private data class NavItem(val tab: WaveTab, val icon: ImageVector)

private val items = listOf(
    NavItem(WaveTab.Home, Icons.Rounded.Home),
    NavItem(WaveTab.Search, Icons.Rounded.Search),
    NavItem(WaveTab.Library, Icons.Rounded.LibraryMusic),
    NavItem(WaveTab.Settings, Icons.Rounded.Settings)
)

@Composable
fun BottomNavigationBar(
    selectedTab: WaveTab,
    onTabSelected: (WaveTab) -> Unit
) {
    NavigationBar(
        containerColor = WaveBackground.copy(alpha = 0.98f),
        tonalElevation = 0.dp
    ) {
        items.forEachIndexed { index, item ->
            val selectedColor = when (index) {
                0 -> WavePurple
                1 -> WaveBlue
                2 -> WavePink
                else -> WavePurple
            }

            NavigationBarItem(
                selected = selectedTab == item.tab,
                onClick = { onTabSelected(item.tab) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.tab.label
                    )
                },
                label = { Text(item.tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = selectedColor,
                    selectedTextColor = selectedColor,
                    indicatorColor = selectedColor.copy(alpha = 0.14f),
                    unselectedIconColor = WaveTextSecondary,
                    unselectedTextColor = WaveTextSecondary
                )
            )
        }
    }
}
