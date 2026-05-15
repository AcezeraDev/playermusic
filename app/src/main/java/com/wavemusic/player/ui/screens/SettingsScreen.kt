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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.wavemusic.player.data.model.AccentTheme
import com.wavemusic.player.data.model.AudioQuality
import com.wavemusic.player.data.model.EqualizerPreset
import com.wavemusic.player.data.model.SleepTimerOption
import com.wavemusic.player.ui.components.MusicIconCluster
import com.wavemusic.player.ui.components.NeonCard
import com.wavemusic.player.ui.components.SettingsItem
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
    equalizerPreset: EqualizerPreset,
    customEqualizerGains: List<Short>,
    accentTheme: AccentTheme,
    notificationsEnabled: Boolean,
    crossfadeEnabled: Boolean,
    continueListeningEnabled: Boolean,
    resumePlaybackPositionEnabled: Boolean,
    localVideosEnabled: Boolean,
    sleepTimerLabel: String?,
    appVersion: String,
    onAudioQualitySelected: (AudioQuality) -> Unit,
    onEqualizerSelected: (EqualizerPreset, List<Short>) -> Unit,
    onAccentThemeSelected: (AccentTheme) -> Unit,
    onNotificationsChanged: (Boolean) -> Unit,
    onCrossfadeChanged: (Boolean) -> Unit,
    onContinueListeningChanged: (Boolean) -> Unit,
    onResumePlaybackPositionChanged: (Boolean) -> Unit,
    onLocalVideosChanged: (Boolean) -> Unit,
    onSleepTimerSelected: (SleepTimerOption?) -> Unit,
    onExportBackup: () -> String,
    onImportBackup: (String) -> Boolean,
    modifier: Modifier = Modifier
) {
    var darkTheme by rememberSaveable { mutableStateOf(true) }
    var showAboutDialog by rememberSaveable { mutableStateOf(false) }
    var showAudioQualityDialog by rememberSaveable { mutableStateOf(false) }
    var showEqualizerDialog by rememberSaveable { mutableStateOf(false) }
    var showThemeDialog by rememberSaveable { mutableStateOf(false) }
    var showTimerDialog by rememberSaveable { mutableStateOf(false) }
    var showBackupDialog by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 22.dp, end = 20.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            NeonCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(34.dp),
                colors = listOf(WavePurple, WaveBlue, WavePink),
                contentPadding = PaddingValues(20.dp)
            ) {
                Column {
                Text(
                    text = "Configurações",
                    color = WaveTextPrimary,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Preferências premium do Wave Music",
                    color = WaveTextSecondary,
                    style = MaterialTheme.typography.bodyLarge
                )
                MusicIconCluster(
                    modifier = Modifier.padding(top = 12.dp),
                    icons = listOf(Icons.Rounded.GraphicEq, Icons.Rounded.Timer, Icons.Rounded.Notifications),
                    colors = listOf(WaveBlue, WavePurple, WavePink)
                )
                }
            }
        }

        item {
            SettingRow("Tema escuro", "Visual neon sempre ativo", Icons.Rounded.DarkMode, onClick = { darkTheme = !darkTheme }) {
                Switch(darkTheme, { darkTheme = it }, colors = switchColors())
            }
        }
        item {
            SettingRow("Tema neon", accentTheme.label, Icons.Rounded.Palette, onClick = { showThemeDialog = true })
        }
        item {
            SettingRow("Qualidade do áudio", audioQuality.label, Icons.Rounded.GraphicEq, onClick = { showAudioQualityDialog = true })
        }
        item {
            SettingRow("Equalizador", equalizerPreset.label, Icons.Rounded.Tune, onClick = { showEqualizerDialog = true })
        }
        item {
            SettingRow("Timer", sleepTimerLabel ?: "Desligado", Icons.Rounded.Timer, onClick = { showTimerDialog = true })
        }
        item {
            SettingRow("Crossfade", "Fade suave ao iniciar músicas", Icons.Rounded.RestartAlt, onClick = { onCrossfadeChanged(!crossfadeEnabled) }) {
                Switch(crossfadeEnabled, onCrossfadeChanged, colors = switchColors())
            }
        }
        item {
            SettingRow("Continuar ouvindo", "Retoma a última música carregada", Icons.Rounded.Save, onClick = { onContinueListeningChanged(!continueListeningEnabled) }) {
                Switch(continueListeningEnabled, onContinueListeningChanged, colors = switchColors())
            }
        }
        item {
            SettingRow(
                title = "Notificações",
                subtitle = if (notificationsEnabled) "Controles de mídia ativados" else "Controles de mídia desativados",
                icon = Icons.Rounded.Notifications,
                onClick = { onNotificationsChanged(!notificationsEnabled) }
            ) {
                Switch(notificationsEnabled, onNotificationsChanged, colors = switchColors())
            }
        }
        item {
            SettingRow(
                "Retomar minutagem",
                if (resumePlaybackPositionEnabled) "Volta no mesmo ponto da música" else "Sempre começa do início",
                Icons.Rounded.RestartAlt,
                onClick = { onResumePlaybackPositionChanged(!resumePlaybackPositionEnabled) }
            ) {
                Switch(resumePlaybackPositionEnabled, onResumePlaybackPositionChanged, colors = switchColors())
                }
            }
        item {
            SettingRow(
                "Videos locais",
                if (localVideosEnabled) "Arquivos de video entram na biblioteca" else "Biblioteca focada em audio",
                Icons.Rounded.VideoLibrary,
                onClick = { onLocalVideosChanged(!localVideosEnabled) }
            ) {
                Switch(localVideosEnabled, onLocalVideosChanged, colors = switchColors())
            }
        }
        item {
            SettingRow("Widget", "Adicione o widget pela tela inicial do Android", Icons.Rounded.Widgets)
        }
        item {
            SettingRow("Backup local", "Exportar/importar playlists em JSON", Icons.Rounded.Backup, onClick = { showBackupDialog = true })
        }
        item {
            SettingRow("Sobre o app", "Wave Music $appVersion • Kotlin + Jetpack Compose", Icons.Rounded.Info, onClick = { showAboutDialog = true })
        }
    }

    if (showAudioQualityDialog) {
        OptionDialog(
            title = "Qualidade do áudio",
            subtitle = "Arquivos locais são reproduzidos na qualidade original.",
            options = AudioQuality.entries,
            selected = audioQuality,
            label = { it.label },
            description = { it.description },
            onDismiss = { showAudioQualityDialog = false },
            onConfirm = onAudioQualitySelected
        )
    }
    if (showEqualizerDialog) {
        EqualizerDialog(
            selected = equalizerPreset,
            customGains = customEqualizerGains,
            onDismiss = { showEqualizerDialog = false },
            onConfirm = onEqualizerSelected
        )
    }
    if (showThemeDialog) {
        OptionDialog(
            title = "Tema neon",
            subtitle = "Escolha a cor principal do app.",
            options = AccentTheme.entries,
            selected = accentTheme,
            label = { it.label },
            description = { "Gradiente ${it.label.lowercase()} para telas e controles." },
            swatch = { it.primary },
            onDismiss = { showThemeDialog = false },
            onConfirm = onAccentThemeSelected
        )
    }
    if (showTimerDialog) {
        SleepTimerDialog(
            activeLabel = sleepTimerLabel,
            onDismiss = { showTimerDialog = false },
            onConfirm = onSleepTimerSelected
        )
    }
    if (showBackupDialog) {
        BackupDialog(
            onDismiss = { showBackupDialog = false },
            onExportBackup = onExportBackup,
            onImportBackup = onImportBackup
        )
    }
    if (showAboutDialog) {
        AboutAppDialog(appVersion = appVersion, onDismiss = { showAboutDialog = false })
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
    SettingsItem(
        title = title,
        subtitle = subtitle,
        icon = icon,
        onClick = onClick,
        trailing = trailing
    )
}

