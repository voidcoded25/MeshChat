package com.meshchat.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meshchat.app.core.data.MeshStats
import com.meshchat.app.core.ui.DiagnosticsViewModel
import com.meshchat.app.core.Logger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: DiagnosticsViewModel = viewModel()
) {
    val stats by viewModel.stats.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val exportPath by viewModel.exportPath.collectAsState()
    
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { 
            // TODO: Implement actual export to URI
            viewModel.exportLogs(context.filesDir)
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.refreshStats()
        viewModel.refreshLogs()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diagnostics") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshStats() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = { viewModel.clearLogs() }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear Logs")
                    }
                    IconButton(
                        onClick = { exportLauncher.launch(null) },
                        enabled = !isExporting
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Export Logs")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StatisticsCard(stats = stats)
            }
            
            item {
                LogsCard(
                    logs = logs,
                    isExporting = isExporting,
                    exportPath = exportPath
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsCard(stats: MeshStats) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Mesh Statistics",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            StatRow("Ephemeral ID", stats.currentEphemeralId.ifEmpty { "Not set" })
            StatRow("MTU Size", "${stats.mtuSize} bytes")
            StatRow("Active Connections", "${stats.activeConnections}")
            StatRow("Total Relayed", "${stats.totalRelayedMessages}")
            StatRow("Seen Set Size", "${stats.seenSetSize}")
            StatRow("Battery Impact", stats.batteryImpact.name)
            StatRow("Uptime", stats.getFormattedUptime())
            StatRow("Last Message", stats.getFormattedLastMessageTime())
            StatRow("Total Bytes", stats.getFormattedBytesTransferred())
            StatRow("Chunks", "${stats.chunkCount}")
            StatRow("Reassembly Success", "${stats.reassemblySuccessCount}")
            StatRow("Deduplication Hits", "${stats.dedupHits}")
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsCard(
    logs: List<Logger.LogEntry>,
    isExporting: Boolean,
    exportPath: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Logs (${logs.size})",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
            
            if (exportPath != null) {
                Text(
                    text = "Exported to: $exportPath",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            if (logs.isEmpty()) {
                Text(
                    text = "No logs available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 32.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logs.take(100)) { entry ->
                        LogEntryItem(entry = entry)
                    }
                }
            }
        }
    }
}

@Composable
fun LogEntryItem(entry: Logger.LogEntry) {
    val color = when (entry.level) {
        "E" -> MaterialTheme.colorScheme.error
        "W" -> MaterialTheme.colorScheme.tertiary
        "I" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Text(
        text = "${entry.level}/${entry.tag}: ${entry.message}",
        style = MaterialTheme.typography.bodySmall,
        color = color,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.fillMaxWidth()
    )
}
