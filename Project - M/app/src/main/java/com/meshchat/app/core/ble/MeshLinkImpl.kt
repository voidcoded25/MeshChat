package com.meshchat.app.core.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import com.meshchat.app.core.Logger
import com.meshchat.app.core.data.InboundFrame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Implementation of MeshLink using GattClientManager.
 * Provides a connection to a peer device with data transfer capabilities.
 */
class MeshLinkImpl(
    private val peerDevice: BluetoothDevice,
    private val gattClientManager: GattClientManager
) : MeshLink {
    
    override val peerAddress: String = peerDevice.address
    
    override val isConnected: Boolean
        get() = gattClientManager.isConnected()
    
    override val mtuSize: Int
        get() = gattClientManager.mtuSize.value
    
    override val inboundFlow: Flow<InboundFrame> = gattClientManager.inboundFrames
    
    override val connectionStatus: String
        get() = gattClientManager.getConnectionStatus()
    
    /**
     * Connect to the peer device
     */
    suspend fun connect(): Boolean {
        Logger.i("Connecting to peer: $peerAddress", "MeshLinkImpl")
        return gattClientManager.connectToDevice(peerDevice)
    }
    
    override suspend fun send(frame: ByteArray): Boolean {
        if (!isConnected) {
            Logger.w("Cannot send data: not connected to peer", "MeshLinkImpl")
            return false
        }
        
        Logger.i("Sending ${frame.size} bytes to peer: $peerAddress", "MeshLinkImpl")
        return gattClientManager.sendData(frame)
    }
    
    override suspend fun close() {
        Logger.i("Closing connection to peer: $peerAddress", "MeshLinkImpl")
        gattClientManager.disconnect()
    }
    
    /**
     * Get connection state flow
     */
    fun getConnectionStateFlow(): StateFlow<Int> = gattClientManager.connectionState
    
    /**
     * Get MTU size flow
     */
    fun getMtuSizeFlow(): StateFlow<Int> = gattClientManager.mtuSize
    
    /**
     * Get error message flow
     */
    fun getErrorMessageFlow(): StateFlow<String?> = gattClientManager.errorMessage
    
    /**
     * Clear any error messages
     */
    fun clearError() {
        gattClientManager.clearError()
    }
    
    /**
     * Check if connection is in a specific state
     */
    fun isInState(state: Int): Boolean {
        return gattClientManager.connectionState.value == state
    }
    
    /**
     * Check if connection is established
     */
    fun isConnectionEstablished(): Boolean {
        return gattClientManager.connectionState.value == BluetoothProfile.STATE_CONNECTED
    }
    
    /**
     * Check if connection is in progress
     */
    fun isConnecting(): Boolean {
        return gattClientManager.connectionState.value == BluetoothProfile.STATE_CONNECTING
    }
    
    /**
     * Check if connection is disconnecting
     */
    fun isDisconnecting(): Boolean {
        return gattClientManager.connectionState.value == BluetoothProfile.STATE_DISCONNECTING
    }
}