@Composable
private fun <T> OptionDialog(
    title: String,
    subtitle: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    description: (T) -> String,
    swatch: ((T) -> Color)? = null,
    onDismiss: () -> Unit,
    onConfirm: (T) -> Unit
) {
    var localSelection by remember(selected) { mutableStateOf(selected) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(Modifier.fillMaxWidth(), color = WaveSurface, shape = RoundedCornerShape(28.dp), tonalElevation = 0.dp) {
            Column(Modifier.padding(18.dp)) {
                DialogHeader(title, subtitle, onDismiss)
                Spacer(Modifier.size(12.dp))
                options.forEach { option ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { localSelection = option },
                        color = if (option == localSelection) WavePurple.copy(alpha = 0.22f) else WaveSurfaceBright.copy(alpha = 0.36f),
                        shape = RoundedCornerShape(18.dp),
                        tonalElevation = 0.dp
                    ) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = option == localSelection,
                                onClick = { localSelection = option },
                                colors = RadioButtonDefaults.colors(selectedColor = WavePink, unselectedColor = WaveTextSecondary)
                            )
                            swatch?.let {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(it(option))
                                )
                                Spacer(Modifier.size(10.dp))
                            }
                            Column(Modifier.weight(1f)) {
                                Text(label(option), color = WaveTextPrimary, fontWeight = FontWeight.Bold)
                                Text(description(option), color = WaveTextSecondary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Spacer(Modifier.size(8.dp))
                }
                Text(
                    text = "Aviso: músicas locais já usam a qualidade do arquivo. Preferências avançadas são aplicadas quando o Android e o arquivo permitem.",
                    color = WaveTextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(WaveBlue.copy(alpha = 0.12f))
                        .padding(12.dp)
                )
                Spacer(Modifier.size(16.dp))
                DialogActions(onDismiss) {
                    onConfirm(localSelection)
                    onDismiss()
                }
            }
        }
    }
}

