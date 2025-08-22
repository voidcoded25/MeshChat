package com.meshchat.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshchat.app.core.data.PeerDevice
import com.meshchat.app.core.ble.MeshLink
import com.meshchat.app.core.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PeersViewModel : ViewModel() {
    
    private val _peers = MutableStateFlow<List<PeerDevice>>(emptyList())
    val peers: StateFlow<List<PeerDevice>> = _peers.asStateFlow()
    
    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()
    
    private val _activeConnections = MutableStateFlow<Map<String, MeshLink>>(emptyMap())
    val activeConnections: StateFlow<Map<String, MeshLink>> = _activeConnections.asStateFlow()
    
    init {
        // Load mock data for now - will be replaced with actual BLE discovery
        loadMockPeers()
    }
    
    private fun loadMockPeers() {
        viewModelScope.launch {
            // Mock data for demonstration - replace with actual BLE discovery
            val mockPeers = listOf(
                PeerDevice(
                    address = "00:11:22:33:44:55",
                    addressHash = "a1b2c3d4",
                    ephemeralId = byteArrayOf(0x01, 0x23, 0x45, 0x67, 0x89, 0xAB, 0xCD, 0xEF),
                    rssi = -45
                ),
                PeerDevice(
                    address = "AA:BB:CC:DD:EE:FF",
                    addressHash = "e5f6g7h8",
                    ephemeralId = byteArrayOf(0xFE, 0xDC, 0xBA, 0x98, 0x76, 0x54, 0x32, 0x10),
                    rssi = -67
                ),
                PeerDevice(
                    address = "12:34:56:78:9A:BC",
                    addressHash = "i9j0k1l2",
                    ephemeralId = byteArrayOf(0x55, 0xAA, 0x55, 0xAA, 0x55, 0xAA, 0x55, 0xAA),
                    rssi = -89
                )
            )
            _peers.value = mockPeers
            Logger.i("Loaded ${mockPeers.size} mock peers", "PeersViewModel")
        }
    }
    
    fun startDiscovery() {
        viewModelScope.launch {
            _isDiscovering.value = true
            Logger.i("Started peer discovery", "PeersViewModel")
            // TODO: Integrate with MeshBleManager.startScanning()
        }
    }
    
    fun stopDiscovery() {
        viewModelScope.launch {
            _isDiscovering.value = false
            Logger.i("Stopped peer discovery", "PeersViewModel")
            // TODO: Integrate with MeshBleManager.stopScanning()
        }
    }
    
    fun refreshPeers() {
        viewModelScope.launch {
            Logger.i("Refreshing peers", "PeersViewModel")
            loadMockPeers()
        }
    }
    
    fun getPeerById(address: String): PeerDevice? {
        return _peers.value.find { it.address == address }
    }
    
    fun connectToPeer(peer: PeerDevice) {
        viewModelScope.launch {
            Logger.i("Attempting to connect to peer: ${peer.addressHash}", "PeersViewModel")
            // TODO: Implement actual connection logic using MeshBleManager
            // For now, this is a placeholder
        }
    }
    
    fun sendTestMessage(peer: PeerDevice) {
        viewModelScope.launch {
            val connection = _activeConnections.value[peer.address]
            if (connection != null && connection.isConnected) {
                val testMessage = "hello-mesh".toByteArray(Charsets.UTF_8)
                val success = connection.send(testMessage)
                if (success) {
                    Logger.i("Test message sent successfully to ${peer.addressHash}", "PeersViewModel")
                } else {
                    Logger.w("Failed to send test message to ${peer.addressHash}", "PeersViewModel")
                }
            } else {
                Logger.w("No active connection to ${peer.addressHash}", "PeersViewModel")
            }
        }
    }
    
    fun disconnectFromPeer(peer: PeerDevice) {
        viewModelScope.launch {
            val connection = _activeConnections.value[peer.address]
            if (connection != null) {
                connection.close()
                _activeConnections.value = _activeConnections.value - peer.address
                Logger.i("Disconnected from peer: ${peer.addressHash}", "PeersViewModel")
            }
        }
    }
    
    fun clearPeers() {
        _peers.value = emptyList()
        Logger.i("Cleared all peers", "PeersViewModel")
    }
}
