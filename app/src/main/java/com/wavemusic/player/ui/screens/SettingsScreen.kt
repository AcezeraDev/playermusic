package com.wavemusic.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.wavemusic.player.data.AudioQuality
import com.wavemusic.player.ui.theme.WaveBlue
import com.wavemusic.player.ui.theme.WavePink
import com.wavemusic.player.ui.theme.WavePurple
import com.wavemusic.player.ui.theme.WaveSurface
import com.wavemusic.player.ui.theme.WaveSurfaceBright
import com.wavemusic.player.ui.theme.WaveTextPrimary
import com.wavemusic.player.ui.theme.WaveTextSecondary

@Composable
fun SettingsScreen(
    audioQuality: AudioQuality,
    notificationsEnabled: Boolean,
    appVersion: String,
    onAudioQualitySelected: (AudioQuality) -> Unit,
    onNotificationsChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var darkTheme by rememberSaveable { mutableStateOf(true) }
    var showAboutDialog by rememberSaveable { mutableStateOf(false) }
    var showAudioQualityDialog by rememberSaveable { mutableStateOf(false) }

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
                    text = "Preferências do Wave Music",
                    color = WaveTextSecondary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        item {
            SettingRow(
                title = "Tema escuro",
                subtitle = "Visual neon sempre ativo",
                icon = Icons.Rounded.DarkMode,
                onClick = { darkTheme = !darkTheme }
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
                subtitle = audioQuality.label,
                icon = Icons.Rounded.GraphicEq,
                onClick = { showAudioQualityDialog = true }
            )
        }

        item {
            SettingRow(
                title = "Notificações",
                subtitle = if (notificationsEnabled) {
                    "Controles de mídia ativados"
                } else {
                    "Controles de mídia desativados"
                },
                icon = Icons.Rounded.Notifications,
                onClick = { onNotificationsChanged(!notificationsEnabled) }
            ) {
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = onNotificationsChanged,
                    colors = switchColors()
                )
            }
        }

        item {
            SettingRow(
                title = "Sobre o app",
                subtitle = "Wave Music $appVersion • Kotlin + Jetpack Compose",
                icon = Icons.Rounded.Info,
                onClick = { showAboutDialog = true }
            )
        }
    }

    if (showAudioQualityDialog) {
        AudioQualityDialog(
            selectedQuality = audioQuality,
            onDismiss = { showAudioQualityDialog = false },
            onConfirm = { selected ->
                onAudioQualitySelected(selected)
                showAudioQualityDialog = false
            }
        )
    }

    if (showAboutDialog) {
        AboutAppDialog(
            appVersion = appVersion,
            onDismiss = { showAboutDialog = false }
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() },
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
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            trailing?.invoke()
        }
    }
}

@Composable
private fun AudioQualityDialog(
    selectedQuality: AudioQuality,
    onDismiss: () -> Unit,
    onConfirm: (AudioQuality) -> Unit
) {
    var localSelection by remember(selectedQuality) { mutableStateOf(selectedQuality) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = WaveSurface,
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                DialogHeader(
                    title = "Qualidade do áudio",
                    subtitle = "Escolha sua preferência de reprodução.",
                    onDismiss = onDismiss
                )

                Spacer(modifier = Modifier.size(12.dp))

                AudioQuality.entries.forEach { quality ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { localSelection = quality },
                        color = if (quality == localSelection) {
                            WavePurple.copy(alpha = 0.22f)
                        } else {
                            WaveSurfaceBright.copy(alpha = 0.36f)
                        },
                        shape = RoundedCornerShape(18.dp),
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = quality == localSelection,
                                onClick = { localSelection = quality },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = WavePink,
                                    unselectedColor = WaveTextSecondary
                                )
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = quality.label,
                                    color = WaveTextPrimary,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = quality.description,
                                    color = WaveTextSecondary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                }

                Surface(
                    color = WaveBlue.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(18.dp),
                    tonalElevation = 0.dp
                ) {
                    Text(
                        text = "Aviso: como as músicas são arquivos locais, o Wave Music reproduz a qualidade original do arquivo. Esta preferência fica salva e pode ser usada quando houver fontes ajustáveis no futuro.",
                        color = WaveTextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.size(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = WaveTextSecondary)
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Button(
                        onClick = { onConfirm(localSelection) },
                        colors = ButtonDefaults.buttonColors(containerColor = WavePink),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text("Salvar")
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutAppDialog(
    appVersion: String,
    onDismiss: () -> Unit
) {
    var showChangelog by rememberSaveable { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = WaveSurface,
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                DialogHeader(
                    title = "Wave Music",
                    subtitle = "Player local moderno para suas músicas baixadas.",
                    onDismiss = onDismiss
                )

                Spacer(modifier = Modifier.size(14.dp))

                InfoLine(
                    icon = Icons.Rounded.Info,
                    title = "Versão",
                    value = appVersion
                )
                InfoLine(
                    icon = Icons.Rounded.Code,
                    title = "Tecnologias",
                    value = "Kotlin + Jetpack Compose + Material 3"
                )
                InfoLine(
                    icon = Icons.Rounded.Person,
                    title = "Desenvolvedor",
                    value = "AcezeraDev"
                )

                Spacer(modifier = Modifier.size(8.dp))
                HorizontalDivider(color = WaveSurfaceBright)
                Spacer(modifier = Modifier.size(8.dp))

                TextButton(onClick = { showChangelog = !showChangelog }) {
                    Icon(
                        imageVector = Icons.Rounded.NewReleases,
                        contentDescription = null,
                        tint = WaveBlue
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = if (showChangelog) "Ocultar novidades" else "Ver novidades",
                        color = WaveBlue
                    )
                }

                if (showChangelog) {
                    Surface(
                        color = WavePurple.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(18.dp),
                        tonalElevation = 0.dp
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Changelog",
                                color = WaveTextPrimary,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "• Configurações funcionais\n• Qualidade do áudio salva\n• Notificação com controles\n• Menus, detalhes e playlists melhorados",
                                color = WaveTextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.size(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = WavePurple),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text("Fechar")
                }
            }
        }
    }
}

@Composable
private fun DialogHeader(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit
) {
    Row(verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = WaveTextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Text(
                text = subtitle,
                color = WaveTextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Fechar",
                tint = WaveTextSecondary
            )
        }
    }
}

@Composable
private fun InfoLine(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(WavePurple, WavePink))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = WaveTextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.size(12.dp))
        Column {
            Text(
                text = title,
                color = WaveTextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = value,
                color = WaveTextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
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