@Composable
private fun EqualizerDialog(
    selected: EqualizerPreset,
    customGains: List<Short>,
    onDismiss: () -> Unit,
    onConfirm: (EqualizerPreset, List<Short>) -> Unit
) {
    var localSelection by remember(selected) { mutableStateOf(selected) }
    var localBands by remember(customGains) { mutableStateOf(customGains) }
    val safeBands = if (localBands.size == EqualizerPreset.Custom.bandGains.size) {
        localBands
    } else {
        EqualizerPreset.Custom.bandGains
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(Modifier.fillMaxWidth(), color = WaveSurface, shape = RoundedCornerShape(28.dp), tonalElevation = 0.dp) {
            Column(Modifier.padding(18.dp)) {
                DialogHeader("Equalizador", "Escolha um preset ou ajuste as bandas do modo personalizado.", onDismiss)
                Spacer(Modifier.size(12.dp))
                EqualizerPreset.entries.forEach { option ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { localSelection = option },
                        color = if (option == localSelection) WavePurple.copy(alpha = 0.22f) else WaveSurfaceBright.copy(alpha = 0.36f),
                        shape = RoundedCornerShape(18.dp),
                        tonalElevation = 0.dp
                    ) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = option == localSelection,
                                onClick = { localSelection = option },
                                colors = RadioButtonDefaults.colors(selectedColor = WavePink, unselectedColor = WaveTextSecondary)
                            )
                            Column(Modifier.weight(1f)) {
                                Text(option.label, color = WaveTextPrimary, fontWeight = FontWeight.Bold)
                                Text(
                                    if (option == EqualizerPreset.Custom) "Ajuste manual salvo no aparelho." else "Preset ${option.label}",
                                    color = WaveTextSecondary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    Spacer(Modifier.size(8.dp))
                }
                if (localSelection == EqualizerPreset.Custom) {
                    Surface(
                        color = WaveBlue.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(18.dp),
                        tonalElevation = 0.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Grave", "Medio grave", "Medio", "Presenca", "Agudo").forEachIndexed { index, label ->
                                val value = safeBands.getOrElse(index) { 0 }.toFloat()
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = label,
                                        color = WaveTextSecondary,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = value.toInt().toString(),
                                        color = WaveTextPrimary,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                                Slider(
                                    value = value,
                                    onValueChange = { newValue ->
                                        localBands = safeBands.toMutableList().also {
                                            it[index] = newValue.toInt().toShort()
                                        }
                                    },
                                    valueRange = -1200f..1200f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = WavePink,
                                        activeTrackColor = WavePink,
                                        inactiveTrackColor = WaveSurfaceBright
                                    )
                                )
                            }
                        }
                    }
                    Spacer(Modifier.size(12.dp))
                }
                DialogActions(onDismiss) {
                    onConfirm(localSelection, safeBands)
                    onDismiss()
                }
            }
        }
    }
}

