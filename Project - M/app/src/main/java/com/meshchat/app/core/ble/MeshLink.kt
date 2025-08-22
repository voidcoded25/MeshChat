package com.meshchat.app.core.ble

import com.meshchat.app.core.data.InboundFrame
import kotlinx.coroutines.flow.Flow

/**
 * Represents an active data link to a peer device in the mesh network.
 * Provides methods for sending data and receiving incoming frames.
 */
interface MeshLink {
    
    /**
     * The address of the connected peer device
     */
    val peerAddress: String
    
    /**
     * Check if the link is currently connected
     */
    val isConnected: Boolean
    
    /**
     * Get the current MTU size for this connection
     */
    val mtuSize: Int
    
    /**
     * Send data to the peer device
     * @param frame The data frame to send
     * @return true if the data was queued for transmission, false otherwise
     */
    suspend fun send(frame: ByteArray): Boolean
    
    /**
     * Get a flow of incoming data frames from this peer
     */
    val inboundFlow: Flow<InboundFrame>
    
    /**
     * Close the connection and clean up resources
     */
    suspend fun close()
    
    /**
     * Get connection status information
     */
    val connectionStatus: String
}
