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
import com.wavemusic.player.data.AccentTheme
import com.wavemusic.player.data.AudioQuality
import com.wavemusic.player.data.EqualizerPreset
import com.wavemusic.player.data.SleepTimerOption
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
    accentTheme: AccentTheme,
    notificationsEnabled: Boolean,
    crossfadeEnabled: Boolean,
    continueListeningEnabled: Boolean,
    sleepTimerLabel: String?,
    appVersion: String,
    onAudioQualitySelected: (AudioQuality) -> Unit,
    onEqualizerSelected: (EqualizerPreset) -> Unit,
    onAccentThemeSelected: (AccentTheme) -> Unit,
    onNotificationsChanged: (Boolean) -> Unit,
    onCrossfadeChanged: (Boolean) -> Unit,
    onContinueListeningChanged: (Boolean) -> Unit,
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
        OptionDialog(
            title = "Equalizador",
            subtitle = "Preset aplicado quando o Android permite controlar o áudio local.",
            options = EqualizerPreset.entries,
            selected = equalizerPreset,
            label = { it.label },
            description = { "Preset ${it.label}" },
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
                Icon(icon, null, tint = WaveTextPrimary, modifier = Modifier.size(25.dp))
            }
            Spacer(modifier = Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = WaveTextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, color = WaveTextSecondary, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            trailing?.invoke()
        }
    }
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
