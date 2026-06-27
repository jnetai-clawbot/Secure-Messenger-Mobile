package com.jnetaol.securemessenger.ui.screens

import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jnetaol.securemessenger.MainViewModel
import com.jnetaol.securemessenger.data.model.AppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    var showColorPicker by remember { mutableStateOf<String?>(null) }
    var showStoragePicker by remember { mutableStateOf(false) }

    val storagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            val docUri = it.toString()
            val updated = settings.copy(storageLocation = docUri)
            viewModel.updateSettings(updated)
            viewModel.showToast("Storage location changed")
        }
    }

    if (showColorPicker != null) {
        ColorPickerDialog(
            currentColor = when (showColorPicker) {
                "primary" -> Color(settings.primaryColor)
                "background" -> Color(settings.backgroundColor)
                "text" -> Color(settings.textColor)
                "accent" -> Color(settings.accentColor)
                else -> Color.White
            },
            onColorSelected = { color ->
                val updated = when (showColorPicker) {
                    "primary" -> settings.copy(primaryColor = color.value.toLong())
                    "background" -> settings.copy(backgroundColor = color.value.toLong())
                    "text" -> settings.copy(textColor = color.value.toLong())
                    "accent" -> settings.copy(accentColor = color.value.toLong())
                    else -> settings
                }
                viewModel.updateSettings(updated)
                showColorPicker = null
            },
            onDismiss = { showColorPicker = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
        ) {
            SettingsSection("Network") {
                SettingsItem(
                    icon = Icons.Default.Dns,
                    title = "Server Address",
                    subtitle = settings.serverAddress,
                    onClick = {
                        val updated = settings.copy(serverAddress = if (settings.serverAddress == "0.0.0.0") "192.168.1.1" else "0.0.0.0")
                        viewModel.updateSettings(updated)
                    }
                )
                SettingsItem(
                    icon = Icons.Default.SettingsEthernet,
                    title = "Server Port",
                    subtitle = settings.serverPort.toString(),
                    onClick = {
                        val updated = settings.copy(serverPort = if (settings.serverPort == 8080) 9090 else 8080)
                        viewModel.updateSettings(updated)
                    }
                )
                SettingsToggle(
                    icon = Icons.Default.Wifi,
                    title = "Use Local Network",
                    checked = settings.useLocalNetwork,
                    onCheckedChange = {
                        viewModel.updateSettings(settings.copy(useLocalNetwork = it))
                    }
                )
            }

            SettingsSection("Chat") {
                SettingsToggle(
                    icon = Icons.Default.Lock,
                    title = "Encryption Enabled",
                    checked = settings.encryptionEnabled,
                    onCheckedChange = {
                        viewModel.updateSettings(settings.copy(encryptionEnabled = it))
                    }
                )
                SettingsToggle(
                    icon = Icons.Default.FileDownload,
                    title = "Auto-accept Files",
                    checked = settings.autoAcceptFiles,
                    onCheckedChange = {
                        viewModel.updateSettings(settings.copy(autoAcceptFiles = it))
                    }
                )
                SettingsToggle(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    checked = settings.notificationsEnabled,
                    onCheckedChange = {
                        viewModel.updateSettings(settings.copy(notificationsEnabled = it))
                    }
                )
                SettingsToggle(
                    icon = Icons.Default.VolumeUp,
                    title = "Sound",
                    checked = settings.soundEnabled,
                    onCheckedChange = {
                        viewModel.updateSettings(settings.copy(soundEnabled = it))
                    }
                )
                SettingsToggle(
                    icon = Icons.Default.Vibration,
                    title = "Vibration",
                    checked = settings.vibrationEnabled,
                    onCheckedChange = {
                        viewModel.updateSettings(settings.copy(vibrationEnabled = it))
                    }
                )
                SettingsItem(
                    icon = Icons.Default.History,
                    title = "Keep History (Days)",
                    subtitle = "${settings.keepHistoryDays} days",
                    onClick = {
                        val newDays = if (settings.keepHistoryDays == 30) 60 else if (settings.keepHistoryDays == 60) 90 else 30
                        viewModel.updateSettings(settings.copy(keepHistoryDays = newDays))
                    }
                )
            }

            SettingsSection("Storage") {
                SettingsItem(
                    icon = Icons.Default.Folder,
                    title = "Storage Location",
                    subtitle = if (settings.storageLocation.isEmpty()) "Default (Downloads/SecureMessenger)" else settings.storageLocation.take(40) + "...",
                    onClick = {
                        storagePickerLauncher.launch(null)
                    }
                )
                SettingsItem(
                    icon = Icons.Default.Restore,
                    title = "Reset to Default",
                    subtitle = "Use default download location",
                    onClick = {
                        viewModel.updateSettings(settings.copy(storageLocation = ""))
                        viewModel.showToast("Storage location reset to default")
                    }
                )
            }

            SettingsSection("Theme") {
                SettingsToggle(
                    icon = Icons.Default.DarkMode,
                    title = "Dark Mode",
                    checked = settings.isDarkMode,
                    onCheckedChange = {
                        viewModel.updateSettings(settings.copy(isDarkMode = it))
                    }
                )
                SettingsItem(
                    icon = Icons.Default.Palette,
                    title = "Primary Color",
                    subtitle = "Tap to change",
                    onClick = { showColorPicker = "primary" },
                    trailing = {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(settings.primaryColor))
                        )
                    }
                )
                SettingsItem(
                    icon = Icons.Default.FormatPaint,
                    title = "Background Color",
                    subtitle = "Tap to change",
                    onClick = { showColorPicker = "background" },
                    trailing = {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(settings.backgroundColor))
                        )
                    }
                )
                SettingsItem(
                    icon = Icons.Default.TextFields,
                    title = "Text Color",
                    subtitle = "Tap to change",
                    onClick = { showColorPicker = "text" },
                    trailing = {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(settings.textColor))
                        )
                    }
                )
                SettingsItem(
                    icon = Icons.Default.ColorLens,
                    title = "Accent Color",
                    subtitle = "Tap to change",
                    onClick = { showColorPicker = "accent" },
                    trailing = {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(settings.accentColor))
                        )
                    }
                )
                SettingsItem(
                    icon = Icons.Default.Restore,
                    title = "Reset Colors",
                    subtitle = "Restore default theme colors",
                    onClick = {
                        viewModel.updateSettings(settings.copy(
                            primaryColor = 0xFF1A73E8,
                            backgroundColor = if (settings.isDarkMode) 0xFF121212 else 0xFFF5F5F5,
                            textColor = if (settings.isDarkMode) 0xFFFFFFFF else 0xFF1A1A1A,
                            accentColor = 0xFF4CAF50
                        ))
                        viewModel.showToast("Colors reset to defaults")
                    }
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(
            title,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
        if (trailing != null) {
            trailing()
        } else {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun SettingsToggle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            title,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
fun ColorPickerDialog(
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = listOf(
        Color(0xFF1A73E8), Color(0xFFE91E63), Color(0xFF9C27B0),
        Color(0xFF673AB7), Color(0xFF3F51B5), Color(0xFF2196F3),
        Color(0xFF00BCD4), Color(0xFF009688), Color(0xFF4CAF50),
        Color(0xFF8BC34A), Color(0xFFFFEB3B), Color(0xFFFF9800),
        Color(0xFFFF5722), Color(0xFF795548), Color(0xFF607D8B),
        Color(0xFF000000), Color(0xFFFFFFFF), Color(0xFF121212),
        Color(0xFFF5F5F5), Color(0xFF424242), Color(0xFFBDBDBD)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Color") },
        text = {
            Column {
                var selectedColor by remember { mutableStateOf(currentColor) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(selectedColor)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Column {
                    for (row in colors.chunked(7)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .clickable { selectedColor = color }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onColorSelected(selectedColor) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface
    )
}
