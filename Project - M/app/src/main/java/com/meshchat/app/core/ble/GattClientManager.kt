package com.meshchat.app.core.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
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
 * Manages GATT client connections to peer devices.
 * Handles connecting, discovering services, and data transfer.
 */
class GattClientManager(private val context: Context) {
    
    private var gatt: BluetoothGatt? = null
    private var inboxCharacteristic: BluetoothGattCharacteristic? = null
    
    private val _connectionState = MutableStateFlow(BluetoothProfile.STATE_DISCONNECTED)
    val connectionState: StateFlow<Int> = _connectionState.asStateFlow()
    
    private val _mtuSize = MutableStateFlow(23) // Default MTU
    val mtuSize: StateFlow<Int> = _mtuSize.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _inboundFrames = MutableStateFlow<List<InboundFrame>>(emptyList())
    val inboundFrames: StateFlow<List<InboundFrame>> = _inboundFrames.asStateFlow()
    
    companion object {
        private val CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
    
    /**
     * Connect to a peer device
     */
    fun connectToDevice(device: BluetoothDevice): Boolean {
        try {
            Logger.i("Connecting to device: ${device.address}", "GattClientManager")
            
            gatt = device.connectGatt(context, false, gattCallback)
            return true
            
        } catch (e: Exception) {
            Logger.e("Failed to connect to device", e, "GattClientManager")
            _errorMessage.value = "Connection failed: ${e.message}"
            return false
        }
    }
    
    /**
     * Disconnect from the current device
     */
    fun disconnect() {
        try {
            gatt?.disconnect()
            gatt?.close()
            gatt = null
            inboxCharacteristic = null
            _connectionState.value = BluetoothProfile.STATE_DISCONNECTED
            Logger.i("Disconnected from device", "GattClientManager")
            
        } catch (e: Exception) {
            Logger.e("Failed to disconnect", e, "GattClientManager")
        }
    }
    
    /**
     * Send data to the connected peer
     */
    fun sendData(data: ByteArray): Boolean {
        val characteristic = inboxCharacteristic
        if (characteristic == null) {
            Logger.w("No inbox characteristic available", "GattClientManager")
            return false
        }
        
        if (_connectionState.value != BluetoothProfile.STATE_CONNECTED) {
            Logger.w("Not connected to peer", "GattClientManager")
            return false
        }
        
        try {
            characteristic.value = data
            val success = gatt?.writeCharacteristic(characteristic) ?: false
            
            if (success) {
                Logger.i("Data sent successfully: ${data.size} bytes", "GattClientManager")
            } else {
                Logger.w("Failed to send data", "GattClientManager")
            }
            
            return success
            
        } catch (e: Exception) {
            Logger.e("Failed to send data", e, "GattClientManager")
            return false
        }
    }
    
    /**
     * GATT callback
     */
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address
            
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Logger.i("Connected to device: $deviceAddress", "GattClientManager")
                    _connectionState.value = newState
                    _errorMessage.value = null
                    
                    // Start service discovery
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Logger.i("Disconnected from device: $deviceAddress", "GattClientManager")
                    _connectionState.value = newState
                    inboxCharacteristic = null
                }
            }
            
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Logger.e("Connection state change failed: $status", null, "GattClientManager")
                _errorMessage.value = "Connection failed with status: $status"
            }
        }
        
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Logger.i("Services discovered", "GattClientManager")
                
                // Find the mesh service and INBOX characteristic
                val meshService = gatt.getService(BleConstants.SERVICE_UUID)
                if (meshService != null) {
                    inboxCharacteristic = meshService.getCharacteristic(BleConstants.INBOX_CHARACTERISTIC_UUID)
                    
                    if (inboxCharacteristic != null) {
                        Logger.i("INBOX characteristic found", "GattClientManager")
                        
                        // Enable notifications
                        enableNotifications(gatt, inboxCharacteristic!!)
                        
                        // Request MTU if available
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            gatt.requestMtu(BleConstants.MAX_MTU_SIZE)
                        }
                    } else {
                        Logger.e("INBOX characteristic not found", null, "GattClientManager")
                        _errorMessage.value = "INBOX characteristic not found"
                    }
                } else {
                    Logger.e("Mesh service not found", null, "GattClientManager")
                    _errorMessage.value = "Mesh service not found"
                }
            } else {
                Logger.e("Service discovery failed: $status", null, "GattClientManager")
                _errorMessage.value = "Service discovery failed: $status"
            }
        }
        
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (characteristic.uuid == BleConstants.INBOX_CHARACTERISTIC_UUID) {
                Logger.i("Received data: ${value.size} bytes", "GattClientManager")
                
                // Create inbound frame
                val frame = InboundFrame(
                    sourceAddress = gatt.device.address,
                    data = value
                )
                
                // Add to inbound frames
                _inboundFrames.value = _inboundFrames.value + frame
                
                Logger.d("Data frame added: ${frame.getDataAsString()}", "GattClientManager")
            }
        }
        
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Logger.i("MTU changed to: $mtu", "GattClientManager")
                _mtuSize.value = mtu
            } else {
                Logger.w("MTU change failed: $status", "GattClientManager")
            }
        }
        
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Logger.d("Characteristic write successful", "GattClientManager")
            } else {
                Logger.w("Characteristic write failed: $status", "GattClientManager")
            }
        }
    }
    
    /**
     * Enable notifications for the INBOX characteristic
     */
    private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        try {
            // Enable local notifications
            gatt.setCharacteristicNotification(characteristic, true)
            
            // Write to client characteristic configuration descriptor
            val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)
            if (descriptor != null) {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
                Logger.i("Notifications enabled", "GattClientManager")
            } else {
                Logger.w("Client characteristic config descriptor not found", "GattClientManager")
            }
            
        } catch (e: Exception) {
            Logger.e("Failed to enable notifications", e, "GattClientManager")
        }
    }
    
    /**
     * Check if currently connected
     */
    fun isConnected(): Boolean = _connectionState.value == BluetoothProfile.STATE_CONNECTED
    
    /**
     * Get connection status string
     */
    fun getConnectionStatus(): String {
        return when (_connectionState.value) {
            BluetoothProfile.STATE_DISCONNECTED -> "Disconnected"
            BluetoothProfile.STATE_CONNECTING -> "Connecting"
            BluetoothProfile.STATE_CONNECTED -> "Connected"
            BluetoothProfile.STATE_DISCONNECTING -> "Disconnecting"
            else -> "Unknown"
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * Clear all inbound frames
     */
    fun clearInboundFrames() {
        _inboundFrames.value = emptyList()
    }
}
