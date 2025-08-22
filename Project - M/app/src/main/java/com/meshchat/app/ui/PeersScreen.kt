package com.meshchat.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meshchat.app.R
import com.meshchat.app.core.data.PeerDevice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeersScreen(
    onNavigateBack: () -> Unit,
    onOpenTestChat: (PeerDevice) -> Unit,
    viewModel: PeersViewModel = viewModel()
) {
    val peers by viewModel.peers.collectAsState()
    val isDiscovering by viewModel.isDiscovering.collectAsState()
    val activeConnections by viewModel.activeConnections.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.peers)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshPeers() },
                        enabled = !isDiscovering
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (peers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_peers_found),
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(peers) { peer ->
                    val isConnected = activeConnections.containsKey(peer.address)
                    PeerDeviceCard(
                        peer = peer,
                        onConnect = { viewModel.connectToPeer(peer) },
                        onTestChat = { onOpenTestChat(peer) },
                        onSendTest = { viewModel.sendTestMessage(peer) },
                        onDisconnect = { viewModel.disconnectFromPeer(peer) },
                        isConnected = isConnected
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeerDeviceCard(
    peer: PeerDevice,
    onConnect: () -> Unit,
    onTestChat: () -> Unit,
    onSendTest: () -> Unit,
    onDisconnect: () -> Unit,
    isConnected: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header row with address hash and RSSI
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ID: ${peer.addressHash}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SignalCellular4Bar,
                        contentDescription = "Signal Strength",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${peer.rssi} dBm",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Address
            Text(
                text = "Address: ${peer.address}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Ephemeral ID
            Text(
                text = "Ephemeral ID:",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = peer.getEphemeralIdHex(),
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Connection status
            if (isConnected) {
                Text(
                    text = "Connected",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isConnected) {
                    Button(
                        onClick = onConnect,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = "Connect",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Connect")
                    }
                } else {
                    Button(
                        onClick = onDisconnect,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Disconnect")
                    }
                }
                
                if (isConnected) {
                    Button(
                        onClick = onSendTest,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Text("Send Test")
                    }
                }
                
                Button(
                    onClick = onTestChat,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.test_chat))
                }
            }
        }
    }
}
