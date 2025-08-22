package com.meshchat.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meshchat.app.core.data.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.saveSettings() },
                        enabled = !isSaving
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mesh Network Settings
            SettingsSection(title = "Mesh Network") {
                // Rotation Interval
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ephemeral ID Rotation",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "${settings.ephemeralIdRotationIntervalMinutes} min",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Slider(
                    value = settings.ephemeralIdRotationIntervalMinutes.toFloat(),
                    onValueChange = { viewModel.updateRotationInterval(it.toInt()) },
                    valueRange = Settings.MIN_ROTATION_INTERVAL.toFloat()..Settings.MAX_ROTATION_INTERVAL.toFloat(),
                    steps = (Settings.MAX_ROTATION_INTERVAL - Settings.MIN_ROTATION_INTERVAL - 1),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                // Scan Mode
                Text(
                    text = "Scan Mode",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 16.dp)
                )
                
                Settings.ScanMode.values().forEach { scanMode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.scanMode == scanMode,
                            onClick = { viewModel.updateScanMode(scanMode) }
                        )
                        Text(
                            text = scanMode.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                
                // Relay Mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Relay Mode",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = settings.relayMode,
                        onCheckedChange = { viewModel.updateRelayMode(it) }
                    )
                }
                
                // Auto Connect
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Auto Connect",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = settings.autoConnect,
                        onCheckedChange = { viewModel.updateAutoConnect(it) }
                    )
                }
            }
            
            // Logging Settings
            SettingsSection(title = "Logging") {
                // Log Level
                Text(
                    text = "Log Level",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Settings.LogLevel.values().forEach { logLevel ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.logLevel == logLevel,
                            onClick = { viewModel.updateLogLevel(logLevel) }
                        )
                        Text(
                            text = logLevel.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                
                // Max Log Entries
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Max Log Entries",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "${settings.maxLogEntries}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Slider(
                    value = settings.maxLogEntries.toFloat(),
                    onValueChange = { viewModel.updateMaxLogEntries(it.toInt()) },
                    valueRange = Settings.MIN_LOG_ENTRIES.toFloat()..Settings.MAX_LOG_ENTRIES.toFloat(),
                    steps = (Settings.MAX_LOG_ENTRIES - Settings.MIN_LOG_ENTRIES) / 100 - 1,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            // Notification Settings
            SettingsSection(title = "Notifications") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enable Notifications",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = settings.enableNotifications,
                        onCheckedChange = { viewModel.updateNotifications(it) }
                    )
                }
                
                if (settings.enableNotifications) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sound",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Switch(
                            checked = settings.enableSound,
                            onCheckedChange = { viewModel.updateSound(it) }
                        )
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Vibration",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Switch(
                            checked = settings.enableVibration,
                            onCheckedChange = { viewModel.updateVibration(it) }
                        )
                    }
                }
            }
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.resetToDefaults() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset to Defaults")
                }
                
                Button(
                    onClick = { viewModel.saveSettings() },
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isSaving) "Saving..." else "Save")
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            content()
        }
    }
}
