package com.meshchat.app.core.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothLeAdvertiser
import android.bluetooth.BluetoothLeScanner
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import com.meshchat.app.core.Logger
import com.meshchat.app.core.data.PeerDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

/**
 * Manages Bluetooth LE operations for the mesh network.
 * Handles advertising and scanning for peer discovery.
 */
class MeshBleManager(private val context: Context) {
    
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var advertiser: BluetoothLeAdvertiser? = null
    private var scanner: BluetoothLeScanner? = null
    
    private val _discoveredPeers = MutableStateFlow<List<PeerDevice>>(emptyList())
    val discoveredPeers: StateFlow<List<PeerDevice>> = _discoveredPeers.asStateFlow()
    
    private var isAdvertising = false
    private var isScanning = false
    
    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Logger.i("BLE advertising started successfully", "MeshBleManager")
            isAdvertising = true
        }
        
        override fun onStartFailure(errorCode: Int) {
            Logger.e("BLE advertising failed with error: $errorCode", null, "MeshBleManager")
            isAdvertising = false
        }
    }
    
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            processScanResult(result)
        }
        
        override fun onScanFailed(errorCode: Int) {
            Logger.e("BLE scanning failed with error: $errorCode", null, "MeshBleManager")
            isScanning = false
        }
    }
    
    /**
     * Start advertising with the given ephemeral ID
     */
    fun startAdvertising(ephemeralId: ByteArray): Boolean {
        if (isAdvertising) {
            Logger.w("Already advertising", "MeshBleManager")
            return false
        }
        
        advertiser = bluetoothAdapter?.bluetoothLeAdvertiser
        if (advertiser == null) {
            Logger.e("Bluetooth LE advertiser not available", null, "MeshBleManager")
            return false
        }
        
        try {
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0) // No timeout
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build()
            
            val advertiseData = AdvertiseData.Builder()
                .addServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID))
                .addServiceData(ParcelUuid(BleConstants.SERVICE_UUID), ephemeralId)
                .build()
            
            advertiser?.startAdvertising(settings, advertiseData, advertiseCallback)
            Logger.i("Started BLE advertising with ephemeral ID: ${ephemeralId.joinToString("") { "%02x".format(it) }}", "MeshBleManager")
            return true
            
        } catch (e: Exception) {
            Logger.e("Failed to start advertising", e, "MeshBleManager")
            return false
        }
    }
    
    /**
     * Stop advertising
     */
    fun stopAdvertising() {
        if (!isAdvertising) return
        
        try {
            advertiser?.stopAdvertising(advertiseCallback)
            isAdvertising = false
            Logger.i("Stopped BLE advertising", "MeshBleManager")
        } catch (e: Exception) {
            Logger.e("Failed to stop advertising", e, "MeshBleManager")
        }
    }
    
    /**
     * Start scanning for peers
     */
    fun startScanning(): Boolean {
        if (isScanning) {
            Logger.w("Already scanning", "MeshBleManager")
            return false
        }
        
        scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            Logger.e("Bluetooth LE scanner not available", null, "MeshBleManager")
            return false
        }
        
        try {
            val scanFilter = ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID))
                .build()
            
            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0) // Report results immediately
                .build()
            
            scanner?.startScan(listOf(scanFilter), scanSettings, scanCallback)
            isScanning = true
            Logger.i("Started BLE scanning", "MeshBleManager")
            return true
            
        } catch (e: Exception) {
            Logger.e("Failed to start scanning", e, "MeshBleManager")
            return false
        }
    }
    
    /**
     * Stop scanning
     */
    fun stopScanning() {
        if (!isScanning) return
        
        try {
            scanner?.stopScan(scanCallback)
            isScanning = false
            Logger.i("Stopped BLE scanning", "MeshBleManager")
        } catch (e: Exception) {
            Logger.e("Failed to stop scanning", e, "MeshBleManager")
        }
    }
    
    /**
     * Process scan results and update discovered peers
     */
    private fun processScanResult(result: ScanResult) {
        try {
            val device = result.device
            val scanRecord = result.scanRecord
            
            // Extract service data (ephemeral ID)
            val serviceData = scanRecord?.getServiceData(ParcelUuid(BleConstants.SERVICE_UUID))
            if (serviceData == null || serviceData.size != BleConstants.EPHEMERAL_ID_SIZE) {
                return
            }
            
            val ephemeralId = serviceData
            val address = device.address
            val addressHash = hashAddress(address)
            val rssi = result.rssi
            
            val peerDevice = PeerDevice(
                address = address,
                addressHash = addressHash,
                ephemeralId = ephemeralId,
                rssi = rssi
            )
            
            updateDiscoveredPeers(peerDevice)
            
        } catch (e: Exception) {
            Logger.e("Failed to process scan result", e, "MeshBleManager")
        }
    }
    
    /**
     * Update the list of discovered peers
     */
    private fun updateDiscoveredPeers(newPeer: PeerDevice) {
        val currentPeers = _discoveredPeers.value.toMutableList()
        
        // Remove existing peer with same address
        currentPeers.removeAll { it.address == newPeer.address }
        
        // Add new peer
        currentPeers.add(newPeer)
        
        // Sort by RSSI (stronger signal first)
        currentPeers.sortByDescending { it.rssi }
        
        _discoveredPeers.value = currentPeers
        
        Logger.d("Updated peer: ${newPeer.address} (${newPeer.getEphemeralIdHex()}) RSSI: ${newPeer.rssi}", "MeshBleManager")
    }
    
    /**
     * Hash the Bluetooth address for privacy
     */
    private fun hashAddress(address: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(address.toByteArray())
            hash.take(8).joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Logger.e("Failed to hash address", e, "MeshBleManager")
            address.substring(0, 8) // Fallback to first 8 chars
        }
    }
    
    /**
     * Check if currently advertising
     */
    fun isCurrentlyAdvertising(): Boolean = isAdvertising
    
    /**
     * Check if currently scanning
     */
    fun isCurrentlyScanning(): Boolean = isScanning
    
    /**
     * Clear all discovered peers
     */
    fun clearPeers() {
        _discoveredPeers.value = emptyList()
    }
    
    /**
     * Connect to a peer device and return a MeshLink
     */
    fun connectToPeer(peer: PeerDevice): MeshLink? {
        try {
            // Find the BluetoothDevice by address
            val device = bluetoothAdapter?.getRemoteDevice(peer.address)
            if (device == null) {
                Logger.e("Failed to get remote device for address: ${peer.address}", null, "MeshBleManager")
                return null
            }
            
            // Create a new GattClientManager for this connection
            val gattClientManager = GattClientManager(context)
            
            // Create and return the MeshLink
            val meshLink = MeshLinkImpl(device, gattClientManager)
            Logger.i("Created MeshLink for peer: ${peer.address}", "MeshBleManager")
            
            return meshLink
            
        } catch (e: Exception) {
            Logger.e("Failed to create MeshLink for peer: ${peer.address}", e, "MeshBleManager")
            return null
        }
    }
}
