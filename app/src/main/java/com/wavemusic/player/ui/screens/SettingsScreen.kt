package com.wavemusic.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wavemusic.player.ui.theme.WaveBlue
import com.wavemusic.player.ui.theme.WavePink
import com.wavemusic.player.ui.theme.WavePurple
import com.wavemusic.player.ui.theme.WaveSurface
import com.wavemusic.player.ui.theme.WaveSurfaceBright
import com.wavemusic.player.ui.theme.WaveTextPrimary
import com.wavemusic.player.ui.theme.WaveTextSecondary

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    var darkTheme by rememberSaveable { mutableStateOf(true) }
    var notifications by rememberSaveable { mutableStateOf(true) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 22.dp, end = 20.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Configurações",
                    color = WaveTextPrimary,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Preferências visuais do Wave Music",
                    color = WaveTextSecondary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        item {
            SettingRow(
                title = "Tema escuro",
                subtitle = "Visual neon sempre ativo",
                icon = Icons.Rounded.DarkMode
            ) {
                Switch(
                    checked = darkTheme,
                    onCheckedChange = { darkTheme = it },
                    colors = switchColors()
                )
            }
        }

        item {
            SettingRow(
                title = "Qualidade do áudio",
                subtitle = "Reprodução local na qualidade original do arquivo",
                icon = Icons.Rounded.GraphicEq
            )
        }

        item {
            SettingRow(
                title = "Notificações",
                subtitle = "Pronto para controle em segundo plano no futuro",
                icon = Icons.Rounded.Notifications
            ) {
                Switch(
                    checked = notifications,
                    onCheckedChange = { notifications = it },
                    colors = switchColors()
                )
            }
        }

        item {
            SettingRow(
                title = "Sobre o app",
                subtitle = "Wave Music 1.0 • Player local em Kotlin + Compose",
                icon = Icons.Rounded.Info
            )
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    trailing: @Composable (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = WaveSurface.copy(alpha = 0.78f),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(WavePurple, WaveBlue))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = WaveTextPrimary,
                    modifier = Modifier.size(25.dp)
                )
            }
            Spacer(modifier = Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = WaveTextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = WaveTextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            trailing?.invoke()
        }
    }
}

@Composable
private fun switchColors() = SwitchDefaults.colors(
    checkedThumbColor = WaveTextPrimary,
    checkedTrackColor = WavePurple,
    uncheckedThumbColor = WaveTextSecondary,
    uncheckedTrackColor = WaveSurfaceBright
)
