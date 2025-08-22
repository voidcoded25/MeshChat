package com.meshchat.app.core.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import com.meshchat.app.core.Logger
import com.meshchat.app.core.data.InboundFrame
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Manages the GATT server for receiving incoming connections and data.
 * Handles the INBOX characteristic for data transfer.
 */
class GattServerManager(private val context: Context) {
    
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    
    private var gattServer: BluetoothGattServer? = null
    private var isServerRunning = false
    
    private val _inboundFrames = MutableStateFlow<List<InboundFrame>>(emptyList())
    val inboundFrames: StateFlow<List<InboundFrame>> = _inboundFrames.asStateFlow()
    
    private val _connectedDevices = MutableStateFlow<Set<String>>(emptySet())
    val connectedDevices: StateFlow<Set<String>> = _connectedDevices.asStateFlow()
    
    companion object {
        private val CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
    
    /**
     * Start the GATT server
     */
    fun startServer(): Boolean {
        if (isServerRunning) {
            Logger.w("GATT server already running", "GattServerManager")
            return false
        }
        
        if (bluetoothAdapter?.bluetoothLeAdvertiser == null) {
            Logger.e("Bluetooth LE not supported", null, "GattServerManager")
            return false
        }
        
        try {
            gattServer = bluetoothManager.openGattServer(context, gattServerCallback)
            if (gattServer == null) {
                Logger.e("Failed to open GATT server", null, "GattServerManager")
                return false
            }
            
            // Create and add the mesh service
            val meshService = createMeshService()
            val success = gattServer?.addService(meshService) ?: false
            
            if (success) {
                isServerRunning = true
                Logger.i("GATT server started successfully", "GattServerManager")
                return true
            } else {
                Logger.e("Failed to add mesh service", null, "GattServerManager")
                return false
            }
            
        } catch (e: Exception) {
            Logger.e("Failed to start GATT server", e, "GattServerManager")
            return false
        }
    }
    
    /**
     * Stop the GATT server
     */
    fun stopServer() {
        if (!isServerRunning) return
        
        try {
            gattServer?.close()
            gattServer = null
            isServerRunning = false
            _connectedDevices.value = emptySet()
            Logger.i("GATT server stopped", "GattServerManager")
        } catch (e: Exception) {
            Logger.e("Failed to stop GATT server", e, "GattServerManager")
        }
    }
    
    /**
     * Create the mesh service with INBOX characteristic
     */
    private fun createMeshService(): BluetoothGattService {
        val service = BluetoothGattService(
            BleConstants.SERVICE_UUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )
        
        // Create INBOX characteristic
        val inboxCharacteristic = BluetoothGattCharacteristic(
            BleConstants.INBOX_CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        
        // Add client characteristic configuration descriptor for notifications
        val descriptor = BluetoothGattDescriptor(
            CLIENT_CHARACTERISTIC_CONFIG,
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
        )
        inboxCharacteristic.addDescriptor(descriptor)
        
        service.addCharacteristic(inboxCharacteristic)
        return service
    }
    
    /**
     * GATT server callback
     */
    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: android.bluetooth.BluetoothDevice, status: Int, newState: Int) {
            val deviceAddress = device.address
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Logger.i("Device connected: $deviceAddress", "GattServerManager")
                    _connectedDevices.value = _connectedDevices.value + deviceAddress
                    
                    // Request MTU if available
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        gattServer?.requestMtu(device, BleConstants.MAX_MTU_SIZE)
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Logger.i("Device disconnected: $deviceAddress", "GattServerManager")
                    _connectedDevices.value = _connectedDevices.value - deviceAddress
                }
            }
        }
        
        override fun onCharacteristicWriteRequest(
            device: android.bluetooth.BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            val deviceAddress = device.address
            
            if (characteristic.uuid == BleConstants.INBOX_CHARACTERISTIC_UUID) {
                Logger.i("Received data from $deviceAddress: ${value.size} bytes", "GattServerManager")
                
                // Create inbound frame
                val frame = InboundFrame(
                    sourceAddress = deviceAddress,
                    data = value
                )
                
                // Add to inbound frames
                _inboundFrames.value = _inboundFrames.value + frame
                
                // Send response if needed
                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                }
                
                Logger.d("Data frame added: ${frame.getDataAsString()}", "GattServerManager")
            } else {
                // Unknown characteristic
                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
                }
            }
        }
        
        override fun onMtuChanged(device: android.bluetooth.BluetoothDevice, mtu: Int) {
            Logger.i("MTU changed for ${device.address}: $mtu", "GattServerManager")
        }
        
        override fun onServiceAdded(status: Int, service: BluetoothGattService) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Logger.i("Mesh service added successfully", "GattServerManager")
            } else {
                Logger.e("Failed to add mesh service: $status", null, "GattServerManager")
            }
        }
    }
    
    /**
     * Check if server is currently running
     */
    fun isServerCurrentlyRunning(): Boolean = isServerRunning
    
    /**
     * Get the latest inbound frame
     */
    fun getLatestInboundFrame(): InboundFrame? {
        return _inboundFrames.value.lastOrNull()
    }
    
    /**
     * Clear all inbound frames
     */
    fun clearInboundFrames() {
        _inboundFrames.value = emptyList()
    }
    
    /**
     * Get a flow of new inbound frames
     */
    fun getInboundFlow(): StateFlow<List<InboundFrame>> = inboundFrames
}