@Composable
private fun SleepTimerDialog(
    activeLabel: String?,
    onDismiss: () -> Unit,
    onConfirm: (SleepTimerOption?) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(Modifier.fillMaxWidth(), color = WaveSurface, shape = RoundedCornerShape(28.dp), tonalElevation = 0.dp) {
            Column(Modifier.padding(18.dp)) {
                DialogHeader("Timer", activeLabel?.let { "Ativo: $it" } ?: "Pausar automaticamente ao terminar.", onDismiss)
                Spacer(Modifier.size(12.dp))
                SleepTimerOption.entries.forEach { option ->
                    TextButton(
                        onClick = {
                            onConfirm(option)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(option.label, color = WaveTextPrimary)
                    }
                }
                HorizontalDivider(color = WaveSurfaceBright)
                TextButton(
                    onClick = {
                        onConfirm(null)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Desligar timer", color = WavePink)
                }
            }
        }
    }
}

@Composable
private fun BackupDialog(
    onDismiss: () -> Unit,
    onExportBackup: () -> String,
    onImportBackup: (String) -> Boolean
) {
    var backupText by remember { mutableStateOf(onExportBackup()) }
    var message by remember { mutableStateOf("Copie o JSON para guardar seu backup local.") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(Modifier.fillMaxWidth(), color = WaveSurface, shape = RoundedCornerShape(28.dp), tonalElevation = 0.dp) {
            Column(Modifier.padding(18.dp)) {
                DialogHeader("Backup local", message, onDismiss)
                Spacer(Modifier.size(12.dp))
                OutlinedTextField(
                    value = backupText,
                    onValueChange = { backupText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 320.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = textFieldColors()
                )
                Spacer(Modifier.size(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { backupText = onExportBackup(); message = "Backup exportado de novo." }) {
                        Text("Exportar", color = WaveBlue)
                    }
                    Spacer(Modifier.size(8.dp))
                    Button(
                        onClick = {
                            message = if (onImportBackup(backupText)) "Backup importado com sucesso." else "JSON inválido."
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WavePink),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text("Importar")
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutAppDialog(appVersion: String, onDismiss: () -> Unit) {
    var showChangelog by rememberSaveable { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(Modifier.fillMaxWidth(), color = WaveSurface, shape = RoundedCornerShape(28.dp), tonalElevation = 0.dp) {
            Column(Modifier.padding(18.dp)) {
                DialogHeader("Wave Music", "Player local moderno para suas músicas baixadas.", onDismiss)
                Spacer(Modifier.size(14.dp))
                InfoLine(Icons.Rounded.Info, "Versão", appVersion)
                InfoLine(Icons.Rounded.Code, "Tecnologias", "Kotlin + Jetpack Compose + Material 3")
                InfoLine(Icons.Rounded.Person, "Desenvolvedor", "AcezeraDev")
                Spacer(Modifier.size(8.dp))
                HorizontalDivider(color = WaveSurfaceBright)
                TextButton(onClick = { showChangelog = !showChangelog }) {
                    Icon(Icons.Rounded.NewReleases, null, tint = WaveBlue)
                    Spacer(Modifier.size(8.dp))
                    Text(if (showChangelog) "Ocultar novidades" else "Ver novidades", color = WaveBlue)
                }
                if (showChangelog) {
                    Text(
                        text = "• Fila e histórico\n• Estatísticas locais\n• Equalizador e timer\n• Backup JSON\n• Widget simples\n• Player visual premium",
                        color = WaveTextSecondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(WavePurple.copy(alpha = 0.16f))
                            .padding(12.dp)
                    )
                }
                Spacer(Modifier.size(12.dp))
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
private fun DialogHeader(title: String, subtitle: String, onDismiss: () -> Unit) {
    Row(verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = WaveTextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(subtitle, color = WaveTextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
        IconButton(onClick = onDismiss) {
            Icon(Icons.Rounded.Close, "Fechar", tint = WaveTextSecondary)
        }
    }
}

@Composable
private fun DialogActions(onDismiss: () -> Unit, onSave: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onDismiss) {
            Text("Cancelar", color = WaveTextSecondary)
        }
        Spacer(Modifier.size(8.dp))
        Button(onClick = onSave, colors = ButtonDefaults.buttonColors(containerColor = WavePink), shape = RoundedCornerShape(100.dp)) {
            Text("Salvar")
        }
    }
}

@Composable
private fun InfoLine(icon: ImageVector, title: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(WavePurple, WavePink))),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = WaveTextPrimary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.size(12.dp))
        Column {
            Text(title, color = WaveTextSecondary, style = MaterialTheme.typography.bodySmall)
            Text(value, color = WaveTextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
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

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = WaveTextPrimary,
    unfocusedTextColor = WaveTextPrimary,
    focusedBorderColor = WaveBlue,
    unfocusedBorderColor = WaveSurfaceBright,
    focusedContainerColor = WaveSurface.copy(alpha = 0.56f),
    unfocusedContainerColor = WaveSurface.copy(alpha = 0.48f),
    cursorColor = WaveBlue
)

